import type { Env, GameDto, GameProvider, ResolveCandidate, ResolveResponse } from "../types.ts";
import { extractCandidates } from "./titleParser.ts";
import { looksLikeUrl, resolveUrlToText } from "./resolveUrl.ts";
import { cached, CacheTtl } from "../kv.ts";

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

async function matchCandidates(candidates: string[], provider: GameProvider): Promise<ResolveCandidate[]> {
  const scored = new Map<number, ResolveCandidate>();
  for (const candidate of candidates) {
    const results = await provider.search(candidate);
    for (const game of results) {
      const confidence = scoreMatch(candidate, game.name);
      if (confidence <= 0) continue;
      const existing = scored.get(game.id);
      if (!existing || existing.confidence < confidence) {
        scored.set(game.id, { id: game.id, name: game.name, confidence, coverUrl: game.coverUrl });
      }
    }
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

  const localCandidates = extractCandidates(resolvedTitle);
  const searchStrings = localCandidates.length > 0 ? localCandidates : [resolvedTitle];
  const candidates = await matchCandidates(searchStrings, provider);

  return {
    resolvedTitle,
    source,
    candidates,
    needsManualEntry: candidates.length === 0,
  };
}
