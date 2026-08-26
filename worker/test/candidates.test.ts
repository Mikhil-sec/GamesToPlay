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

/**
 * Closed-test report, 2026-08-23: "resident evil requiem matched to resident evil OG".
 *
 * Whole-word containment scores a strict prefix of a title alarmingly well — "Resident Evil"
 * inside "Resident Evil Requiem" came out at 0.925, clearing both the 0.9 bar that stops
 * `matchCandidates` searching and the app's 0.85 bar for calling a result confident. The
 * leftover capitalised word is the whole signal that the caption named something else.
 */
test("a strict prefix of the title in the caption never scores as confident", () => {
  const caption = "Resident Evil Requiem";
  const prefix = verifyAgainstText(caption, "Resident Evil");
  assert.ok(prefix > 0, "the prefix does still appear in the caption");
  assert.ok(prefix < 0.85, `"Resident Evil" scored ${prefix} against "${caption}" — must not be confident`);
  assert.ok(
    verifyAgainstText(caption, "Resident Evil Requiem") > prefix,
    "the full title must outrank its own prefix",
  );
});

test("a fragment score stays under the early-stop bar so the search continues", () => {
  // 0.9 is `resolveGame.ts`'s CONFIDENT_ENOUGH. A fragment clearing it is what let a wrong
  // answer end the search before the right one was ever looked up.
  assert.ok(verifyAgainstText("Resident Evil Requiem gameplay is wild", "Resident Evil") < 0.9);
});

test("prose after a title is not mistaken for the title continuing", () => {
  // The regression risk of the fragment rule: demoting matches that are actually complete.
  assert.ok(verifyAgainstText("Elden Ring is brutal", "Elden Ring") >= 0.85);
  assert.ok(verifyAgainstText("Playing Hades tonight", "Hades") >= 0.75);
});

test("video-title boilerplate after a title is not the title continuing", () => {
  // A separator ends the title...
  assert.ok(verifyAgainstText("RESIDENT EVIL REQUIEM - Announcement Trailer", "Resident Evil Requiem") >= 0.85);
  assert.ok(verifyAgainstText("Silksong – Official Reveal", "Silksong") >= 0.75);
  // ...and so does a known trailing word, with no separator at all.
  assert.ok(verifyAgainstText("Resident Evil Requiem Official Trailer", "Resident Evil Requiem") >= 0.85);
});

test("a subtitle the caption spells out beats the base game", () => {
  const caption = "Elden Ring Shadow of the Erdtree is brutal";
  assert.ok(
    verifyAgainstText(caption, "Elden Ring: Shadow of the Erdtree") >
      verifyAgainstText(caption, "Elden Ring"),
  );
});

/**
 * The report was actually about **"Resident Evil 9 Requiem"** — the way the internet writes a
 * game IGDB calls "Resident Evil Requiem". The digit is the whole problem: `'9'` is not
 * uppercase, so it broke the proper-noun run in half ("Resident Evil" + "Requiem"), and it
 * didn't count as the title carrying on either. Every numbered sequel was affected, not just
 * this one.
 */
test("a sequel number does not break its own title into a run", () => {
  const candidates = rankedCandidates("Resident Evil 9 Requiem Trailer");
  assert.ok(
    candidates.some((c) => c.toLowerCase() === "resident evil 9 requiem"),
    `the whole title must be a candidate, got: ${candidates.slice(0, 5).join(" | ")}`,
  );
  assert.ok(
    candidates.findIndex((c) => c.toLowerCase() === "resident evil 9 requiem") <
      candidates.findIndex((c) => c.toLowerCase() === "resident evil"),
    "the full title must be tried before its prefix",
  );
});

test("a following sequel number counts as the title continuing", () => {
  const caption = "Resident Evil 9 Requiem";
  assert.ok(
    verifyAgainstText(caption, "Resident Evil") < 0.85,
    "the 1996 original must not be a confident answer for a caption naming RE9",
  );
});

test("the sequel outranks the base game rather than tying with it", () => {
  // Both are incomplete matches here, so a flat ceiling would have clamped them equal and
  // thrown away the only thing that separates them.
  const caption = "Resident Evil 4 Remake is amazing";
  assert.ok(verifyAgainstText(caption, "Resident Evil 4") > verifyAgainstText(caption, "Resident Evil"));
  assert.ok(verifyAgainstText("Final Fantasy 7 Rebirth", "Final Fantasy") < 0.85);
});

test("a name matches through a sequel number the official title omits", () => {
  // IGDB: "Resident Evil Requiem". The internet: "Resident Evil 9 Requiem".
  const caption = "Resident Evil 9 Requiem";
  const relaxed = verifyAgainstText(caption, "Resident Evil Requiem");
  assert.ok(relaxed > 0, "the real game must not score 0 just because of the interior digit");
  assert.ok(relaxed > verifyAgainstText(caption, "Resident Evil"), "and it must beat the original");
  assert.ok(relaxed < 0.85, "but not confidently — cf. Mass Effect 2 Legendary Edition");
});

test("a four-digit year is not read as a sequel number", () => {
  assert.ok(verifyAgainstText("Elden Ring 2024 gameplay", "Elden Ring") >= 0.85);
});
