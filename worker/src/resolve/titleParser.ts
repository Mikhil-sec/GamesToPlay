/**
 * Stage 2 — text to candidate title strings. This is a TypeScript port of
 * app/.../core/util/TitleParser.kt, kept deliberately identical in behavior since both run
 * the exact same real-world captured strings (docs/05-TECH-ARCHITECTURE.md calls this "the
 * one piece of logic where correctness is directly visible to judges"). If you change one,
 * change the other and re-run both test suites.
 *
 * Noise-stripping rules are kept in one table so they can be tuned without touching logic.
 */
const NOISE_PATTERNS: RegExp[] = [
  /https?:\/\/\S+/g,
  /www\.\S+/g,
  // Scheme-less links: share sheets hand over "youtu.be/abc" and "instagram.com/reel/xyz"
  // just as often as full URLs, and without this they survive cleaning and end up offered to
  // the user as though they were a game title.
  /[\w-]+(?:\.[\w-]+)+\/\S*/g,
  /\[[^\]]*]/g,
  /\([^)]*\)/g,
  /\br\/\w+/gi,
  /@\w+/g,
  /\bPART\s*\d+\b/gi,
  /\bEP\.?\s*\d+\b/gi,
  /\b(REVIEW|GAMEPLAY|LET'?S PLAY|WALKTHROUGH|FULL GAME|OFFICIAL TRAILER|TRAILER)\b/gi,
  /\b(19|20)\d{2}\b/g,
  /\p{Extended_Pictographic}/gu,
];

const HASHTAG = /#(\w+)/g;
const CAMEL_BOUNDARY = /(?<=[a-z0-9])(?=[A-Z])/g;
const WHITESPACE = /\s+/g;

function unCamelAndClean(hashtag: string): string {
  return hashtag.replace(CAMEL_BOUNDARY, " ").toLowerCase().trim();
}

function stripNoise(text: string): string {
  let result = text;
  for (const pattern of NOISE_PATTERNS) {
    result = result.replace(pattern, " ");
  }
  result = result.split("|")[0];
  return result.replace(WHITESPACE, " ").trim();
}

/**
 * @returns candidate title strings, strongest signal first (hashtags), then the cleaned
 * full text as a fallback candidate. Never throws; empty input yields an empty list so
 * callers fall through to manual search (docs/02-PRODUCT-SPEC.md degradation ladder).
 */
export function extractCandidates(rawText: string | null | undefined): string[] {
  if (!rawText || !rawText.trim()) return [];

  const hashtagCandidates = [...rawText.matchAll(HASHTAG)]
    .map((m) => unCamelAndClean(m[1]))
    .filter((c) => c.length >= 2);

  const withoutHashtags = rawText.replace(HASHTAG, "");
  const cleanedFullText = stripNoise(withoutHashtags);

  const all = new Set<string>(hashtagCandidates);
  if (cleanedFullText.length >= 2) all.add(cleanedFullText);

  return [...all];
}
