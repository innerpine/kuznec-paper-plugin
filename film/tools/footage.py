"""Prepares a desk clip for the film.

  python3 -I footage.py clip.mp4 --start 2.0 --dur 3.5 --out ../assets/footage/desk.webm

- trims and re-encodes all-intra (every frame a keyframe) as VP9 WebM, so seeking is exact;
- finds the phone screen on the first frame (largest dark region → rotated rectangle by PCA)
  and writes <out>.quad.json next to the clip ({vw, vh, cx, cy, w, h, angle, r}).
Check the overlay image it writes; pass --quad to override the detection by hand.
"""
import argparse, json, os, subprocess
import numpy as np
from PIL import Image, ImageDraw


def frame(path, t, w, h):
    raw = subprocess.run(['ffmpeg', '-v', 'error', '-ss', str(t), '-i', path, '-frames:v', '1', '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-'],
                         capture_output=True, check=True).stdout
    return np.frombuffer(raw, np.uint8).reshape(h, w, 3)


def largest_component(mask):
    h, w = mask.shape
    lab = np.zeros((h, w), np.int32)
    best, best_n, cur = 0, 0, 0
    for y0, x0 in zip(*np.nonzero(mask)):
        if lab[y0, x0]:
            continue
        cur += 1
        stack, n = [(y0, x0)], 0
        lab[y0, x0] = cur
        while stack:
            y, x = stack.pop(); n += 1
            for yy, xx in ((y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)):
                if 0 <= yy < h and 0 <= xx < w and mask[yy, xx] and not lab[yy, xx]:
                    lab[yy, xx] = cur; stack.append((yy, xx))
        if n > best_n:
            best, best_n = cur, n
    return lab == best


def detect(img, thresh=48, scale=0.25):
    small = Image.fromarray(img).resize((int(img.shape[1] * scale), int(img.shape[0] * scale)))
    a = np.asarray(small).astype(np.float32)
    luma = 0.2126 * a[..., 0] + 0.7152 * a[..., 1] + 0.0722 * a[..., 2]
    comp = largest_component(luma < thresh)
    ys, xs = np.nonzero(comp)
    pts = np.stack([xs, ys], 1).astype(np.float64) / scale
    c = pts.mean(0)
    cov = np.cov((pts - c).T)
    evals, evecs = np.linalg.eigh(cov)
    major = evecs[:, 1]                    # long axis = screen height
    ang_h = np.arctan2(major[1], major[0])
    # width axis angle (screen's x axis), kept within ±45°
    ang = ang_h - np.pi / 2
    while ang > np.pi / 4: ang -= np.pi / 2
    while ang < -np.pi / 4: ang += np.pi / 2
    R = np.array([[np.cos(ang), np.sin(ang)], [-np.sin(ang), np.cos(ang)]])
    q = (pts - c) @ R.T
    lo, hi = np.percentile(q, 0.5, 0), np.percentile(q, 99.5, 0)
    w, h = hi - lo
    mid = (lo + hi) / 2
    c = c + mid @ R
    return {'cx': float(c[0]), 'cy': float(c[1]), 'w': float(w), 'h': float(h), 'angle': float(ang), 'r': float(0.12 * w)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('clip'); ap.add_argument('--start', type=float, default=0); ap.add_argument('--dur', type=float, default=3.5)
    ap.add_argument('--out', required=True); ap.add_argument('--height', type=int, default=2160)
    ap.add_argument('--quad', help='JSON override for the detected quad')
    a = ap.parse_args()
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-ss', str(a.start), '-t', str(a.dur), '-i', a.clip, '-an',
                    '-vf', f"scale=-2:'min({a.height},ih)'", '-c:v', 'libvpx-vp9', '-g', '1', '-keyint_min', '1', '-lag-in-frames', '0',
                    '-auto-alt-ref', '0', '-crf', '20', '-b:v', '0', '-row-mt', '1', '-deadline', 'good', '-cpu-used', '2', '-pix_fmt', 'yuv420p', a.out], check=True)
    info = json.loads(subprocess.run(['ffprobe', '-v', 'error', '-select_streams', 'v', '-show_entries', 'stream=width,height', '-of', 'json', a.out],
                                     capture_output=True, check=True).stdout)['streams'][0]
    vw, vh = info['width'], info['height']
    img = frame(a.out, 0, vw, vh)
    q = json.loads(a.quad) if a.quad else detect(img)
    q.update({'vw': vw, 'vh': vh, 'size': min(vw, vh)})
    base = os.path.splitext(a.out)[0]
    json.dump(q, open(base + '.quad.json', 'w'), indent=1)
    # overlay for checking the detection
    im = Image.fromarray(img).convert('RGB'); d = ImageDraw.Draw(im)
    ca, sa = np.cos(q['angle']), np.sin(q['angle'])
    pts = [(q['cx'] + ca * x * q['w'] / 2 - sa * y * q['h'] / 2, q['cy'] + sa * x * q['w'] / 2 + ca * y * q['h'] / 2) for x, y in ((-1, -1), (1, -1), (1, 1), (-1, 1))]
    d.polygon(pts, outline=(255, 0, 80), width=6)
    im.thumbnail((1200, 1200)); im.save(base + '.quad.jpg', quality=85)
    print(json.dumps(q, indent=1))


if __name__ == '__main__':
    main()
