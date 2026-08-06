# CONTINUE? — Build Status

> **Read this first in any fresh session**, after `CLAUDE.md` and before `09-PENDING-INPUTS.md`.
> This is the living ledger of what's actually built, what's verified, what's broken, and
> what's next. Update it whenever you finish a chunk of work or discover something that
> changes this picture — don't let it go stale like a comment nobody re-reads.
>
> Last updated: 2026-08-06, end of the Phase 2 build session.

---

## TL;DR

**Phase 1 (Weeks 1–2) and now most of Phase 2 (Weeks 2 remainder, 3, and partial 4/5) are
built and verified compiling, testing, and signing on the real toolchain.** This session
added the three signature Week 3 features (DRAW, Credits Roll, pairwise ranking) in full,
plus the Week 2 remainder (Stacks CRUD, GRID/LIST toggle, platform/genre/length filters),
the DRAW-side economy loop from Week 4 (CONTINUE? gate, coin spend, ad-watch-for-coin, GO PRO
trigger — all against `Fake`/real-but-gracefully-empty billing), and a first slice of Week 5
(YOU tab stats/trophies, one working share card). See §6 for the precise per-week ledger,
including what's still genuinely missing in each week.

**Not built this session, and worth knowing before you assume otherwise:** the STACK 3D
"wow" view (GRID/LIST only), the RevenueCatUI paywall screen itself, FREE PLAY mode, Customer
Center, 4 of the 5 share card types, the full Time Budget tap-to-expand visualization, and the
clipboard nudge's actual detection logic (the settings toggle persists, but nothing watches
the clipboard yet). None of these are blocked on credentials — they're just not built yet.
**This build has not been re-verified on a real device this session** — only Gradle
compile/test/assemble were run (see §2). Do that before trusting the UI is actually correct.

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
| Actual on-device run | ⚠️ **Not re-verified this session.** Last confirmed 2026-08-06 on a Galaxy Tab S6 Lite, but that was *before* all of §4b below was written. Compile/test/assemble all pass, but nobody has tapped through DRAW's lever, the swipe cards, Credits Roll, or RANK on a real screen yet — treat the interaction feel and any runtime-only bugs as unverified until that happens. |
| `wrangler dev` (local Worker server) | ❌ Still not verified — same sandbox limitation as before, unchanged this session. |

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

Two things, in order:

1. **Real-device verification of everything in §4b.** This session's build was verified via
   Gradle compile/test/assemble only (all passing, including R8-minified release) — nobody has
   actually pulled the DRAW lever, swiped a card, sat through Credits Roll, or run a RANK
   comparison on a real screen. The gesture/physics code (drag thresholds, velocity commit,
   spring specs) is exactly the kind of thing that reads fine in source and feels wrong on
   glass. Do this before demoing or recording anything.
2. **The STACK 3D view.** It's the one piece of Week 2/3 the design doc calls out by name as
   "the wow view" and it's the biggest visible gap left in the pile-browsing experience.
   After that, Week 4's paywall UI and Week 5's remaining share cards are the next-highest
   value, in either order — neither is blocked on anything.
