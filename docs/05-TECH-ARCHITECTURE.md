# CONTINUE? — Technical Architecture

## Stack

| Concern | Choice |
|---|---|
| Language | Kotlin 2.x |
| UI | Jetpack Compose, Material 3 (heavily re-themed) |
| Architecture | MVVM + unidirectional data flow; `StateFlow` state holders |
| DI | Hilt |
| Local DB | Room (**source of truth**) |
| Prefs | DataStore (Proto or Preferences) |
| Networking | Retrofit + OkHttp + kotlinx.serialization |
| Images | Coil 3 |
| Navigation | Navigation Compose, type-safe routes |
| Purchases | `purchases-android` **10.12.0+**, `purchases-ui-android` |
| Ads | Google Mobile Ads SDK (AdMob) |
| Backend | Cloudflare Worker (TypeScript) + KV cache |
| Testing | JUnit, Turbine, Compose UI tests, Robolectric for parsers |

`minSdk 26` · `targetSdk 36` · Compose BOM latest stable · R8 full mode for release.

---

## Module layout

Single Gradle module to start (speed matters more than purity at this scale), organized
**feature-first**. Split into modules only if build times become painful.

```
app/src/main/java/com/<you>/continueapp/
├── core/
│   ├── design/          # theme, tokens, typography, motion specs
│   ├── ui/              # shared composables: GameCard, CoinCounter, ArcadeButton…
│   ├── data/            # Room db, DAOs, entities, DataStore
│   ├── network/         # Retrofit services, DTOs, Worker client
│   ├── billing/         # RevenueCat wrapper: entitlements, coins, paywall
│   ├── ads/             # AdMob wrapper + reward verification
│   └── util/            # title parser, haptics, share-image renderer
├── feature/
│   ├── onboarding/
│   ├── pile/            # library, stacks, time budget
│   ├── discover/        # search, rails
│   ├── draw/            # the machine, cards, CONTINUE? screen
│   ├── nowplaying/
│   ├── complete/        # credits roll
│   ├── rank/            # pairwise ranking
│   ├── share/           # card renderers
│   ├── profile/         # stats, trophies, settings
│   └── sharetarget/     # the share-sheet Activity
└── MainActivity.kt
```

---

## Data model (Room)

```kotlin
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: Long,            // IGDB id
    val slug: String,
    val name: String,
    val coverUrl: String?,
    val backgroundUrl: String?,
    val released: String?,               // ISO date
    val metacritic: Int?,
    val rating: Float?,                  // IGDB rating
    val playtimeHoursNormally: Int?,     // IGDB game_time_to_beat.normally
    val playtimeHoursHastily: Int?,      // IGDB game_time_to_beat.hastily
    val playtimeHoursCompletely: Int?,   // IGDB game_time_to_beat.completely
    val genresJson: String,              // denormalized: simpler, fast enough
    val tagsJson: String,
    val platformsJson: String,
    val cachedAt: Long,
)

@Entity(tableName = "pile_entries")
data class PileEntryEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val gameId: Long,
    val state: PileState,                // BACKLOG, PLAYING, COMPLETED, DROPPED, WISHLIST
    val addedAt: Long,
    val startedAt: Long?,
    val finishedAt: Long?,
    val droppedAt: Long?,
    val ownedPlatform: String?,          // which platform THEY own it on
    val source: AddSource,               // SEARCH, SHARE_TARGET, STEAM_IMPORT, CLIPBOARD
    val hoursPlayed: Float?,
    val pinnedUntil: Long?,              // "save for later"
    val snoozedUntil: Long?,             // "not tonight" — 14 days
    val lastDrawnAt: Long?,
    val notes: String?,
)

@Entity(tableName = "rankings")
data class RankingEntity(
    @PrimaryKey val gameId: Long,
    val bucket: RankBucket,              // LOVED, LIKED, FINE, NAH
    val position: Int,                   // global ordinal, 1 = best
    val verdict: String?,                // 140-char hot take
    val wouldReplay: Boolean,
    val rankedAt: Long,
)

@Entity(tableName = "stacks")
data class StackEntity(
    @PrimaryKey(autoGenerate = true) val stackId: Long = 0,
    val name: String,
    val emoji: String?,
    val sortOrder: Int,
    val createdAt: Long,
)

@Entity(tableName = "stack_members", primaryKeys = ["stackId", "gameId"])
data class StackMemberEntity(val stackId: Long, val gameId: Long, val sortOrder: Int)

@Entity(tableName = "draw_history")
data class DrawEntity(
    @PrimaryKey(autoGenerate = true) val drawId: Long = 0,
    val drawnAt: Long,
    val timeBudget: TimeBudget,
    val mood: Mood,
    val platformsJson: String,
    val gameIdsJson: String,
    val acceptedGameId: Long?,
)
```

**Design notes**
- `games` is a *cache* of IGDB data; `pile_entries` is *user data*. Only user data is
  precious — that separation makes cloud backup trivial and keeps us compliant with IGDB's
  terms (we cache only what a user's own pile needs).
- Denormalizing genres/tags/platforms to JSON is the right call at this scale: fewer joins,
  much less code, and we never query across users.
- Everything the UI reads comes from Room as `Flow`. The network only ever *fills* Room.

---

## The Cloudflare Worker (required, not optional)

Three things force a backend:

1. **IGDB/Twitch credentials must not ship in a public repo's APK**, and IGDB's 4 req/sec limit means calls must be centralized and cached.
2. **Spending virtual currency requires a RevenueCat *secret* key**, which likewise cannot
   be in the app.
3. **Share-target URL resolution** (oEmbed lookups for YouTube/Shorts/TikTok/Reddit) must be
   server-side, because these parsers break whenever a platform changes its markup — and a
   Worker fix ships in minutes while an app fix waits on Play review.

A single free-tier Worker covers all three. TypeScript, deployed with Wrangler, secrets via
`wrangler secret put`.

### Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/games/search?q=` | Proxy IGDB search; cache 24h in KV |
| `GET` | `/games/:id` | Proxy IGDB detail; cache 7d |
| `GET` | `/games/trending` | Curated rails; cache 6h |
| `GET` | `/games/short` | Highly-rated under 8h; cache 24h |
| `POST` | `/resolve` | **Share-target brain.** Takes raw shared text and/or a URL → returns ranked IGDB candidates |
| `GET` | `/steam/owned?vanity=` | Steam `GetOwnedGames` + IGDB matching |
| `POST` | `/coins/spend` | Validate + call RevenueCat to debit coins |
| `GET` | `/health` | Uptime |

### `/resolve` — the two-stage share pipeline

```
POST /resolve
{ "text": "https://youtube.com/shorts/abc123", "subject": null }

200 {
  "resolvedTitle": "I beat Elden Ring at level 1 #eldenring",
  "source": "youtube_shorts",
  "candidates": [
    { "id": 326243, "name": "Elden Ring", "confidence": 0.94, "coverUrl": "…" },
    { "id": 28,     "name": "Elden Ring: Shadow of the Erdtree", "confidence": 0.51, … }
  ]
}
```

**Stage 1 — URL → text.** Detect the platform, normalize the URL (Shorts `/shorts/<id>` →
`watch?v=<id>`; follow `vm.tiktok.com` redirects), then hit the public oEmbed endpoint.
YouTube's (`https://www.youtube.com/oembed?url=…&format=json`) needs no API key. Cache
resolutions in KV by URL — the same viral video gets shared many times.

**Stage 2 — text → game.** Hashtag un-concatenation first (`#eldenring` → "elden ring"),
then noise stripping, then scored IGDB matching. Return ranked candidates with confidence,
never a single forced answer.

Instagram Reels cannot be resolved (Meta removed public oEmbed in April 2025) — return
`{ "source": "instagram", "candidates": [], "needsManualEntry": true }` so the app can open
its search field instead of dead-ending.

**Keep the noise-stripping rules in a single data table**, so they can be tuned without
touching logic. Unit-test Stage 2 against real captured strings.

### `/coins/spend` contract

```
POST /coins/spend
{ "appUserId": "<RevenueCat app user id>", "amount": 1, "sku": "extra_draw" }

200 { "ok": true,  "newBalance": 12 }
402 { "ok": false, "error": "INSUFFICIENT_FUNDS", "balance": 0 }
```

The Worker validates the amount against a **server-side price table** — never trust a price
sent by the client — then calls RevenueCat's v2 virtual-currency transactions API with the
secret key. **Confirm the exact endpoint and payload against current RevenueCat docs when
implementing**; that API is newer than the rest of the SDK surface.

### Caching strategy
KV-cache aggressively. IGDB allows only **4 requests/second**, so caching is what makes the
app survive a judging spike at all. Cache by normalized query string; queue and back off on
429.

### Offline seed
Bundle a JSON asset of ~500 popular games (id, name, cover URL, genres, playtime) so search
returns useful results even with no network or a dead upstream API. After RAWG went dark
mid-planning this is a requirement, not insurance — it is what guarantees the demo video can
be recorded. See `docs/08-GAME-DATA.md`. It also protects against
the worst possible demo-day failure.

---

## RevenueCat integration shape

```kotlin
// Application.onCreate
Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
Purchases.configure(
    PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_PUBLIC_KEY).build()
)
```

The public SDK key is safe in the repo, but still read it from `local.properties` →
`BuildConfig` so the pattern is right and rotation is easy.

**`BillingRepository`** exposes:
```kotlin
val isPro: StateFlow<Boolean>            // from customerInfo.entitlements["pro"]?.isActive
val proExpiresAt: StateFlow<Long?>       // powers the FREE PLAY countdown
val coinBalance: StateFlow<Int>
suspend fun purchase(pkg: Package): PurchaseResult
suspend fun spendCoins(amount: Int, sku: String): SpendResult   // via the Worker
suspend fun refreshBalance()             // invalidate cache, then fetch
```

Register a `CustomerInfoUpdatedListener` so the temporary entitlement from an ad reward
flips `isPro` live — the FREE PLAY banner must appear without a restart.

**Gating:** one `ProGate` composable/helper. Never scatter `if (isPro)` through feature code.

---

## AdMob integration shape

```kotlin
class AdRepository {
    suspend fun loadCoinAd(): RewardedAd?        // unit A → grants 1 COIN
    suspend fun loadFreePlayAd(): RewardedAd?    // unit B → grants pro for 60 min
    suspend fun show(activity: Activity, ad: RewardedAd): AdResult
}
```

- Preload the next rewarded ad after each show, so `INSERT COIN` is never a spinner.
- Call `enableRewardVerification()` after load.
- On `rewardVerificationStarted`, show an arcade "VERIFYING…" state; on
  `rewardVerificationCompleted`, refresh balance/entitlement and play the coin animation.
- **Never grant client-side.** The verified callback is the only source of truth.
- Handle no-fill gracefully: if an ad fails to load, don't show a dead button — swap it for
  the coin-spend or Pro option and quietly log it.

---

## Share-image rendering

Render an offscreen Compose layout to a `Bitmap`:
1. Compose the card into a `ComposeView` sized to 1080×1920 (or 1080×1080 for square).
2. Draw to a `Bitmap` via `graphicsLayer.toImageBitmap()` (Compose) or a
   `PixelCopy`/`Canvas` path.
3. Write to `cacheDir/shares/`, expose via `FileProvider`, fire `ACTION_SEND` with
   `image/png` and `FLAG_GRANT_READ_URI_PERMISSION`.

Do the render off the main thread, and preload the box art with Coil **before** rendering —
a share card with a missing image is the classic bug here.

---

## Share Target implementation

```xml
<activity android:name=".feature.sharetarget.ShareTargetActivity"
          android:theme="@style/Theme.Transparent"
          android:excludeFromRecents="true"
          android:launchMode="singleTask">
    <intent-filter android:label="Add to CONTINUE?">
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>
    <!-- Screenshots: the Instagram fallback, and a headline feature in its own right -->
    <intent-filter android:label="Add to CONTINUE?">
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="image/*" />
    </intent-filter>
</activity>
```

Transparent Activity hosting a Compose bottom sheet. Reads `EXTRA_TEXT` / `EXTRA_SUBJECT`
(or `EXTRA_STREAM` for images), calls `/resolve`, shows the match. Finishes with a clean
dismiss transition.

**Image path:** run **ML Kit Text Recognition v2** on-device (`com.google.mlkit:text-
recognition`) over the shared bitmap. It's free, offline, and adds ~few MB. Feed the
extracted lines into `/resolve` as text. This handles Instagram Reels, plus screenshots of
Discord, X, store pages, and photos of physical game cases.

**The title parser must be a pure function with unit tests** — it's the one piece of logic
where correctness is directly visible to judges. Build a fixture file of real captured
strings from YouTube, YouTube Shorts, TikTok, Steam, and Reddit, and test against it.

---

## Performance requirements

- Cold start to first frame **< 1.5s** — use the Compose splash API, no blocking work in
  `Application.onCreate` beyond `Purchases.configure`.
- Pile scrolling at 120Hz with 200+ games — `LazyVerticalGrid` with stable keys, Coil
  memory cache, no allocations in the item composable.
- Baseline Profile for release — a genuinely large, cheap win for scroll jank.
- Ad and RevenueCat SDK init must never block the first frame.

---

## Testing priorities

Given the timeline, test only where breakage is invisible or catastrophic:

1. **Title parser** — heavy unit tests, many real-world strings
2. **Draw selection algorithm** — deterministic with a seeded RNG; assert filters and weights
3. **Ranking binary search** — property tests that the list stays correctly ordered
4. **Coin spend** — server rejection, insufficient funds, offline
5. **Entitlement gating** — Pro on/off, temporary entitlement expiry mid-session

Skip UI snapshot testing; it's not worth the time at this scale.

---

## Security & repo hygiene (public repo!)

- `local.properties` in `.gitignore`; keys read into `BuildConfig` at build time
- Keystore **never** committed — and **backed up in two places**, since losing it means
  never updating the app again
- CI (GitHub Actions): build + test on PR; run a secret scan (`gitleaks`) on every push
- Ship a `local.properties.example` and a README section so a judge can actually build it —
  Next Gen judges will try
- The Worker source lives in `/worker` in the same repo, with secrets in Wrangler only
