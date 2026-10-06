"""Pixel sprites for the upgrade covers (own designs, 16x16), exported to sprites.json.

Each sprite is a grid of palette keys; '.' is empty. 'o' outlines are added
automatically around filled cells unless a cell is marked with '_' (keep empty).
"""
import json, sys
from PIL import Image

N = 16

def blank():
    return [['.'] * N for _ in range(N)]

def put(g, x, y, c):
    if 0 <= x < N and 0 <= y < N:
        g[y][x] = c

def line(g, x0, y0, x1, y1, c):
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        put(g, x0, y0, c)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy; x0 += sx
        if e2 <= dx:
            err += dx; y0 += sy

def rows(g, x, y, lines):
    for j, row in enumerate(lines):
        for i, c in enumerate(row):
            if c != ' ':
                put(g, x + i, y + j, c)

def outline(g, c='o', diag=False):
    add = []
    for y in range(N):
        for x in range(N):
            if g[y][x] != '.':
                continue
            nb = [(1, 0), (-1, 0), (0, 1), (0, -1)]
            if diag:
                nb += [(1, 1), (1, -1), (-1, 1), (-1, -1)]
            for dx, dy in nb:
                xx, yy = x + dx, y + dy
                if 0 <= xx < N and 0 <= yy < N and g[yy][xx] not in '.o_':
                    add.append((x, y)); break
    for x, y in add:
        g[y][x] = c
    for y in range(N):
        for x in range(N):
            if g[y][x] == '_':
                g[y][x] = '.'

def sword(blade_l='l', blade_m='m', short=False):
    g = blank()
    n = 6 if short else 9
    for i in range(n):
        x, y = 14 - i, 1 + i
        put(g, x - 1, y, blade_l)
        put(g, x, y, blade_m)
    put(g, 14, 0, blade_m)
    # cross guard, perpendicular to the blade
    bx, by = 14 - n, 1 + n          # first cell below the blade
    line(g, bx - 2, by - 2, bx + 2, by + 2, 'g')
    put(g, bx - 1, by - 1, 'G'); put(g, bx + 1, by + 1, 'G')
    # grip and pommel
    line(g, bx - 1, by + 1, bx - 3, by + 3, 'h')
    for (dx, dy) in [(-5, 4), (-4, 4), (-5, 5), (-4, 5)]:
        put(g, bx + dx, by + dy, 'p')
    return g

S = {}

# 1. Вампиризм: steel blade bleeding at the edge, drops under the tip
g = sword('l', 'r')
outline(g)
S['vampirism'] = g

# 2. Пламенное лезвие: hot blade
g = sword('y', 'f')
outline(g)
S['flame'] = g

# 3. Ядовитый порез: short dagger, green edge
g = sword('l', 'v', short=True)
outline(g)
S['poison'] = g

# 4. Ночной глаз: helmet with an eye in the visor
g = blank()
rows(g, 2, 2, [
    "   HHHHHH   ",
    "  HiiHHHHH  ",
    " HiHHHHHHHH ",
    " HiHHHHHHHH ",
    "HHHHHHHHHHHH",
    "HHddddddddHH",
    "HHdwwEEwwdHH",
    "HHddddddddHH",
    "HH HHHHHH HH",
    "HH  HHHH  HH",
    "HH        HH",
])
outline(g)
S['nighteye'] = g

# 5. Магический щит: heater shield with a rune
g = blank()
rows(g, 2, 1, [
    "AAAAAAAAAAAA",
    "AaaaaaaaaaaA",
    "AaBBBaaBBBaA",
    "AaBaaaaaaBaA",
    "AaaaaRRaaaaA",
    "AaaaRaaRaaaA",
    "AaaaRaaRaaaA",
    "AaaaaRRaaaaA",
    " AaBaaaaBaA ",
    " AaBBaaBBaA ",
    "  AaaaaaaA  ",
    "   AaaaaA   ",
    "    AaaA    ",
    "     AA     ",
])
outline(g)
S['shield'] = g

# 6. Последний рубеж: gold chestplate with a heart
g = blank()
rows(g, 1, 1, [
    "CCCC      CCCC",
    "CccCC    CCccC",
    "CcccCCCCCCcccC",
    "CccccccccccccC",
    " CcccKKccKKcC ",
    " CccKKKKKKKKc ",
    "  CcKKKKKKKKC ",
    "  CccKKKKKKcC ",
    "  CcccKKKKccC ",
    "  CccccKKcccC ",
    "  CccccccccC  ",
    "  CCcccccccC  ",
    "   CCCCCCCC   ",
])
outline(g)
S['laststand'] = g

# 7. Шаг ветра: leggings with wind streaks
g = blank()
rows(g, 3, 1, [
    "LLLLLLLLLL",
    "LllllllllL",
    "LlLLLLLLlL",
    "Llll  lllL",
    "Llll  lllL",
    "Llll  lllL",
    "Llll  lllL",
    "LLll  llLL",
    "Llll  lllL",
    "Llll  lllL",
    "Llll  lllL",
    "LLLL  LLLL",
])
outline(g)
for y, x0, x1 in [(4, 0, 2), (8, 13, 15), (12, 0, 1), (14, 4, 7)]:
    line(g, x0, y, x1, y, 'w')
S['wind'] = g

def boot(g, x, y, c='B', cl='b'):
    rows(g, x, y, [
        "BBBB     ",
        "Bbbb     ",
        "Bbbb     ",
        "Bbbb     ",
        "BbbbBBBB ",
        "BbbbbbbbB",
        "BBBBBBBBB",
    ])

# 8. Пероход: boot with a feather
g = blank()
boot(g, 1, 8)
line(g, 14, 0, 7, 7, 'q')
for (x, y) in [(13, 2), (12, 3), (11, 4), (10, 5)]:
    put(g, x + 1, y + 1, 'Q'); put(g, x - 1, y - 1, 'Q')
outline(g)
S['feather'] = g

# 9. Кроличий прыжок: boot on a spring with an arc
g = blank()
boot(g, 4, 5)
rows(g, 5, 12, ["sssssss", " SSSSS ", "sssssss"])
outline(g)
S['rabbit'] = g

# 10. Наковальня (avatar)
g = blank()
rows(g, 0, 3, [
    "NNNNNNNNNNNNNN  ",
    "NnnnnnnnnnnnnNNN",
    " NNNNnnnnnnnnnNN",
    "    NNnnnnnnN   ",
    "     NnnnnnN    ",
    "     NnnnnnN    ",
    "    NNnnnnnNN   ",
    "   NnnnnnnnnnN  ",
    "  NnnnnnnnnnnnN ",
    "  NNNNNNNNNNNNN ",
])
outline(g)
S['anvil'] = g

PALETTE = {
    'o': '#221c18',
    'l': '#e9eef2', 'm': '#a9b4bd', 'g': '#c8962e', 'G': '#f0c75a', 'h': '#5b3a24', 'p': '#c8962e',
    'r': '#d0142e', 'y': '#ffd54a', 'f': '#ff7a1a', 'v': '#7cff4f',
    'H': '#8d97a3', 'i': '#d5dce4', 'd': '#20242b', 'w': '#9ff5ff', 'E': '#ffffff',
    'A': '#3e2a8f', 'a': '#7a5cff', 'B': '#c9b8ff', 'R': '#ffffff',
    'C': '#a8740f', 'c': '#ffcf3d', 'K': '#e2263f',
    'L': '#0f6c66', 'N': '#2b2f36', 'n': '#6b7380',
    'b': '#5d2f6e',
    'q': '#ffffff', 'Q': '#cfe8ff', 's': '#d8dde3', 'S': '#8a939c', 'j': '#ffffff',
}

OVERRIDE = {
    'wind': {'l': '#e6fbf8'},
    'feather': {'B': '#1f5f99', 'b': '#7cc4ff'},
    'rabbit': {'B': '#a3285e', 'b': '#ff8fc0'},
}

def color(name, c):
    return OVERRIDE.get(name, {}).get(c, PALETTE.get(c, '#ff00ff'))

if __name__ == '__main__':
    out = {k: {'grid': [''.join(r) for r in v], 'palette': {c: color(k, c) for c in set(''.join(''.join(r) for r in v)) - {'.'}}} for k, v in S.items()}
    json.dump(out, open(sys.argv[1], 'w'), indent=1, ensure_ascii=False)
    # contact sheet for eyeballing
    sc = 16
    im = Image.new('RGB', (len(S) * (N * sc + sc), N * sc + 2 * sc), '#f4f1ea')
    for k, (name, gr) in enumerate(S.items()):
        for y in range(N):
            for x in range(N):
                c = gr[y][x]
                if c == '.':
                    continue
                col = color(name, c)
                ox = k * (N * sc + sc) + sc // 2
                im.paste(col, (ox + x * sc, sc + y * sc, ox + x * sc + sc, sc + y * sc + sc))
    im.save(sys.argv[2])
