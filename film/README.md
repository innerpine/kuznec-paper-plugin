# Kuznec — launch film

A 27-second, 1440×1440 launch film for the plugin: one continuous take, 120 BPM, 54 beats.
Everything is in [`film.html`](film.html), and every frame is a pure function of time (`seek(t)`).

- `out/beatmap.html` — the beat map (generated from the JSON block in `film.html`)
- `out/stills/` — open, glass, stage, desk
- `out/kuznec-preview-*.mp4` — rendered previews

## Preview

Serve the folder and open `film.html` in a browser; it scales to fit and has a scrubber.

```bash
cd film/tools && npm install
node -e "import('./serve.mjs').then(async m => console.log((await m.serve('..')).url + '/film.html'))"
```

## Render

```bash
cd film/tools
python3 -I mix.py ../out/audio.wav                    # SFX only
python3 -I mix.py ../out/audio.wav --music track.mp3 --drop 32.0 --breakdown 64.0 --back 72.0
node render.mjs --out ../out/kuznec.mp4 --audio ../out/audio.wav --workers 4
python3 -I qc.py ../out/kuznec.mp4 ../out/qc          # a frame per beat + single-frame pop hunt
```

- `render.mjs` renders 60 fps with 4 subframes per frame and blends them with ffmpeg `tmix`.
- `mix.py` places every SFX by its measured peak, places the track so its drop lands on beat 13
  (optionally splicing it so the breakdown sits on beats 47–52 and the beat returns on 53),
  and normalises to −14 LUFS.

## Assets

| What | Where | Source |
| --- | --- | --- |
| Covers, wallpaper, avatar | `assets/covers/` | Rendered here: own 16×16 pixel sprites (`tools/sprites.py`) extruded to voxels (`tools/covers.mjs`) |
| Fonts | `assets/fonts/` | Archivo (wdth 62–125), Geist, Geist Mono — Google Fonts, OFL |
| SFX | `assets/sfx/` | [uisfx](https://github.com/romainsimon/uisfx), CC0 — stand-in for Mixkit (these files are pre-rendered from synthesis recipes; swap in recorded Mixkit SFX under the same names) |
| Desk footage | `assets/footage/` | Rendered stand-in (`tools/desk.mjs`) — stand-in for a Pexels clip |
| Music | — | Not included yet (Mixkit) |

### Swapping in the real desk clip

```bash
python3 -I tools/footage.py pexels-clip.mp4 --start 2 --dur 3.5 --out assets/footage/desk.webm
```

This writes an all-intra VP9 WebM (Playwright's Chromium does not decode H.264) and
`desk.quad.json` with the phone screen it found; check `desk.quad.jpg`, then point `FOOTAGE`
in `film.html` at the new files.
