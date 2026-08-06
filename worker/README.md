# CONTINUE? Worker

Cloudflare Worker backing the Android app — game data proxy, share-target URL resolution,
and (eventually) coin spend verification. See `docs/05-TECH-ARCHITECTURE.md` for the full
contract and `docs/08-GAME-DATA.md` for why IGDB is behind this instead of called directly.

## Status

No Twitch/IGDB credentials exist yet (`docs/09-PENDING-INPUTS.md`). Every route works today
against the bundled offline seed set (`app/src/main/assets/seed_games.json`, shared with the
Android app so there's one source of truth) via `SeedGameProvider`. Once
`TWITCH_CLIENT_ID`/`TWITCH_CLIENT_SECRET` are set, `src/index.ts` automatically switches to
`IgdbGameProvider` — no other code changes needed.

## Local development

```bash
cd worker
npm install
npm run dev        # wrangler dev — serves on localhost, no live secrets required
npm test           # runs the Stage-2 title-parser unit tests (node --test)
npm run typecheck  # tsc --noEmit
```

`npm run dev` works with zero configuration: KV falls back to wrangler's local emulation and
the game provider falls back to the seed set.

## Deploying for real

```bash
wrangler login
wrangler kv namespace create CACHE      # paste the returned id into wrangler.toml
wrangler secret put TWITCH_CLIENT_ID
wrangler secret put TWITCH_CLIENT_SECRET
wrangler secret put STEAM_API_KEY           # optional, needed for /steam/owned
wrangler secret put REVENUECAT_SECRET_KEY   # optional, needed for /coins/spend
npm run deploy
```

Then point the Android app's `local.properties` `WORKER_BASE_URL` at the deployed URL.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| GET | `/health` | Reports which provider is active (`igdb` or `seed`) |
| GET | `/games/search?q=` | 24h KV cache |
| GET | `/games/:id` | 7d KV cache |
| GET | `/games/trending` | 6h KV cache |
| GET | `/games/short` | 24h KV cache; "highly rated, short" |
| POST | `/resolve` | `{ text, subject }` → ranked candidates; two-stage pipeline |
| GET | `/steam/owned?vanity=` | 501 until `STEAM_API_KEY` is set |
| POST | `/coins/spend` | 501 until `REVENUECAT_SECRET_KEY` is set — never fakes a balance |

## IGDB attribution

Any screen in the app that shows data returned by these endpoints (once IGDB is live) must
display "The data was freely provided by IGDB.com" with their logo and a live link —
non-negotiable per `docs/08-GAME-DATA.md` and the project's `CLAUDE.md`.
