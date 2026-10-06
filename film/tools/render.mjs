// Renders the film: 60 fps, 4 subframes per frame blended with ffmpeg tmix, in parallel workers.
//   node render.mjs --out ../out/kuznec.mp4 --audio ../out/audio.wav [--workers 4] [--fps 60] [--sub 4] [--from 0 --to N]
// Frame f samples t = f/fps + k/(fps·sub), k = 0..sub-1; the last frame (t = 27 s) equals the first.
import { chromium } from 'playwright';
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { serve } from './serve.mjs';

const arg = (k, d) => { const i = process.argv.indexOf(`--${k}`); return i > 0 ? process.argv[i + 1] : d; };
const HERE = path.dirname(new URL(import.meta.url).pathname);
const out = path.resolve(arg('out', path.join(HERE, '../out/kuznec.mp4')));
const audio = arg('audio') && path.resolve(arg('audio'));
const fps = Number(arg('fps', 60)), sub = Number(arg('sub', 4)), workers = Number(arg('workers', Math.max(1, os.cpus().length)));
const work = path.join(path.dirname(out), '.render');
fs.mkdirSync(work, { recursive: true });

const { srv, url } = await serve(path.resolve(HERE, '..'));
const browser = await chromium.launch({ args: ['--autoplay-policy=no-user-gesture-required'] });
const probe = await browser.newPage();
await probe.goto(`${url}/film.html`);
await probe.waitForFunction(() => typeof window.seek === 'function');
const DUR = await probe.evaluate(() => window.DUR);
await probe.close();
const N = Math.round(DUR * fps) + 1;
const from = Number(arg('from', 0)), to = Number(arg('to', N));
const chunks = [];
for (let w = 0; w < workers; w++) {
  const a = from + Math.floor(((to - from) * w) / workers), b = from + Math.floor(((to - from) * (w + 1)) / workers);
  if (b > a) chunks.push([a, b]);
}
console.log(`frames ${from}..${to - 1} of ${N} (${DUR}s @ ${fps}fps × ${sub} subframes), ${chunks.length} workers`);
const t0 = Date.now();
let done = 0;

async function renderChunk([a, b], w) {
  const file = path.join(work, `chunk-${String(a).padStart(5, '0')}.mkv`);
  const ff = spawn('ffmpeg', ['-v', 'error', '-y', '-f', 'image2pipe', '-c:v', 'png', '-framerate', String(fps * sub), '-i', '-',
    '-vf', `tmix=frames=${sub}:weights='${Array(sub).fill(1).join(' ')}',select='eq(mod(n\\,${sub})\\,${sub - 1})',setpts=N/(${fps}*TB)`,
    '-r', String(fps), '-c:v', 'libx264', '-preset', 'medium', '-crf', '8', '-pix_fmt', 'yuv444p', file], { stdio: ['pipe', 'inherit', 'inherit'] });
  const closed = new Promise((res, rej) => ff.on('close', (c) => (c === 0 ? res() : rej(new Error(`ffmpeg exit ${c}`)))));
  const page = await browser.newPage({ viewport: { width: 1440, height: 1440 }, deviceScaleFactor: 1 });
  page.on('pageerror', (e) => console.log(`[w${w} pageerror]`, e.message));
  await page.goto(`${url}/film.html`);
  await page.waitForFunction(() => typeof window.seek === 'function');
  const cdp = await page.context().newCDPSession(page);
  for (let f = a; f < b; f++) {
    for (let k = 0; k < sub; k++) {
      const t = Math.min(DUR, f / fps + k / (fps * sub));
      await page.evaluate((tt) => window.seek(tt), t);
      const { data } = await cdp.send('Page.captureScreenshot', { format: 'png', clip: { x: 0, y: 0, width: 1440, height: 1440, scale: 1 }, optimizeForSpeed: true });
      if (!ff.stdin.write(Buffer.from(data, 'base64'))) await new Promise((r) => ff.stdin.once('drain', r));
    }
    done++;
    if (done % 60 === 0) {
      const el = (Date.now() - t0) / 1000;
      console.log(`${done}/${to - from} frames  ${el.toFixed(0)}s elapsed, ~${((el / done) * (to - from - done)).toFixed(0)}s left`);
    }
  }
  ff.stdin.end();
  await closed;
  await page.close();
  return file;
}

const files = await Promise.all(chunks.map((c, w) => renderChunk(c, w)));
await browser.close();
srv.close();

// join the chunks, encode the delivery file, add the sound
const list = path.join(work, 'list.txt');
fs.writeFileSync(list, files.map((f) => `file '${f}'`).join('\n'));
const args = ['-v', 'error', '-y', '-f', 'concat', '-safe', '0', '-i', list];
if (audio) args.push('-i', audio);
args.push('-map', '0:v');
if (audio) args.push('-map', '1:a', '-c:a', 'aac', '-b:a', '256k', '-shortest');
args.push('-c:v', 'libx264', '-preset', 'slow', '-crf', '15', '-pix_fmt', 'yuv420p', '-profile:v', 'high', '-r', String(fps), '-movflags', '+faststart', out);
await new Promise((res, rej) => spawn('ffmpeg', args, { stdio: 'inherit' }).on('close', (c) => (c === 0 ? res() : rej(new Error(`ffmpeg ${c}`)))));
console.log(`wrote ${out} in ${((Date.now() - t0) / 1000).toFixed(0)}s`);
