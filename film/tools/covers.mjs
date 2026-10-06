// Renders the upgrade covers and the avatar: node covers.mjs <sprites.json> <outdir> [size] [only]
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import http from 'node:http';

const [spritesPath, outDir, sizeArg, only] = process.argv.slice(2);
const size = Number(sizeArg || 2048);
const sprites = JSON.parse(fs.readFileSync(spritesPath, 'utf8'));
const deg = (d) => (d * Math.PI) / 180;

const COVERS = {
  vampirism: { bg: ['#f0525f', '#6d0716'], rot: [deg(8), deg(-24), deg(-4)], seed: 3, rim: '#ffb3bd',
    particles: [{ n: 9, colors: ['#e3102f', '#b00a22', '#ff3b50'], x: [-9, 7], y: [-12, -2], z: [-2, 6], s: [0.7, 1.15], sy: 1.5, glow: 0.2 }] },
  flame: { bg: ['#ffc15a', '#c9370b'], rot: [deg(10), deg(22), deg(3)], seed: 5, rim: '#fff0a0', light: 'rgba(255,250,210,0.45)',
    particles: [{ n: 16, colors: ['#ffd54a', '#ff8a1a', '#ff4d12'], x: [-6, 12], y: [-4, 13], z: [-2, 7], s: [0.5, 1.2], glow: 0.6 }] },
  poison: { bg: ['#c5f56a', '#155c33'], rot: [deg(6), deg(-18), deg(6)], seed: 11, rim: '#e8ffb0',
    particles: [{ n: 10, colors: ['#7cff4f', '#3fd13a', '#b6ff8a'], x: [-6, 9], y: [-12, 0], z: [-1, 6], s: [0.55, 1.1], sy: 1.4, glow: 0.45 }] },
  nighteye: { bg: ['#3e57c9', '#0a0f33'], rot: [deg(12), deg(-26), deg(0)], seed: 13, rim: '#9ff5ff', light: 'rgba(160,230,255,0.22)', glow: 1.4, shadow: 0.38,
    particles: [{ n: 26, colors: ['#ffffff', '#cfe9ff', '#9ff5ff'], x: [-22, 22], y: [-22, 22], z: [-8, -4], s: [0.25, 0.5], glow: 1.2 }] },
  shield: { bg: ['#b09bff', '#35189a'], rot: [deg(10), deg(28), deg(-3)], seed: 17, rim: '#e6dcff', glow: 0.9,
    particles: [{ n: 10, colors: ['#ffffff', '#c9b8ff', '#7a5cff'], x: [-14, 14], y: [-13, 13], z: [-3, 7], s: [0.6, 1.2], glow: 0.5 }] },
  laststand: { bg: ['#fff1c9', '#e2a23a'], rot: [deg(9), deg(-22), deg(2)], seed: 19, rim: '#fff6d8', shadow: 0.24,
    particles: [{ n: 8, colors: ['#e2263f', '#ff5468', '#ffcf3d'], x: [-14, 14], y: [-12, 12], z: [-2, 7], s: [0.6, 1.1], glow: 0.25 }] },
  wind: { bg: ['#7ff0df', '#08736b'], rot: [deg(8), deg(24), deg(-2)], seed: 23, rim: '#e6fbf8',
    particles: [{ n: 7, colors: ['#ffffff', '#e6fbf8'], x: [-15, 13], y: [-12, 12], z: [0, 7], s: [0.45, 0.6], sx: 7, spin: false, glow: 0.35 }] },
  feather: { bg: ['#d4efff', '#3887d1'], rot: [deg(10), deg(-20), deg(0)], seed: 29, rim: '#ffffff', shadow: 0.22,
    particles: [{ n: 9, colors: ['#ffffff', '#cfe8ff'], x: [-14, 14], y: [-6, 14], z: [-2, 7], s: [0.5, 0.95], sx: 2.2, glow: 0.3 }] },
  rabbit: { bg: ['#ffd0e4', '#d94b88'], rot: [deg(8), deg(24), deg(4)], seed: 31, rim: '#fff0f6',
    particles: [{ n: 8, colors: ['#ffffff', '#ff8fc0', '#ffd0e4'], x: [-14, -4], y: [-10, 8], z: [-1, 6], s: [0.5, 1.0], glow: 0.3 }] },
  anvil: { bg: ['#3d424b', '#0f1114'], rot: [deg(14), deg(-26), deg(0)], seed: 37, rim: '#ffb36b', scale: 1.12, pos: [0, -0.5, 0], shadow: 0.4,
    particles: [{ n: 12, colors: ['#ffd54a', '#ff8a1a', '#ffffff'], x: [-6, 12], y: [5, 15], z: [0, 7], s: [0.35, 0.7], glow: 1.4 }] },
};

// Lock-screen wallpaper: portrait twin of the flame cover with extra embers above and below
// the centre square (placed outside it, so the square stays identical to the cover).
COVERS['flame-wall'] = { ...COVERS.flame, sprite: 'flame', portrait: 3120 / 1440,
  particles: [...COVERS.flame.particles,
    { n: 22, colors: ['#ffd54a', '#ff8a1a', '#ff4d12'], x: [-16, 16], y: [17, 34], z: [-2, 6], s: [0.5, 1.2], glow: 0.6, seed2: 1 },
    { n: 14, colors: ['#ffd54a', '#ff8a1a'], x: [-16, 16], y: [-34, -17], z: [-2, 6], s: [0.5, 1.1], glow: 0.6, seed2: 2 }] };

const root = path.resolve('.');
const srv = http.createServer((req, res) => {
  const f = path.join(root, decodeURIComponent(req.url.split('?')[0]));
  if (!f.startsWith(root) || !fs.existsSync(f)) { res.writeHead(404); return res.end(); }
  const ext = path.extname(f);
  res.writeHead(200, { 'content-type': ext === '.html' ? 'text/html' : ext === '.js' ? 'text/javascript' : 'application/octet-stream' });
  fs.createReadStream(f).pipe(res);
}).listen(0);
const port = srv.address().port;

const browser = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const page = await browser.newPage();
page.on('console', (m) => console.log('[page]', m.text()));
page.on('pageerror', (e) => console.log('[pageerror]', e.message));
await page.goto(`http://127.0.0.1:${port}/covers.html`);
await page.waitForFunction(() => window.ready === true);
fs.mkdirSync(outDir, { recursive: true });
for (const [name, cfg] of Object.entries(COVERS)) {
  if (only && !only.split(',').includes(name)) continue;
  const t0 = Date.now();
  const url = await page.evaluate(([c, s, n]) => window.renderCover(c, s, n), [cfg, sprites[cfg.sprite || name], size]);
  fs.writeFileSync(path.join(outDir, `${name}.jpg`), Buffer.from(url.split(',')[1], 'base64'));
  console.log(name, ((Date.now() - t0) / 1000).toFixed(1) + 's');
}
await browser.close();
srv.close();
