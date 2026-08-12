import type { Env } from "./types.ts";

/**
 * Abuse controls for a Worker that is, by necessity, an **unauthenticated public endpoint**.
 *
 * The app has no accounts and no server-issued credentials, so there is no secret we could
 * ship in the APK that wouldn't be extractable in minutes — anything embedded in a public
 * open-source Android app is public. The realistic defence is therefore not authentication
 * but **making abuse unprofitable and bounded**: cap what any one caller can do, cap what
 * *everyone together* can do, and never let one cheap request cause expensive downstream work.
 *
 * What's actually at risk (all of it free-tier, all of it exhaustible in minutes by a script):
 * - **Workers KV writes — 1,000/day.** The tightest limit by far and the one that bites first.
 *   Every uncached search is one write, so ~1,000 unique queries breaks caching for the day
 *   and every later request falls through to IGDB.
 * - **IGDB — 4 requests/second.** Sustained abuse gets the Twitch app rate-limited or banned.
 *   Losing IGDB access mid-competition would be unrecoverable inside the deadline.
 * - **Workers requests — 100,000/day.**
 */

/** Requests per minute per IP for ordinary read endpoints. Generous for a human, useless for a bot. */
export const PER_IP_LIMIT_NOTE = "see [[ratelimits]] in wrangler.toml";

/**
 * Longest accepted search query. Also keeps the derived KV key under KV's 512-byte key limit —
 * without this an over-long query throws inside `cached()` instead of failing cleanly here.
 */
export const MAX_QUERY_LENGTH = 100;

/** Longest accepted shared text. Real captions are ~100 chars; this is already very generous. */
export const MAX_SHARE_TEXT_LENGTH = 2_000;

/** Hard ceiling on request body size, checked before parsing. */
export const MAX_BODY_BYTES = 8_192;

/**
 * Cap on how many owned Steam games get matched against the provider in one import.
 *
 * Without a cap this endpoint is the worst amplifier in the codebase: a 2,000-game Steam
 * library would fan one inbound request out to 2,000 IGDB searches, blowing both the 4 req/sec
 * ceiling and the daily KV write budget from a single curl.
 */
export const MAX_STEAM_GAMES = 100;

/** Cloudflare always sets this; the fallback only matters for local `wrangler dev`. */
export function clientKey(request: Request): string {
  return request.headers.get("CF-Connecting-IP") ?? "local-dev";
}

export interface RateLimitDecision {
  allowed: boolean;
  scope?: "ip" | "global";
}

/**
 * Two independent gates, because they stop different attacks:
 *
 * - **Per-IP** stops one script hammering us.
 * - **Global** stops a *distributed* attack (or an unexpected traffic spike) from draining the
 *   shared IGDB and KV budgets, since a per-IP limit alone is worthless against many IPs.
 *   Keyed on a constant so every request in the account shares one counter.
 *
 * Fails **open** if the binding is missing, so a misconfigured deploy degrades to today's
 * behaviour rather than taking the whole API down.
 */
export async function checkRateLimits(
  env: Env,
  request: Request,
  limiter: RateLimit | undefined,
): Promise<RateLimitDecision> {
  if (limiter) {
    const perIp = await limiter.limit({ key: clientKey(request) });
    if (!perIp.success) return { allowed: false, scope: "ip" };
  }
  if (env.GLOBAL_LIMITER) {
    const global = await env.GLOBAL_LIMITER.limit({ key: "igdb-fanout" });
    if (!global.success) return { allowed: false, scope: "global" };
  }
  return { allowed: true };
}

/** Trims and length-caps a query. Returns null when there's nothing worth searching for. */
export function sanitizeQuery(raw: string | null): string | null {
  const trimmed = (raw ?? "").trim();
  if (trimmed.length === 0) return null;
  return trimmed.slice(0, MAX_QUERY_LENGTH);
}

/** Same idea for shared text/subject from the Android share sheet. */
export function sanitizeShareText(raw: unknown): string | null {
  if (typeof raw !== "string") return null;
  const trimmed = raw.trim();
  if (trimmed.length === 0) return null;
  return trimmed.slice(0, MAX_SHARE_TEXT_LENGTH);
}

/**
 * Reads a JSON body with a size ceiling, returning null rather than throwing.
 *
 * `Content-Length` is attacker-controlled, so it's only an early-out; the decoded length is
 * re-checked after reading.
 */
export async function readJsonBody<T>(request: Request): Promise<T | null> {
  const declared = Number(request.headers.get("Content-Length") ?? "0");
  if (declared > MAX_BODY_BYTES) return null;
  const text = await request.text().catch(() => null);
  if (text === null || text.length > MAX_BODY_BYTES) return null;
  try {
    return JSON.parse(text) as T;
  } catch {
    return null;
  }
}

/**
 * Exact-host or true-subdomain match.
 *
 * Replaces `hostname.includes("tiktok.com")`, which was a genuine SSRF hole: the Worker
 * fetches URLs the *client* supplies, and `vm.tiktok.com.attacker.example` contains the
 * substring `vm.tiktok.com`, so an attacker could have pointed our server-side fetch at any
 * host they controlled. Suffix matching must be anchored on a dot boundary.
 */
export function hostMatches(hostname: string, domain: string): boolean {
  const host = hostname.toLowerCase().replace(/^www\./, "");
  const target = domain.toLowerCase();
  return host === target || host.endsWith(`.${target}`);
}
