"""Builds film/out/beatmap.html from the beat map embedded in film.html."""
import json, re, html, sys, os
root = os.path.join(os.path.dirname(__file__), '..')
src = open(os.path.join(root, 'film.html'), encoding='utf-8').read()
bm = json.loads(re.search(r'<script type="application/json" id="beatmap">(.*?)</script>', src, re.S).group(1))
BEAT = 60 / bm['bpm']
sec_of = {}
for name, a, b in bm['sections']:
    for n in range(a, b + 1): sec_of[n] = name
COL = {'Open': '#0B0B0C', 'Glass': '#5B7FA6', 'Stage': '#9A6B3C', 'Order': '#0B0B0C', 'Desk': '#6B6B70', 'Return': '#0B0B0C'}
mus = bm['music']
rows = []
for e in bm['map']:
    n = e['n']; t = (n - 1) * BEAT
    tag = ''
    if n == mus['drop']: tag = '<span class="tag drop">дроп</span>'
    if mus['breakdown'][0] <= n <= mus['breakdown'][1]: tag = '<span class="tag brk">брейк</span>'
    if n == mus['return']: tag = '<span class="tag ret">бит вернулся</span>'
    sfx = ' '.join(f'<code>{html.escape(f)}</code>' for f, _, _ in e['sfx']) or '<span class="mute">—</span>'
    bar = (n - 1) // 4 + 1
    rows.append(f'<tr class="{"bar" if (n - 1) % 4 == 0 else ""}"><td class="n">{n}</td><td class="t">{t:05.2f}</td><td class="b">{bar}.{(n - 1) % 4 + 1}</td>'
                f'<td><span class="sec" style="--c:{COL[sec_of[n]]}">{sec_of[n]}</span></td><td class="ev">{html.escape(e["ev"])} {tag}</td><td class="sfx">{sfx}</td></tr>')
# timeline strip
strip = ''.join(f'<div class="cell" style="--c:{COL[sec_of[n]]}" title="{n}"></div>' for n in range(1, bm['beats'] + 1))
page = f'''<!doctype html><html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Kuznec — карта битов</title>
<style>
:root {{ --paper:#F3EFE7; --ink:#0B0B0C; --mute:#8b877f; --line:#E1DBD0; }}
@media (prefers-color-scheme: dark) {{ :root:not([data-theme="light"]) {{ --paper:#141414; --ink:#F3EFE7; --mute:#8e8a83; --line:#2a2a2a; }} }}
body {{ margin:0; background:var(--paper); color:var(--ink); font:15px/1.45 'Geist', system-ui, sans-serif; }}
main {{ max-width:1100px; margin:0 auto; padding:32px 16px 64px; }}
h1 {{ font-size:34px; letter-spacing:-.5px; margin:0 0 4px; }}
.lede {{ color:var(--mute); margin:0 0 24px; }}
.strip {{ display:grid; grid-template-columns:repeat(54,1fr); gap:2px; margin:0 0 6px; }}
.cell {{ height:22px; background:var(--c); border-radius:3px; }}
.legend {{ display:flex; flex-wrap:wrap; gap:14px; color:var(--mute); font-size:13px; margin-bottom:24px; }}
.legend i {{ display:inline-block; width:10px; height:10px; border-radius:2px; margin-right:6px; vertical-align:-1px; }}
.wrap {{ overflow-x:auto; }}
table {{ border-collapse:collapse; width:100%; min-width:760px; }}
th {{ text-align:left; font-weight:600; color:var(--mute); font-size:12px; text-transform:uppercase; letter-spacing:.06em; padding:8px; border-bottom:1px solid var(--line); }}
td {{ padding:7px 8px; border-bottom:1px solid var(--line); vertical-align:top; }}
tr.bar td {{ border-top:1px solid var(--ink); }}
.n {{ font-weight:700; width:34px; }} .t,.b {{ font-family:'Geist Mono', ui-monospace, monospace; color:var(--mute); width:56px; }}
.sec {{ display:inline-block; padding:1px 8px; border-radius:20px; background:var(--c); color:#fff; font-size:12px; }}
code {{ font:12px 'Geist Mono', ui-monospace, monospace; background:var(--line); padding:1px 5px; border-radius:4px; white-space:nowrap; }}
.sfx {{ width:260px; }} .mute {{ color:var(--mute); }}
.tag {{ display:inline-block; margin-left:6px; padding:0 7px; border-radius:20px; font-size:12px; border:1px solid currentColor; }}
.drop {{ color:#c0392b; }} .brk {{ color:var(--mute); }} .ret {{ color:#2e7d32; }}
</style></head><body><main>
<h1>Kuznec. — карта битов</h1>
<p class="lede">120 BPM · 54 бита · 27 с · один дубль. Дроп на бите 13 (зум приземляется), брейк на битах 47–52 (стол), бит возвращается на 53 вместе с вордмарком. Последний кадр = первый.</p>
<div class="strip">{strip}</div>
<div class="legend">{''.join(f'<span><i style="background:{COL[s]}"></i>{s} {a}–{b}</span>' for s, a, b in bm['sections'])}</div>
<div class="wrap"><table><thead><tr><th>Бит</th><th>с</th><th>такт</th><th>Сцена</th><th>Что происходит</th><th>Звук (CC0, по пику)</th></tr></thead>
<tbody>{''.join(rows)}</tbody></table></div>
</main></body></html>'''
out = os.path.join(root, 'out', 'beatmap.html')
open(out, 'w', encoding='utf-8').write(page)
print(out)
