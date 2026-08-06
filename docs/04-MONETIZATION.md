# CONTINUE? — Monetization Architecture

> Targets the **Catvertising Award** directly, and supplies the "thoughtful RevenueCat use"
> evidence for **Next Gen**.

## The thesis, in one paragraph

Most apps bolt ads on as a tax the user pays for being cheap. CONTINUE? is built around an
arcade, where **continuing has always cost a coin** — so a rewarded ad isn't an interruption
grafted onto the product, it's the product's central metaphor made literal. Ads sit at the
bottom of a three-layer stack (ads → coins → subscription) that RevenueCat unifies into one
view of customer value. Every ad in this app is **user-initiated**, **explicitly priced**,
and **gives the user something they wanted a second earlier**.

**There are no interstitials. There is no banner. Ever.** State this proudly in the
submission — it is the differentiator.

---

## The three layers

```
┌─────────────────────────────────────────────────────────┐
│  LAYER 3 — PRO SUBSCRIPTION        entitlement: "pro"   │
│  Unlimited draws · unlimited stacks · Steam import      │
│  all share themes · Wrapped · cloud backup · NO ADS     │
│  + 50 coins/month stipend                               │
└─────────────────────────────────────────────────────────┘
              ▲ converts                    ▲ converts
┌─────────────────────────────────────────────────────────┐
│  LAYER 2 — COINS       RevenueCat Virtual Currency      │
│  Earned by ads, streaks, clears · bought as consumables │
│  Spent on draws, share themes, app skins                │
└─────────────────────────────────────────────────────────┘
              ▲ earns
┌─────────────────────────────────────────────────────────┐
│  LAYER 1 — ADS      AdMob rewarded, tracked by RevenueCat│
│  Always user-initiated. Grants coins OR temporary Pro.  │
└─────────────────────────────────────────────────────────┘
```

Coins are the connective tissue: they can be **earned** (ads), **bought** (IAP), or
**granted** (subscription stipend). That single design choice is what makes the three layers
one system instead of three bolted-on tactics — and it's precisely what the Catvertising
criteria mean by *"smart integration with the rest of your revenue stack."*

---

## Layer 3 — PRO subscription

**Entitlement id:** `pro`

**Offering:** `default`

| Package | Product id | Price | Notes |
|---|---|---|---|
| Monthly | `continue_pro_monthly` | $3.99 | **7-day free trial** |
| Annual | `continue_pro_annual` | $19.99 | Best value badge, ~58% saving |
| Lifetime | `continue_pro_lifetime` | $39.99 | Non-consumable. Gamers love owning things. |

The 7-day free trial satisfies the Shipaton requirement that judges can test premium
features without a promo code. **Also generate a Play promo code as a backup** and include
it in the Devpost submission — belt and braces.

### What Pro unlocks
Unlimited draws · unlimited stacks · full Steam import · all share-card themes · all app
skins · Year in Games · cloud backup & restore · **no ads** · 50 coins/month.

### Paywall
Use **RevenueCat's remote paywall** (`purchases-ui-android`, `PaywallActivityLauncher` /
`Paywall` composable) so copy, pricing emphasis, and layout can be changed from the
dashboard **without shipping an APK** — which matters enormously when the Play review queue
is the bottleneck. Style it to match the neo-arcade system.

Paywall triggers (all soft, never a wall on launch):
- The `CONTINUE?` screen's `GO PRO` option
- Attempting a 3rd stack
- The blurred remainder of a Steam import
- A locked share theme
- `YOU` tab → an upgrade row

Also integrate the **RevenueCat Customer Center** in Settings for self-serve management,
cancellation flows, and restore. Cheap to add, and it reads as production-grade.

---

## Layer 2 — Coins (RevenueCat Virtual Currency)

**Currency code:** `COIN` · display: a gold coin · name: "Coins"

### Earning

| Action | Coins | Cap |
|---|---|---|
| Watch a rewarded ad | **+1** | 5/day |
| Daily streak check-in | +1 | 1/day |
| Clear a game | +5 | — |
| Rank 5 games | +2 | — |
| First Steam import | +10 | once |
| Pro monthly stipend | +50 | monthly |

### Buying (consumable IAP)

| Product id | Coins | Price |
|---|---|---|
| `coins_50` | 50 | $0.99 |
| `coins_150` | 150 (+20 bonus) | $2.49 |
| `coins_500` | 500 (+100 bonus) | $6.99 |

Configure these in RevenueCat to **automatically grant currency on purchase** (products can
be associated with a virtual currency so the balance credits without app-side logic).

### Spending

| Item | Cost |
|---|---|
| Extra draw | 1 |
| Reroll a dealt card | 1 |
| Unlock a share-card theme (permanent) | 25 |
| Unlock an app skin (permanent) | 75 |

### Implementation notes (important)

- Read balance with `Purchases.sharedInstance.virtualCurrencies()`; call
  `invalidateVirtualCurrenciesCache()` after any grant or spend before re-reading, since the
  balance is cached.
- **Spending requires a server call.** It's a `POST` to RevenueCat's v2 virtual-currency
  transactions endpoint using a **secret** API key, which must never be in the app — this is
  a public repo. All spends go through our Cloudflare Worker. See
  `docs/05-TECH-ARCHITECTURE.md`.
- **Verify the exact v2 endpoint path and payload against current RevenueCat docs at
  implementation time** rather than trusting this document — the API surface is newer than
  most of the SDK.
- Treat the server as the source of truth for balance. Show an optimistic local decrement
  for responsiveness, then reconcile; if the server rejects, roll back with a shake
  animation and a clear message.
- Handle the offline case: queue nothing. If a spend can't reach the server, tell the user
  and don't grant the item. Never grant locally — that's how you build an exploit.

---

## Layer 1 — Ads (AdMob + RevenueCat Ads)

### What RevenueCat Ads actually is (get this right)

It is **not** an ad network and **not** an ad server. It is a tracking/attribution and
reward-verification layer on top of your existing ad SDK. We serve ads with **Google AdMob**
and use RevenueCat to (a) ingest ad revenue into unified LTV alongside subscriptions and
(b) **server-side verify rewarded-ad completions** so rewards can't be spoofed.

### Setup

1. AdMob account created and linked to the Play app.
2. **Real rewarded ad units** — test ad unit IDs cannot be used with server-side
   verification, and SSV is the whole point of our design.
3. AdMob connected to RevenueCat so units sync into the dashboard.
4. Per rewarded unit, set the SSV callback URL to:
   `https://api.revenuecat.com/v1/incoming-webhooks/admob-ssv-rewarded`
5. Configure reward rules on the RevenueCat dashboard's **Ads → Rewards** page.
6. Requirements: `purchases-android` **10.12.0+**, Charts v3 enabled, and explicit opt-in
   to the experimental reward-verification APIs.

### Android integration shape

```kotlin
// after the rewarded ad loads
rewardedAd.enableRewardVerification()

// when showing
rewardedAd.show(activity) { /* AdMob's own reward callback */ }

// RevenueCat verification callbacks:
//   rewardVerificationStarted   -> show a "VERIFYING…" arcade spinner
//   rewardVerificationCompleted -> grant/refresh UI from the verified result
```

Use `loadAndTrack` / `setTrackingFullScreenContentCallback` so impression, click, and revenue
events flow to RevenueCat with no extra code.

**Constraint to design around:** an ad unit can carry **at most one virtual currency reward**,
though it may carry multiple entitlement rewards. So we need **two separate rewarded ad
units** — one that grants coins, one that grants the temporary Pro entitlement.

### The four placements

Every one is user-initiated and priced in plain language before the ad starts.

**1. `INSERT COIN` — the CONTINUE? screen ★ headline placement**
Free user, second draw of the day. The arcade continue screen offers `INSERT COIN`
(rewarded ad → 1 coin → draw again). Thematically perfect: in an arcade, continuing costs a
coin. The countdown is atmosphere and **loops rather than locking anyone out**.

**2. `FREE PLAY MODE` — ad grants 60 minutes of real Pro ★ the differentiator**
RevenueCat can grant a **temporary entitlement** (minimum 30 minutes) as a server-verified
ad reward. We grant the genuine `pro` entitlement for **60 minutes**, presented as an arcade
`FREE PLAY` banner with a live countdown in the top bar.

This is the strongest idea in the monetization design:
- The user gets *real* premium — unlimited draws, every share theme, full Steam import.
- It is the best possible conversion mechanism: people who feel Pro for an hour buy Pro.
- It is server-verified, so it can't be gamed.
- And it makes the ad unambiguously **additive** — it hands the user *more app*, which is
  exactly what the Catvertising criteria ask for.

Offered at: the CONTINUE? screen, the locked-theme sheet, and the Steam import wall.
Limit 2/day so it stays a taste rather than a replacement for subscribing.

**3. Share-theme single use**
A locked premium share theme can be used once via a rewarded ad instead of 25 coins.
The user wanted the theme; the ad is the price they chose. Purely cosmetic, zero harm.

**4. Steam import unlock**
Free import shows 10 of N games with the rest blurred. One rewarded ad unlocks the full
import once. High perceived value, and it happens at the exact moment the user most wants
the app to work.

### Placements we explicitly rejected — say this in the submission

Naming what you refused is strong evidence of intent:
- ❌ App-open interstitials — the fastest way to make a daily-habit app un-daily
- ❌ Banner ads — they'd wreck a design-award layout and earn pennies
- ❌ Forced ads between draws — would poison the core loop
- ❌ Ads anywhere in the Credits Roll — never monetize the emotional peak

### Pro users see zero ads
And Pro's coin stipend means they never need the coin economy either. Removing ads must be a
*real*, complete benefit, not a reduction in frequency.

---

## Analytics to instrument

Because the Catvertising write-up is far more convincing with numbers:

- Rewarded ad: offered → started → completed → verified (funnel with drop-off)
- Coins earned by source; coins spent by sink
- `FREE PLAY` grants → conversion to paid within 24h ← **the headline metric**
- Paywall views by trigger, and conversion per trigger
- D1 / D7 retention, split by whether the user ever completed a draw

RevenueCat Charts v3 gives ad revenue + subscription revenue in one LTV view — screenshot
that for the submission.

---

## Setup checklist (RevenueCat dashboard, via MCP where possible)

- [x] Create project `CONTINUE` → `proj747a0e7c`
- [x] Add the Play Store app → `app28ef647c38`, package `com.mikhilnaika.continueapp`
- [x] Public Android SDK key → `goog_uEkBWERrtERDlPxqQbcYpxvxXEH`
- [x] Entitlement `pro` → `entl9d1d013bd1`
- [x] Virtual currency `COIN`
- [ ] Products: 3 subscriptions + 3 consumable coin packs *(blocked on Play Console — the
      products must exist in Play before RevenueCat can sync them)*
- [ ] Offering `default` with packages `$rc_monthly`, `$rc_annual`, `$rc_lifetime`
- [ ] Offering `coins` with the three consumable packages
- [ ] Virtual currency `COIN`, with the coin packs associated for auto-grant
- [ ] Enable Charts v3
- [ ] Connect AdMob; create 2 rewarded units (coins / temporary-Pro)
- [ ] Reward rules: unit A → 1 `COIN`; unit B → `pro` entitlement for 60 minutes
- [ ] Build and publish the paywall
- [ ] Configure Customer Center
- [ ] Store the secret API key **only** as a Cloudflare Worker secret
