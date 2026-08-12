import type { Env, GameDto, GameProvider, ResolveCandidate, ResolveResponse } from "../types.ts";
import { rankedCandidates, verifyAgainstText } from "./candidates.ts";
import { looksLikeUrl, resolveUrlToText } from "./resolveUrl.ts";
import { cached, CacheTtl } from "../kv.ts";

/**
 * How many provider searches one resolve may spend. IGDB allows 4 req/sec (docs/08-GAME-DATA.md)
 * and a share must feel instant, so we try the best few guesses and stop — the ordering in
 * `rankedCandidates` is what makes a small budget sufficient.
 */
const MAX_SEARCHES = 6;

/** Stop early once a candidate is this convincing; more searching can't beat it. */
const CONFIDENT_ENOUGH = 0.9;

/** Very rough lexical overlap score — good enough to rank IGDB/seed search hits by how
 * closely they match a noisy candidate string, without pulling in a real fuzzy-match library. */
function scoreMatch(candidate: string, gameName: string): number {
  const a = candidate.toLowerCase().replace(/[^a-z0-9 ]/g, "").trim();
  const b = gameName.toLowerCase().replace(/[^a-z0-9 ]/g, "").trim();
  if (!a || !b) return 0;
  if (a === b) return 1;
  if (b.includes(a) || a.includes(b)) return 0.85;

  const aWords = new Set(a.split(/\s+/));
  const bWords = new Set(b.split(/\s+/));
  const overlap = [...aWords].filter((w) => bWords.has(w)).length;
  return overlap / Math.max(aWords.size, bWords.size, 1);
}

/**
 * Searches the ranked candidates in order and scores every hit against the **original**
 * caption, not against the candidate that found it.
 *
 * That distinction is what makes short guesses safe: searching the single word `"Palworld"`
 * is only allowed to win because the name `"Palworld"` is then confirmed to appear in the
 * caption. Anything the caption doesn't contain falls back to weak lexical overlap and loses.
 */
async function matchCandidates(
  env: Env,
  candidates: string[],
  provider: GameProvider,
  originalText: string,
): Promise<ResolveCandidate[]> {
  const scored = new Map<number, ResolveCandidate>();
  let best = 0;

  for (const candidate of candidates.slice(0, MAX_SEARCHES)) {
    // Same cache key space as `/games/search`, so a resolve warms the search cache and vice
    // versa — without this a single share could spend 6 uncached IGDB requests.
    const results = await cached(env, `search:${candidate.toLowerCase()}`, CacheTtl.SEARCH, () =>
      provider.search(candidate),
    ).catch(() => [] as GameDto[]);
    for (const game of results) {
      const verified = verifyAgainstText(originalText, game.name);
      const confidence = verified > 0 ? verified : scoreMatch(candidate, game.name) * 0.5;
      if (confidence <= 0) continue;
      const existing = scored.get(game.id);
      if (!existing || existing.confidence < confidence) {
        scored.set(game.id, { id: game.id, name: game.name, confidence, coverUrl: game.coverUrl });
      }
      best = Math.max(best, confidence);
    }
    if (best >= CONFIDENT_ENOUGH) break;
  }
  return [...scored.values()].sort((a, b) => b.confidence - a.confidence).slice(0, 5);
}

/**
 * The full `/resolve` pipeline — docs/05-TECH-ARCHITECTURE.md §`/resolve`. Stage 1 (URL to
 * text) only runs when the shared text actually looks like a bare URL; plain captions skip
 * straight to Stage 2.
 */
export async function resolveGame(
  env: Env,
  provider: GameProvider,
  text: string | null,
  subject: string | null,
): Promise<ResolveResponse> {
  const trimmed = text?.trim() ?? "";
  let resolvedTitle: string | null = null;
  let source = "text";

  if (trimmed && looksLikeUrl(trimmed)) {
    const cacheKey = `resolve:url:${trimmed}`;
    const urlResolution = await cached(env, cacheKey, CacheTtl.RESOLVE, () => resolveUrlToText(trimmed));
    resolvedTitle = urlResolution.text;
    source = urlResolution.source;

    if (resolvedTitle === null) {
      // Instagram and anything else unresolvable server-side — never dead-end, hand back to
      // the app's manual search field with the ladder's rung 3 (docs/02-PRODUCT-SPEC.md §2a).
      return { resolvedTitle: null, source, candidates: [], needsManualEntry: true };
    }
  } else {
    resolvedTitle = trimmed || subject || null;
  }

  if (!resolvedTitle) {
    return { resolvedTitle: null, source: "empty", candidates: [], needsManualEntry: true };
  }

  const searchStrings = rankedCandidates(resolvedTitle);
  const candidates = await matchCandidates(
    env,
    searchStrings.length > 0 ? searchStrings : [resolvedTitle],
    provider,
    resolvedTitle,
  );

  return {
    resolvedTitle,
    source,
    candidates,
    needsManualEntry: candidates.length === 0,
  };
}
