import { test } from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import {
  isPileLandingRequest,
  MAX_PAYLOAD_CHARS,
  PILE_PAGE_SCRIPT,
  pileLinkResponse,
  pileScriptHash,
  renderPilePage,
} from "../src/routes/pileLink.ts";

/**
 * `/p` is the one route whose privacy promise is structural: the pile lives in the fragment,
 * so the page must be identical for everyone and must never need the payload server-side.
 * These tests pin that down, plus the CSP that lets exactly one inline script run.
 */

test("the page is static — no per-request content", () => {
  assert.equal(renderPilePage(), renderPilePage());
});

test("the CSP hash matches the script actually embedded", async () => {
  const expected = "sha256-" + createHash("sha256").update(PILE_PAGE_SCRIPT, "utf8").digest("base64");
  assert.equal(await pileScriptHash(), expected);
  const response = await pileLinkResponse();
  const csp = response.headers.get("Content-Security-Policy") ?? "";
  assert.ok(csp.includes(`script-src '${expected}'`), csp);
  assert.ok(csp.includes("default-src 'none'"));
  assert.ok(renderPilePage().includes(`<script>${PILE_PAGE_SCRIPT}</script>`));
});

test("the script validates the fragment before using it", () => {
  // The payload is only ever used after this check, and only in an href.
  assert.ok(PILE_PAGE_SCRIPT.includes("/^[A-Za-z0-9_-]+$/.test(payload)"));
  assert.ok(PILE_PAGE_SCRIPT.includes(`payload.length > ${MAX_PAYLOAD_CHARS}`));
  assert.ok(!PILE_PAGE_SCRIPT.includes("innerHTML"));
  assert.ok(!PILE_PAGE_SCRIPT.includes("document.write"));
  assert.ok(!PILE_PAGE_SCRIPT.includes("fetch"));
});

/** Runs the page script against a fake DOM and reports what it did. */
function runScript(hash: string, userAgent: string) {
  const open = { href: "#", hidden: true };
  const note = { textContent: "" };
  const fn = new Function("location", "document", "navigator", PILE_PAGE_SCRIPT);
  fn({ hash }, { getElementById: (id: string) => (id === "open" ? open : note) }, { userAgent });
  return { open, note };
}

test("on Android, a valid payload becomes the intent fallback", () => {
  const { open, note } = runScript("#AbC_dE-9", "Mozilla/5.0 (Linux; Android 14)");
  assert.equal(open.hidden, false);
  assert.equal(
    open.href,
    "intent://p/AbC_dE-9#Intent;scheme=continueapp;package=com.mikhilnaika.continueapp;" +
      "S.browser_fallback_url=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dcom.mikhilnaika.continueapp;end",
  );
  assert.equal(note.textContent, "");
});

test("a hostile or missing payload never reaches the href", () => {
  for (const hash of ["", "#", "#a;b", "#abc;S.browser_fallback_url=https://evil", "#<script>", "#" + "a".repeat(MAX_PAYLOAD_CHARS + 1)]) {
    const { open, note } = runScript(hash, "Android");
    assert.equal(open.hidden, true, hash);
    assert.equal(open.href, "#", hash);
    assert.match(note.textContent, /incomplete/);
  }
});

test("off Android, the page explains instead of offering a dead button", () => {
  const { open, note } = runScript("#abc", "Mozilla/5.0 (iPhone)");
  assert.equal(open.hidden, true);
  assert.match(note.textContent, /Android/);
});

test("the intent fallback targets the pile host of the custom scheme", () => {
  assert.ok(PILE_PAGE_SCRIPT.includes(`"intent://p/" + payload + "#Intent;scheme=continueapp;package="`));
  assert.ok(PILE_PAGE_SCRIPT.includes('"com.mikhilnaika.continueapp"'));
});

test("the page is kept out of search and carries no referrer", async () => {
  const response = await pileLinkResponse();
  assert.equal(response.headers.get("X-Robots-Tag"), "noindex, nofollow");
  assert.equal(response.headers.get("Referrer-Policy"), "no-referrer");
  assert.match(response.headers.get("Content-Type") ?? "", /^text\/html/);
});

test("only /p and /p/ over GET are the landing page", () => {
  assert.ok(isPileLandingRequest("/p", "GET"));
  assert.ok(isPileLandingRequest("/p/", "GET"));
  assert.ok(!isPileLandingRequest("/p/abc", "GET"));
  assert.ok(!isPileLandingRequest("/pp", "GET"));
  assert.ok(!isPileLandingRequest("/P", "GET"));
  assert.ok(!isPileLandingRequest("/p", "POST"));
});
