import type { Env, GameProvider, GenreRef, SearchResponse } from "./types.ts";
import { cached, CacheTtl } from "./kv.ts";
import { SeedGameProvider } from "./providers/SeedGameProvider.ts";
import { IgdbGameProvider } from "./providers/IgdbGameProvider.ts";
import { LiveTwitchAuthClient } from "./twitch/TwitchAuthClient.ts";
import { resolveGame } from "./resolve/resolveGame.ts";
import { getSteamOwnedGames } from "./routes/steamOwned.ts";
import { spendCoins } from "./routes/coinsSpend.ts";
import { assetLinksBody, gameLinkResponse, sanitizeCampaign } from "./routes/gameLink.ts";
import {
  checkRateLimits,
  MAX_BATCH_IDS,
  PAGE_SIZE,
  readJsonBody,
  sanitizeIds,
  sanitizePage,
  sanitizeQuery,
  sanitizeShareText,
} from "./security.ts";

/**
 * No `Access-Control-Allow-Origin`, deliberately.
 *
 * The only intended client is a native Android app, which neither sends `Origin` nor enforces
 * CORS — so the header buys us nothing. It previously said `*`, which let **any web page**
 * use this Worker as a free, unattributed games API on our IGDB quota. Omitting it makes
 * browsers refuse to read our responses cross-origin, which removes the cheapest way to
 * freeload. (It is not a security boundary against scripted clients — rate limiting is.)
 */
function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "X-Content-Type-Options": "nosniff",
      "Cache-Control": "no-store",
    },
  });
}

/**
 * docs/08-GAME-DATA.md's structural lesson from losing RAWG mid-planning: the provider is
 * swappable in one place. Falls back to the bundled seed set until Twitch credentials exist
 * (docs/09-PENDING-INPUTS.md) — every route below stays fully testable either way.
 */
function selectProvider(env: Env): GameProvider {
  if (env.TWITCH_CLIENT_ID && env.TWITCH_CLIENT_SECRET) {
    return new IgdbGameProvider(env, new LiveTwitchAuthClient(env));
  }
  return new SeedGameProvider();
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const provider = selectProvider(env);

    try {
      /**
       * Digital Asset Links — answered before anything else, including rate limiting.
       *
       * This is a static constant served from memory: no IGDB call, no KV read, nothing worth
       * rationing. More importantly, the clients are Android's install-time verifier and
       * Google's crawler rather than our app, and a 429 to *them* doesn't slow an attacker
       * down — it silently un-verifies the App Links for whoever was installing, and every
       * shared game link starts opening a browser instead of the app with nothing anywhere
       * reporting an error. Exempt on purpose; see routes/gameLink.ts.
       */
      if (url.pathname === "/.well-known/assetlinks.json") {
        return new Response(JSON.stringify(assetLinksBody()), {
          headers: {
            "Content-Type": "application/json",
            "X-Content-Type-Options": "nosniff",
            // Android re-verifies on install and on update. A stale copy at the edge is
            // exactly how a fingerprint correction fails to take effect.
            "Cache-Control": "no-store",
          },
        });
      }

      // Rate limiting runs before routing, so an unknown path costs an attacker the same
      // budget as a real one and can't be used to probe for free. `/health` is exempt only in
      // its shallow form — the `?deep=1` variant spends a real IGDB request, so it's gated
      // below with everything else.
      const isExpensive = url.pathname === "/resolve";
      const isShallowHealth = url.pathname === "/health" && url.searchParams.get("deep") !== "1";
      if (!isShallowHealth) {
        const limiter = isExpensive ? env.RESOLVE_LIMITER : env.API_LIMITER;
        const decision = await checkRateLimits(env, request, limiter);
        if (!decision.allowed) {
          return json(
            {
              error: "RATE_LIMITED",
              scope: decision.scope,
              message: "Too many requests — slow down and try again shortly.",
            },
            429,
          );
        }
      }

      if (url.pathname === "/health") {
        const provider_name = env.TWITCH_CLIENT_ID ? "igdb" : "seed";
        // `?deep=1` actually runs a query instead of just asserting credentials exist. The
        // shallow check reported a healthy "igdb" for a Worker whose every list endpoint was
        // returning zero games (deprecated `category` filter — see IgdbGameProvider), so the
        // one signal we had was the one that couldn't catch it. Opt-in because it spends an
        // uncached IGDB request against the 4 req/sec ceiling.
        if (url.searchParams.get("deep") === "1") {
          const sample = await provider.search("zelda").catch((e: Error) => e);
          if (sample instanceof Error) {
            return json({ ok: false, provider: provider_name, error: sample.message }, 503);
          }
          return json({ ok: sample.length > 0, provider: provider_name, sampleCount: sample.length });
        }
        return json({ ok: true, provider: provider_name });
      }

      if (url.pathname === "/games/search" && request.method === "GET") {
        // Length-capped before use: an over-long query would otherwise be spent on an IGDB
        // request *and* build a KV key past KV's 512-byte limit, throwing inside `cached()`.
        const q = sanitizeQuery(url.searchParams.get("q"));
        if (!q) return json({ results: [] } satisfies SearchResponse);
        const results = await cached(env, `search:${q.toLowerCase()}`, CacheTtl.SEARCH, () => provider.search(q));
        return json({ results } satisfies SearchResponse);
      }

      // Every rail takes `?page=0..2` — the "SHOW MORE" button. The page number is part of
      // the cache key, so page 0 stays exactly as cheap as it was.
      if (url.pathname === "/games/trending" && request.method === "GET") {
        const p = sanitizePage(url.searchParams.get("page"));
        const results = await cached(env, `trending:p${p}`, CacheTtl.TRENDING, () =>
          provider.trending(p * PAGE_SIZE),
        );
        return json({ results } satisfies SearchResponse);
      }

      if (url.pathname === "/games/short" && request.method === "GET") {
        const p = sanitizePage(url.searchParams.get("page"));
        const results = await cached(env, `short:p${p}`, CacheTtl.SHORT, () =>
          provider.shortAndSweet(p * PAGE_SIZE),
        );
        return json({ results } satisfies SearchResponse);
      }

      if (url.pathname === "/games/new" && request.method === "GET") {
        const p = sanitizePage(url.searchParams.get("page"));
        const results = await cached(env, `new:p${p}`, CacheTtl.NEW_RELEASES, () =>
          provider.newReleases(p * PAGE_SIZE),
        );
        return json({ results } satisfies SearchResponse);
      }

      if (url.pathname === "/games/gems" && request.method === "GET") {
        const p = sanitizePage(url.searchParams.get("page"));
        const results = await cached(env, `gems:p${p}`, CacheTtl.HIDDEN_GEMS, () =>
          provider.hiddenGems(p * PAGE_SIZE),
        );
        return json({ results } satisfies SearchResponse);
      }

      /**
       * Refresh many already-known games at once.
       *
       * Deliberately **not** cached in KV. The app calls this with whatever ids happen to be in
       * one user's pile, so the key would be user-shaped and every distinct pile would burn a
       * write out of the 1,000/day budget — the exact failure mode the genre route is written
       * to avoid. Uncached it costs two IGDB requests and zero writes, and the app only calls
       * it when it actually holds stale rows.
       */
      if (url.pathname === "/games/batch" && request.method === "GET") {
        const ids = sanitizeIds(url.searchParams.get("ids"), MAX_BATCH_IDS);
        if (ids.length === 0) return json({ results: [] } satisfies SearchResponse);
        const results = await provider.byIds(ids);
        return json({ results } satisfies SearchResponse);
      }

      /**
       * A genre rail, addressed by **name** because that's what the app has — the genre strings
       * on its own cached games — and it should never have to hardcode IGDB's ids.
       *
       * The name is resolved to an id in memory against a cached copy of the whole genre table,
       * and only the resolved id is ever used as a cache key. That ordering is the point: keying
       * on the client's string instead would let anyone mint unlimited KV entries, against a
       * 1,000 writes/day quota that is the tightest limit in the whole stack (docs/12-SECURITY.md).
       * Unresolvable names cost one cheap lookup and write nothing.
       */
      if (url.pathname === "/games/genre" && request.method === "GET") {
        const name = sanitizeQuery(url.searchParams.get("name"));
        if (!name) return json({ results: [] } satisfies SearchResponse);
        const table = await cached<GenreRef[]>(env, "genres", CacheTtl.GENRES, () => provider.genres());
        const match = table.find((g) => g.name.toLowerCase() === name.toLowerCase());
        if (!match) return json({ results: [] } satisfies SearchResponse);
        const p = sanitizePage(url.searchParams.get("page"));
        const results = await cached(env, `genre:${match.id}:p${p}`, CacheTtl.GENRE, () =>
          provider.byGenre(match.id, p * PAGE_SIZE),
        );
        return json({ results } satisfies SearchResponse);
      }

      // `\d{1,9}` rather than `\d+`: an arbitrarily long digit string becomes an unusable
      // `Number` and would still cost a provider call and a KV key to discover that.
      const detailMatch = url.pathname.match(/^\/games\/(\d{1,9})$/);
      if (detailMatch && request.method === "GET") {
        const id = Number(detailMatch[1]);
        const game = await cached(env, `detail:${id}`, CacheTtl.DETAIL, () => provider.detail(id));
        return game ? json(game) : json({ error: "NOT_FOUND" }, 404);
      }

      /**
       * The friend-loop landing page — `/g/<igdbId>`, optionally `?c=pick|dare|cleared`.
       *
       * Same `\d{1,9}` bound and the same `detail:<id>` cache key as `/games/<id>` above, so a
       * share link for an already-seen game costs nothing at all. `canonicalUrl` is rebuilt
       * from our own path rather than echoing `request.url`, which would put any query string
       * an attacker appended into the page's `og:url` and `<link rel=canonical>`.
       */
      const shareMatch = url.pathname.match(/^\/g\/(\d{1,9})$/);
      if (shareMatch && request.method === "GET") {
        const id = Number(shareMatch[1]);
        const campaign = sanitizeCampaign(url.searchParams.get("c"));
        const canonical = `${url.origin}/g/${id}`;
        return await gameLinkResponse(env, provider, id, campaign, canonical);
      }

      if (url.pathname === "/resolve" && request.method === "POST") {
        const payload = await readJsonBody<{ text?: unknown; subject?: unknown }>(request);
        if (payload === null) return json({ error: "BAD_REQUEST" }, 400);
        // Length-capped: `rankedCandidates` generates word windows, so cost grows with input
        // size — an unbounded body would be a cheap way to burn Worker CPU time.
        const response = await resolveGame(
          env,
          provider,
          sanitizeShareText(payload.text),
          sanitizeShareText(payload.subject),
        );
        return json(response);
      }

      if (url.pathname === "/steam/owned" && request.method === "GET") {
        const vanity = sanitizeQuery(url.searchParams.get("vanity"));
        if (!vanity) return json({ error: "BAD_REQUEST" }, 400);
        const result = await getSteamOwnedGames(env, provider, vanity);
        return json(result.body, result.status);
      }

      if (url.pathname === "/coins/spend" && request.method === "POST") {
        const payload = await readJsonBody<{ appUserId?: unknown; amount?: unknown; sku?: unknown }>(request);
        if (
          payload === null ||
          typeof payload.appUserId !== "string" ||
          typeof payload.amount !== "number" ||
          typeof payload.sku !== "string"
        ) {
          return json({ error: "BAD_REQUEST" }, 400);
        }
        const result = await spendCoins(env, {
          appUserId: payload.appUserId.slice(0, 128),
          amount: payload.amount,
          sku: payload.sku.slice(0, 64),
        });
        return json(result.body, result.status);
      }

      return json({ error: "NOT_FOUND" }, 404);
    } catch (error) {
      // Log the real error for `wrangler tail`, but never return it: internal messages have
      // leaked provider URLs and query syntax before, which is free reconnaissance.
      console.error(error);
      return json({ error: "INTERNAL_ERROR" }, 500);
    }
  },
};
