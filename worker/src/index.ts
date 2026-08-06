import type { Env, GameProvider, SearchResponse } from "./types.ts";
import { cached, CacheTtl } from "./kv.ts";
import { SeedGameProvider } from "./providers/SeedGameProvider.ts";
import { IgdbGameProvider } from "./providers/IgdbGameProvider.ts";
import { LiveTwitchAuthClient } from "./twitch/TwitchAuthClient.ts";
import { resolveGame } from "./resolve/resolveGame.ts";
import { getSteamOwnedGames } from "./routes/steamOwned.ts";
import { spendCoins } from "./routes/coinsSpend.ts";

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
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
      if (url.pathname === "/health") {
        return json({ ok: true, provider: env.TWITCH_CLIENT_ID ? "igdb" : "seed" });
      }

      if (url.pathname === "/games/search" && request.method === "GET") {
        const q = url.searchParams.get("q") ?? "";
        const results = await cached(env, `search:${q.toLowerCase()}`, CacheTtl.SEARCH, () => provider.search(q));
        return json({ results } satisfies SearchResponse);
      }

      if (url.pathname === "/games/trending" && request.method === "GET") {
        const results = await cached(env, "trending", CacheTtl.TRENDING, () => provider.trending());
        return json({ results } satisfies SearchResponse);
      }

      if (url.pathname === "/games/short" && request.method === "GET") {
        const results = await cached(env, "short", CacheTtl.SHORT, () => provider.shortAndSweet());
        return json({ results } satisfies SearchResponse);
      }

      const detailMatch = url.pathname.match(/^\/games\/(\d+)$/);
      if (detailMatch && request.method === "GET") {
        const id = Number(detailMatch[1]);
        const game = await cached(env, `detail:${id}`, CacheTtl.DETAIL, () => provider.detail(id));
        return game ? json(game) : json({ error: "NOT_FOUND" }, 404);
      }

      if (url.pathname === "/resolve" && request.method === "POST") {
        const payload = (await request.json()) as { text?: string | null; subject?: string | null };
        const response = await resolveGame(env, provider, payload.text ?? null, payload.subject ?? null);
        return json(response);
      }

      if (url.pathname === "/steam/owned" && request.method === "GET") {
        const vanity = url.searchParams.get("vanity") ?? "";
        const result = await getSteamOwnedGames(env, provider, vanity);
        return json(result.body, result.status);
      }

      if (url.pathname === "/coins/spend" && request.method === "POST") {
        const payload = (await request.json()) as { appUserId: string; amount: number; sku: string };
        const result = await spendCoins(env, payload);
        return json(result.body, result.status);
      }

      return json({ error: "NOT_FOUND" }, 404);
    } catch (error) {
      console.error(error);
      return json({ error: "INTERNAL_ERROR", message: (error as Error).message }, 500);
    }
  },
};
