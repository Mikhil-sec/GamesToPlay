import { strict as assert } from "node:assert";
import { test } from "node:test";
import { MAX_BATCH_IDS, MAX_PAGE, sanitizeIds, sanitizePage } from "../src/security.ts";

test("page defaults to 0 when absent or unparseable", () => {
  assert.equal(sanitizePage(null), 0);
  assert.equal(sanitizePage(""), 0);
  assert.equal(sanitizePage("banana"), 0);
  assert.equal(sanitizePage("NaN"), 0);
});

test("page is clamped to the cap in both directions", () => {
  assert.equal(sanitizePage("1"), 1);
  assert.equal(sanitizePage(String(MAX_PAGE)), MAX_PAGE);
  // The cap is the KV-write defence, so it has to hold for anything a client can send.
  assert.equal(sanitizePage("9999"), MAX_PAGE);
  assert.equal(sanitizePage("1e9"), MAX_PAGE);
  assert.equal(sanitizePage("-4"), 0);
  assert.equal(sanitizePage("1.9"), 1);
});

test("ids parse to distinct positive integers", () => {
  assert.deepEqual(sanitizeIds("1,2,3"), [1, 2, 3]);
  assert.deepEqual(sanitizeIds(" 7 , 7 , 8 "), [7, 8]);
  assert.deepEqual(sanitizeIds(null), []);
  assert.deepEqual(sanitizeIds(""), []);
});

test("ids drop anything that isn't a usable game id", () => {
  // Dropped, not rejected: one stale id in a pile must not cost the whole refresh.
  assert.deepEqual(sanitizeIds("1,,abc,-3,0,4"), [1, 4]);
  // The point of parsing to Number before the query is built — no apicalypse can survive it.
  assert.deepEqual(sanitizeIds('1);drop table games;--,2'), [2]);
  assert.deepEqual(sanitizeIds("1.5,2"), [2]);
});

test("ids are capped so one request can't fan out unbounded work", () => {
  const many = Array.from({ length: 500 }, (_, i) => i + 1).join(",");
  assert.equal(sanitizeIds(many).length, MAX_BATCH_IDS);
  assert.equal(sanitizeIds(many, 5).length, 5);
});
