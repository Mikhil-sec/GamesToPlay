# CONTINUE? — Design System (Neo-Arcade)

> **The governing principle: modern layout, retro accents.**
> Cheap retro looks cheap. Scanlines over everything, five neon colours, and a pixel font in
> body copy reads as a student project. What wins a design award is a *contemporary,
> well-spaced, typographically disciplined app* that deploys arcade language precisely —
> and rarely. Restraint is the whole strategy.
>
> Test for every retro flourish: **would this survive on a 2026 flagship at 120Hz in
> daylight?** If not, cut it.

---

## 1. Colour

Dark-only. This app does not need a light theme — an arcade is a dark room, and committing
to one mode buys us polish everywhere else. (Respect system contrast settings regardless.)

### Base — near-black, never pure black

| Token | Hex | Use |
|---|---|---|
| `surface.void` | `#08090C` | App background |
| `surface.cabinet` | `#101218` | Cards, sheets |
| `surface.raised` | `#181B23` | Elevated cards, menus |
| `surface.felt` | `#0E1A16` | The DRAW table felt |
| `outline.dim` | `#242833` | Hairlines, dividers |

Pure `#000000` is banned — it crushes on OLED and kills depth. Everything sits on a very
slightly blue-shifted black so neon reads as *emission* rather than a flat fill.

### Accents — one hot, one cool, and that's it

| Token | Hex | Use |
|---|---|---|
| `accent.coin` | `#F7C948` | **Primary.** Coins, DRAW button, CTAs, currency. Warm arcade gold. |
| `accent.neon` | `#00E5A0` | **Secondary.** Success, cleared, positive verdicts. Acid green. |
| `accent.hot` | `#FF3D7F` | **Tertiary, sparingly.** Retire/drop, alerts, the CONTINUE? countdown. Hot magenta. |
| `accent.cool` | `#5B8CFF` | Info, links, platform chips. |

**Rule: at most two accents visible in a single viewport.** Gold carries the app; green and
magenta are punctuation.

### Text

| Token | Hex | Use |
|---|---|---|
| `text.primary` | `#F2F4F8` | Titles, body |
| `text.secondary` | `#9AA3B2` | Metadata, labels |
| `text.tertiary` | `#5A6373` | Disabled, hints |

Contrast: body text must clear **4.5:1**, large display type **3:1**. Verify — neon on near
black is easy to get wrong in the other direction (too bright is fatiguing).

### Glow

Neon glow is *earned*, not ambient. Implement as a soft outer shadow plus a low-alpha radial
behind the element, never a blur on text itself (it destroys legibility). Only these get
glow: the DRAW button, the coin counter on change, the CONTINUE? countdown, and a cleared
game's rank badge.

---

## 2. Typography

Three faces, clear roles. All from Google Fonts (licensing-safe, bundle them).

| Role | Font | Usage |
|---|---|---|
| **Display / arcade** | **Chakra Petch** (or **Silkscreen** for true pixel moments) | `CONTINUE?`, `GAME CLEARED`, section headers, the countdown. Uppercase, tight tracking. |
| **UI / body** | **Inter** | Everything functional. Never pixel-fonted. |
| **Numerals / stats** | **JetBrains Mono** | Counters, hours, ranks, coin balance. Tabular figures so numbers don't jitter when animating. |

**Hard rule: no pixel font below 16sp, and never in a paragraph.** Pixel type is for
5-word moments only.

### Scale

| Token | Size / Line | Font |
|---|---|---|
| `display.xl` | 48 / 52 | Chakra Petch, 700, `letterSpacing: 0.02em` |
| `display.l` | 32 / 36 | Chakra Petch, 700 |
| `title.l` | 22 / 28 | Inter, 600 |
| `title.m` | 17 / 24 | Inter, 600 |
| `body` | 15 / 22 | Inter, 400 |
| `label` | 12 / 16 | Inter, 600, `letterSpacing: 0.08em`, uppercase |
| `mono.l` | 28 / 32 | JetBrains Mono, 700, tabular |

---

## 3. Shape, depth, texture

- **Radius:** cards `14dp` · sheets `24dp` top · chips `full` · buttons `12dp`.
  The arcade button is a true circle.
- **Elevation:** do not use Material's default shadows — they're grey and muddy on
  near-black. Use a **1dp top hairline** at `#FFFFFF0D` plus a soft dark drop shadow.
  Light comes from above, as on a physical cabinet.
- **Scanlines:** a 2px repeating overlay at **3–4% opacity max**, applied *only* to the DRAW
  cabinet screen and the Credits Roll. Never over the whole app, never over body text.
  Provide a global "Reduce retro effects" setting.
- **Chromatic aberration:** a 1px red/cyan offset, used only on the `CONTINUE?` title and
  the countdown. One or two places in the entire app.
- **Noise:** a very subtle film grain (2% alpha) over full-screen ritual moments only.

---

## 4. Motion

Motion *is* the design award submission. Budget real time here.

### Springs, not curves
Default to physics. Compose `spring()` for anything the user touches or that should feel
weighty:

| Token | Spec | Use |
|---|---|---|
| `spring.snappy` | `dampingRatio 0.75, stiffness 900` | Buttons, chips, toggles |
| `spring.card` | `dampingRatio 0.68, stiffness 380` | Card deals, flips, swipes |
| `spring.heavy` | `dampingRatio 0.85, stiffness 180` | The lever, sheets, big transitions |
| `ease.emphasized` | Material emphasized, 400ms | Screen transitions, fades |

### The five signature animations

These five carry the award. Everything else can be tasteful and standard.

**1. The DRAW lever**
Draggable with real resistance, `spring.heavy` return, a hard detent at the bottom. On
release: cabinet CRT flicker (3 frames of brightness/scale jitter), then the deal.

**2. The card deal**
Three cards arc from a slot with distinct bezier trajectories, staggered ~90ms, landing with
a slight overshoot and a 2–4° random resting rotation so it looks dealt, not laid out. Then
sequential 3D `rotationY` flips with a subtle specular sweep across the face mid-flip.

**3. Swipe verdicts**
`Modifier.pointerInput` + `Animatable`. Card rotates up to 12° with horizontal drag,
translates with the finger, and shows a verdict stamp (`PLAYING IT` / `NOT TONIGHT`) whose
opacity tracks drag distance. **Velocity decides the commit**, not distance alone — a fast
flick past 25% should fire. Fully interruptible mid-flight.

**4. The Credits Roll**
Black cut → CRT power-on flash (a bright horizontal line expanding vertically) → per-letter
reveal of `GAME CLEARED` → key art Ken Burns zoom → credits scroll at a constant, unhurried
rate → coin-shower particles (30–50 sprites, gravity + rotation + fade). Skippable on tap
at any point.

**5. The coin**
A gold coin that spins on its Y axis with a specular flash at the edge-on frame. Used for:
earning, spending, adding a game, clearing a game. This is the app's mascot motion — one
element, reused everywhere, which is what makes a design system feel authored.

### Shared-element transitions
Use Compose shared-element transitions for game art: grid → detail, card → Now Playing,
detail → Credits Roll. Continuity of the box art across screens is what separates a premium
app from a stack of screens.

### Discipline
- Nothing blocks input. Every ritual is tap-to-skip.
- Target 120Hz; profile with the Compose recomposition tools.
- Respect `Settings.Global.ANIMATOR_DURATION_SCALE` and the system reduce-motion setting —
  when reduced, cross-fade instead of animating, and skip particles entirely.

---

## 5. Haptics

Haptics are half of "tactile" and cost almost nothing to implement.

| Event | Effect |
|---|---|
| Lever detent | `HapticFeedbackConstants.CONFIRM` / heavy tick |
| Each card landing | Light tick, staggered with the visual |
| Card flip | Medium tick |
| Swipe commit | Heavy |
| Coin earned | Double light tick |
| Game cleared | Custom `VibrationEffect.createWaveform` — a short celebratory pattern |
| Countdown tick | Light tick per digit |

Global toggle in settings, on by default.

---

## 6. Sound (optional but high leverage)

Very short, very quiet, off in silent mode, with a settings toggle. Sound turns "nice" into
"delightful" in a demo video, which is where judges form their impression.

Coin insert · card deal · lever clunk · CRT power-on · game-cleared chime.

**Must be original or CC0 with a documented licence** — the demo-video rules prohibit
unlicensed third-party audio, and store review can flag it too. Keep the licence file in the
repo.

---

## 7. Iconography & app icon

- Icons: **Material Symbols Rounded**, weight 300, at 24dp. Do not draw custom icons for
  functional UI — spend that time on motion instead.
- **App icon:** an original mark. A gold coin dropping into a slot, or the `?` from
  `CONTINUE?` rendered as an arcade marquee, on near-black with a subtle gold glow.
  Must be original artwork — **no game box art, no influencer branding**. Needs to read at
  48dp and survive Play's adaptive-icon mask.
- Deliverables: 1024×1024 (Devpost), 512×512 (Play), adaptive foreground/background,
  monochrome layer for themed icons.

---

## 8. Empty & error states

Never a blank screen. Each empty state is an arcade attract-mode screen with a one-line joke
and a single clear action.

- **Empty pile:** `INSERT GAME TO BEGIN` + `IMPORT FROM STEAM` / `SEARCH`
- **Empty draw:** `NOT ENOUGH GAMES IN THE MACHINE` + `ADD SOME`
- **Offline:** `CONNECTION LOST — YOUR PILE STILL WORKS` (and it genuinely does)
- **No results:** `NO MATCH FOUND` + `TRY ANOTHER NAME`

---

## 9. What judges should be told to look at

For the Design Award write-up, point them at exactly these, in this order:

1. **The DRAW lever and card deal** — physics-driven, not canned.
2. **The Credits Roll** on clearing a game.
3. **Pairwise ranking** — a genuinely new interaction for this category.
4. **The share cards** — rendered live from Compose, not templates.
5. **The Time Budget visualization** — data as an emotional object.
