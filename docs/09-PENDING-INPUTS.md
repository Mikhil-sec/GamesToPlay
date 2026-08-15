# CONTINUE? — Pending Inputs

> **Living checklist.** This is the single place tracking every credential, account, and
> decision still waiting on Mikhil. Claude should read this at the start of every session,
> update it as items resolve, and never block on an item marked "not yet needed."
>
> Last updated: 2026-08-15 — second phone-test round; phone layout fixes across the nav bar,
> PILE, DRAW and offline cover caching. **`versionCode 4` / `0.4.0` built and signed**
> (supersedes `versionCode 3`, which was never uploaded).
>
> **Open items needing Mikhil, in priority order:** ① 18 tester Gmail addresses (starts the
> 14-day clock — still the biggest schedule risk, and now the *only* thing on the critical
> path); ② upload the **already-built, signed `versionCode 4` .aab**; ③ privacy policy hosting
> (**required at the first production/closed-testing submission**, so it is effectively part of
> ①); ④ IGDB partnership reply (needs signing email + repo URL, see
> `docs/11-IGDB-PARTNERSHIP.md`); ⑤ Play service-account JSON for RevenueCat; ⑥ store listing
> assets (icon, feature graphic, screenshots — the phone screenshots should come from
> `versionCode 4`, not the older builds).
>
> **No longer needed:** designing a paywall in the RevenueCat dashboard (hand-built instead,
> docs/10-BUILD-STATUS.md §The paywall) — and pasting `TWITCH_CLIENT_SECRET`, which Mikhil has
> now done; the data-dump pipeline ran successfully and its output is in the shipped `.aab`.

---

## 🔴 Blocking — needed before the relevant feature can go live

| Item | Status | Needed for | Notes |
|---|---|---|---|
| **Twitch Client ID** | ✅ Obtained 2026-08-11: `bvzzw402pd90vedamcjuibfde6i73y` | Worker's IGDB proxy | Safe to commit (not a secret). Still needs `wrangler secret put TWITCH_CLIENT_ID` to actually take effect. |
| **Twitch Client Secret** | ✅ Set as a Worker secret 2026-08-11 — `/health` on the deployed Worker confirms `"provider":"igdb"`, real data is flowing | Worker's IGDB proxy | Done. (One hiccup along the way: `wrangler secret put` was first run with the Client ID typed as the *variable name* instead of `TWITCH_CLIENT_ID`, creating a stray secret literally named `bvzzw402pd90vedamcjuibfde6i73y`. That was deleted with `wrangler secret delete` and redone correctly — if a `wrangler secret list` ever shows an odd-looking secret name again, that's the failure mode to check for.) |
| **Twitch Client Secret — local copy for tooling** | ✅ **Done 2026-08-14.** Pasted into `worker/.dev.vars` (gitignored, verified with `git check-ignore`) | `tools/igdb_dump_index.mjs` | The indexer ran successfully: 17,095 games indexed from 289.8MB of `games` + 41MB `covers` + 18.4MB `alternative_names` + 0.9MB `game_time_to_beats`, output at `app/src/main/assets/game_index.tsv.gz` (0.58MB) and wired into `ShareTargetViewModel` — see `docs/10-BUILD-STATUS.md`. If the secret is ever rotated in the Twitch console, the old one is invalidated — update both `worker/.dev.vars` **and** re-run `npx wrangler secret put TWITCH_CLIENT_SECRET` from `worker/`, since the deployed Worker reads its own copy from Cloudflare, not this file. |
| **IGDB offline game index** | ✅ **Built and shipping** as of `versionCode 3` | Share-target matching accuracy, and making it work offline | `game_index.tsv.gz` — regenerate any time with `node tools/igdb_dump_index.mjs` (dumps are cached in `tools/.dump-cache/`, so a re-run after the first is fast). Not on an automatic refresh schedule; dumps update daily on IGDB's side but nothing re-runs this pipeline automatically. Re-run occasionally so the index doesn't go stale, and definitely re-run if `tools/igdb_dump_index.mjs` itself changes. |
| **RevenueCat paywall design** | ✅ **Closed 2026-08-14 — no dashboard work needed.** GO PRO is a hand-built Compose paywall (`feature/paywall/`) | The GO PRO screen | Replaced the RevenueCatUI template the same session it was written; a generic template is a bad answer in a design category. Prices/trial length still come from the store at runtime, so Play Console price changes need no code and no dashboard paywall. Fully demoable in a **debug** build via `FakeBillingRepository.proTiers()` — which is also where the only hardcoded prices in the app live, so update them there if Play pricing ever changes. |
| **IGDB data dumps** | ✅ **Enabled for our Client ID 2026-08-14** | Accurate offline search + share matching | `GET /v4/dumps` and `/v4/dumps/{endpoint}` now work for us. Daily CSVs; the `s3_url` is presigned and expires in **5 minutes**. Pipeline and the reasoning for putting the index in the app rather than the Worker are in `docs/08-GAME-DATA.md` §Data dumps. Blocked only on the client secret above. |
| **IGDB commercial partnership** | ✅ Sent, and **IGDB replied 2026-08-12** asking 7 questions to start the agreement | Compliance (Shipaton rules require authorization to use 3rd-party APIs) | **Answer now — see `docs/11-IGDB-PARTNERSHIP.md`** for the reasoning, the drafted answers, and what "data dumps" unlocks (bulk table downloads that would remove the 4 req/sec ceiling entirely and make offline-first honest). Only two items need Mikhil: which email signs, and the public repo URL. Partnership is **free**. |
| **Google Play developer account** | ✅ Live as of 2026-08-11 | Publishing (Gaming, Design, Catvertising categories) | **Correction, 2026-08-11: the 12-tester/14-day closed-testing gate is per-app, not per-account** (confirmed by Mikhil's own research after this doc previously assumed otherwise). Mikhil has a second app on the same account already in internal testing, but it has **not** started its own 14-day clock either — so CONTINUE? gets no head start from it. Each app clears the gate independently. This makes starting CONTINUE?'s closed test **the single biggest schedule risk in the project** — see `docs/01-PLAY-STORE-CRITICAL-PATH.md`. |
| **18 tester Gmail addresses** | ⏳ Not yet collected | Starts the 14-day closed-testing clock | User is confident about reaching 20 when needed. Collect into a Google Group now — the account is live, nothing is blocking this anymore. **This is the next highest-priority action in the whole project.** |
| **Keystore backed up** | ✅ **Confirmed backed up by Mikhil, 2026-08-12.** Generated at `app/keystore/continue-release.jks`, passwords in gitignored `key.properties` | Signing the release bundle | Closed. Don't re-ask. |
| **Signed release App Bundle (.aab)** | ✅ `versionCode 1`/`0.1.0` **uploaded to Internal testing 2026-08-11**. ⚠️ `versionCode 2` and `3` were built but **never uploaded** — both superseded, don't upload them. ✅ **`versionCode 4`/`0.4.0` is BUILT AND SIGNED** at `app/build/outputs/bundle/release/app-release.aab` (2026-08-15, 32.4 MB, signer cert valid to 2056) — **just upload it**. Carries everything from 3 (image-quality fixes, share-sheet exit/overlay fix, GO PRO paywall, dispenser deal animation, offline share-matching index) plus the 2026-08-15 phone-layout round: coin counter scoped to DRAW/YOU, the DRAW nav button centred on its own slot, PILE scrolling as one surface, DRAW's dials collapsible with the lever always pullable, and offline cover caching | Play Console Internal testing track | `versionCode 2` carries the app-side half of the 2026-08-12 fix (empty-response fallback, `PULL THE LEVER` placeholder removed). Note the *root* fix was server-side, so `versionCode 1` already shows games without reinstalling. For every future build `versionCode` must strictly increase (Play rejects a repeat/lower code on any track); `versionName` is cosmetic. Rebuild with `JAVA_HOME=/c/Android/jdk21/jdk-21.0.12+8 ./gradlew bundleRelease` after bumping both in `app/build.gradle.kts` lines 33-34. |

## 🟡 Needed later — not blocking current work

| Item | Status | Needed for | Notes |
|---|---|---|---|
| AdMob account + 2 real rewarded ad units | ✅ **All three real ids now in `local.properties` (2026-08-12):** App ID `ca-app-pub-5970073296518593~4180553585`, `ADMOB_UNIT_COIN=…/8342445864`, `ADMOB_UNIT_FREE_PLAY=…/5752921483`. `RealAdRepository` already reads these via `BuildConfig` — no code change needed, just the rebuild. | Ad rewards (coins + Free Play Mode) | Real units almost certainly won't **fill** (serve an actual ad) until the app is live on Play — that's normal AdMob behavior for unpublished apps, not a bug. Cannot link to Play Console until then either (AdMob's linking flow only finds published apps). If ads show "no fill" on-device before launch, that's expected — the code path is correct and will start working the moment the app goes live. | **This is not actually blocking, and the app already handles it correctly.** Google's official *public test* rewarded unit ids work on any build, live or not, and the app is already running against the test AdMob App ID (`ca-app-pub-3940256099942544~3347511713`, see bug #6 in `docs/10-BUILD-STATUS.md`). So the full watch-ad→earn-coin loop is demoable and recordable **today** with test ads — no "coming soon" placeholder needed, which would be the worse outcome for the Catvertising category. What test units genuinely *cannot* do is **server-side verification (SSV)**, so the reward currently has to be granted client-side. Swap in real unit ids + turn on SSV once production access lands; that's a config change, not a code change. |
| RevenueCat products, offerings, packages | ✅ **Pre-created in RevenueCat 2026-08-11 via MCP** — now waiting on the *matching* Play Console products | Subscriptions, coin packs | RevenueCat-side config is done: 6 products, 2 offerings, 6 packages, entitlement attachments, and COIN auto-grants all exist. They will read as "not found in store" until Mikhil creates products in Play Console with **exactly** the ids in the table at the bottom of this file. No RevenueCat work remains for this item. |
| Play Console → RevenueCat service account | Not started | RevenueCat validating real purchases | Play Console → Setup → API access → create a service account with **Financial data / Manage orders** + **View app information**, download the JSON, upload it to the RevenueCat Android app config. Without it RevenueCat cannot verify Play purchases. |
| Cloudflare Worker deployed | ✅ Deployed 2026-08-11 at `https://continue-worker.gamestoplay.workers.dev` | All game data, share-target resolution, coin spend | `local.properties` `WORKER_BASE_URL` updated to match. `/health` confirms `{"ok":true,"provider":"igdb"}` — **real IGDB data is live**, not the seed fallback. Share Target auto-match now works end-to-end for real. |
| Privacy policy hosted | Not started | Play Store listing requirement | Host as a static page on the same Cloudflare account. Needed at first Play upload, not before. |
| App icon, feature graphic, screenshots | Not started | Play listing + Devpost assets | Week 6–7 per the roadmap. |
| Play promo code (trial backup) | Not started | Judge access if the 7-day trial isn't enough | Generate in week 7. |

## ✅ Already resolved — recorded for reference

| Item | Value |
|---|---|
| Devpost academic email | Confirmed — user has a qualifying academic email for Next Gen |
| App name | **CONTINUE?** |
| Design direction | Neo-arcade / CRT, dark-only |
| Package name (**PERMANENT — do not change after this**) | `com.mikhilnaika.continueapp` |
| RevenueCat project | `CONTINUE` — `proj747a0e7c` |
| RevenueCat Android app | `CONTINUE? Android` — `app28ef647c38` |
| RevenueCat public SDK key | `goog_uEkBWERrtERDlPxqQbcYpxvxXEH` |
| RevenueCat entitlement | `pro` — `entl9d1d013bd1` |
| RevenueCat virtual currency | `COIN` ("Coins") |
| Cloudflare account | Exists (free tier) |
| Game data provider | **IGDB**, not RAWG — RAWG confirmed unreachable Aug 5, 2026 |
| Phase 1 build (roadmap Weeks 1–2) | Built and verified compiling/testing/signing on 2026-08-06 — see `docs/10-BUILD-STATUS.md` |
| Phase 2 build (Week 3 in full, Week 2 remainder, slices of 4/5) | Built 2026-08-06 same day; confirmed installing/launching on-device after fixing one real crash (negative padding). Full on-device tap-through still pending — see `docs/10-BUILD-STATUS.md` §8. |
| Local dev toolchain | Android SDK at `C:\Android\sdk`, JDK 21 at `C:\Android\jdk21\jdk-21.0.12+8` — see `docs/10-BUILD-STATUS.md` §1 |
| Google Play developer account | ✅ Live as of 2026-08-11. Has a **second app** on the same account, in internal testing, own 14-day clock not yet started either (see the 🔴 table — the gate is per-app) |
| Twitch app | ✅ Registered 2026-08-11. Client ID `bvzzw402pd90vedamcjuibfde6i73y` (safe, public). Confidential client, redirect `https://localhost`, category "Application Integration" |
| Cloudflare Worker | ✅ Deployed 2026-08-11 at `https://continue-worker.gamestoplay.workers.dev`, subdomain `gamestoplay.workers.dev`, KV namespace id `5ae55aa10e6441fb986c1e6208871938` (already in `wrangler.toml`). `/health` returns real `igdb` provider. |
| Signed release bundle | ✅ `app-release.aab` built 2026-08-11, `versionCode 1`/`versionName 0.1.0` — see 🔴 table for exact path and next-build instructions |

---

## Play Console product ids — must match RevenueCat exactly

Created in RevenueCat on 2026-08-11. Play Console products must use **these exact ids**, or
RevenueCat will not resolve them. For subscriptions the *base plan id* matters too, because
RevenueCat's store identifier is `productId:basePlanId`.

**Status 2026-08-12: all 5 shipping products exist in Play Console and RevenueCat is
reconciled to match.** Annual was dropped — see the note below the table.

| What | Play product id | Play base plan id | Price | RevenueCat product id | Play |
|---|---|---|---|---|---|
| Pro Monthly (7-day free trial) | `continue_pro_monthly` | `monthly` | $3.99 | `prodb3fdae1092` | ✅ |
| ~~Pro Annual~~ **dropped** | ~~`continue_pro_annual`~~ | — | — | `prod4fb121bb30` (**archived**) | ❌ never created |
| Pro Lifetime (one-time, non-consumable) | `continue_pro_lifetime` | — | **$9.99** | `prodbf1ac2c0d6` | ✅ |
| 50 Coins (consumable) | `coins_50` | — | $0.99 | `prod5139a08b31` | ✅ |
| 170 Coins (consumable) | `coins_150` | — | $2.49 | `prod9e562e9992` | ✅ |
| 600 Coins (consumable) | `coins_500` | — | **$4.99** | `prod6ffd8b210d` | ✅ |

**Annual was deliberately dropped (Mikhil's call, 2026-08-11).** With Lifetime at $9.99, a
$19.99/yr annual is strictly dominated — nobody rational buys it — so the tier was never
created in Play Console. RevenueCat was reconciled to match on 2026-08-12: package
`$rc_annual` (`pkge03cff770e3`) **deleted** from the `default` offering and product
`prod4fb121bb30` **archived** (reversible via `unarchive-product` if annual ever returns).
The archived product is still listed on entitlement `pro` and in the COIN 600-grant group;
that's inert (an archived product can't be purchased) and left in place so re-adding annual
is a one-step undo.

The `default` offering is now **Monthly + Lifetime**, which is the ladder the paywall should
be designed around. ✅ **Pricing decided 2026-08-12 (Mikhil):** Monthly exists largely to make
Lifetime look obvious, and suppressed recurring revenue is an accepted, deliberate trade-off —
revisit only if the app finds real scale. The full rationale is written up in
`docs/07-SUBMISSION-KIT.md` §Pricing rationale, because judges will ask why there's no annual
tier and the answer needs to read as strategy rather than oversight.

Prices are set in Play Console when creating each product — RevenueCat just imports/displays
them, no MCP action needed for a price change. `docs/04-MONETIZATION.md` still shows the old
figures; treat this table as authoritative for pricing.

**Play Console one-time-product setup, as of the current Play Console UI (2026-08-11):**
Google restructured one-time products around **purchase options** — there is no longer a
separate consumable/non-consumable checkbox. The purchase-option **type** you pick *is* the
consumable/non-consumable choice:
- **One-time purchase** (buy once, can't repurchase) → use for `continue_pro_lifetime`
- **Repeatable purchase** (can buy again) → use for `coins_50`, `coins_150`, `coins_500`

Purchase option ids only need to be unique *within* a product (not across products) and don't
propagate to RevenueCat — the id we suggested was `lifetime` for the lifetime product and
`default` for each coin pack, but any value works. ✅ **Done 2026-08-12** — all 5 shipping
products are created in Play Console.

Offerings (as of 2026-08-12): `default` (`ofrng7872611f54`, is_current) with **`$rc_monthly` +
`$rc_lifetime`**; `coins` (`ofrng0089c03ce3`) with `$rc_custom_coins_50` / `_150` / `_500`.

COIN auto-grant on purchase: `coins_50`→50, `coins_150`→170, `coins_500`→600,
`continue_pro_monthly`→50/cycle, `continue_pro_lifetime`→600 once. **The lifetime amount was
a judgement call** — the spec says "50/month", which only maps cleanly onto the monthly plan.
Change via `update-virtual-currency` if that's wrong.

---

## How to use this file

- **Building can start now.** Nothing in the 🔴 or 🟡 tables blocks Phase 1 — the build uses
  fake `BillingRepository`/`AdRepository` implementations and a `GameDataSource` abstraction
  specifically so real credentials can drop in later without refactoring.
- When an item resolves, move it to ✅ and note the value (or note "stored in Wrangler" for
  secrets — never paste secrets into this file, since **this repo is public**).
- If you start a fresh chat, point Claude at this file first — it's the fastest way to get
  the current state without re-reading every doc.
