# CONTINUE? — Pending Inputs

> **Living checklist.** This is the single place tracking every credential, account, and
> decision still waiting on Mikhil. Claude should read this at the start of every session,
> update it as items resolve, and never block on an item marked "not yet needed."
>
> Last updated: 2026-08-05

---

## 🔴 Blocking — needed before the relevant feature can go live

| Item | Status | Needed for | Notes |
|---|---|---|---|
| **Twitch Client ID** | ⏳ Registering — pending Twitch approval | Worker's IGDB proxy | App name `CONTINUE App`, Confidential client, category "Application Integration". Once issued, paste the **Client ID only** here or in chat — it's safe to commit (not a secret). |
| **Twitch Client Secret** | ⏳ Same registration | Worker's IGDB proxy | **Never paste in chat or commit to a file.** Goes straight into `wrangler secret put TWITCH_CLIENT_SECRET` when the Worker is deployed. |
| **IGDB partnership email sent** | ⏳ Drafted, not yet sent | Compliance (Shipaton rules require authorization to use 3rd-party APIs) | Draft is in the chat history above. Send to `partner@igdb.com` once the Twitch app exists. Keep the reply — it's compliance evidence. |
| **Google Play developer account** | ⏳ Registration submitted, verification in progress | Publishing (Gaming, Design, Catvertising categories) | Started ~Aug 5. This is the critical path — see `docs/01-PLAY-STORE-CRITICAL-PATH.md`. |
| **18 tester Gmail addresses** | ⏳ Not yet collected | Starts the 14-day closed-testing clock | User is confident about reaching 20 when needed. Collect into a Google Group before the Play account goes live, so the clock can start the same day. |
| **Keystore generated + backed up** | ⏳ Not started | Signing the release APK | Generate in week 1. **Back up in two places immediately** — losing it means never updating the app again. |

## 🟡 Needed later — not blocking current work

| Item | Status | Needed for | Notes |
|---|---|---|---|
| AdMob account + 2 real rewarded ad units | Not started | Ad rewards (coins + Free Play Mode) | Test ad unit IDs cannot use server-side verification — must be real units. Has its own approval latency; start once the Play account is live. |
| RevenueCat products, offerings, packages | Blocked on Play Console | Subscriptions, coin packs | Play products must exist before RevenueCat can sync them. Project/app/entitlement/virtual currency are already live (see below) — only the store-linked products remain. |
| Cloudflare Worker deployed | Not started | All game data, share-target resolution, coin spend | Account exists (confirmed). Worker code is Phase 1 scope for the build. |
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

---

## How to use this file

- **Building can start now.** Nothing in the 🔴 or 🟡 tables blocks Phase 1 — the build uses
  fake `BillingRepository`/`AdRepository` implementations and a `GameDataSource` abstraction
  specifically so real credentials can drop in later without refactoring.
- When an item resolves, move it to ✅ and note the value (or note "stored in Wrangler" for
  secrets — never paste secrets into this file, since **this repo is public**).
- If you start a fresh chat, point Claude at this file first — it's the fastest way to get
  the current state without re-reading every doc.
