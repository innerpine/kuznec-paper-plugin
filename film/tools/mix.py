"""Sound for the film.

Every event in the beat map gets its downloaded SFX, placed so the file's measured peak lands
on the event time. An optional music track is placed so its drop lands on the drop beat
(start on a downbeat), and can be spliced so its breakdown sits on the desk and the beat
returns with the wordmark. The mix is loudness-normalised to -14 LUFS.

  python3 -I mix.py out.wav [--music track.mp3 --drop 32.0 [--breakdown 64.0 --back 72.0]]
"""
import argparse, json, os, re, subprocess, sys, tempfile
import numpy as np

SR = 48000
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..')


def decode(path):
    raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', path, '-f', 'f32le', '-ac', '2', '-ar', str(SR), '-'],
                         check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype=np.float32).reshape(-1, 2).copy()


def peak_time(x):
    """Time of the loudest moment, from a 2 ms envelope (robust to single-sample spikes)."""
    env = np.abs(x).max(axis=1)
    k = max(1, int(0.002 * SR))
    env = np.convolve(env, np.ones(k) / k, mode='same')
    return int(np.argmax(env)) / SR


def place(buf, clip, t0, gain_db=0.0):
    g = 10 ** (gain_db / 20)
    i0 = int(round(t0 * SR))
    a, b = max(0, i0), min(len(buf), i0 + len(clip))
    if b > a:
        buf[a:b] += clip[a - i0:b - i0] * g


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('out')
    ap.add_argument('--music')
    ap.add_argument('--drop', type=float, help='time of the drop inside the track (s)')
    ap.add_argument('--breakdown', type=float, help='time the quiet breakdown starts inside the track (s)')
    ap.add_argument('--back', type=float, help='time the beat comes back inside the track (s)')
    ap.add_argument('--music-gain', type=float, default=-3.0)
    ap.add_argument('--report', default=os.path.join(ROOT, 'out', 'sfx-placement.json'))
    a = ap.parse_args()

    src = open(os.path.join(ROOT, 'film.html'), encoding='utf-8').read()
    bm = json.loads(re.search(r'<script type="application/json" id="beatmap">(.*?)</script>', src, re.S).group(1))
    beat = 60 / bm['bpm']
    dur = bm['beats'] * beat
    B = lambda n: (n - 1) * beat
    buf = np.zeros((int(round((dur + 1 / 60) * SR)), 2), dtype=np.float64)

    # music: drop on the drop beat; optional splice so breakdown/return land on their beats
    if a.music:
        m = decode(a.music)
        drop_at = B(bm['music']['drop'])
        start = a.drop - drop_at                      # track time that plays at film t = 0
        if a.breakdown is not None and a.back is not None:
            bk0, back_at = B(bm['music']['breakdown'][0]), B(bm['music']['return'])
            # 1) from the start to the breakdown beat
            seg1 = m[int(start * SR):int((start + bk0) * SR)]
            # 2) breakdown material, as long as the film's breakdown, ending right where the beat returns
            seg2 = m[int((a.back - (back_at - bk0)) * SR):int(a.back * SR)]
            # 3) the return
            seg3 = m[int(a.back * SR):int((a.back + dur - back_at + 1) * SR)]
            fade = int(0.012 * SR)
            def xjoin(p, q):
                if len(p) < fade or len(q) < fade:
                    return np.concatenate([p, q])
                r = np.linspace(0, 1, fade)[:, None]
                return np.concatenate([p[:-fade], p[-fade:] * (1 - r) + q[:fade] * r, q[fade:]])
            music = xjoin(xjoin(seg1, seg2), seg3)
        else:
            music = m[max(0, int(start * SR)):]
            if start < 0:
                music = np.concatenate([np.zeros((int(-start * SR), 2)), m])
        place(buf, music[:len(buf)], 0.0, a.music_gain)

    report = []
    cache = {}
    for e in bm['map']:
        for name, t, gain in e['sfx']:
            path = os.path.join(ROOT, 'assets', 'sfx', f'{name}.mp3')
            if name not in cache:
                clip = decode(path)
                cache[name] = (clip, peak_time(clip))
            clip, pk = cache[name]
            place(buf, clip, t - pk, gain)
            report.append({'beat': e['n'], 'sfx': name, 'event': round(t, 4), 'peak_in_file': round(pk, 4), 'starts': round(t - pk, 4), 'gain_db': gain})
    os.makedirs(os.path.dirname(a.report), exist_ok=True)
    json.dump(report, open(a.report, 'w'), indent=1)

    # short fades at both ends so the loop point is clean
    f = int(0.004 * SR)
    buf[:f] *= np.linspace(0, 1, f)[:, None]
    buf[-f:] *= np.linspace(1, 0, f)[:, None]
    with tempfile.TemporaryDirectory() as td:
        raw = os.path.join(td, 'mix.f32')
        buf.astype(np.float32).tofile(raw)
        common = ['ffmpeg', '-v', 'error', '-y', '-f', 'f32le', '-ar', str(SR), '-ac', '2', '-i', raw]
        # two-pass loudnorm to -14 LUFS integrated
        probe = ['ffmpeg', '-hide_banner', '-nostats', '-v', 'info'] + common[3:]
        r = subprocess.run(probe + ['-af', 'loudnorm=I=-14:TP=-1.0:LRA=11:print_format=json', '-f', 'null', '-'], capture_output=True, text=True)
        js = json.loads(r.stderr[r.stderr.rindex('{'):])
        af = (f"loudnorm=I=-14:TP=-1.0:LRA=11:measured_I={js['input_i']}:measured_TP={js['input_tp']}:"
              f"measured_LRA={js['input_lra']}:measured_thresh={js['input_thresh']}:offset={js['target_offset']}:linear=true")
        subprocess.run(common + ['-af', af, '-ar', str(SR), '-c:a', 'pcm_s24le', a.out], check=True)
    r = subprocess.run(['ffmpeg', '-v', 'info', '-i', a.out, '-af', 'ebur128=framelog=quiet', '-f', 'null', '-'], capture_output=True, text=True)
    m = re.findall(r'I:\s+(-?[\d.]+) LUFS', r.stderr)
    print(f'{a.out}: {len(report)} SFX placed, integrated loudness {m[-1] if m else "?"} LUFS')


if __name__ == '__main__':
    main()
