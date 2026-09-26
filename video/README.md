# The CONTINUE? demo video

The Devpost demo video (1:50), made in code with [Remotion](https://www.remotion.dev) so the
motion design matches the app exactly: the app's fonts (`public/fonts`, copied from
`app/src/main/res/font`), its colour tokens (`src/theme.ts`, from `docs/03-DESIGN-SYSTEM.md`) and a
soundtrack composed from the app's own synth instruments (`../tools/make_video_score.py`, CC0).

Everything about the edit lives in **`timeline.json`**: shot order, lengths (in bars at 104 bpm,
so cuts land on the beat), clip files and trim points, headline text, push-ins and sound effects.

## Workflow

```sh
npm install
npm run shotlist     # writes SHOTLIST.md: every clip to record and its minimum length
npm run studio       # live preview in the browser; scrub, and see missing clips as labelled cards
npm run render       # the final 1920×1080 60 fps video → out/continue.mp4
```

1. **Record** each clip in `SHOTLIST.md` (see "Recording" below) and save the chosen take into
   `public/clips/` under exactly its listed name. Clips are gitignored; they never go in the repo.
2. **Trim** by setting a clip's `from` (seconds into the file) in `timeline.json`, and watch
   the result in the studio.
3. **Change a shot's length, music section or sound effects?** Edit `bars`/`music`/`sfx`, then
   `npm run score` (needs `pip install numpy scipy soundfile`). It rebuilds the score and
   `public/audio/mix.wav`, the score plus every effect, soft-limited, which is the only audio the
   video plays. Clip audio is muted; the phone's own recording of the app's sounds was near-silent.
4. **Render**, then upload `out/continue.mp4` to YouTube as **Public**.

Text markup in `timeline.json`: `*gold words*`, `_green words_`. A line's `at` is the beat within
the shot where it appears. `push` zooms the phone in on a point (`focusY` = fraction of the
screen height from the top) at a beat.

Optional: put the official "Get it on Google Play" badge PNG (from Google's badge generator, not
modified) at `public/brand/google-play-badge.png` and the end card uses it; otherwise it shows the
store line as text.

## Recording

- **Screen:** [scrcpy](https://github.com/Genymobile/scrcpy) over USB (USB debugging on). One file per
  clip: `scrcpy --record=04_share_from_chat.mp4 -b 20M --max-fps=60`. It records the app's sound
  on Android 11+. In the app, set **MUSIC off, SOUND on**; the score supplies the music.
- **Camera shots (📷):** a second phone, landscape, 4K/60, exposure locked, dark room, screen at
  full brightness.
- **Clean status bar:** Android demo mode (commands in `docs/07-SUBMISSION-KIT.md`), plus Do Not
  Disturb.

## Rules this video must keep (Devpost + Shipaton)

Under 2:00 · shows the app running on the device · no third-party trademarks, copyrighted music
or material: **never share from a creator's YouTube/TikTok video, never show an ad's content**,
keep game art incidental · no influencer names or likenesses anywhere.
