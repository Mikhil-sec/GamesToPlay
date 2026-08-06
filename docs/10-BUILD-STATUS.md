# CONTINUE? — Build Status

> **Read this first in any fresh session**, after `CLAUDE.md` and before `09-PENDING-INPUTS.md`.
> This is the living ledger of what's actually built, what's verified, what's broken, and
> what's next. Update it whenever you finish a chunk of work or discover something that
> changes this picture — don't let it go stale like a comment nobody re-reads.
>
> Last updated: 2026-08-06, end of the Phase 1 build session.

---

## TL;DR

**Phase 1 (roadmap Weeks 1–2 scope) is built and verified compiling, testing, and signing
correctly on a real toolchain.** The Week 3–8 signature features (DRAW, Credits Roll,
pairwise ranking, share cards, paywall UI) are **not built yet** — only seams exist.
Nothing about that is blocked on your pending Play Store / Twitch credentials; it's just not
built yet. See "What's next" at the bottom.

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
| `./gradlew assembleDebug` | ✅ BUILD SUCCESSFUL — 72MB APK at `app/build/outputs/apk/debug/app-debug.apk` |
| `./gradlew test` | ✅ BUILD SUCCESSFUL — all unit tests pass |
| `TitleParserTest` (Kotlin) | ✅ 13/13 pass |
| `worker/test/titleParser.test.ts` (TypeScript port) | ✅ 13/13 pass via `node --test --experimental-strip-types` |
| Worker `tsc --noEmit` | ✅ zero errors |
| `./gradlew assembleRelease` | ✅ BUILD SUCCESSFUL, and `apksigner verify` confirms it's signed with the real `continue-release.jks` (CN=Mikhil Naika, OU=CONTINUE) |
| `wrangler dev` (local Worker server) | ❌ **Not verified** — hung with no output in this sandbox (likely an interactive-prompt or sandboxing quirk). Worker logic is verified via typecheck+tests instead. Try `cd worker && npm run dev` on your own machine. |
| Actual on-device run | ✅ **Verified 2026-08-06** on a real Samsung Galaxy Tab S6 Lite (SM-P619) over ADB — installs, launches, and renders the onboarding cold-open screen correctly. Found and fixed a real launch-time crash in the process — see §3.6. |

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

## 5. RevenueCat — precisely how far the integration goes

**Real and working:**
- SDK configured with the real public key at app startup → anonymous app-user ID flows to
  the actual RevenueCat project (`proj747a0e7c`) from the first install, no products needed.
- `BillingRepository` listens for entitlement changes and correctly reads the `pro`
  entitlement's active/expiry state.
- The purchase call (`Purchases.sharedInstance.purchase(...)`) is written against the real
  SDK 10.12.0 API and compiles/ships correctly (proven by the successful signed release build).

**Not real yet — Week 4 scope, not started:**
- **No screen anywhere injects `BillingRepository` or `AdRepository`.** Verified by grep —
  zero feature files reference either outside their own `core/billing`/`core/ads` packages.
  They're correct, ready seams, not wired to any UI.
- No paywall (`PaywallView`), no Offerings/Packages fetch, no `ProGate`.
- No coin spend, no ad→coin or ad→Free-Play-Mode flow, no Customer Center.

---

## 6. What's NOT built, by roadmap week — and what's actually blocking it

| Week | Scope | Status | Blocked on user? |
|---|---|---|---|
| 2 (remainder) | Stacks UI, STACK/LIST views, full filter set (platform/genre/length bucket) | ❌ Not built | No |
| **3** | **DRAW** (dials, lever physics, weighted selection algorithm, card deal/flip/swipe), **Credits Roll**, **pairwise ranking** | ❌ Not built — only a placeholder screen exists | **No — nothing here needs Play/Twitch/AdMob** |
| 4 | Paywall UI, virtual currency spend, AdMob rewarded flow, FREE PLAY mode, `ProGate`, Customer Center, Steam import review screen | ❌ Not built | Partially — real products/ads need Play Console, but the *flow* can be built now against Fake billing/ads |
| 5 | **Share cards**, YOU tab stats/trophies, full Time Budget visualization, clipboard nudge | ❌ Not built | No |
| 6–8 | Motion/haptics polish, real-device testing, submission assets | Not started (correctly — too early) | No |

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
Nothing in Phase 2 (Week 3 features) needs to wait for either.

---

## 8. Recommended next priority

**Week 3: DRAW, Credits Roll, pairwise ranking.** These are three of the five features the
design doc calls "never cut" and the ones judges actually see. None of them need the pending
credentials. This is almost certainly what to pick up in a fresh session unless the user says
otherwise.
