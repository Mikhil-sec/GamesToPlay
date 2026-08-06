<div align="center">

# CONTINUE?

**Your pile of shame, finally fun.**

A gaming bucket list for Android that works like an arcade cabinet.

*Built for RevenueCat Shipaton 2026*

</div>

---

## The problem

Every gamer has a backlog. Mine has 87 games in it.

Every app built to manage that backlog is a spreadsheet with box art — and a spreadsheet of
87 unplayed games is a guilt machine. That's why people stop opening them.

CONTINUE? reframes the backlog as an arcade continue screen. In an arcade, `CONTINUE?` was
never failure. It was a second chance, a countdown, and a coin.

## What it does

| | |
|---|---|
| **Save** | Share any game into your pile from anywhere on Android — a YouTube review, a Reddit thread, a Steam page. A sheet slides up over the app you're already in and adds it in ~2 seconds. Or import your entire Steam library in one tap. |
| **Organize** | Stacks, smart filters, a "shortest first" view, and a hard cap of three games in NOW PLAYING — because focus is a feature. A Time Budget bar tells you the truth: *412 hours, 87 games, done in 2029.* |
| **Decide** | The real problem is paralysis. Set your available time, your mood, and the console you're near, then pull the lever. It deals three cards from your own pile and explains why each matched. |
| **Complete** | Clearing a game plays a full Credits Roll — your stats scrolling like film credits over the key art. |
| **Rate** | No stars. Two games go head to head, and a binary search places the winner in your personal all-time ranking. You don't get an 8/10 — you get *your #4 of all time*. |
| **Share** | Your top ten as an arcade high-score table. Your pile as a stat card that says *"412 hours. 87 games. Send help."* |

## Built with

**Kotlin** · **Jetpack Compose** · Hilt · Room · Retrofit · Coil ·
**RevenueCat** (`purchases-android`) · **Google AdMob** · **Cloudflare Workers**

## Architecture

Offline-first. Room is the source of truth and the UI only ever reads from it; the network
fills the cache. Your pile works on a plane.

A small Cloudflare Worker sits between the app and the outside world for two specific
reasons: this repository is public, so it cannot ship API keys, and RevenueCat's
virtual-currency spend endpoint requires a secret key. Both live server-side.

```
┌─────────────┐     ┌──────────────────┐     ┌──────────┐
│  Compose UI │────▶│  Room (truth)    │◀────│  Worker  │──▶ IGDB
└─────────────┘     └──────────────────┘     └──────────┘──▶ Steam
       │                                          │
       │            ┌──────────────────┐          └────────▶ RevenueCat (secret)
       └───────────▶│  RevenueCat SDK  │
                    │  AdMob SDK       │
                    └──────────────────┘
```

Full design and architecture documentation lives in [`docs/`](docs/).

## Monetization

Three layers, unified by RevenueCat:

1. **Ads** — AdMob rewarded only. Always user-initiated. **No interstitials, no banners.**
2. **Coins** — RevenueCat Virtual Currency. Earned from server-verified ad rewards, bought
   as consumables, or granted monthly by a subscription.
3. **Pro** — subscription with a 7-day free trial.

The headline mechanic: a rewarded ad grants **60 minutes of real Pro access**, via
RevenueCat's server-verified temporary entitlements. Ads unlock the app rather than
interrupt it.

## Building it yourself

```bash
git clone <repo-url>
cd GamesToPlay
cp local.properties.example local.properties
# add your own REVENUECAT_PUBLIC_KEY, ADMOB_APP_ID, and WORKER_BASE_URL
./gradlew assembleDebug
```

The Worker lives in [`worker/`](worker/):

```bash
cd worker
npm install
wrangler secret put TWITCH_CLIENT_ID
wrangler secret put TWITCH_CLIENT_SECRET
wrangler secret put REVENUECAT_SECRET_KEY
wrangler deploy
```

## Attribution

Game data and artwork are provided by [IGDB](https://www.igdb.com). The data was freely
provided by IGDB.com.

## Licence

[MIT](LICENSE) © 2026 Mikhil Naika

---

<div align="center">
<sub>No influencer's name, likeness, or branding is used anywhere in this project.</sub>
</div>
