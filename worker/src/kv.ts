import type { Env } from "./types.ts";

/** Cache-aside helper — IGDB allows only 4 req/sec, so caching is what survives a judging spike. */
export async function cached<T>(
  env: Env,
  key: string,
  ttlSeconds: number,
  compute: () => Promise<T>,
): Promise<T> {
  const hit = await env.CACHE.get(key, "json");
  if (hit !== null) return hit as T;

  const value = await compute();
  // waitUntil isn't available here without ctx; callers on the hot path accept the small
  // latency cost of awaiting the put so a slow KV write never drops the cache silently.
  await env.CACHE.put(key, JSON.stringify(value), { expirationTtl: ttlSeconds });
  return value;
}

export const CacheTtl = {
  SEARCH: 60 * 60 * 24, // 24h
  DETAIL: 60 * 60 * 24 * 7, // 7d
  TRENDING: 60 * 60 * 6, // 6h
  SHORT: 60 * 60 * 24, // 24h
  RESOLVE: 60 * 60 * 24 * 30, // 30d — the same viral video gets shared many times
  TWITCH_TOKEN: 60 * 60 * 24 * 55, // Twitch tokens last ~60 days; refresh a few days early
};
