# CONTINUE? — Build Status

> **Read this first in any fresh session**, after `CLAUDE.md` and before `09-PENDING-INPUTS.md`.
> This is the living ledger of what's actually built, what's verified, what's broken, and
> what's next. Update it whenever you finish a chunk of work or discover something that
> changes this picture — don't let it go stale like a comment nobody re-reads.
>
> Last updated: 2026-08-15 — a **second phone-test feedback round** (the first build Mikhil has
> run on both a phone and the tablet). Five items reported, all layout/ergonomics; four needed
> work, all four are fixed below. The paywall and the new dispenser deal animation were both
> called out as working well on glass — **those two are now device-verified**, which closes the
> single biggest gap from the previous entry.
>
> **A signed `versionCode 4` / `0.4.0` `app-release.aab` is built and current** at
> `app/build/outputs/bundle/release/` (32.4 MB, signer cert valid to 2056), carrying everything
> below. `versionCode 3` was **never uploaded** — upload this one instead.
> Rebuild after any further app change: `JAVA_HOME=/c/Android/jdk21/jdk-21.0.12+8 ./gradlew bundleRelease`.

---

## 2026-08-15 (later) — the closed-testing paperwork, written end to end

No app code changed. This session attacked the *other* half of the critical path: the store
listing, which is what actually blocks a closed-test rollout (an internal-testing upload needs
almost no paperwork, which is why `versionCode 1` sailed through and why this looked deceptively
close to done).

**New: `docs/13-STORE-LISTING.md` — the answer sheet.** Every Play Console field with its answer
already written: app name/short/full description (length-checked against Play's 30/80/4000
limits), category and tags, the Data Safety declaration, the IARC content-rating answers, target
audience, the advertising-ID declaration, release notes, and the order the sections have to be
filled in to unblock fastest.

**The Data Safety form was derived from sources, not guessed**, because it's the most common
rejection cause here and a wrong answer outlives the mistake. Google's published Mobile Ads SDK
disclosure gives four of the six declarations (device IDs, app interactions, diagnostics, and
IP-derived approximate location — all *shared*); RevenueCat's own docs give purchase history
(collected, not shared, not linked, since our app user IDs are anonymous). The sixth is ours:
search terms and shared captions reach the Worker, and the KV cache outlives the request, so
"in-app search history" is declared rather than claimed ephemeral. Explicitly *not* declared:
photos — shared images are OCR'd on-device by ML Kit and never leave the phone, and the app has
no photo-library permission at all.

**Privacy policy + landing page written and committed** as `docs/privacy.html` and
`docs/index.html` (plus `docs/.nojekyll`), styled in the app's own palette. Publishing is a
`git push` and one GitHub Pages toggle (main → `/docs`), landing at
`https://mikhil-sec.github.io/GamesToPlay/privacy.html`. Two claims were caught and removed
while writing it, both of which would have been false:
- an EEA/UK **consent flow** — the app has no CMP at all (nothing references
  `UserMessagingPlatform`), which is a real gap under Google's EU user consent policy and is now
  logged as such rather than papered over;
- a **RevenueCat app user ID shown on the YOU tab** as the handle for deletion requests — it
  isn't displayed anywhere, so the policy asks for the Play order ID instead.

**Store graphics generated:** `store/icon-512.png`, `store/icon-1024.png` and
`store/feature-graphic-1024x500.png`, produced by `tools/StoreAssets.java` (plain JDK 21 + AWT,
no build, no dependency) from the launcher icon's own path coordinates, `Color.kt`, and the
bundled Chakra Petch faces — so the store mark and the installed icon are the same artwork and
can't drift. All original: no box art, no third-party logo, no influencer branding.

**Still needs a device: the screenshots.** `adb devices` was empty again this session.
`tools/capture_screenshots.sh` prompts through the shot list and captures with `exec-out`; they
must come from `versionCode 4` on a *phone*, since the 2026-08-15 layout round changed every
screen worth showing.

**Listing copy is scoped to the shipped build on purpose.** The Devpost draft in
`docs/07-SUBMISSION-KIT.md` sells Steam import, Free Play Mode and five share cards; one is
disabled and two don't exist. The Play description covers only what `0.4.0` actually does.

---

## 2026-08-15 — phone vs tablet: the layout round

Mikhil ran `0.3.0` on both a phone and the Galaxy Tab and reported that **the same build looks
right on the tablet and wrong on the phone** — the second time this project has learned that a
wide device hides an entire class of bug (see 2026-08-14 item 1, and bug #9 in §3). Everything
here is a fix to *shared* chrome, so all of it improves the tablet too.

**Confirmed working on a device, first time:** the GO PRO paywall ("looks REALLY good") and the
card-dispenser deal animation. Offline mode was also exercised — it works, with the caveats in
§Offline below.

### 1. The coin counter was taxing every screen 52dp of header

It lived in `ArcadeScaffold`'s top bar on all four tabs. On a tablet that's free; on a phone it
pushed PILE's tabs visibly down and made the whole screen read as starting too low. It now
appears **only on DRAW (where coins are spent) and YOU (the account screen)** —
`ArcadeScaffold(showCoinCounter =)`, decided in `ContinueNavHost` from the current route.

**The subtle part:** the top bar still renders when the counter is hidden, as a zero-content
`Row` that consumes `WindowInsets.statusBars`. Deleting the bar outright would have been the
obvious move and would have re-broken 2026-08-14 item 2 — the app draws edge-to-edge, so with
no top bar the content doesn't sit *higher*, it slides *under the clock*.

### 2. The raised DRAW button was never centred on its own slot

On the phone the gold DRAW circle sat on top of the "DISCOVER" label. Root cause: the bar has
**two items on the left and one on the right**, so its empty middle column is centred at
**62.5%** of the width — but the button was `align(TopCenter)` on the whole bar, i.e. **50%**.
It was always 12.5%-of-screen-width too far left; a tablet's 240dp-wide columns simply had room
to absorb the error and a phone's 90dp ones did not.

Fixed by making the button a **real child of the nav Row** (a weighted `Box` in the middle
slot), so it self-centres on its own column whatever the item counts are. It still uses
`offset`, never `padding`, for the -24dp raise — see bug #9.

Nav labels also got smaller and tighter (10sp/0.02em, `maxLines = 1`, ellipsised, clamped to
the column). "DISCOVER" is the widest word in the bar and at a raised system font scale it
outgrew its own column, which is what made the collision visible rather than merely present.

### 3. PILE is now one scrolling surface, not a fixed header over a scrolling grid

Reported on **both** devices: expanding SORT & FILTER ate the screen and you could never scroll
*past* the filters — only the covers moved. Tabs, the time-budget bar, the action row and the
filter chips are now **full-span items inside the LazyVerticalGrid / LazyColumn** rather than a
`Column` wrapped around them. Same fix in both view modes; the empty state became an item too.

All the lazy lists in the app (PILE, DISCOVER search, DISCOVER rails, YOU) also picked up 32dp
of bottom `contentPadding` — the raised DRAW button overhangs the nav bar by 24dp and was
sitting on top of the last row.

### 4. DRAW's dials could crowd the lever out of existence

On a phone the three dial sections wrap to eight-plus rows of chips once a pile has a realistic
platform list, leaving the lever a pink sliver that **could not be dragged at all** — the
signature interaction of the app, unreachable. Three changes:

- The dials **collapse**, defaulting to collapsed on short screens (measured with
  `BoxWithConstraints`, not a hardcoded phone/tablet breakpoint, so landscape is covered too).
- When collapsed, a one-line summary (`2 HOURS · STORY · 3 PLATFORMS`) keeps the settings
  visible — same rule as PILE's active-filter count: **hiding a control must never hide its
  state**, or a draw comes back filtered by something the user can't see.
- The lever is **measured before** the dials region and so always keeps its full pull height;
  the dials scroll in whatever space is genuinely left. Collapsed, the lever centres in the
  whole cabinet, which is the better screen anyway — one thing to do, in the middle.

The pull threshold became a **fraction of the track** (0.68) instead of a hardcoded 150dp, so
the short-screen (<560dp) 150dp track still commits at the same relative point.

### Offline — covers now survive it

Mikhil's offline pass found the pile works but **no cover art loads**. Room held the games;
every cover still went to the network, so an offline pile was a wall of grey. Two fixes:

- `ContinueApplication` now implements `SingletonImageLoader.Factory` with an explicit **192MB
  Coil disk cache** (plus a 25% memory cache and crossfade). An IGDB cover URL contains the
  image's own hash, so a cached cover can never be stale — only ever absent.
- `GameCard` paints the title's initial **underneath** the cover unconditionally, so a card
  with no art yet reads as the game instead of an empty slab. It used to draw the initial only
  when `coverUrl == null`, which is the one case that *isn't* the common one.

This does not make first-ever-seen covers appear offline — nothing can — and offline search is
still limited to the on-device index, which Mikhil correctly judged acceptable.

**Verified:** `assembleDebug` + `test` green (both variants), `bundleRelease` signed as
`versionCode 4`. **Not verified on a device — `adb devices` was empty for this whole session.**
Every item above is a layout change reasoned from the screenshots, so the next device run
should start on a *phone*, on PILE and DRAW.

---

## 2026-08-14 (later) — the offline index is now real, and wired in

Continuation of the same day's session. `TWITCH_CLIENT_SECRET` was pasted into
`worker/.dev.vars` (created earlier this session, previously blocked on this one paste) and
`node tools/igdb_dump_index.mjs` was run for real.

**One bug found immediately, fixed before shipping anything:** the built index's `year` column
was the literal string `"NaN"` for every row. Cause: the CSV data dump encodes
`first_release_date` as an **already-formatted datetime string** (`"2023-08-15 00:00:00"`),
while the REST API (what `IgdbGameProvider.ts` talks to) returns the **same field name** as a
Unix timestamp. The indexer copied the REST-API parsing (`Number(value) * 1000` into `Date`),
which doesn't throw on a non-numeric string — `Number("2023-08-15…")` is `NaN`, and `new
Date(NaN).getUTCFullYear()` silently returns `NaN`, which then serializes as the string "NaN".
Exactly the shape of bug this project has now hit three times (see bug #10 in §3 below, and the
`backgroundUrl: null` miss from earlier today): **a field with the same name means a different
encoding on a different surface, and nothing errors when you get it wrong.** Fixed in
`tools/igdb_dump_index.mjs`'s new `parseDumpYear()`, which is now the one place in the repo
that knows the dump's date format differs from the API's. Rebuilt after the fix: **0 rows** with
"NaN" confirmed by direct check of the output file.

**What the pipeline actually produced**, downloading all four dumps for real (~350MB total,
resumed cleanly through the 5-minute presigned-URL expiry, no sandbox issues this time):

| Dump | Size | Rows kept |
|---|---|---|
| `games` | 289.8 MB | 17,095 (of 372,146 scanned — `game_type=0`, no version parent, ≥3 ratings) |
| `covers` | 41.0 MB | 332,444 indexed |
| `alternative_names` | 18.4 MB | 24,398 attached |
| `game_time_to_beats` | 0.9 MB | 7,362 indexed |

Output: `app/src/main/assets/game_index.tsv.gz`, **0.58 MB gzipped** — a trivial APK-size cost
for 17k games' worth of exact + alternative-name matching.

### Wired into the app, not left as a built-but-unused asset

Building the index was necessary but not sufficient — the point was to fix share matching, and
that meant actually replacing the matching path. New:

- **`core/util/GameNameCandidates.kt`** — Kotlin port of `worker/src/resolve/candidates.ts`'s
  `rankedCandidates()`/`verifyAgainstText()`/`normalize()`. This logic had no prior Kotlin
  mirror (only `titleParser.ts`'s candidate *extraction* was ported, as `TitleParser.kt`,
  deliberately left unchanged this session). Kept behaviourally identical to the TS on purpose:
  9 tests in `GameNameCandidatesTest.kt` mirror `worker/test/candidates.test.ts` line for line,
  same fixture captions (`PALWORLD`, `LEAGUE`), same assertions.
- **`core/offline/OfflineGameIndex.kt`** — loads the gzipped TSV from assets, builds a
  normalized-name -> record map (every alternative name too), and mirrors `resolveGame.ts`'s
  `matchCandidates()`: try ranked candidates in order, score every hit against the *original*
  text (never the candidate that found it — same anti-false-positive rule as the server), stop
  early once confident. The one structural difference from the server: "search" is an exact
  dictionary lookup instead of an IGDB API call, because the whole dictionary is on-device now.
  Parsing is factored into a pure `internal fun parse(InputStream)` specifically so
  `OfflineGameIndexTest` can exercise it against the **real shipped asset file** from a plain
  JVM test — no Android/Robolectric needed — including a regression test that fails if "NaN"
  ever reappears in the shipped file, and a live check that `"BG3"` resolves via
  `alternative_names` (the whole reason this pipeline exists).
- **`ShareTargetViewModel`** now tries the offline index *first*, before any network call. A
  confident offline hit (`GameNameCandidates.CONFIDENT_ENOUGH = 0.9`, same threshold as the
  Worker) skips the Worker resolve entirely — **CLAUDE.md constraint #5 (offline-first) is now
  actually true for the headline demo feature**, not just claimed for it, and shares in
  airplane mode work. A weaker or absent offline hit falls through to the existing network
  path, and the two candidate lists are merged by IGDB id (safe: both are the same `games`
  table, one read via REST, one via CSV dump), keeping whichever confidence is higher per id.
- **`OfflineGameIndex` is warmed up at app startup** (`ContinueApplication.onCreate`, same
  pattern already used for `SeedLoader`) so decompressing ~41k rows doesn't happen on the
  critical path of the first share.
- **`IgdbImage.coverUrl(imageId, size)`** added — the dump's `covers` table gives a bare
  `image_id`, not a URL, so offline matches need to build one the same way the Worker does.

**Not done, and deliberately out of scope for this pass:** the offline record only carries
`id`/`name`/`coverImageId`/`ratingCount` — not year/rating/hours, even though the index has
them. Enriching a `GameEntity` with that data on add (for offline-matched games specifically)
would be a real improvement over the current "bare id+name+cover, then null forever" add flow —
but that flow is pre-existing for *every* share-added game, offline or online, so fixing it
belongs to its own pass rather than riding along here.

**Verified:** `assembleDebug` + `test` green (14 new tests, all passing, both variants), worker
`tsc`/tests untouched and still green, `versionCode 3` `.aab` rebuilt with the new asset and
re-signed. **Not yet verified on a device** — nobody has shared a real caption against a phone
running this build.

### An unrelated pre-existing lint error, found and (mostly) fixed while verifying

Ran `./gradlew lint` as an extra check before finalizing — not previously part of this
project's verification loop. Found one real lint **error** (not warning), pre-existing and
unrelated to anything this session touched: `feature/share/PileShareScreen.kt`'s `produceState`
call tripped `ProduceStateDoesNotAssignValue`. Traced it and confirmed it was **functionally
harmless** — `PileShareViewModel`'s `isLoading` flips `true`→`false` exactly once, which re-keys
and reruns the producer regardless — but genuinely fragile shape (a conditional-only
`value = …` is a real bug the moment a key can change more than once, which is exactly what the
check exists to catch). Rewrote to extract the branch into a `pileCardOrNull()` helper so the
lambda has one unconditional assignment. **The lint error still fires on the rewritten code**,
even after three structurally different attempts and a `--rerun-tasks` to rule out caching —
this looks like a limitation of this lint-checker version with this particular
`produceState<T>(initialValue=…, key)` overload/generic-argument shape, not a real bug. Left as
a known-harmless, unresolved lint finding rather than sunk further time into it; `lint` is not
part of the app's actual verification loop (`assembleDebug`/`test`, both green, are).

---

## 2026-08-14 — first phone test, the paywall, and IGDB data dumps

Everything before this was tested on a Galaxy Tab S6 Lite. The first run on a phone found a
**layout break that a tablet structurally could not surface**, which is the headline lesson of
the session.

### Nine reported items, and what changed

1. **PILE's header was broken on a phone.** The five state tabs and three icon buttons shared
   one un-scrollable `Row`. A `Row` hands leftover children zero width rather than wrapping, so
   `NOW PLAYING` rendered as a squeezed vertical sliver and the last three tabs were
   **unreachable entirely**. Tabs are now a full-width `horizontalScroll` strip of their own,
   with the actions moved to their own row below.
   **Lesson, and it rhymes with bug #9:** a wide test device can hide a layout bug indefinitely,
   the same way a screen that hides the bottom bar hid a crash in it. Test the narrow case.
2. **Coin counter sat under the status bar.** `enableEdgeToEdge` is on but neither bar consumed
   insets. `ArcadeScaffold`'s top bar now takes `statusBars` padding and the nav bar takes
   `navigationBars` padding — background first, padding second, so the cabinet colour still runs
   edge to edge behind both.
3. **Filters can now be collapsed** (§1 of the report). Sort + filter chips cost four rows on a
   phone, more than the games did. They're behind a `SORT & FILTER` disclosure, collapsed by
   default, which carries the **active-filter count** so a hidden filter can never silently
   explain an empty-looking pile. The GRID/LIST pair also became one button showing the mode
   you'd switch *to*, reclaiming 48dp of header.
4. **Covers were pixelated on the DRAW card** (§2). The Worker serves one `t_cover_big` URL —
   **264x352** — which is fine in a 3-column grid and ~3x upscaled on the DRAW card. The app now
   rewrites the size token per surface (`core/util/IgdbImage.kt`, which carries the measured
   size/byte table).
5. **Credits Roll key art was much worse** (§4), and for a different reason: `backgroundUrl` was
   **hardcoded `null`** in the Worker, so it fell back to painting that same 264px cover across
   a whole screen. Now populated from IGDB `screenshots`/`artworks`. See §Key art below — the
   obvious ordering of those two is the wrong one.
6. **The share sheet had no exit** (§9). No X, no outside-tap dismiss; `windowIsFloating=false`
   means the platform never gives you `windowCloseOnTouchOutside`, so the only way out really
   was to kill the app. Added a scrim that dismisses on tap and an X in the header.
7. **Sharing from TikTok/YouTube yanked the user into the app** (§8). Root cause was
   `android:launchMode="singleTask"` on `ShareTargetActivity`: singleTask forces the activity to
   be the root of its own task, so the system runs a task-switch animation instead of drawing
   over the caller. Removed (back to `standard`) + `taskAffinity=""`, so the sheet is pushed
   onto the *sharing app's* task and Back returns to the video.
8. **The DRAW deal animation** (§3) went from three grey rectangles sliding up — which read as a
   spinner — to cards **ejected one at a time from a dispenser slot**: squashed inside the
   machine, sprung past their resting place, fanned on settle, with the slot pulsing as each one
   passes. Built from transforms only, no art. A literal slot-machine reel rig was considered
   and **not** built: it needs real art to not look cheap, and the request was explicitly "only
   if you're confident". `DEAL_ANIMATION_MS` is now shared with `DrawViewModel` so the phase
   can't end mid-eject the way a hardcoded 900ms would have.
9. Everything else in the 0.2.0 build was reported working.

### Key art — screenshots beat artworks, counterintuitively

Artwork is promotional key art with no HUD, so it looks like the obvious pick. Its aspect ratio
is unconstrained: Elden Ring's first artwork returns from `t_1080p` as **1920x295**, an
ultrawide banner that upscales ~8x when cropped into a portrait phone — worse than the cover it
replaced. Screenshots are captures and are dependably 1920x1080. So: **screenshots first,
artworks as fallback.** Verified against live responses, not the schema.

Games added before this change have `backgroundUrl == null` in Room forever, since nothing
re-reads a game once it's in the pile. `CreditsRollViewModel` now backfills key art on demand
via a new `/games/{id}` data-source method, after the state emit so the cinematic still starts
on time.

### The paywall is built (Week 4's biggest gap) — hand-designed, not RevenueCatUI

`feature/paywall/` is a **custom Compose paywall**, reachable from the DRAW gate's GO PRO
**and** from a new banner on YOU — previously the only route to it was exhausting the daily
free draw, so a judge could easily never have seen it.

**Why not RevenueCatUI.** It was built with RevenueCatUI first and replaced the same session.
A dashboard-rendered paywall can be re-themed without shipping an APK, which is real value —
but its templates can't produce the arcade cabinet this app is, and a paywall that looks like
every other RevenueCat paywall is a bad answer in a design category. The part of the
dashboard's value that actually mattered is kept: **prices are still read from the store at
runtime** (`ProTier.priceFormatted` is Google's own `Price.formatted`), so a price change in
Play Console still needs no code change, and it is localised for free.

On the screen: a pulsing cabinet marquee, four perks that are each a capability the shipped
build really has (with the free-tier limit each one removes stated, so the value is checkable),
selectable Monthly/Lifetime tier cards, a CTA whose label follows the selection, plain-words
subscription terms, and RESTORE PURCHASE.

Three decisions worth not re-litigating:
- **Lifetime is preselected.** Monthly exists largely to make Lifetime look obvious
  (docs/07-SUBMISSION-KIT.md §Pricing rationale), so the default should be the one we want
  taken — and the one that isn't a recurring charge someone has to remember to cancel.
- **Renewal terms are on the screen, not only in the store listing.** Play policy requires it,
  and a trial that converts silently is the fastest route to a refund and a one-star review.
- **A cancelled purchase shows no error.** A cancel is a choice, not a failure; treating it as
  an error is how a paywall starts feeling hostile.

**`core/billing/ProTier.kt`** is the new seam: `feature/paywall` never imports a RevenueCat
type. That is what lets `FakeBillingRepository` return convincing tiers, so **the paywall is
fully demoable and screenshottable in a debug build**, which cannot reach Play Billing at all.
The real implementation flattens RevenueCat's
`Package` -> `StoreProduct` -> `SubscriptionOption` -> `PricingPhase` chain, which is also
where the free-trial length actually lives (it is *not* on the product).

`DrawViewModel.goPro()` is **gone**. It used to buy `currentOfferingPackage()` — whatever
happened to be first in the offering — with no price shown and no choice between tiers.

`restorePurchases()` was added to `BillingRepository` at the same time. Play requires a restore
path for non-consumables and Lifetime is one; without it a reinstall silently loses a real
purchase.

### IGDB data dumps — pipeline built, not yet run

Access was granted for our Client ID on 2026-08-14. `tools/igdb_dump_index.mjs` is written and
syntax-clean but **has never been executed**, because it needs `TWITCH_CLIENT_SECRET`, which
correctly exists only inside `wrangler secret put`. It reads from `worker/.dev.vars`
(gitignored) — one paste unblocks it.

The important architectural decision is recorded in docs/08-GAME-DATA.md: **the index ships in
the app, not the Worker**, because Workers Free allows only **10ms of CPU per request** and
scanning a multi-megabyte index blows that on the first request into every cold isolate.

Worker changes this session: `backgroundUrl` populated, `CACHE_VERSION` v2 → v4. Deployed and
verified against live traffic (all three rate limiters still bound). 28/28 Worker tests pass,
`tsc --noEmit` clean, `assembleDebug` + `test` green. **Nothing in this session has been run on
a device** — `adb devices` was empty throughout.

---

## 2026-08-12 (last) — security audit before making the repo public

Full pass ahead of pushing to the public GitHub repo. **Findings and fixes are written up in
`docs/12-SECURITY.md`** — read that, not this summary, before touching the Worker or secrets.

- **Secrets: clean.** No secret file has *ever* been committed (whole-history scan). Every live
  secret file is gitignored. The only secret-shaped strings in history are the RevenueCat
  *public* key and `getProperty("storePassword")` call sites.
- **The Worker had no abuse controls at all** — a public, unauthenticated endpoint on free-tier
  quotas. The binding constraint is **KV writes: 1,000/day**; one uncached search is one write,
  so ~1,000 scripted queries would have killed caching for the day and dumped everything onto
  IGDB. Added three rate limiters (per-IP, a tighter one for the 6x-amplifying `/resolve`, and
  an account-wide backstop against distributed abuse), amplification caps, and input/body size
  limits.
- **Real SSRF fixed.** `/resolve` fetches client-supplied URLs and gated them with
  `hostname.includes("tiktok.com")` — so `vm.tiktok.com.attacker.example` passed and could have
  aimed our server-side fetch at any host. Now dot-anchored matching, https-only, with tests.
- **`/steam/owned` was the worst amplifier**: unbounded `Promise.all` over a whole Steam
  library turned one request into thousands of IGDB searches. Capped at 100, sequential.
- **CORS `*` removed** (it let any web page use us as a free games API) and internal error
  messages no longer leak to clients.
- **Android:** `usesCleartextTraffic="false"` added. Backup rules confirmed to exclude
  DataStore, so backup/restore can't duplicate coins.

> **Repeat of a lesson this project keeps learning:** the first rate-limiter deploy **silently
> did nothing** — wrangler v3 ignores `[[ratelimits]]` without warning, and 150/150 requests
> still returned `200`. Only firing real traffic caught it. Fixed by upgrading to wrangler v4
> (needs `@cloudflare/workers-types@5` in the same install). Same shape as the IGDB `category`
> bug: **config that looks applied but isn't.** Always verify with traffic, never with source.

Worker test count is now **28** (added `test/security.test.ts`).

---

## 2026-08-12 (later) — first real device-feedback pass, six fixes

Mikhil tested the `versionCode 1` build against the fixed Worker and reported six things. All
six are fixed; this is the first round of changes driven by someone actually using the app.

1. **Tapping a game in PILE silently teleported it to NOW PLAYING.** A BACKLOG card's `onClick`
   called `moveToPlaying` directly, so the card just vanished from the list with no
   confirmation and no visible undo — it read as a bug, not an action. Both tap and long-press
   now open the same **MOVE TO** chooser offering all five states (NOW PLAYING / CLEARED / THE
   PILE / WANTED / RETIRED), each with a one-line description, with the game's current state
   shown as a disabled row rather than hidden so the list never reshuffles. CLEARED still
   routes through the Credits Roll, and NOW PLAYING still routes through `moveToPlaying` so
   the cap-of-3 swap prompt survives. All five `PileState` values already existed — only the
   menu was incomplete.
2. **Coin balance was invisible.** `CoinCounter` existed but was never placed. It's now in a
   top bar in `ArcadeScaffold` (plus a `PRO` badge), fed by a new `AppChromeViewModel`, and
   hidden on the same immersive routes as the bottom bar.
3. **Rewarded ad played but granted nothing** — `RealBillingRepository.earnCoins()` and
   `spendCoins()` both returned a hardcoded `SpendResult.Error` about missing server-side
   verification, so the entire coin economy was dead in release builds. Now backed by a real
   on-device `CoinLedger` (DataStore). See §Coins below for why local is the right call.
4. **Share target matched nothing from a YouTube title** — the big one, see §Share matching.
5. **Manual-entry field prefilled a whole video caption** with no way to clear it. Added a
   trailing clear icon and a placeholder.
6. **IGDB partnership reply arrived.** IGDB confirmed we're a commercial use and asked seven
   questions to start a (free) agreement. See `docs/11-IGDB-PARTNERSHIP.md`.

**Also fixed, unprompted — an actual compliance gap:** the mandatory IGDB attribution existed
only on the YOU tab, which shows *no* IGDB data, while DISCOVER (search results, both rails,
all cover art) had none. Attribution added to DISCOVER. This is contractual under the
partnership terms, not cosmetic.

**AdMob ad unit ids are now `BuildConfig` values** (`ADMOB_UNIT_COIN`,
`ADMOB_UNIT_FREE_PLAY`) read from `local.properties`, defaulting to Google's public test
units. Real units exist but won't fill ads until the app is live on Play, so test units stay
correct for now — and the switch is a config edit, not a code change.

### Share matching — why it failed and what changed

Measured against the live IGDB API, **`search` is near-exact, not fuzzy**:

| Query | Results |
|---|---|
| `Each Pal has their own method of transporting items Pocketpair Palworld` | 0 |
| `Pocketpair Palworld` | **0** |
| `Palworld` | 3 |
| `a tale of two bush ganks League of Legends` | 0 |
| `League of Legends` | 11 |

One extra word kills the query. We were sending IGDB the entire caption, so it essentially
never matched — even though the game's name was sitting right there in the string.

New `worker/src/resolve/candidates.ts` builds an **ordered shortlist of substrings** to try
(hashtags → whole capitalized runs → windows inside those runs → trailing n-grams → the full
caption), and `resolveGame` searches the top 6 with an early exit. Crucially, every hit is
scored by `verifyAgainstText` — **does this game's name actually appear as whole words in the
original caption?** — which is what makes searching a single word like `"Palworld"` safe.
Longer matched names beat shorter ones, and roman numerals are folded (`III` ⇄ `3`).

Verified live after deploy:

| Caption | Match |
|---|---|
| `…items📦 Pocketpair Palworld` | **Palworld** (0.85) — beats the real game *"Pal"* |
| `a tale of two bush ganks 🤔 League of Legends` | **League of Legends** (0.99) |
| `Baldur's Gate 3 romance guide` | **Baldur's Gate III** (0.97) |
| `Final Fantasy VII Rebirth is peak` | **Final Fantasy VII** (0.97) |
| `my cat sat on the keyboard lol` | *no match → manual entry* ✅ |

Resolve's searches now also go through the KV cache under the same key space as
`/games/search`, so a share warms the search cache and vice versa — without that, one share
could spend 6 uncached IGDB requests against a 4 req/sec ceiling.

`titleParser.ts`/`TitleParser.kt` were deliberately **not** changed, so their intentional
Kotlin/TypeScript mirroring and 13 shared test cases still hold. 9 new tests cover the new
module, built from the two real captions captured off Mikhil's device.

### Coins — deliberately on-device, and why

`CoinLedger` (DataStore) is now the source of truth for the COIN balance, not RevenueCat.
Two reasons, both structural:
- **CLAUDE.md constraint #5 is offline-first**, and coins gate DRAW — the app's central
  action. A server-authoritative balance makes the core loop fail on a train.
- **RevenueCat virtual currency cannot be credited from a client at all.** Granting goes
  through the REST API with the *secret* key, which must never ship in a public repo's APK.

So: coins **earned** in-app are authoritative locally; coins **purchased** are granted by
RevenueCat server-side and folded in via `creditPurchased`. This is not fraud-proof — clearing
app data resets it — and that's an accepted trade-off until AdMob server-side verification is
possible (which needs real ad units, which need a production Play listing). Everything is
shaped so that swap changes the *source*, not any call site: features only ever talk to
`BillingRepository`.

---

## 2026-08-12 — DISCOVER and search returned zero games (fixed)

Mikhil installed the Internal-testing build and found **no games at all** — empty DISCOVER,
empty search. Before IGDB was wired up, the seed set at least showed placeholder titles, so
this was a regression. Two independent bugs stacked, and either one alone would have been
survivable:

1. **Worker (root cause).** Every list query filtered on `where category = 0`. IGDB
   **deprecated `category` in favour of `game_type`** and stopped populating it. Filtering a
   deprecated field is not a syntax error — IGDB answers `200 OK` with `[]`. So
   `/games/search`, `/games/trending`, and `/games/short` all returned zero results with no
   error anywhere. Fixed by switching to `game_type = 0`. See bug #10 in §3.
2. **App (why it was total, not partial).** `WorkerGameDataSource` used
   `runCatching { … }.getOrElse { seedFallback() }`. A **successful** response carrying an
   empty list is not an exception, so the fallback never fired and 426 seeded games sat unused
   in Room while the UI showed nothing. Fixed so empty is treated like failure. See bug #11.

**Three things made this hard to see, and all three are now fixed:**
- `/health` only asserted that credentials *existed*, so it cheerfully reported
  `{"ok":true,"provider":"igdb"}` for a Worker serving nothing. There is now a
  `/health?deep=1` that runs a real query and returns `ok:false` on zero results.
- `cached()` stored the empty arrays for 6–24h, so the bug outlived its own cause. It now
  **refuses to cache empty lists**, and cache keys are **versioned** (`CACHE_VERSION` in
  `kv.ts`) so a query-shape change invalidates everything at once.
- Nothing failed loudly. IGDB's error body is now included in thrown errors.

**Fixed in the same pass, because the data was finally real enough to judge:**
- **Playtimes are now populated.** Every list result carries real `hastily`/`normally`/
  `completely` hours from `/game_time_to_beats`, fetched in **one** extra batched request per
  list rather than one per game. They were all `null` before — which quietly starved the time
  budget and DRAW's TIME dial, the two features that most need them.
- **TRENDING is no longer unreleased games.** It sorted by `hypes`, which counts *pre-release*
  follows, so a backlog app was recommending games nobody can play yet. Now: released in the
  last 3 years, sorted by `total_rating_count`.
- **SHORT & SWEET is now actually short.** It approximated "short" with `aggregated_rating >
  75`, which is a quality filter wearing a length costume. Now sourced from real time-to-beat
  data under 8 hours — returns Portal (4h), Journey (3h), Limbo (4h), Inside (4h).
- **Removed the `PULL THE LEVER` placeholder onboarding step**, which shipped to testers
  showing them internal text: *"is a Week 3 build (docs/06-BUILD-ROADMAP.md)"*. DRAW has been
  fully built since 2026-08-06, so the step was stale as well as embarrassing. Onboarding now
  ends at SEED, and **"SEARCH FOR GAMES" lands on DISCOVER** instead of dropping the user on
  an empty PILE to find search themselves.

**Verified after deploy** (real responses, not assumptions): `/health?deep=1` →
`{"ok":true,"sampleCount":20}`; search "elden ring" → Elden Ring with cover art and 45h/119h/
174h playtimes; trending → Clair Obscur, Silksong, Balatro; short → Portal 4h, Journey 3h.

**Consequence worth knowing:** the root fix was *server-side*, so the **already-uploaded
`versionCode 1` build now shows games without reinstalling.** The new `versionCode 2` build is
needed only for the app-side items (empty-list fallback, onboarding cleanup).

---

## 2026-08-11 infra session — what changed (credentials/backend, not app code)

No app code changed this session. What moved:
- **RevenueCat dashboard fully configured** via MCP: 6 products (3 subscriptions, 3
  consumables), 2 offerings (`default` is_current, `coins`), 6 packages, entitlement
  attachments, COIN auto-grants. Exact ids and the required matching Play Console product
  table are in `docs/09-PENDING-INPUTS.md`. Pricing was revised down from the original spec:
  `continue_pro_lifetime` $39.99→$9.99, `coins_500` $6.99→$4.99.
- **Cloudflare Worker deployed for real**, `https://continue-worker.gamestoplay.workers.dev`,
  with Twitch secrets set — `/health` confirms the **real IGDB provider is live**, not the
  seed fallback. This means Share Target auto-match (§ below) now actually works end-to-end.
- **First signed release App Bundle built**: `app-release.aab` (`versionCode 1`,
  `versionName "0.1.0"`) at `app/build/outputs/bundle/release/`, ready for Play Console
  Internal testing upload. Not yet confirmed uploaded as of session end.
- **Correction to the Play Store critical path**: the 12-tester/14-day closed-testing gate is
  **per-app, not per-account** — Mikhil's other app on the same account hasn't cleared it
  either, so CONTINUE? has no head start. This is now flagged as the single biggest schedule
  risk — see `docs/01-PLAY-STORE-CRITICAL-PATH.md`.
- Play Console setup (products, purchase options) was in progress when the session ended —
  **status of the 6 Play-side products is unconfirmed**, check `docs/09-PENDING-INPUTS.md`.

---

## TL;DR

**Phase 1 (Weeks 1–2) and most of Phase 2 (Weeks 2 remainder, 3, and partial 4/5) are built,
and now confirmed running on a real device**, not just compiling. This session added the
three signature Week 3 features (DRAW, Credits Roll, pairwise ranking) in full, plus the
Week 2 remainder (Stacks CRUD, GRID/LIST toggle, platform/genre/length filters), the
DRAW-side economy loop from Week 4 (CONTINUE? gate, coin spend, ad-watch-for-coin, GO PRO
trigger — all against `Fake`/real-but-gracefully-empty billing), and a first slice of Week 5
(YOU tab stats/trophies, one working share card). See §6 for the precise per-week ledger,
including what's still genuinely missing in each week.

**One real crash was found and fixed installing on-device** (negative `Modifier.padding` on
the raised DRAW button — see §3 bug #9). After the fix, the app installs, launches, and the
bottom nav bar (with DRAW button) renders without crashing on a Samsung Galaxy Tab S6 Lite.
**That's as far as on-device verification got this session** — the app was confirmed alive
and in the foreground (no further crash in logcat), but nobody had manually tapped through
DRAW's lever, the swipe cards, Credits Roll, RANK, Stacks, filters, or the share card on the
device before the session ended (context window ran long, session was wrapped up to hand off
fresh). **A fresh session should pick up by asking Mikhil what he's tested and finish tapping
through the rest**, fixing whatever else logcat turns up the same way bug #9 was fixed. Treat
anything not explicitly listed as "on-device confirmed" in §2 as compile-verified only, not
interaction-verified.

**Not built this session, and worth knowing before you assume otherwise:** the STACK 3D
"wow" view (GRID/LIST only), the RevenueCatUI paywall screen itself, FREE PLAY mode, Customer
Center, 4 of the 5 share card types, the full Time Budget tap-to-expand visualization, and the
clipboard nudge's actual detection logic (the settings toggle persists, but nothing watches
the clipboard yet). None of these are blocked on credentials — they're just not built yet.

**Also worth knowing:** the Android Share Target (docs/02-PRODUCT-SPEC.md §2a, the "share
from YouTube/TikTok" feature) is fully built, registers correctly in the OS share sheet, and
**its auto-match now works for real** — the Worker is deployed and, since the 2026-08-12 fix
above, `/resolve` has actual IGDB games to match against (it was matching against nothing
before, so every share silently fell through to manual entry). This is the headline demo
feature and it has never been exercised on-device; worth putting near the top of the
device-testing pass.

---

## 1. Local build environment (now set up, don't redo this)

| Thing | Path |
|---|---|
| Android SDK | `C:\Android\sdk` (Platform 36, Build-Tools 35.0.0, Platform-Tools) |
| JDK for Gradle | `C:\Android\jdk21\jdk-21.0.12+8` (Temurin 21 — **required**, the JDK 25 on this machine is too new for Gradle 8.10.2) |
| `local.properties` | Has `sdk.dir=C:/Android/sdk` and the **real** RevenueCat public key |

**To build:** always set `JAVA_HOME` to the JDK 21 path first, e.g. in Git Bash:
```bash
export JAVA_HOME="/c/Android/jdk21/jdk-21.0.12+8"
cd "/c/Dev/GamesToPlay"
./gradlew assembleDebug     # or assembleRelease, test, etc.
```
Without that `JAVA_HOME` override, `gradlew` will pick up the system JDK 25 and likely fail.

**Known environment quirk:** this sandbox's network cannot sustain large single-connection
downloads (curl gets connection-reset after several minutes on anything >~50MB). Gradle/Maven
dependency resolution (many small-to-medium files) works fine. If a large binary is ever
needed again, either fetch it in byte-range chunks (see `tools/chunked_download.sh`) or have
the user download it in their browser and extract it locally — that's what got the Android
SDK cmdline-tools and JDK 21 installed this session.

---

## 2. What's verified working (ran for real, not just typechecked)

| Check | Result |
|---|---|
| `./gradlew assembleDebug` | ✅ BUILD SUCCESSFUL (Phase 2 re-verified) |
| `./gradlew assembleRelease` | ✅ BUILD SUCCESSFUL — R8 minify + resource shrink both passed clean, a good signal against missing-class/reflection issues from all the new Hilt/Room/Compose code |
| `./gradlew test` | ✅ BUILD SUCCESSFUL — 35/35 unit tests pass (debug + release variants), including the two new pure-logic suites below |
| `TitleParserTest` (Kotlin) | ✅ 13/13 pass |
| `MoodMapperTest` (Kotlin, new) | ✅ 8/8 pass — docs/08-GAME-DATA.md mood table |
| `DrawSelectorTest` (Kotlin, new) | ✅ 9/9 pass — docs/02-PRODUCT-SPEC.md §3 weighted selection, hard filters, relaxation |
| `PairwiseRankerTest` (Kotlin, new) | ✅ 6/6 pass — docs/02-PRODUCT-SPEC.md §5 insertion-position math |
| `worker/test/titleParser.test.ts` (TypeScript port) | ✅ 13/13 pass via `node --test --experimental-strip-types` (unchanged this session) |
| Worker `tsc --noEmit` | ✅ zero errors (unchanged this session) |
| Actual on-device run | ✅ **Re-verified 2026-08-06** on the same Galaxy Tab S6 Lite (device `R52W70DGYEA`) — installs, launches, no crash, process stays alive in foreground. Found and fixed one real launch-time crash in the process (bug #9 in §3). ⚠️ **But** this only confirms cold-launch-to-PILE-screen; DRAW's lever, swipe cards, Credits Roll, RANK, Stacks, filters, and the share card have **not** been manually tapped through on-device yet — do that next (see TL;DR and §8). |
| `wrangler dev` (local Worker server) | ❌ Still not verified — same sandbox limitation as before. Not needed in practice: deploy-then-curl against the real Worker is the loop that's been working. |
| **Deployed Worker returning real IGDB data** | ✅ **Verified 2026-08-12 against live responses.** `/health?deep=1` → `{"ok":true,"sampleCount":20}`; `/games/search?q=elden ring` → Elden Ring with cover art, 95.2 rating, 45h/119h/174h playtimes; `/games/trending` → 20 released games with covers; `/games/short` → Portal 4h, Journey 3h, Limbo 4h. |

---

## 3. Bugs found and fixed while getting the real build working

These are worth knowing so they don't get silently reintroduced:

1. **Nested KDoc comments.** Kotlin block comments nest. Three doc comments contained a
   literal `` `/games/*` `` or `` `image/*` `` — the embedded `/*` opened a second comment
   level that the closing `*/` didn't fully close, causing "Unclosed comment" across three
   unrelated-looking files (`GameDto.kt`, `ShareTargetActivity.kt`, `ShareTargetViewModel.kt`).
   Rephrased to avoid literal `/*` inside any comment.
2. **Wrong Font import package.** Used `androidx.compose.ui.font.*`; the real package is
   `androidx.compose.ui.text.font.*`. Fixed in `core/design/Type.kt`.
3. **Missing experimental opt-in.** Variable-font support (`Font(..., variationSettings = ...)`)
   needs `@OptIn(ExperimentalTextApi::class)`. Added at file level in `Type.kt`.
4. **`com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter` doesn't resolve
   from Kotlin sources in this project's toolchain** — the dependency resolves fine in
   Gradle's graph and the class is visible to `javap`, but `kotlinc` reports "Unresolved
   reference" for it. Root cause not diagnosed (didn't seem worth more time). **Replaced
   entirely** with an in-house `core/network/JsonConverterFactory.kt` (~20 lines) built on
   kotlinx.serialization's own public `serializer(Type)` JVM helper. The dependency was
   removed from `gradle/libs.versions.toml` and `app/build.gradle.kts`. If you ever see that
   jakewharton artifact suggested again (e.g. by an IDE quick-fix), don't re-add it without
   testing — prefer `JsonConverterFactory`.
5. **`local.properties` had the placeholder RevenueCat key**, not the real one from
   `CLAUDE.md`. Fixed — real key `goog_uEkBWERrtERDlPxqQbcYpxvxXEH` is in there now.
   `local.properties` is gitignored, so this fix doesn't show up in git history — if you ever
   regenerate `local.properties` from `local.properties.example`, remember to swap this back in.
6. **Real crash found on-device**: `local.properties` also still had the literal placeholder
   `ca-app-pub-xxxxxxxxxxxxxxxx~xxxxxxxxxx` for `ADMOB_APP_ID`. AdMob's `MobileAdsInitProvider`
   validates that ID at process startup — *before* `Application.onCreate` runs — so an invalid
   ID crashed the app on every single launch with `IllegalStateException: Invalid application
   ID`, on a real device, immediately. **Fixed** by using Google's official public test AdMob
   App ID (`ca-app-pub-3940256099942544~3347511713` — safe, documented, not a secret) until a
   real one exists. Re-verified after the fix: installs, launches, and renders cleanly on a
   real Samsung Galaxy Tab S6 Lite. **Same lesson as bug #5** — check every value in
   `local.properties` against real/valid data, not just the RevenueCat key, whenever it's
   regenerated from the template.
7. **`Modifier.graphicsLayer` is in `androidx.compose.ui.graphics`, not `androidx.compose.ui.draw`**,
   on Compose UI 1.7.6 (this project's BOM). Importing the intuitive-looking `androidx.compose.ui.draw.graphicsLayer`
   compiles as a plain unresolved-reference error on the import line itself — no ambiguity,
   no warning, it just silently fails to resolve. Confirmed by inspecting the actual class in
   the cached `ui-release-api.jar` (`androidx/compose/ui/graphics/GraphicsLayerModifierKt.class`).
   Hit in `DrawScreen.kt`, `DrawCardStack.kt`, `CreditsRollScreen.kt` — fixed by switching the
   import. If an IDE or a memory of an older Compose version suggests `ui.draw`, don't trust
   it on this BOM.
8. **`GraphicsLayer` recording (`rememberGraphicsLayer()` / `drawLayer()` / `toImageBitmap()`)
   from `androidx.compose.ui.graphics.layer` does not resolve on this BOM (2024.12.01 /
   Compose UI 1.7.6)** — that capture API landed in a later Compose release. Attempted to use
   it for the share-card screen and hit `Unresolved reference` on every entry point.
   **Don't reach for it again without first bumping the Compose BOM** — the share card
   renderer was rewritten to draw directly with `android.graphics.Canvas` instead (see
   `core/share/ShareCardRenderer.kt`), which has zero Compose-version risk and produces the
   identical PNG deliverable.
9. **Real crash found on-device (2026-08-06, Phase 2 install)**: `core/ui/ArcadeScaffold.kt`
   used `Modifier.padding(top = (-24).dp)` to visually raise the circular DRAW button above
   the bottom nav bar. `Modifier.padding` throws `IllegalArgumentException: Padding must be
   non-negative` on negative values — it always would have, but this code path only executes
   once the bottom bar actually renders, and it never had during Phase 1's on-device check
   because that check happened on the onboarding screen, which hides the bottom bar. First
   real exercise of the bottom bar (i.e. reaching PILE) crashed on launch every time. **Fixed**
   by switching to `Modifier.offset(y = (-24).dp)` — `offset` (unlike `padding`) explicitly
   allows negative values and is the correct API for this kind of visual displacement.
   Re-verified: installs, launches, reaches PILE, bottom bar with the raised DRAW button
   renders without crashing. **Lesson**: a screen that hides its bottom bar (or any shared
   chrome) can mask a crash in that chrome indefinitely — don't treat "onboarding works
   on-device" as "the bottom bar works on-device already," check both explicitly.
10. **A deprecated IGDB filter silently returned zero rows (2026-08-12).** Every Worker list
    query used `where category = 0`; IGDB deprecated `category` in favour of `game_type` and
    stopped populating it. **Filtering on a deprecated IGDB field returns `200 OK` with `[]`,
    not a 4xx** — so there was no error to find, in any log, at any layer. Fixed by switching
    to `game_type = 0` (see `MAIN_GAMES_ONLY` in `IgdbGameProvider.ts`, which now carries a
    comment saying why). **Lesson:** a third-party filter that stops matching is invisible —
    it looks exactly like "no results exist." When a provider deprecates a field, assume the
    field will one day return nothing rather than erroring, and put at least one assertion on
    *non-empty* results somewhere you'll actually see it.
11. **`runCatching { … }.getOrElse { fallback }` does not catch an empty success
    (2026-08-12).** `WorkerGameDataSource`'s offline-first fallback to the Room seed set only
    fired on a thrown exception, so when the Worker returned `{"results":[]}` with a 200, the
    app rendered nothing while 426 usable seeded games sat in the database. Fixed with an
    explicit `.ifEmpty { seedFallback() }`. **Lesson:** "offline-first" has to include
    "empty-first" — to a user, a healthy backend serving nothing is indistinguishable from an
    outage, so it should degrade identically. (The rewrite also stopped swallowing
    `CancellationException`, which `runCatching` catches and which breaks structured
    concurrency.)

---

## 4. What's built (Phase 1 = roadmap Weeks 1–2)

- **Project scaffold**: Gradle Kotlin DSL, Compose, Hilt, Room, DataStore, Navigation,
  Retrofit + kotlinx.serialization, Coil3. `minSdk 26` / `targetSdk 36`.
- **Release signing**: `app/keystore/continue-release.jks` generated and wired via
  `key.properties` (gitignored). **Back this keystore up in two places — still not done as
  far as this assistant knows; ask the user to confirm.**
- **Design system**: color/type/motion tokens, bundled Chakra Petch/Inter/JetBrains Mono
  fonts, `ArcadeButton`, `ArcadeDrawButton`, `GameCard`, `CoinCounter`, `ArcadeScaffold`,
  `EmptyState`.
- **Room data layer**: all five entities/DAOs from `docs/05-TECH-ARCHITECTURE.md` exactly
  (three playtime fields on `GameEntity`, etc).
- **Offline seed set**: 426 real, curated game titles (genre-median playtime estimates, not
  IGDB-accurate) in `app/src/main/assets/seed_games.json`, regenerable via
  `tools/generate_seed.mjs`. IDs are negative so they never collide with real IGDB ids.
- **`GameDataSource`**: Retrofit client to the Worker, with automatic fallback to the Room
  seed data when the Worker is unreachable (offline-first is real, not just claimed).
- **Billing/Ads seams**: `BillingRepository`/`AdRepository` interfaces, Fake (debug) + Real
  (release) impls, Hilt-wired. **`Purchases.configure()` runs for real in
  `ContinueApplication.onCreate`** — every install gets a real anonymous RevenueCat
  app-user ID now that the placeholder key bug (§3.5) is fixed. See §5 for exactly how far
  this goes and doesn't go.
- **PILE screen**: grid view, all 5 states, NOW PLAYING cap-of-3 with swap-out prompt,
  sort (shortest-first / recent / A-Z), compact Time Budget bar.
- **DISCOVER screen**: 300ms-debounced search, instant add, TRENDING/SHORT & SWEET rails.
- **Android Share Target**: transparent bottom-sheet Activity for `text/plain` and `image/*`,
  ML Kit on-device OCR for the image path, full graceful-degradation ladder (confident →
  ambiguous top-3 → manual search, never dead-ends).
- **Title parser**: pure function, unit-tested in both Kotlin (app) and TypeScript (Worker),
  kept intentionally identical.
- **Onboarding**: cold open → pile-size question → seed step (search/skip; Steam import
  disabled with a "coming soon" label, correctly deferred to Week 4) → first-draw placeholder
  → done.
- **Cloudflare Worker** (`/worker`): all endpoints from the spec. `GameProvider` abstraction
  with `SeedGameProvider` (active today, no credentials needed) and `IgdbGameProvider`
  (switches on automatically once `TWITCH_CLIENT_ID`/`SECRET` are set via `wrangler secret
  put` — no code changes needed). Two-stage `/resolve` pipeline. `/coins/spend` and
  `/steam/owned` correctly fail closed (501) pending `REVENUECAT_SECRET_KEY` /
  `STEAM_API_KEY`. **Not deployed** — that's a `wrangler login && wrangler deploy` away,
  entirely on the user, not blocked on anything code-related.

---

## 4b. What's built (Phase 2 = roadmap Week 2 remainder, Week 3, and slices of 4/5)

Built in this session, on top of Phase 1, with nothing here blocked on Play Store/Twitch/AdMob
credentials — every item below runs against real Room data and (where applicable) the `Fake`
billing/ads repos in debug builds, same pattern as Phase 1.

**Week 3 — DRAW, Credits Roll, pairwise ranking (all three, in full):**
- `core/util/MoodMapper.kt` — genres/themes/release-year → `Mood` set, per the docs/08 table,
  unit-tested (`MoodMapperTest`).
- `feature/draw/DrawSelector.kt` — the full weighted-sampling algorithm from
  docs/02-PRODUCT-SPEC.md §3: hard filters (platform, time-budget-with-tolerance) relaxed in
  order with an on-screen "loosened to fit" message, then weighted sampling on mood match
  (×3), never-drawn-before (×2), longest-in-pile (×1.5, scaled over 180 days), highly-rated
  (×1.3), and 14-day snooze decay (×0.2 → back to 1.0). Pure function, unit-tested
  (`DrawSelectorTest`, 9 cases).
- `feature/draw/DrawScreen.kt` + `DrawCardStack.kt` + `DrawGateScreen.kt` — the full DRAW UI:
  TIME/MOOD/PLATFORM dial chips, a real drag-with-spring-resistance lever (not a button) that
  fires on threshold with a heavy haptic, a staggered card-deal animation, 3D flip-to-reveal
  cards, and velocity-aware swipe-to-verdict in all 4 directions (up = PLAYING IT → moves to
  NOW PLAYING; left = NOT TONIGHT → 14-day snooze; right = SAVE FOR LATER → pinned; down =
  RETIRE → dropped). Each card shows its "why matched" reasons.
- The **CONTINUE? economy gate** (docs §3): free users get 1 draw/day (tracked via
  `DrawDao.since()`), the 2nd+ attempt shows the looping 9→0 countdown screen with INSERT COIN
  (watch a rewarded ad → +1 coin via a new `BillingRepository.earnCoins()`), USE A COIN
  (spends via existing `spendCoins`), and GO PRO (queries the real
  `Purchases.sharedInstance.getOfferings()` via a new `currentOfferingPackage()` — correctly
  shows "not live yet" today since no Play Console product exists, and will Just Work once one
  does, no code changes needed).
- `feature/completion/CreditsRollScreen.kt` — the ~7-second skippable cinematic (CRT flash →
  typewriter "GAME CLEARED" → key-art fade/zoom → scrolling stats → +5 coins → RANK IT ▸),
  wired from a new long-press "MARK COMPLETE" action on NOW PLAYING cards in `PileScreen`.
  Completion coin grant reuses the same `earnCoins()` gate as ad rewards (fails closed on
  `RealBillingRepository` with the same "not configured yet" message, same as everywhere else
  coins are involved).
- `feature/rank/PairwiseRanker.kt` + `RankViewModel.kt` + `RankScreen.kt` — coarse bucket pick
  (LOVED/LIKED/FINE/NAH) then binary-search pairwise comparisons (capped at 5, per docs), with
  the insertion-position math split into a pure, unit-tested object (`PairwiseRankerTest`, 6
  cases) separate from the interactive ViewModel. Captures an optional 140-char verdict and a
  WOULD REPLAY toggle. Ends on "YOUR #N OF ALL TIME".

**Week 2 remainder:**
- GRID/LIST view toggle on PILE (**STACK's 3D "wow" view was not built** — see §6, this is a
  real gap, not just a rename).
- Platform / genre / length-bucket (`<5h`, `5-15h`, `15-40h`, `40h+`) filter chips, derived
  live from whatever's actually in the pile.
- Full Stacks CRUD: create, rename, delete (with a confirmation dialog), add/remove a game
  (from a new "ADD TO STACK" section in PILE's long-press menu, and from the Stacks screen
  itself), and the **Free: 2 stacks / Pro: unlimited** cap is enforced with a clear message —
  `feature/stacks/`.

**Week 4 slice (DRAW-adjacent economy only — paywall UI itself is still Week 4 remainder):**
- `BillingRepository.earnCoins()` and `currentOfferingPackage()` added to the interface; `Fake`
  grants/no-ops appropriately for debug demoing, `Real` fails closed with the same honest
  "not configured yet" pattern used throughout this codebase for anything touching
  not-yet-live Play Console products.
- The ad-watch-for-coin and coin-spend UI flows are real and wired end-to-end in the DRAW
  gate — this is the first place in the app any screen actually injects `BillingRepository` or
  `AdRepository` (Phase 1's build status noted zero screens did; that's no longer true).

**Week 5 slice:**
- `feature/profile/` (YOU tab) rebuilt from its Phase-1 stub: THIS YEAR stats (cleared, hours,
  longest clear, fastest clear, current draw-streak), HIGH SCORES (the live ranked list from
  RANK), TROPHIES (FIRST CONTINUE, PILE SLAYER, CRITIC, SPRING CLEANING, NIGHT SHIFT — real
  unlock conditions against Room data, not hardcoded), and working HAPTICS/CLIPBOARD-DETECTION
  settings toggles wired to the existing `UserPreferencesRepository`.
- One of five share cards — **"THE PILE"** ("N HOURS. N GAMES. SEND HELP.") — end-to-end:
  rendered natively via `android.graphics.Canvas` (see bug #8 in §3 for why not live Compose
  capture), saved through a newly-added `FileProvider`, and fired via real `ACTION_SEND`.
  Reachable from a new share icon on PILE.

---

## 5. RevenueCat — precisely how far the integration goes

**Real and working:**
- SDK configured with the real public key at app startup → anonymous app-user ID flows to
  the actual RevenueCat project (`proj747a0e7c`) from the first install, no products needed.
- `BillingRepository` listens for entitlement changes and correctly reads the `pro`
  entitlement's active/expiry state.
- The purchase call (`Purchases.sharedInstance.purchase(...)`) is written against the real
  SDK 10.12.0 API and compiles/ships correctly (proven by the successful signed release build).

**Also real and working as of Phase 2:**
- DRAW's CONTINUE? gate injects `BillingRepository` and `AdRepository` for real (§4b) — coin
  spend, ad-watch-for-coin, and the GO PRO purchase attempt (which correctly queries real
  offerings and no-ops gracefully today) are all live UI, not just seams.
- `earnCoins()` (ad rewards, completion rewards) and `currentOfferingPackage()` exist on the
  interface now.

**New as of 2026-08-11 — dashboard config is now complete ahead of Play Console:**
- All 6 products, both offerings (`default` is_current, `coins`), all 6 packages, the
  entitlement attachments, and COIN auto-grants were created via the RevenueCat MCP. They
  read as "not found in store" until Play Console has products with the matching ids — the
  exact id table lives in `docs/09-PENDING-INPUTS.md`. **Nothing further is needed on the
  RevenueCat side** except uploading the Play service account JSON and building the paywall.
- Consequence for the app: `currentOfferingPackage()` will start returning a real package
  the moment Play products go live, with no code change. The `default` offering already
  exists, so `getOfferings().current` is no longer null-by-configuration.

**Still not real — genuine Week 4 remainder:**
- No `PaywallView`/RevenueCatUI paywall screen anywhere — GO PRO's fallback message ("not live
  yet") is honest and correct today, but there's no actual paywall UI waiting behind it for
  when a real offering exists. That's still to build.
- No `ProGate` composable applied broadly across Pro-gated features (stack-limit enforcement
  in §4b checks `isPro` inline, which works, but there's no reusable gate component yet).
- No ad→Free-Play-Mode flow (only ad→coin exists).
- No Customer Center.

---

## 6. What's NOT built, by roadmap week — and what's actually blocking it

| Week | Scope | Status | Blocked on user? |
|---|---|---|---|
| 2 (remainder) | Stacks CRUD, filters | ✅ Built (§4b) | — |
| 2 (remainder) | **STACK view** — the signature receding-3D-stack "wow" view | ❌ Still not built (GRID/LIST only) | No |
| **3** | DRAW, Credits Roll, pairwise ranking | ✅ Built in full (§4b) | — |
| 4 | Virtual currency spend, ad-watch-for-coin, GO PRO purchase attempt | ✅ Built (§4b) | — |
| 4 | **Paywall UI** (`PaywallView`/RevenueCatUI), reusable `ProGate`, ad→Free-Play-Mode, Customer Center, Steam import review screen | ❌ Not built | Paywall UI itself can be built now against no products (it'll just show nothing purchasable); Customer Center same. Real products still need Play Console. |
| 5 | YOU tab stats/trophies, 1 of 5 share cards ("THE PILE") | ✅ Built (§4b) | — |
| 5 | **4 remaining share cards** (CLEARED, HIGH SCORE, THE STACK, YEAR IN GAMES), full Time Budget tap-to-expand visualization, **clipboard nudge detection logic** (toggle exists, nothing watches the clipboard yet) | ❌ Not built | No |
| 6–8 | Motion/haptics polish pass, real-device testing, submission assets | Not started (correctly — still too early for polish/assets; but **real-device testing of everything in §4b is overdue**, see §2) | No |

**Genuinely blocked on Play Store account / Twitch credentials:**
- Real game data/cover art (Worker auto-falls-back to the seed set meanwhile — nothing
  breaks, screens just show placeholder-ish data)
- Real RevenueCat products/offerings, real AdMob ad units
- Actual Play Store listing, the 14-day closed-testing clock

**Everything else is just not built yet, not blocked.**

---

## 7. Pending on Mikhil (see `docs/09-PENDING-INPUTS.md` for the authoritative table)

As of 2026-08-06: Google Play developer account and Twitch/IGDB Client ID are both still in
progress, **expected to land by roughly 2026-08-14** (end of the following week, per Mikhil).
Nothing built or planned in the near term needs to wait for either.

---

## 8. Recommended next priority

> **Superseded 2026-08-15 (later) — read this first.** Current order:
> 0. **Publish the privacy policy** (push + GitHub Pages toggle, 5 min) and **take 4+ phone
>    screenshots of `versionCode 4`** (`tools/capture_screenshots.sh`). Those two are the only
>    remaining inputs to a complete store listing — every other field is pre-answered in
>    **`docs/13-STORE-LISTING.md`**.
> 1. **Collect the 12+ tester emails and start the closed test.** This outranks every code item
>    below and always will until the clock is running — 14 consecutive days, per-app, and the
>    Shipaton deadline is 2026-09-30. Nothing in the build blocks it: `versionCode 4` is signed
>    and sitting in `app/build/outputs/bundle/release/`.
> 2. **Install `versionCode 4` on a *phone*** and re-check PILE (scroll past the filters) and
>    DRAW (hide/show dials, pull the lever) — the four fixes from 2026-08-15 are layout changes
>    that have not been seen on glass. `./gradlew installDebug` is faster for iteration, and the
>    debug build shows a fully populated paywall via `FakeBillingRepository`.
> 3. Still never exercised on a device at all: **RANK, Stacks, and offline share matching** (try
>    sharing a caption in airplane mode — a confident match should resolve with no network).
> 4. Then, in value order: the STACK 3D view, the 4 remaining share cards, store assets.
>
> **Closed, no longer needed:** pasting `TWITCH_CLIENT_SECRET` (done); designing a dashboard
> paywall (hand-built, and now device-confirmed as the best-looking screen in the app).


Still #1, and now narrower: PILE, DISCOVER, search, the rewarded ad, and the share target have
been exercised on a real device (2026-08-12) and their bugs fixed. **The DRAW lever, swipe
cards, Credits Roll, RANK, and Stacks have not.** Those are the gesture/physics-heavy screens —
exactly the ones most likely to read fine in source and feel wrong on glass.

1. **Finish real-device verification of everything in §4b.** The app now installs and reaches
   PILE without crashing (bug #9 fixed), but that's the *start* of on-device testing, not the
   end of it — nobody has actually pulled the DRAW lever, swiped a card, sat through Credits
   Roll, or run a RANK comparison on a real screen yet. The gesture/physics code (drag
   thresholds, velocity commit, spring specs) is exactly the kind of thing that reads fine in
   source and feels wrong on glass, or crashes on an interaction path cold-launch never
   exercises (same lesson as bug #9). Do this before demoing or recording anything. Ask
   Mikhil what he's already tapped through before assuming a clean slate.
2. **The STACK 3D view.** It's the one piece of Week 2/3 the design doc calls out by name as
   "the wow view" and it's the biggest visible gap left in the pile-browsing experience.
   After that, Week 4's paywall UI and Week 5's remaining share cards are the next-highest
   value, in either order — neither is blocked on anything.
