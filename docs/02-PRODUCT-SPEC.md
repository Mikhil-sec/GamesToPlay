# CONTINUE? — Product Specification

## Navigation

Bottom bar, 4 destinations, with DRAW as a raised arcade button in the centre:

```
┌──────────────────────────────────────────────┐
│                                              │
│                 (content)                    │
│                                              │
├──────────────────────────────────────────────┤
│   PILE      DISCOVER   ⬤DRAW⬤   YOU          │
└──────────────────────────────────────────────┘
```

`DRAW` is visually a physical arcade button — domed, with a coloured glow, that depresses
with a spring and haptic on press. It is the app's centre of gravity.

---

## 1. PILE — the library

The user's backlog. Offline-first; everything here works with no network.

### Game states

| State | Label | Meaning |
|---|---|---|
| `BACKLOG` | THE PILE | Owned/intended, not started. The default. |
| `PLAYING` | NOW PLAYING | **Hard cap of 3.** |
| `COMPLETED` | CLEARED | Finished. |
| `DROPPED` | RETIRED | Abandoned, framed without shame. |
| `WISHLIST` | WANTED | Not owned yet — kept separate so the pile stays honest. |

**The cap of 3 on NOW PLAYING is deliberate and must be enforced.** Attempting a 4th prompts
"Your cabinet only fits 3. What are you swapping out?" It's an opinion about focus, and it
creates a small, pleasant decision instead of an infinite list.

### Views

Toggle in the top bar, state persisted:

1. **STACK** (default, signature) — games as physical cases in a receding 3D-ish stack,
   flicked through with momentum and snap. Uses `graphicsLayer` rotation/scale/alpha keyed
   to scroll offset. This is the "wow" view.
2. **GRID** — dense box-art grid, 3 columns. The practical view.
3. **LIST** — compact rows with title, platform, hours, status. The power view.

### The Time Budget bar — hours per week is now yours to set

Pinned under the top bar on PILE. Compact by default:

```
412 HRS · 87 GAMES · FINISHED BY 2029 ▸
```

Tapping expands a full-screen visualization: a horizontal bar of every game in the pile,
width proportional to its length, coloured by genre — a literal picture of the debt. Below
it, a slider for "hours I actually play per week" that live-updates the projected finish
date. Funny, a little bleak, and highly shareable (it has its own share card).

The finish-year copy adapts: under 6 months → "Finished by March"; over 10 years →
"Finished by 2041. Consider retiring some."

### Stacks (collections)

User-created groupings: "Steam Deck queue", "Halloween horror", "Couch co-op with Sam".
A game can belong to many stacks. Drag-and-drop to reorder and to move between stacks.

- **Free: 2 stacks.** Pro: unlimited.

### Sort & filter

Sort: date added · title · length (short→long) · rating · platform · release date. **Every one
of these is a chip generated from the `PileSort` enum**, so an option can't be declared and then
be unreachable — which is what had happened to three of the six, two of which also did nothing
when selected.

Filter, in three labelled groups: **length bucket** (<5h / 5–15h / 15–40h / 40h+, single-select),
**genre & mood**, **platform** (both multi-select).

Three rules, all of them fixes for the 2026-08-28 report that the filters were inconsistent
between categories and with DRAW:

- **One vocabulary, three screens.** Genre chips are `GameTaxonomy` facets — matched across
  IGDB's genres, themes *and* game modes — so HORROR, MULTIPLAYER and CO-OP exist at all, and
  so PILE, DRAW's GENRE dial and STATS narrow the same pile by the same things.
- **The chip set is built from the whole pile, not the current tab**, so it doesn't change under
  your thumb when you switch tab.
- **Every chip carries its count in the tab you're looking at**, and a chip matching nothing here
  shows `0` rather than vanishing — a filter that silently disappears is how an empty-looking
  pile gets blamed on the app.

Multi-select is **OR within a group, AND between groups**: HORROR + CO-OP means either; HORROR +
PS5 means both. That's the only arrangement where adding a second chip of the same kind can't
make the list shrink to nothing.

**"Shortest first"** deserves a dedicated one-tap chip — it's how people actually beat a
backlog, and surfacing it is a genuine insight about the use case.

### Dates — logging games you cleared before you had the app

Every pile entry can be given a **started** and a **cleared** date, from two places:

- **DISCOVER → long-press (or tap the title) → ALREADY CLEARED** — adds a game straight to
  CLEARED with the dates you pick. `addedAt` is backdated too, so "time in the pile" and the
  RECENT sort both read as history rather than as something added today.
- **PILE → a game → EDIT DATES** — for one already in the pile. Setting a cleared date files it
  under CLEARED, and the dialog says so before it writes.

A cleared date can't precede a started date, and neither can be in the future — both are blocked
in the picker rather than validated afterwards.

**A backdated clear pays no coins.** Clearing a game is worth +5 because it took months; logging
a game you finished in 2019 takes four taps, and paying for it would be a faster coin faucet than
the repeatable-clear loop closed in the same release. See `docs/04-MONETIZATION.md`.

### STATS — what the pile is made of

Reached from PILE's header. A scope selector (EVERYTHING, or any one state) over:

- **THE SPREAD** — every state as one proportional bar. 40 in THE PILE against 3 CLEARED is the
  number a backlog app exists to make you feel.
- **GAMES / HOURS / SPAN** tiles.
- **DIVERSITY, 0–100** — normalised Shannon entropy over the facet mix, not a category count:
  nineteen shooters and one puzzle game is not a varied pile, and a count says it is.
- **Bars for genre & mood, length, platform and release decade**, all from the same
  `GameTaxonomy` facets as the filter chips — so every number on the screen can be reached by
  tapping a chip on PILE.

Drawn with layout (`Box` width fractions, a `Row` of weights), not a chart library: no extra
dependency in a public repo, and it reads correctly to TalkBack as ordinary text.

---

## 2. Capture — how games get in

Four paths, in order of how much they matter.

### 2a. Android Share Target ★ the standout feature

Registered for `text/plain` and `text/uri-list` via an intent filter, with
`android:label="Add to CONTINUE?"`.

The flow:
1. User is in YouTube / Reddit / Chrome / Twitch / X and shares a video, post, or link.
2. CONTINUE? appears in the share sheet. Tapping it opens a **transparent bottom-sheet
   Activity** over the source app — the full app never launches.
3. The sheet resolves the title (see below), queries IGDB, and shows the matched game with
   box art and a confidence indicator.
4. One tap → added to the pile. The sheet confirms with a coin-drop animation and
   auto-dismisses in ~1.2s. Total time: about two seconds.
5. If the match is wrong or ambiguous, the sheet shows the top 3 candidates plus a
   "search instead" field.

### The hard part: most apps share a bare URL, not a title

This is the single biggest technical risk in the feature, and it must be designed for
explicitly. When you share from a short-form video app, `EXTRA_TEXT` usually contains
**only a URL** — no title, no caption. `EXTRA_SUBJECT` is inconsistent and often absent.

So resolution happens in **two stages**: get *text* for the link, then find the *game* in
that text.

#### Stage 1 — URL → text (server-side, in the Worker)

| Source | Method | Reliability |
|---|---|---|
| **YouTube** (`youtube.com/watch`, `youtu.be`) | Public **oEmbed**: `https://www.youtube.com/oembed?url=…&format=json` → `title` + `author_name`. **No API key.** | ✅ Excellent |
| **YouTube Shorts** (`/shorts/<id>`) | Same oEmbed endpoint — normalize the URL to `watch?v=<id>` first | ✅ Excellent |
| **Steam** (`store.steampowered.com/app/<id>/<Slug>/`) | Un-slug the name directly from the path; no fetch needed | ✅ Excellent |
| **TikTok** (`tiktok.com`, `vm.tiktok.com`) | Follow the short-link redirect, then TikTok's oEmbed → `title` holds the caption | ⚠️ Good — verify at build time; treat as best-effort |
| **Reddit** | Append `.json` to the permalink for the post title, or use oEmbed | ✅ Good |
| **X / Twitter** | OpenGraph tags where available | ⚠️ Unreliable |
| **Instagram Reels** | ❌ **Public oEmbed was removed in April 2025** — Meta now requires an app access token, and scraping is actively blocked | ❌ **Not resolvable** |

**Do this resolution in the Worker, not the app.** These parsers break whenever a platform
changes, and a server-side fix ships in minutes while an app-side fix waits on Play review.
Given that review latency is our critical path, this is not a close call.

#### Stage 2 — text → game (title parser, a pure unit-tested function)

Captions are noisy: `"this boss took me 3 hours 😭 #eldenring #gaming #fyp"`.

- **Hashtags are the strongest signal.** Un-camel and de-concatenate them
  (`#eldenring` → "elden ring", `#BaldursGate3` → "baldurs gate 3") and try them first.
- Strip video-title noise: everything after the first `|`, bracketed tags (`[4K]`,
  `(Official Trailer)`), and boilerplate (`REVIEW`, `Gameplay`, `Let's Play`, `Part 12`,
  `EP.4`, trailing years).
- Strip emoji, `r/<sub>` prefixes, and `@handles`.
- Score every candidate substring against IGDB and rank by match confidence.
- **Never hard-fail.** Show the top 3 candidates with a "search instead" field.

#### The Instagram answer — share a screenshot instead

Since Reels links can't be resolved, register for `image/*` as well and run **ML Kit text
recognition on-device** (free, offline, fast) over the shared image. Extract candidate
strings, match against IGDB.

This turns a limitation into one of the best features in the app: it also captures game
names from **screenshots of anything** — a Discord message, a tweet, a store page, a
friend's recommendation, a photo of a physical game case. Ship it as a headline capability
rather than an Instagram workaround.

**Graceful degradation ladder** — the sheet must never dead-end:
1. Resolved confidently → show the match, one tap to add
2. Resolved ambiguously → show the top 3 candidates
3. Unresolvable link (Instagram) → open the sheet with a **pre-focused search field** and
   the URL retained, so the user types the name and it's still ~4 seconds
4. Offline → queue the raw text and resolve on next launch

This is the most persuasive 8 seconds of the demo video, and it is structurally unavailable
to iOS entrants at this quality. Build it early and make it flawless.

### 2b. Steam import

Enter a Steam profile URL or vanity name → the Worker calls Steam's public
`GetOwnedGames` → matches titles to IGDB → shows a review screen with checkboxes and
detected playtime. Games with >2h playtime are pre-tagged as already-played.

- **Free:** import the first 10 games, with the rest blurred behind a count
  ("133 more games waiting"). Unlock the full import with a rewarded ad (once) or Pro.
- This is the fastest way to make a fresh install feel real. It is also the strongest
  opening shot of the demo video: empty app → 143 games in four seconds.

### 2c. In-app search (DISCOVER tab)

Debounced IGDB search (via the Worker), 300ms, with results streaming in as cards. Each result has an
instant `+` that adds to the pile with a coin-flip animation without leaving the list.

DISCOVER also carries browsable rails:
- **TRENDING NOW** (IGDB popularity / hypes)
- **NEW RELEASES**
- **SHORT & SWEET** — highly-rated games under 8 hours. This rail is the app's opinion
  made visible, and it converts better than any other.
- **HIDDEN GEMS** — high rating, low review count
- Genre rails

### 2d. Clipboard nudge — **built 2026-08-28**

On foreground, if the clipboard holds text that plausibly names a game, show a dismissible
banner offering to add it. Keep it quiet — this is a delight when it's right and an irritation
when it's wrong, so bias toward silence.

How "bias toward silence" is actually enforced (`ClipboardNudgeViewModel.isPlausibleGameName`,
pinned by `ClipboardNudgeTest`): reject links (they belong to the share target, which resolves
them properly), emails, handles, one-time codes and phone numbers, multi-line text, anything over
60 characters or 8 words, and anything with no letters. Then require a match against the
**on-device** IGDB name index at `CONFIDENT_ENOUGH` — no network call, so the nudge is instant
and costs no Worker quota. A game already in the pile is swallowed silently. A dismissal is
**persisted**, so "no" survives a relaunch.

**Opt-in, off by default, and that is not only a preference.** From Android 12 the OS toasts
*"CONTINUE? pasted from your clipboard"* whenever an app reads clipboard content it didn't write,
so reading unprompted would put that toast on every app open. The setting's description in YOU
says this outright rather than letting the toast be the user's first hint.

*(Optional, low priority: barcode scan for physical cases via ML Kit.)*

---

## 3. DRAW — the decision engine ★ the signature feature

**The problem it solves:** 87 games, two free hours, total paralysis, and you end up
scrolling instead of playing. No other backlog app addresses this. It is the reason someone
opens CONTINUE? *daily*.

### The machine

A full-screen arcade cabinet. Three chunky dials the user sets:

| Dial | Options |
|---|---|
| **TIME** | `30 MIN` · `2 HOURS` · `ALL NIGHT` · `A WHOLE WEEKEND` |
| **MOOD** | `COZY` · `CHAOS` · `STORY` · `BRAIN` · `NOSTALGIA` |
| **PLATFORM** | derived from the platforms present in the user's pile, multi-select |

Below them, a **physical lever**. The user drags it down; it resists with spring physics and
snaps at the bottom with a heavy haptic. Not a button — a *lever*. The difference is the
whole point.

### Mood mapping

Map IGDB genres, themes, and game_modes into moods — the full table is in
`docs/08-GAME-DATA.md`. Keep it in a single, editable table:

| Mood | Matches |
|---|---|
| COZY | simulation, farming, life-sim, puzzle, "relaxing", "wholesome", "atmospheric" |
| CHAOS | action, shooter, fighting, racing, "multiplayer", "fast-paced" |
| STORY | RPG, adventure, visual novel, "story-rich", "narrative" |
| BRAIN | strategy, puzzle, roguelike, "difficult", "tactical" |
| NOSTALGIA | release year < 2010, "retro", "pixel-graphics", "classic" |

### The deal

On lever release:
1. Cabinet screen flickers (CRT power-on).
2. Three cards **deal** out of a slot with real arc trajectories and stagger (~90ms apart),
   landing face-down on felt.
3. They flip in sequence with a 3D `rotationY` flip and a card-snap haptic each.

Each card shows box art, title, estimated length, and — critically — **why it matched**:
`SHORT · COZY · ON YOUR SWITCH`. Explaining the pick is what makes it feel intelligent
rather than random.

### Selection algorithm

Not random. Weighted sampling:
- **Hard filters:** platform match; length fits the time budget (with a tolerance band).
- **Weights:** mood match ×3 · never-drawn-before ×2 · in the pile longest ×1.5 ·
  highly rated ×1.3 · recently passed on ×0.2 (2-week decay).
- Always return 3 distinct games. If fewer than 3 qualify, relax the platform filter first,
  then time, and *say so* on-screen ("Loosened to fit — only 2 short games on Switch").

### Swipe verdicts

| Gesture | Result |
|---|---|
| **Swipe UP** | **PLAYING IT** → moves to NOW PLAYING, coin-shower, confetti, heavy haptic |
| **Swipe LEFT** | *Not tonight* — deprioritized for 14 days |
| **Swipe RIGHT** | *Save for later* — pinned to the top of the pile |
| **Swipe DOWN** | *Retire it* → DROPPED, with kind copy: "No shame. That's 14 hours back." |

Cards must be interruptible, physics-driven (`Animatable` with spring), and rotate slightly
with drag. Velocity decides the commit, not just distance.

### The economy of DRAW — where ads live

**Free users get 1 draw per day.** On the second draw attempt, the **CONTINUE? screen**:

```
        ┌─────────────────────────┐
        │                         │
        │       CONTINUE?         │
        │                         │
        │           9             │   ← big arcade countdown
        │                         │
        │   ▸ INSERT COIN         │   ← watch rewarded ad → 1 coin
        │   ▸ USE A COIN (3)      │   ← spend an existing coin
        │   ▸ GO PRO — UNLIMITED  │   ← RevenueCat paywall
        │                         │
        └─────────────────────────┘
```

The countdown ticks 9→0 with the classic arcade cadence and **loops rather than locking the
user out**. It's atmosphere, not a punishment — never trap anyone. This screen is the
thematic and monetary heart of the app: in an arcade, continuing *costs a coin*, so a
rewarded ad here is the most natural monetization event this product could have.

---

## 4. NOW PLAYING & completion

### Now Playing
Up to 3 games, shown as arcade cabinets on a shelf. Each shows days elapsed and an optional
manual progress slider. Long-press for: mark complete · drop · move back to pile.

Optional gentle nudge after 21 days of no interaction: *"Still playing X? Or should it go
back in the pile?"* — one tap either way. Never nagging, never a notification more than once
a month.

### Credits Roll — the completion ritual

Marking a game complete is the emotional peak of the app. Do not make it a checkbox.

Full-screen, ~7 seconds, skippable by tap:
1. Screen cuts to black; a CRT power-on flash.
2. `GAME CLEARED` in arcade display type, letter by letter, with a chiptune-ish blip per
   character (respect the device silent switch).
3. The game's key art fades up behind at 25% opacity, slowly zooming.
4. **Credits scroll upward**, film-style:
   ```
   CLEARED BY          <player handle>
   TIME IN THE PILE    247 days
   STARTED             12 June 2026
   FINISHED            5 Aug 2026
   YOUR 43RD CLEAR     of 2026
   ```
5. Coin shower particle burst + a celebratory haptic pattern.
6. `RANK IT ▸` button slides up.

Award +5 coins for the completion — completion should *pay*.

---

## 5. RANK — pairwise ranking ★ the innovation

Star ratings are uncalibrated noise; everyone's average is 8/10. Instead we build a **true
ordered list**.

### Step 1 — coarse bucket
Four big cards: `LOVED IT` · `LIKED IT` · `IT WAS FINE` · `NAH`.
This partitions the list so comparisons stay meaningful and short.

### Step 2 — pairwise placement
Within the chosen bucket, binary-search the game's position:

> **WHICH DID YOU ENJOY MORE?**
> [ new game ]      vs.      [ already-ranked game ]

Tap or swipe toward the winner. The loser card shrinks and slides away; the winner pulses.
Each answer halves the search range. Cap at **5 comparisons** — with binary search that
places a game precisely within a 32-game bucket, and we interpolate beyond that.

First game ever ranked: skip comparisons entirely, it just takes position 1.

### Output
A single ordered list per user — their personal all-time ranking. Surfaced as
**"YOUR #4 OF ALL TIME"**, which is a far better thing to post than "4 stars".

Also optionally captured on the rank screen:
- a 140-character **verdict** (one-line hot take)
- platform played on
- a `WOULD REPLAY` toggle

### Why this wins
It's the only genuinely novel *interaction* in the backlog-app category, it's fast, it's
strangely addictive, and it produces the data that powers the best share card in the app.

---

## 6. SHARE

All share cards render an offscreen Compose layout to a Bitmap, save via `FileProvider`, and
fire `ACTION_SEND` (`image/png`). Each carries a small `CONTINUE?` wordmark and a short link
— our growth loop.

| Card | Content | Why people post it |
|---|---|---|
| **CLEARED** | Key art, title, "MY #4 OF ALL TIME", hours, clear count | Achievement flex |
| **HIGH SCORE** | Top 10 as an arcade high-score table, `1ST/2ND/3RD` in arcade type | Extremely arguable → comments → reach |
| **THE PILE** | "412 HOURS. 87 GAMES. SEND HELP." with the time-budget bar | Self-deprecating, universally relatable — the viral one |
| **THE STACK** | A curated stack as a shareable list | Recommendation flex |
| **YEAR IN GAMES** | Wrapped-style annual recap (Pro) | Seasonal spike |

**Themes** are the primary cosmetic coin sink: `CRT` and `CARTRIDGE` free; `HOLOGRAPHIC
FOIL`, `ARCADE MARQUEE`, `NEON NOIR`, `CRT DECAY` cost 25 coins or a rewarded ad for a
single use, all free with Pro. Cosmetic-only monetization never damages the core experience.

---

## 7. YOU — profile & stats

- **HIGH SCORES** — the ranked all-time list, arcade leaderboard styling. **EDIT** turns on
  per-row move-up / move-down / drop controls: pairwise comparison is a good way to *enter* a
  ranking and a poor way to correct one, and a mis-tap during the five questions used to be
  permanent. A game dragged past a bucket boundary adopts the bucket it lands in, so the buckets
  stay contiguous and later automatic placements stay correct. **Dropping a game from the
  leaderboard leaves it exactly where it is in CLEARED** — it is not REMOVE FROM PILE, and the
  confirmation says so.
- **THIS YEAR** — games cleared, hours, longest game, fastest clear, current streak
- **THE PILE** — the time-budget visualization
- **TROPHIES** — arcade-style milestones: `FIRST CONTINUE` · `PILE SLAYER` (10 clears) ·
  `CRITIC` (25 ranked) · `SPRING CLEANING` (10 retired) · `NIGHT SHIFT` (an all-night draw)
- **Settings** — theme/skin, haptics toggle, sound toggle, clipboard-detection toggle,
  data & privacy, IGDB attribution with live link, **RevenueCat Customer Center** for
  subscription management, restore purchases

---

## 8. Onboarding (must be under 40 seconds)

1. **Cold open** — the CRT powers on, `CONTINUE?` glows, coin-slot sound. No sign-up.
2. **"How big is your pile?"** — three buttons: `UNDER 20` · `20–100` · `DON'T ASK`.
   The last one is the honest answer and gets a knowing response. Sets expectations and
   makes the user smile in the first 10 seconds.
3. **Seed the pile** — `IMPORT FROM STEAM` · `SEARCH FOR GAMES` · `SKIP`.
   Steam import here is what makes the app immediately real.
4. **First draw** — push them straight into a DRAW with whatever they added. The aha moment
   must happen inside the first session, not on day three.

No account creation, no email, no permissions requested up front. RevenueCat runs on its
anonymous app-user ID until/unless the user opts into cloud backup.
