# Audio licence

Every sound effect and music track in `app/src/main/res/raw/` (`sfx_*.ogg`, `music_*.ogg`) is an
**original work, synthesised from code** by [`tools/make_audio.py`](../tools/make_audio.py). No
samples, loops, presets or recordings from anyone else are used. The script builds each sound from
oscillators (band-limited square, triangle, sine) and seeded noise, and composes the four music
tracks note by note.

The audio files are dedicated to the public domain under
**[CC0 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/)**. You may copy, modify
and use them for any purpose without asking and without attribution. (The rest of the repository
stays under the MIT licence in [`LICENSE`](../LICENSE).)

To regenerate them byte for byte:

```sh
pip install numpy scipy soundfile
python tools/make_audio.py
```

| File | What it is | Where it plays |
|---|---|---|
| `sfx_coin` | Two-note coin chime | A coin arriving; USE A COIN; INSERT COIN on the cold open |
| `sfx_coin_drop` | A cascade of coins | A RevenueCat coin grant; the Credits Roll payout |
| `sfx_lever` | Ratchet, then the mechanism bottoming out | The DRAW lever |
| `sfx_deal` / `sfx_flip` | Paper swish / two snaps | Cards leaving the dispenser / turning face up |
| `sfx_whoosh` | Swept noise | Swiping a card away |
| `sfx_select` | Rising arpeggio | PLAYING IT; a RANK pick |
| `sfx_blip` | Tiny triangle blip | Every arcade button |
| `sfx_tick` | A detent click | STACK scrolling; GAME CLEARED typing |
| `sfx_error` | Two falling buzzes | A gate or paywall error |
| `sfx_add` | "Item get" chime | A game going into the pile |
| `sfx_friend` | Four-note call | A friend's pile arriving |
| `sfx_powerup` | Sweep into a major chord | PRO unlocked (purchase or FREE PLAY) |
| `sfx_crt_on` | Degauss thunk, hum, static | The cold open |
Music comes in three packs, chosen in YOU → MUSIC PACK. Each pack has the same four slots:
`_title` (onboarding), `_continue` (the CONTINUE? gate), `_shop` (GO PRO) and `_victory` (the
Credits Roll, plays once). All twelve are mastered to the same loudness (−16 dBFS RMS).

| Pack | Files | Sound |
|---|---|---|
| **ARCADE** (free, default) | `music_arcade_*` | Chiptune: pulse-wave leads, triangle bass, noise drums. 132/112/96/150 bpm |
| **AFTER HOURS** (5 coins) | `music_afterhours_*` | Lo-fi: FM electric piano, warm pads, sub bass, brushes, bell melodies. 74/66/84/80 bpm |
| **NEON DRIVE** (5 coins) | `music_neondrive_*` | Synthwave: filtered supersaw pads, saw bass, gated snare, saw lead. 104/96/92/110 bpm |
