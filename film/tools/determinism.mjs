// Renders each time twice — once cold (fresh page) and once after a different time — and
// compares pixels. Any difference means some state leaks between frames.
import { chromium } from 'playwright';
import path from 'node:path';
import { serve } from './serve.mjs';
const times = process.argv.slice(2).map(Number);
const { srv, url } = await serve(path.resolve(path.dirname(new URL(import.meta.url).pathname), '..'));
const browser = await chromium.launch();
const grab = async (page, t) => { await page.evaluate((tt) => window.seek(tt), t); return page.locator('#stage').screenshot(); };
const mk = async () => { const p = await browser.newPage({ viewport: { width: 1440, height: 1440 } }); await p.goto(`${url}/film.html`); await p.waitForFunction(() => typeof window.seek === 'function'); return p; };
const { PNG } = await import('./png.mjs');
for (const t of times) {
  const a = await mk(); const A = await grab(a, t); await a.close();
  const b = await mk(); await grab(b, t - 0.05); await grab(b, t - 0.0042); const Bimg = await grab(b, t); await b.close();
  const da = PNG(A), db = PNG(Bimg);
  let diff = 0, max = 0, x0 = 1e9, y0 = 1e9, x1 = -1, y1 = -1;
  for (let i = 0; i < da.data.length; i++) {
    const d = Math.abs(da.data[i] - db.data[i]); diff += d; if (d > max) max = d;
    if (d > 8) { const px = Math.floor(i / da.bpp); const x = px % da.width, y = Math.floor(px / da.width); x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y); }
  }
  console.log(`t=${t}  mean abs diff ${(diff / da.data.length).toFixed(4)}  max ${max}  bbox(d>8) ${x1 < 0 ? '-' : `${x0},${y0}–${x1},${y1}`}`);
}
await browser.close(); srv.close();
