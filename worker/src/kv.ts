import type { Env } from "./types.ts";

/**
 * Bump this whenever a provider query changes shape or a bad response got cached.
 *
 * Earned the hard way: a deprecated IGDB filter made every list endpoint return `[]` with a
 * 200, and `cached()` dutifully stored those empties for 6–24h — so even after the query was
 * fixed the Worker kept serving nothing. Versioning the key space invalidates the whole cache
 * atomically on deploy; the orphaned old keys expire on their own TTL and cost nothing.
 *
 * v1 → v2 (2026-08-12): `category` → `game_type`, real playtimes, rebuilt trending/short.
 * v2 → v3 (2026-08-14): `backgroundUrl` populated from screenshots/artworks (was always null,
 *   which left the Credits Roll upscaling a 264px cover to full screen).
 *
 * Note the 2026-08-19 rails (new releases, hidden gems, genre) deliberately did **not** bump
 * this: they only add new keys, and no existing query changed shape. Bumping would have cold-
 * started every cache while the closed test was live, for nothing.
 */
const CACHE_VERSION = "v4";

/** Cache-aside helper — IGDB allows only 4 req/sec, so caching is what survives a judging spike. */
export async function cached<T>(
  env: Env,
  key: string,
  ttlSeconds: number,
  compute: () => Promise<T>,
): Promise<T> {
  const versionedKey = `${CACHE_VERSION}:${key}`;
  const hit = await env.CACHE.get(versionedKey, "json");
  if (hit !== null) return hit as T;

  const value = await compute();
  // An empty list is almost always a symptom (bad filter, provider hiccup), not a real answer
  // worth remembering for 24 hours. Skipping the write keeps a transient failure transient.
  if (Array.isArray(value) && value.length === 0) return value;

  // waitUntil isn't available here without ctx; callers on the hot path accept the small
  // latency cost of awaiting the put so a slow KV write never drops the cache silently.
  await env.CACHE.put(versionedKey, JSON.stringify(value), { expirationTtl: ttlSeconds });
  return value;
}

export const CacheTtl = {
  SEARCH: 60 * 60 * 24, // 24h
  DETAIL: 60 * 60 * 24 * 7, // 7d
  TRENDING: 60 * 60 * 6, // 6h
  SHORT: 60 * 60 * 24, // 24h
  NEW_RELEASES: 60 * 60 * 12, // 12h — the window moves daily, but not hourly
  HIDDEN_GEMS: 60 * 60 * 24 * 3, // 3d — critic scores on older games barely move
  GENRES: 60 * 60 * 24 * 30, // 30d — IGDB's genre table is effectively static
  GENRE: 60 * 60 * 24, // 24h
  RESOLVE: 60 * 60 * 24 * 30, // 30d — the same viral video gets shared many times
  TWITCH_TOKEN: 60 * 60 * 24 * 55, // Twitch tokens last ~60 days; refresh a few days early
};
