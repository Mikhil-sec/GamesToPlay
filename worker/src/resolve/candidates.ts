import { extractCandidates } from "./titleParser.ts";

/**
 * Stage 2b — turn one noisy caption into an *ordered shortlist of things worth searching*.
 *
 * Why this exists: IGDB's `search` is near-exact, not fuzzy. Measured against the live API,
 * `"Palworld"` returns 3 hits but `"Pocketpair Palworld"` returns **zero** — one extra word is
 * enough to kill it. So handing IGDB a whole shared caption (which is what we used to do)
 * essentially never matches, even when the game's name is sitting right there in the string.
 *
 * The fix is to guess *substrings* rather than search the whole caption, ordered so the most
 * likely game name is tried first, and then verify the hit against the original text
 * (see `verifyAgainstText`) so a wrong guess can't win.
 */

/** Lowercase words that legitimately appear *inside* a title and shouldn't break a run. */
const CONNECTORS = new Set(["of", "the", "and", "a", "an", "in", "to", "de", "la", "at", "for", "vs"]);

/** Words never worth searching on their own — common caption filler, not game names. */
const STOPWORDS = new Set([
  ...CONNECTORS,
  "i", "my", "me", "we", "you", "your", "he", "she", "it", "they", "this", "that", "these",
  "is", "was", "are", "were", "be", "been", "has", "have", "had", "do", "does", "did", "get",
  "got", "go", "goes", "went", "can", "will", "just", "new", "best", "worst", "how", "why",
  "what", "when", "where", "who", "all", "own", "out", "up", "on", "off", "with", "from",
  "their", "there", "his", "her", "its", "own", "more", "than", "then", "now", "not", "but",
  "so", "if", "or", "as", "by", "own", "own", "each", "own", "method", "items", "own",
  "gameplay", "part", "ep", "episode", "shorts", "short", "video", "funny", "moments",
]);

const MAX_WORDS_IN_CANDIDATE = 6;

function words(text: string): string[] {
  return text.split(/\s+/).filter(Boolean);
}

/**
 * Sequels are written both ways in the wild — IGDB says "Baldur's Gate III", captions say
 * "Baldur's Gate 3". Without folding these together the *older* game wins the match, because
 * only its shorter name is literally present in the caption.
 *
 * Only II and up are converted: a bare "I" is far more often the pronoun than a sequel number.
 */
const ROMAN_NUMERALS: Record<string, string> = {
  ii: "2", iii: "3", iv: "4", v: "5", vi: "6", vii: "7", viii: "8", ix: "9", x: "10",
};

/** `"Elden  Ring!"` → `"elden ring"`. Used for every comparison so punctuation never matters. */
export function normalize(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, " ")
    .replace(/\s+/g, " ")
    .trim()
    .split(" ")
    .map((word) => ROMAN_NUMERALS[word] ?? word)
    .join(" ");
}

function isCapitalized(word: string): boolean {
  const first = word[0];
  return first !== undefined && first === first.toUpperCase() && first !== first.toLowerCase();
}

/**
 * Runs of capitalized words, allowing lowercase connectors in the middle — this is what pulls
 * `"League of Legends"` out of `"a tale of two bush ganks 🤔 League of Legends"`.
 */
function properNounRuns(text: string): string[] {
  const runs: string[] = [];
  let current: string[] = [];

  for (const word of words(text)) {
    const bare = word.replace(/[^\p{L}\p{N}':-]/gu, "");
    if (!bare) continue;

    if (isCapitalized(bare)) {
      current.push(bare);
    } else if (current.length > 0 && CONNECTORS.has(bare.toLowerCase())) {
      current.push(bare); // keep "of" inside "League of Legends"
    } else {
      if (current.length > 0) runs.push(current.join(" "));
      current = [];
    }
  }
  if (current.length > 0) runs.push(current.join(" "));

  // A run can't usefully end on a connector ("Rise of" → "Rise").
  return runs
    .map((run) => {
      const parts = words(run);
      while (parts.length > 0 && CONNECTORS.has(parts[parts.length - 1].toLowerCase())) parts.pop();
      return parts.join(" ");
    })
    .filter((run) => run.length >= 2);
}

/**
 * Every contiguous word window inside a phrase, longest first.
 *
 * Needed because a proper-noun run is often *game name plus noise* — the channel
 * `"Pocketpair Palworld"` is one run, and only its second word is the actual game. Windows let
 * `"Palworld"` get tried without knowing in advance which word matters.
 */
function windows(phrase: string): string[] {
  const parts = words(phrase);
  const out: string[] = [];
  for (let size = Math.min(parts.length, MAX_WORDS_IN_CANDIDATE); size >= 1; size--) {
    for (let start = 0; start + size <= parts.length; start++) {
      const window = parts.slice(start, start + size).join(" ");
      if (size === 1 && STOPWORDS.has(window.toLowerCase())) continue;
      if (window.length >= 2) out.push(window);
    }
  }
  return out;
}

/**
 * Ordered, de-duplicated shortlist of strings to try against the game provider.
 *
 * Order is the whole point — callers only spend a handful of provider requests (IGDB allows
 * 4/sec), so the likeliest candidate has to come first:
 *   1. hashtags — an explicit, human-authored game tag beats anything inferred
 *   2. whole proper-noun runs — `"League of Legends"` lands here
 *   3. windows inside those runs, longest first — `"Palworld"` lands here
 *   4. trailing windows of the caption — game names tend to be appended after the hook
 *   5. the cleaned caption itself — near-useless against IGDB, kept as a last resort for
 *      providers that *are* fuzzy (the seed set's `LIKE` search is)
 */
export function rankedCandidates(rawText: string | null | undefined): string[] {
  if (!rawText || !rawText.trim()) return [];

  const ordered: string[] = [];
  const seen = new Set<string>();
  const push = (candidate: string) => {
    const cleaned = candidate.trim();
    const key = normalize(cleaned);
    if (key.length < 2 || seen.has(key)) return;
    seen.add(key);
    ordered.push(cleaned);
  };

  const base = extractCandidates(rawText); // hashtags first, then cleaned full text
  const cleanedFullText = base[base.length - 1] ?? rawText;
  for (const hashtag of base.slice(0, -1)) push(hashtag);

  const runs = properNounRuns(cleanedFullText).sort((a, b) => words(b).length - words(a).length);
  for (const run of runs) push(run);
  for (const run of runs) for (const window of windows(run)) push(window);

  const allWords = words(cleanedFullText);
  for (let size = Math.min(4, allWords.length); size >= 2; size--) {
    push(allWords.slice(allWords.length - size).join(" "));
  }

  push(cleanedFullText);
  return ordered;
}

/**
 * Confidence that `gameName` is really the game `originalText` is about.
 *
 * The decisive signal is whether the game's name appears **as whole words inside the original
 * caption** — that's what separates a real hit from IGDB returning something vaguely similar
 * to a one-word guess. `"Palworld"` inside `"…items Pocketpair Palworld"` scores high;
 * `"Pal"` matching some unrelated game does not, because more matched words scores higher and
 * a longer name beats a shorter one.
 *
 * @returns 0 when the name doesn't appear in the text at all — callers should treat that as
 *   "unverified" and fall back to lexical scoring rather than trusting it.
 */
export function verifyAgainstText(originalText: string, gameName: string): number {
  const haystack = ` ${normalize(originalText)} `;
  const needle = normalize(gameName);
  if (!needle) return 0;
  if (!haystack.includes(` ${needle} `)) return 0;

  if (needle === normalize(originalText)) return 1;

  // Longer names win. This is the tiebreaker that matters most in practice: the caption
  // "Each Pal has … Palworld" contains *both* "Pal" (a real game) and "Palworld", and only
  // length separates them. Measured in characters, not words, so one long word still beats
  // one short word.
  const lengthScore = Math.min(needle.length / 18, 1);
  // Multi-word names are far less likely to be coincidence than a single common word.
  const specificity = Math.min(words(needle).length / 3, 1);
  return 0.75 + 0.15 * lengthScore + 0.1 * specificity;
}
