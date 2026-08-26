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

/**
 * Words that follow a game name in a video title without being part of it.
 *
 * Used only by `continuesIntoTitleWord`: "Resident Evil Requiem Official Trailer" must not read
 * as though the title carries on past "Requiem", or the correct full match would be demoted as
 * a fragment. Deliberately excludes words that really do appear in titles — "Remake",
 * "Remaster", "Edition", "Definitive" — because demoting a match that stops short of one of
 * those is the right call.
 */
const TITLE_TRAILERS = new Set([
  "official", "trailer", "teaser", "reveal", "announcement", "announce", "cinematic",
  "walkthrough", "playthrough", "review", "reaction", "stream", "live", "guide", "tips",
  "speedrun", "montage", "highlights", "clip", "clips", "news", "update", "patch",
  "ranked", "explained", "everything", "breakdown", "impressions", "preview", "hands",
]);

/**
 * What a match the caption itself says is incomplete gets multiplied by — see
 * `continuesIntoTitleWord`.
 *
 * A *multiplier*, not a ceiling. A flat ceiling looked simpler and was wrong: in
 * "Resident Evil 4 Remake is amazing" both "Resident Evil" and "Resident Evil 4" are incomplete
 * matches, and clamping both to the same number threw away the one thing that separates them —
 * that the longer one is the better answer. `scoreOf` tops out at 1.0, so multiplying by 0.84
 * keeps every penalised match under the app's 0.85 "Confident" bar and under `resolveGame.ts`'s
 * 0.9 early-stop bar, while preserving their order relative to each other.
 */
export const FRAGMENT_PENALTY = 0.84;

/**
 * What a match found only after ignoring a sequel number gets multiplied by.
 *
 * Deliberately below the "Confident" bar. "Resident Evil 9 Requiem" and "Mass Effect 2
 * Legendary Edition" are structurally identical — the caption carries a number the official
 * title doesn't — but in the first the user means Requiem and in the second they most likely
 * mean Mass Effect 2, and nothing in the string separates the two cases. A chooser headed by
 * the right game is the honest answer where a confident guess isn't.
 */
const NUMBER_RELAXED_PENALTY = 0.84;

const MAX_WORDS_IN_CANDIDATE = 6;

function words(text: string): string[] {
  return text.split(/\s+/).filter(Boolean);
}

/**
 * A token that reads as a sequel number rather than as prose or a year.
 *
 * One or two digits only, which is what keeps `"Elden Ring 2024 gameplay"` from being read as a
 * sequel while `"Resident Evil 9"` and `"Portal 2"` are. Roman numerals arrive here already
 * folded to digits by `normalize`, so `"IV"` and `"4"` are the same token.
 */
function isSequelNumber(normalizedToken: string): boolean {
  return /^\d{1,2}$/.test(normalizedToken);
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
    } else if (current.length > 0 && isSequelNumber(normalize(bare))) {
      // A digit is not "capitalized", so every numbered sequel used to break its own title in
      // half here: "Resident Evil 4" produced the run "Resident Evil", and "Resident Evil 9
      // Requiem" produced "Resident Evil" plus "Requiem" — so the base game outranked the
      // sequel the caption actually named (the second 2026-08-23 closed-test report). Numbers
      // continue a run; they never start one.
      current.push(bare);
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

/** A window can't usefully start or end on a connector: `"Rise of"` and `"of Legends"` are junk. */
function trimConnectors(parts: string[]): string[] {
  const out = [...parts];
  while (out.length > 0 && CONNECTORS.has(out[0].toLowerCase())) out.shift();
  while (out.length > 0 && CONNECTORS.has(out[out.length - 1].toLowerCase())) out.pop();
  return out;
}

/**
 * Contiguous word windows inside a phrase, ordered **whole run → shrinking prefixes →
 * shrinking suffixes → interior**.
 *
 * The order is the entire value of this function, because callers only spend [MAX_SEARCHES]
 * provider requests. The previous ordering was every window strictly longest-first, which is
 * backwards for a near-exact search engine: long windows are the *least* likely to match, so
 * the whole budget went to them. Measured on the real caption
 * `"Elden Ring Shadow of the Erdtree is brutal"`, `"Elden Ring"` came 10th of 23 candidates —
 * outside the budget — and the resolve settled for *"Ring Master I: The Shadow of Filias"* at
 * 0.29 confidence. Shrinking the prefix reaches `"Elden Ring"` 3rd instead.
 *
 * Prefixes before suffixes because a caption that names a game usually leads with it, but the
 * `"Pocketpair Palworld"` case (channel name first) is exactly why suffixes still get tried.
 */
function windows(phrase: string): string[] {
  const parts = words(phrase);
  const out: string[] = [];
  const add = (slice: string[]) => {
    const trimmed = trimConnectors(slice);
    if (trimmed.length === 0) return;
    if (trimmed.length === 1 && STOPWORDS.has(trimmed[0].toLowerCase())) return;
    const window = trimmed.join(" ");
    if (window.length >= 2) out.push(window);
  };

  const max = Math.min(parts.length, MAX_WORDS_IN_CANDIDATE);
  add(parts.slice(0, max));
  for (let size = max - 1; size >= 1; size--) add(parts.slice(0, size));
  for (let size = max - 1; size >= 1; size--) add(parts.slice(parts.length - size));
  for (let size = max - 1; size >= 2; size--) {
    for (let start = 1; start + size < parts.length; start++) add(parts.slice(start, start + size));
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
  const needle = normalize(gameName);
  if (!needle) return 0;

  const normalizedText = normalize(originalText);
  if (!` ${normalizedText} `.includes(` ${needle} `)) {
    // Second chance, ignoring a sequel number the caption carries and the official title
    // doesn't: IGDB calls it "Resident Evil Requiem", the internet calls it "Resident Evil 9
    // Requiem", and whole-word containment sees no match at all because of the digit sitting
    // in the middle of the name.
    const relaxed = words(normalizedText).filter((w) => !isSequelNumber(w)).join(" ");
    if (!` ${relaxed} `.includes(` ${needle} `)) return 0;
    return scoreOf(needle) * NUMBER_RELAXED_PENALTY;
  }

  if (needle === normalizedText) return 1;

  const score = scoreOf(needle);
  return continuesIntoTitleWord(originalText, needle) ? score * FRAGMENT_PENALTY : score;
}

/**
 * Longer names win. This is the tiebreaker that matters most in practice: the caption
 * "Each Pal has … Palworld" contains *both* "Pal" (a real game) and "Palworld", and only length
 * separates them. Measured in characters as well as words, so one long word still beats one
 * short word. Tops out at exactly 1.0, which is what makes the penalties above safe ceilings.
 */
function scoreOf(needle: string): number {
  const lengthScore = Math.min(needle.length / 18, 1);
  const specificity = Math.min(words(needle).length / 3, 1);
  return 0.75 + 0.15 * lengthScore + 0.1 * specificity;
}

/**
 * True when the caption carries straight on from the matched name into another word that looks
 * like part of the same title.
 *
 * This is the fix for the closed-test report that "Resident Evil Requiem" matched to the 1996
 * *Resident Evil*. Whole-word containment alone scores a strict prefix of a title remarkably
 * well — `"Resident Evil"` inside `"Resident Evil Requiem"` came out at 0.925, over the 0.9 bar
 * that stops `matchCandidates` searching *and* over the app's 0.85 bar for presenting a result
 * as confident — so a thirty-year-old game was confidently offered for a caption that plainly
 * names a newer one. Nothing in the score noticed the leftover word.
 *
 * The signal is capitalisation in the **original** text, which is why this can't work off the
 * normalized string: a title word carries on in caps ("Resident Evil `Requiem`"), while prose
 * after a title does not ("Elden Ring `is` brutal"). Three things stop it firing on ordinary
 * video titles: a punctuation-only token between the two ("REQUIEM `-` Announcement Trailer")
 * reads as a subtitle boundary and ends the title; `STOPWORDS` cover prose; and
 * `TITLE_TRAILERS` cover the capitalised boilerplate that trails a name in a video title. A
 * following sequel *number* counts as a continuation too, since a digit is never "capitalised"
 * and so would otherwise slip through.
 *
 * Only the *following* word is examined, never the preceding one: a capital at the start of a
 * sentence is indistinguishable from a title word, so "Playing Hades tonight" would otherwise
 * demote a perfectly good match.
 */
function continuesIntoTitleWord(originalText: string, normalizedNeedle: string): boolean {
  const needleWords = words(normalizedNeedle);
  if (needleWords.length === 0) return false;

  const raw = words(originalText);
  // Index the raw tokens by their normalized form. The ones that normalize away (a bare "-",
  // "|", an emoji) are exactly the separators that end a title.
  const normalized = raw.map((word) => normalize(word));

  for (let cursor = 0; cursor < raw.length; cursor++) {
    if (!normalized[cursor]) continue;

    let matched = 0;
    let scan = cursor;
    while (scan < raw.length && matched < needleWords.length && normalized[scan] === needleWords[matched]) {
      matched++;
      scan++;
    }
    if (matched !== needleWords.length) continue;

    // `scan` is the token straight after the match — a separator there (or nothing) means the
    // title ended.
    const next = raw[scan];
    if (next === undefined) return false;
    const nextNormalized = normalized[scan];
    if (!nextNormalized) return false;
    if (STOPWORDS.has(nextNormalized) || TITLE_TRAILERS.has(nextNormalized)) return false;
    // A digit isn't capitalised but is unmistakably part of the title: "Resident Evil" followed
    // by "9" is not the game the caption names.
    if (isSequelNumber(nextNormalized)) return true;
    return isCapitalized(next.replace(/[^\p{L}\p{N}':-]/gu, ""));
  }
  return false;
}
