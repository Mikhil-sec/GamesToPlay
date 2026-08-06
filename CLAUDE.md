# CONTINUE? — Project Context

> Read `docs/00-MASTER-PLAN.md` first, then `docs/09-PENDING-INPUTS.md` to see what's still
> waiting on Mikhil, then the doc for whatever you're building.

## What this is

**CONTINUE?** is an Android app: a gaming bucket list / backlog manager that plays like an
arcade cabinet. Built for the **RevenueCat Shipaton 2026** hackathon.

**It is not a game.** It is a productivity/library app *for gamers*, styled like an arcade.

Tagline: *The games you started deserve an ending.*

## Competition targets (4 categories)

| Category | Requires | Status |
|---|---|---|
| **Gaming — Mr Lewis Blogs Influencer Award** (primary) | Published Play Store URL | Critical path |
| **RevenueCat Design Award** | Published app + design write-up | Same build |
| **Catvertising Award** | Published app + RevenueCat Ads write-up | Same build |
| **Next Gen Award** (student) | Public repo + OSI license + video. **No store release needed.** | Safety net |

**Deadline: Sept 30, 2026, 11:45pm PDT.**

## Non-negotiable constraints

1. **The repo is PUBLIC and open-source** (required for Next Gen). **No secrets in the app.**
   IGDB/Twitch credentials, RevenueCat *secret* keys, and Steam keys live in the Cloudflare
   Worker only. The app ships only the RevenueCat *public* SDK key and the AdMob app ID —
   both are designed to be public.
2. **Never use any influencer's name, likeness, logo, or brand** anywhere — app, store
   listing, marketing, or code. Instant disqualification. This includes "Mr Lewis Blogs".
3. **Game data comes from IGDB, never RAWG.** RAWG is effectively abandonware and was fully
   unreachable as of Aug 5, 2026. IGDB attribution is mandatory: *"The data was freely
   provided by IGDB.com"* plus their logo, on any screen showing their data. IGDB is rate
   limited to **4 req/sec**, so the app must **never** call it directly — all game data
   flows through the Worker, which caches. See `docs/08-GAME-DATA.md`.
4. **No interstitial ads. Ever.** Every ad is user-initiated with an explicit value exchange.
   This is a core product principle and the entire Catvertising thesis.
5. **Offline-first.** The pile must fully work with no network. Room is the source of truth.

## Live RevenueCat config (already provisioned)

| Thing | Value |
|---|---|
| Project | `CONTINUE` — `proj747a0e7c` |
| Android app | `CONTINUE? Android` — `app28ef647c38` |
| **Package name (PERMANENT)** | `com.mikhilnaika.continueapp` |
| Public SDK key | `goog_uEkBWERrtERDlPxqQbcYpxvxXEH` |
| Entitlement | `pro` — `entl9d1d013bd1` |
| Virtual currency | `COIN` ("Coins") |

The public SDK key is **not a secret** — RevenueCat public keys are designed to be embedded
in client apps and are extractable from any APK. Still read it from `local.properties` into
`BuildConfig` so rotation is easy and the pattern is correct.

**Still to configure** (blocked on the Google Play account, since Play products must exist
before RevenueCat can sync them): products, offerings, packages, paywall, AdMob linkage,
and ad reward rules. See `docs/04-MONETIZATION.md`.

## Stack

Kotlin · Jetpack Compose (Material 3, heavily themed) · Hilt · Room · DataStore ·
Retrofit + kotlinx.serialization · Coil 3 · `purchases-android` 10.12.0+ ·
`purchases-ui-android` · Google Mobile Ads SDK · Cloudflare Worker (TypeScript) backend.

minSdk 26 · targetSdk 36 · Compose BOM latest stable.

## Docs map

| File | What's in it |
|---|---|
| `docs/00-MASTER-PLAN.md` | Strategy, how each feature maps to judging criteria, risk register |
| `docs/01-PLAY-STORE-CRITICAL-PATH.md` | **Priority zero.** The 12-tester/14-day gate |
| `docs/02-PRODUCT-SPEC.md` | Every screen and feature, in detail |
| `docs/03-DESIGN-SYSTEM.md` | Neo-arcade tokens, type, motion, haptics |
| `docs/04-MONETIZATION.md` | RevenueCat entitlements, virtual currency, AdMob + SSV |
| `docs/05-TECH-ARCHITECTURE.md` | Modules, data model, API contracts, Worker endpoints |
| `docs/06-BUILD-ROADMAP.md` | Sprint-by-sprint build order |
| `docs/07-SUBMISSION-KIT.md` | Devpost copy, video script, asset checklist |
| `docs/08-GAME-DATA.md` | **IGDB** auth, rate limits, queries, mood mapping, fallbacks |
| `docs/09-PENDING-INPUTS.md` | **Living checklist** of everything still waiting on Mikhil |

## Working agreements

- **Ship a runnable APK from week 1.** The Play closed-testing clock cannot start without
  one. Correctness of the *build pipeline* outranks feature completeness early on.
- Feature-first package structure (`feature/pile`, `feature/draw`, …), not layer-first.
- Every animation must survive a *fast* user: no blocking waits, everything interruptible,
  everything skippable by tapping.
- Prefer real data over placeholders in any screen you demo.
