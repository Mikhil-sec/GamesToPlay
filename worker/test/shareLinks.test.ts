import { test } from "node:test";
import assert from "node:assert/strict";
import { extractUrl } from "../src/resolve/resolveUrl.ts";
import { rankedCandidates } from "../src/resolve/candidates.ts";
import { extractCandidates } from "../src/resolve/titleParser.ts";

/**
 * Regressions from the first real closed-test report (2026-08-19): shares from YouTube and
 * Instagram were putting a raw URL — or a fragment of one — into the manual-entry field, so
 * every share had to be fully cleared before the user could type. Four separate causes, one
 * test each, plus the ranking bug that made the field appear so often in the first place.
 */

const SEARCH_BUDGET = 6;

test("finds a link that is not at the start of the text", () => {
  // Used to fail a "starts with http" test, so the video was never looked up and the URL was
  // searched against IGDB verbatim — which matched a game literally called "Insane".
  const found = extractUrl("this boss is insane https://youtu.be/dQw4w9WgXcQ?si=abc");
  assert.equal(found?.url, "https://youtu.be/dQw4w9WgXcQ?si=abc");
});

test("finds a scheme-less link and makes it absolute", () => {
  const found = extractUrl("youtu.be/dQw4w9WgXcQ");
  assert.equal(found?.url, "https://youtu.be/dQw4w9WgXcQ");
  // `matched` has to be the literal substring so the caller can subtract it from the caption.
  assert.equal(found?.matched, "youtu.be/dQw4w9WgXcQ");
});

test("does not mistake ordinary prose or a punctuated title for a link", () => {
  assert.equal(extractUrl("Elden Ring is great"), null);
  assert.equal(extractUrl("no link here at all"), null);
  assert.equal(extractUrl("S.T.A.L.K.E.R. 2 is out"), null);
});

test("a bare link never survives cleaning as a title candidate", () => {
  // This is the actual user-visible bug: whatever comes out of here is what gets offered in
  // the manual-entry field, and a URL there has to be deleted before anything can be typed.
  for (const share of [
    "https://www.instagram.com/reel/C8xYzAbCdEf/?igsh=abc123",
    "youtu.be/dQw4w9WgXcQ",
    "instagram.com/reel/C8xYzAbCdEf/",
    "www.youtube.com/watch?v=dQw4w9WgXcQ",
  ]) {
    for (const candidate of extractCandidates(share)) {
      assert.ok(
        !/https?:\/\//i.test(candidate) && !/[\w-]+\.[\w-]+\//.test(candidate),
        `link fragment leaked into a title candidate for ${share}: ${candidate}`,
      );
    }
  }
});

test("the game name is reachable inside the search budget for a long caption", () => {
  // Ordering windows strictly longest-first spent the whole budget on the least likely
  // candidates: "Elden Ring" came 10th of 23 and the resolve settled for
  // "Ring Master I: The Shadow of Filias" at 0.29 confidence.
  const rank = rankedCandidates("Elden Ring Shadow of the Erdtree is brutal")
    .findIndex((c) => c.toLowerCase() === "elden ring");
  assert.ok(rank >= 0 && rank < SEARCH_BUDGET, `"Elden Ring" ranked ${rank}, outside the budget`);
});

test("windows never start or end on a connector", () => {
  for (const candidate of rankedCandidates("Rise of the Tomb Raider is good")) {
    assert.ok(!/^(of|the|and|a|an|in|to|at|for|vs)\s/i.test(candidate), `leading connector: ${candidate}`);
    assert.ok(!/\s(of|the|and|a|an|in|to|at|for|vs)$/i.test(candidate), `trailing connector: ${candidate}`);
  }
});

test("still isolates the game name from the captured real-world captions", () => {
  // Guards the fix against the two strings the previous ordering was tuned for.
  const palworld = rankedCandidates("Each Pal has their own method of transporting items Pocketpair Palworld")
    .findIndex((c) => c.toLowerCase() === "palworld");
  assert.ok(palworld >= 0 && palworld < SEARCH_BUDGET, `Palworld ranked ${palworld}`);

  const league = rankedCandidates("a tale of two bush ganks League of Legends")
    .findIndex((c) => c.toLowerCase() === "league of legends");
  assert.ok(league >= 0 && league < SEARCH_BUDGET, `League of Legends ranked ${league}`);
});
