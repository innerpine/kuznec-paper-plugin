"""QC for a rendered film.

  python3 -I qc.py film.mp4 outdir

1. One frame per beat (mid-beat) → beats-*.jpg contact sheets.
2. Pop hunt: mean absolute difference between consecutive frames; a frame whose difference
   is more than 3× its neighbours' (and above the noise floor) is reported as a pop.
"""
import json, os, subprocess, sys
import numpy as np
from PIL import Image, ImageDraw

src, outdir = sys.argv[1], sys.argv[2]
os.makedirs(outdir, exist_ok=True)
BPM, BEATS, FPS = 120, 54, 60
beat = 60 / BPM

# 1) a frame per beat
frames = []
for n in range(1, BEATS + 1):
    t = (n - 1) * beat + beat / 2
    raw = subprocess.run(['ffmpeg', '-v', 'error', '-ss', f'{t:.4f}', '-i', src, '-frames:v', '1', '-vf', 'scale=320:320', '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-'],
                         capture_output=True, check=True).stdout
    frames.append((n, t, Image.frombytes('RGB', (320, 320), raw)))
cols = 9
for part in range(0, BEATS, 27):
    chunk = frames[part:part + 27]
    rows = (len(chunk) + cols - 1) // cols
    sheet = Image.new('RGB', (cols * 328 + 8, rows * 348 + 8), '#1e1e1e')
    d = ImageDraw.Draw(sheet)
    for k, (n, t, im) in enumerate(chunk):
        x, y = 8 + (k % cols) * 328, 8 + (k // cols) * 348
        sheet.paste(im, (x, y))
        d.text((x + 4, y + 324), f'beat {n}  t={t:.2f}', fill='#ddd')
    sheet.save(os.path.join(outdir, f'beats-{part // 27 + 1}.jpg'), quality=88)

# 2) pop hunt on a small grey proxy
S = 240
p = subprocess.run(['ffmpeg', '-v', 'error', '-i', src, '-vf', f'scale={S}:{S}', '-f', 'rawvideo', '-pix_fmt', 'gray', '-'], capture_output=True, check=True).stdout
v = np.frombuffer(p, dtype=np.uint8).reshape(-1, S, S).astype(np.float32)
d = np.abs(np.diff(v, axis=0)).mean(axis=(1, 2))          # d[i] = change from frame i to i+1
pops = []
for i in range(2, len(d) - 2):
    nb = np.array([d[i - 2], d[i - 1], d[i + 1], d[i + 2]])
    ref = max(np.median(nb), 0.15)
    if d[i] > 3 * ref and d[i] > 0.8:
        pops.append({'frame': i + 1, 't': round((i + 1) / FPS, 4), 'diff': round(float(d[i]), 3), 'neighbours': [round(float(x), 3) for x in nb]})
json.dump({'frames': int(len(v)), 'pops': pops, 'diff': [round(float(x), 3) for x in d]}, open(os.path.join(outdir, 'pops.json'), 'w'))
# first vs last frame (loop)
print(f'frames: {len(v)}  first-vs-last mean abs diff: {np.abs(v[0] - v[-1]).mean():.4f}')
print(f'pop candidates: {len(pops)}')
for q in pops:
    print(f"  frame {q['frame']:5d}  t={q['t']:7.3f}  diff {q['diff']:6.2f}  neighbours {q['neighbours']}")
