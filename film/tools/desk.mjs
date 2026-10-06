// node desk.mjs <outdir> <seconds> <fps> — renders the stand-in desk clip as PNG frames + quad.json
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import { serve } from './serve.mjs';
const [outDir, secs = '3', fps = '30'] = process.argv.slice(2);
fs.mkdirSync(outDir, { recursive: true });
const { srv, url } = await serve(path.dirname(new URL(import.meta.url).pathname));
const browser = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const page = await browser.newPage();
page.on('pageerror', (e) => console.log('[pageerror]', e.message));
await page.goto(`${url}/desk.html`);
await page.waitForFunction(() => window.ready === true);
fs.writeFileSync(path.join(outDir, 'quad.json'), JSON.stringify(await page.evaluate(() => window.screenQuad()), null, 1));
const n = Math.round(Number(secs) * Number(fps));
for (let i = 0; i < n; i++) {
  const d = await page.evaluate((t) => window.frame(t), i / Number(fps));
  fs.writeFileSync(path.join(outDir, `f${String(i).padStart(4, '0')}.png`), Buffer.from(d.split(',')[1], 'base64'));
  if (i % 15 === 0) console.log('frame', i, '/', n);
}
await browser.close(); srv.close();
