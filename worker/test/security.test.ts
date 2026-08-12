import { test } from "node:test";
import assert from "node:assert/strict";
import {
  MAX_QUERY_LENGTH,
  MAX_SHARE_TEXT_LENGTH,
  hostMatches,
  sanitizeQuery,
  sanitizeShareText,
} from "../src/security.ts";

/**
 * `hostMatches` is an SSRF boundary, not a convenience helper — `resolveUrlToText` performs
 * server-side fetches of URLs supplied by whoever calls `/resolve`. The substring check it
 * replaced (`hostname.includes("tiktok.com")`) would have let an attacker point our fetch at
 * a host they control. These cases exist to make sure that never regresses.
 */
test("hostMatches accepts the exact domain and true subdomains", () => {
  assert.ok(hostMatches("tiktok.com", "tiktok.com"));
  assert.ok(hostMatches("vm.tiktok.com", "tiktok.com"));
  assert.ok(hostMatches("www.tiktok.com", "tiktok.com"));
  assert.ok(hostMatches("WWW.TikTok.COM", "tiktok.com"));
});

test("hostMatches rejects lookalike hosts an attacker controls", () => {
  // The exact shape the old `includes()` check let through.
  assert.equal(hostMatches("vm.tiktok.com.attacker.example", "tiktok.com"), false);
  assert.equal(hostMatches("tiktok.com.evil.test", "tiktok.com"), false);
  assert.equal(hostMatches("nottiktok.com", "tiktok.com"), false);
  assert.equal(hostMatches("eviltiktok.com", "tiktok.com"), false);
});

test("hostMatches rejects internal and metadata hosts", () => {
  for (const host of ["localhost", "127.0.0.1", "169.254.169.254", "metadata.google.internal"]) {
    assert.equal(hostMatches(host, "youtube.com"), false);
    assert.equal(hostMatches(host, "tiktok.com"), false);
  }
});

test("sanitizeQuery caps length so KV keys stay under the 512-byte limit", () => {
  const sanitized = sanitizeQuery("a".repeat(5_000));
  assert.equal(sanitized?.length, MAX_QUERY_LENGTH);
});

test("sanitizeQuery treats blank and missing input as nothing to search", () => {
  assert.equal(sanitizeQuery(null), null);
  assert.equal(sanitizeQuery(""), null);
  assert.equal(sanitizeQuery("   "), null);
});

test("sanitizeShareText caps length and rejects non-strings", () => {
  assert.equal(sanitizeShareText("x".repeat(50_000))?.length, MAX_SHARE_TEXT_LENGTH);
  for (const bad of [null, undefined, 42, {}, []]) {
    assert.equal(sanitizeShareText(bad), null);
  }
});
