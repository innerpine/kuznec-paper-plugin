// node stills.mjs <outdir> <t1> [t2 ...]   — renders frames of film.html at the given times
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import { serve } from './serve.mjs';

const [outDir, ...times] = process.argv.slice(2);
fs.mkdirSync(outDir, { recursive: true });
const { srv, url } = await serve(path.resolve(path.dirname(new URL(import.meta.url).pathname), '..'));
const browser = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist', '--autoplay-policy=no-user-gesture-required'] });
const page = await browser.newPage({ viewport: { width: 1440, height: 1440 }, deviceScaleFactor: 1 });
page.on('console', (m) => { if (m.type() === 'error' || m.type() === 'warning') console.log('[page]', m.text()); });
page.on('pageerror', (e) => console.log('[pageerror]', e.message));
await page.goto(`${url}/film.html`);
await page.waitForFunction(() => typeof window.seek === 'function');
const stage = page.locator('#stage');
for (const t of times) {
  const t0 = Date.now();
  await page.evaluate((tt) => window.seek(tt), Number(t));
  const file = path.join(outDir, `t${Number(t).toFixed(3).padStart(7, '0')}.png`);
  await stage.screenshot({ path: file });
  console.log(file, `${Date.now() - t0}ms`);
}
await browser.close();
srv.close();
