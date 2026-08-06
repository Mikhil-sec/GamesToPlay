# CONTINUE? — Game Data Source

## Why not RAWG

The original plan specified RAWG. **It was abandoned on Aug 5, 2026**, after direct testing:

```
200  <- https://www.google.com                     (control)
302  <- https://api.igdb.com                       (alive)
000  <- https://api.rawg.io/api/games?page_size=1  (unreachable)
000  <- https://rawg.io/                           (unreachable)
```

`api.rawg.io` returned connection failures from curl and HTTP 522 (Cloudflare cannot reach
origin) from every fetch. There is **no formal shutdown announcement**, but RAWG has been
widely described as unmaintained since 2024, with a history of multi-day total outages, and
other projects have already migrated away from it for exactly this reason.

**The judgement call:** even if RAWG returns, an unmaintained API with multi-day outages is
disqualifying for an app that judges may open at any unpredictable moment between now and
Sept 30. Every screen in this app depends on game data. We do not build the critical path on
abandonware.

**The structural lesson, applied:** the Worker exposes a `GameDataSource` abstraction so the
provider is swappable in one file. Having just been burned by a single-source dependency,
this is no longer theoretical — and it's exactly the "thoughtful technical choices" evidence
the Next Gen criteria ask for.

---

## IGDB (primary)

Owned by Twitch/Amazon, actively maintained, and richer than RAWG was.

### Auth — Twitch OAuth client credentials

1. Create a Twitch account and register an application at dev.twitch.tv/console/apps
   → gives a **Client ID** and **Client Secret**.
2. Exchange them for a token:
   ```
   POST https://id.twitch.tv/oauth2/token
     ?client_id=<id>&client_secret=<secret>&grant_type=client_credentials
   ```
3. Response includes `access_token` and `expires_in` (**~60 days**).
4. Every IGDB request carries:
   ```
   Client-ID: <client id>
   Authorization: Bearer <access token>
   ```

**Cache the token in Worker KV with its expiry** and refresh on 401. Do not fetch a new
token per request — that's a fast route to being rate limited.

### Rate limits — the thing that shapes the architecture

- **4 requests/second**, max **8 concurrent**. Over that returns `429`.

This is why **the app must never call IGDB directly**. Everything goes through the Worker,
which caches in KV. With 20 users hitting search simultaneously we'd blow the limit
instantly without it. Implement a small request queue in the Worker and treat `429` as
retryable with backoff.

### Querying — Apicalypse

IGDB uses a `POST` body query language, not query params:

```
POST https://api.igdb.com/v4/games
Body:
  search "elden ring";
  fields name, cover.image_id, first_release_date, genres.name, themes.name,
         game_modes.name, platforms.name, rating, aggregated_rating, summary;
  where category = 0 & version_parent = null;
  limit 20;
```

`category = 0 & version_parent = null` filters out DLC, bundles, and editions — without it
search results are full of near-duplicates. This matters a lot for match quality in the
share target.

Nested field access (`cover.image_id`, `genres.name`) means **one request** returns what
RAWG needed several for — which helps considerably against the 4/sec ceiling.

### Covers

```
https://images.igdb.com/igdb/image/upload/t_{size}/{image_id}.jpg
```
Sizes: `t_cover_small`, `t_cover_big`, `t_720p`, `t_1080p`.
Use `t_cover_big` for grids, `t_720p` for hero art in the Credits Roll.

### Time to beat — a genuine upgrade over RAWG

The `game_time_to_beat` endpoint returns **`hastily`, `normally`, and `completely`** (in
seconds) rather than RAWG's single averaged number.

This directly improves two features:
- **The TIME dial in DRAW** — "I have 2 hours" can match against `hastily`, while "a whole
  weekend" matches `completely`. The recommendation gets meaningfully smarter.
- **The Time Budget bar** — let the user choose whether their projection assumes rushing or
  completionist play. That's a better feature than the original plan had.

Not every game has this data; fall back to a genre-based median estimate and mark it as an
estimate in the UI.

### Mood mapping (revised for IGDB's taxonomy)

IGDB has `genres`, `themes`, **and** `game_modes` — a richer signal than RAWG's tags.

| Mood | IGDB matches |
|---|---|
| COZY | genres: Simulator, Puzzle, Indie · themes: Non-fiction, Comedy · *not* Horror |
| CHAOS | genres: Shooter, Fighting, Racing, Hack-and-slash · game_modes: Multiplayer, Battle Royale |
| STORY | genres: Adventure, Role-playing (RPG), Visual Novel · themes: Drama, Mystery |
| BRAIN | genres: Strategy, Tactical, Puzzle, Point-and-click · themes: Stealth |
| NOSTALGIA | `first_release_date` < 2010 · themes: Retro-style |

Keep this in a single editable table so it can be tuned without touching logic.

### Attribution — required

Display **"The data was freely provided by IGDB.com"** with their logo and a live link,
on any screen showing their data. Put it in Settings *and* at the foot of search results.

### Commercial use — action needed

IGDB's free tier covers non-commercial use and projects that *intend* to monetize, but
consumer projects that actually monetize are expected to reach out, and may be asked to
implement IGDB Login.

**Email `partner@igdb.com` this week**, explaining: a student hackathon entry, an Android
backlog app, a subscription and ads, expected volume in the low thousands of requests/day.
Keep the reply — the Shipaton rules require entrants to be *authorized* to use third-party
APIs under their terms, so written permission is compliance evidence. Eight weeks is plenty
of lead time; leaving it to week 7 is not.

---

## Steam (secondary, for the import feature)

Independent of IGDB and unaffected by any of this. Confirmed responding.

- `GetOwnedGames` (Steam Web API, needs a free key) → the user's library + playtime
- `store.steampowered.com/api/appdetails?appids=<id>` → no key needed, confirmed 200

Match Steam titles to IGDB by name, using IGDB's `external_games` endpoint where possible
(it carries Steam app IDs, giving exact matches instead of fuzzy ones).

---

## Fallbacks

Ranked, should IGDB fail or refuse commercial use:

1. **TheGamesDB** — free, community-run, confirmed responding (200). Weaker metadata,
   no time-to-beat.
2. **Giant Bomb API** — free with a key, confirmed responding (301). Good editorial data,
   stricter terms.
3. **Wikidata** — CC0, no restrictions or rate limits at all, but messy and cover art is
   poor. The nuclear option that can never be taken away.

## The offline seed set — now mandatory, not optional

Bundle a JSON asset of ~500 popular games (id, name, cover URL, genres, time-to-beat) in the
APK. Originally framed as insurance; after watching a primary data source vanish
mid-planning, treat it as a requirement.

It guarantees that search returns useful results, onboarding works, and **the demo video can
be recorded**, even if every upstream API is down on the day. Generate it once at build time
and commit it.
