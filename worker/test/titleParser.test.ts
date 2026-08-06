import { test } from "node:test";
import assert from "node:assert/strict";
import { extractCandidates } from "../src/resolve/titleParser.ts";

// Mirrors app/src/test/.../TitleParserTest.kt — same fixtures, same expected behavior.

test("blank or null input returns no candidates", () => {
  assert.deepEqual(extractCandidates(null), []);
  assert.deepEqual(extractCandidates(""), []);
  assert.deepEqual(extractCandidates("   "), []);
});

test("hashtag is the strongest signal and comes first", () => {
  const candidates = extractCandidates("this boss took me 3 hours 😭 #eldenring #gaming #fyp");
  assert.ok(candidates.length > 0);
  assert.equal(candidates[0], "eldenring");
});

test("camelCase hashtags are split into words", () => {
  const candidates = extractCandidates("finally beat it #BaldursGate3");
  assert.ok(candidates.includes("baldurs gate3"));
});

test("pascal case hashtag with multiple words splits on every boundary", () => {
  const candidates = extractCandidates("#EldenRingNightreign is out now");
  assert.ok(candidates.includes("elden ring nightreign"));
});

test("youtube style title strips bracketed tags and boilerplate", () => {
  const candidates = extractCandidates("Elden Ring [4K] (Official Trailer) REVIEW");
  assert.equal(candidates.at(-1)?.trim(), "Elden Ring");
});

test("video title separator pipe truncates to the part before it", () => {
  const candidates = extractCandidates("Hollow Knight Silksong | Full Walkthrough Part 12");
  assert.equal(candidates.at(-1)?.trim(), "Hollow Knight Silksong");
});

test("strips urls from shared text", () => {
  const candidates = extractCandidates("check this out https://youtube.com/watch?v=abc123 so good");
  const last = candidates.at(-1) ?? "";
  assert.ok(!last.includes("/"));
  assert.ok(!last.includes("http"));
});

test("strips reddit subreddit prefix and handles", () => {
  const candidates = extractCandidates("r/gaming just beat @some_streamer's game Celeste");
  const cleaned = candidates.at(-1) ?? "";
  assert.ok(!cleaned.includes("r/gaming"));
  assert.ok(!cleaned.includes("@some_streamer"));
});

test("strips trailing release year noise", () => {
  const candidates = extractCandidates("Baldur's Gate 3 2023 GOTY edition gameplay");
  assert.ok(!(candidates.at(-1) ?? "").includes("2023"));
});

test("strips ep and part markers", () => {
  const candidates = extractCandidates("Silksong Let's Play EP.4 Part 12");
  const cleaned = candidates.at(-1) ?? "";
  assert.ok(!cleaned.toLowerCase().includes("ep.4"));
  assert.ok(!cleaned.toLowerCase().includes("part 12"));
});

test("tiktok style caption with emoji and hashtags produces clean candidates", () => {
  const candidates = extractCandidates("beat the final boss on 1 life 🔥🔥 #hollowknightsilksong #boss #fyp");
  assert.equal(candidates[0], "hollowknightsilksong");
  assert.ok(candidates.some((c) => c.includes("beat the final boss")));
});

test("steam style un-slugged path text passes through mostly unchanged", () => {
  const candidates = extractCandidates("Hades II");
  assert.equal(candidates.at(-1), "Hades II");
});

test("does not throw on pathological input", () => {
  const weird = "#".repeat(50) + "()[]||||" + "😀".repeat(20);
  assert.doesNotThrow(() => extractCandidates(weird));
});
