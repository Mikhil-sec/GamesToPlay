import { test } from "node:test";
import assert from "node:assert/strict";
import { rankedCandidates, verifyAgainstText } from "../src/resolve/candidates.ts";

/**
 * The two captions below are **real strings captured from Mikhil's device** on 2026-08-12,
 * both of which previously failed to match anything. Keep them here verbatim — they encode
 * the actual failure mode (IGDB's search is near-exact, so the game name has to be isolated
 * out of the caption before searching) better than any invented example.
 */
const PALWORLD = "Each Pal has their own method of transporting items📦 Pocketpair Palworld";
const LEAGUE = "a tale of two bush ganks 🤔 League of Legends";

/** The candidate that actually matches must appear early enough to fit the search budget. */
function rankOf(text: string, wanted: string): number {
  return rankedCandidates(text).findIndex((c) => c.toLowerCase() === wanted.toLowerCase());
}

test("isolates the game name from a YouTube caption plus channel name", () => {
  const rank = rankOf(PALWORLD, "Palworld");
  assert.ok(rank >= 0, "Palworld should be a candidate");
  assert.ok(rank < 6, `Palworld ranked ${rank}, outside the 6-search budget`);
});

test("keeps a multi-word title together through its lowercase connector", () => {
  const rank = rankOf(LEAGUE, "League of Legends");
  assert.ok(rank >= 0, "League of Legends should be a candidate");
  assert.ok(rank < 6, `League of Legends ranked ${rank}, outside the 6-search budget`);
});

test("prefers the longer proper-noun run over its fragments", () => {
  const candidates = rankedCandidates(LEAGUE);
  assert.ok(
    candidates.indexOf("League of Legends") < candidates.indexOf("League"),
    "the full title should be searched before a single word of it",
  );
});

test("hashtags outrank anything inferred from prose", () => {
  const candidates = rankedCandidates("insane clutch #EldenRing gameplay");
  assert.equal(candidates[0], "elden ring");
});

test("does not emit bare stopwords as searchable candidates", () => {
  for (const candidate of rankedCandidates(PALWORLD)) {
    assert.notEqual(candidate.toLowerCase(), "each");
    assert.notEqual(candidate.toLowerCase(), "their");
  }
});

test("verification accepts a name present in the caption", () => {
  assert.ok(verifyAgainstText(PALWORLD, "Palworld") > 0.8);
  assert.ok(verifyAgainstText(LEAGUE, "League of Legends") > 0.8);
});

test("verification rejects a name absent from the caption", () => {
  assert.equal(verifyAgainstText(PALWORLD, "Stardew Valley"), 0);
  // Guards the risk the short-window strategy introduces: searching "Pal" must not let an
  // unrelated game through just because IGDB returned something for it.
  assert.equal(verifyAgainstText(PALWORLD, "Pal-world!: More Than Just Pals"), 0);
});

test("verification prefers the more specific of two present names", () => {
  const text = "ranked grind on League of Legends today";
  assert.ok(
    verifyAgainstText(text, "League of Legends") > verifyAgainstText(text, "League"),
    "a longer matching name should beat a shorter one",
  );
});

test("does not throw on pathological input", () => {
  for (const input of ["", "   ", "🤔🤔🤔", "#", "a", null, undefined]) {
    assert.doesNotThrow(() => rankedCandidates(input as string));
  }
});
