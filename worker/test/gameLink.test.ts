import { test } from "node:test";
import assert from "node:assert/strict";
import {
  assetLinksBody,
  escapeHtml,
  hasRealPlayFingerprint,
  renderGamePage,
  safeImageUrl,
  sanitizeCampaign,
} from "../src/routes/gameLink.ts";
import type { GameDto } from "../src/types.ts";

/**
 * The friend-loop landing page is the first thing in this project that renders **HTML** from
 * data we don't author, and serves it to third-party unfurl bots. Both of those are new
 * categories of mistake for this codebase, so they get tests rather than careful reading —
 * the standing lesson from the corrupted-regex incident is that reading a diff does not catch
 * this class of bug and a test asserting the user-visible property does.
 */

function game(overrides: Partial<GameDto> = {}): GameDto {
  return {
    id: 1942,
    slug: "the-witcher-3-wild-hunt",
    name: "The Witcher 3: Wild Hunt",
    coverUrl: "https://images.igdb.com/igdb/image/upload/t_cover_big/co1wyy.jpg",
    backgroundUrl: "https://images.igdb.com/igdb/image/upload/t_1080p/ar8k3.jpg",
    released: "2015-05-19",
    metacritic: 93,
    rating: 93.4,
    playtimeHoursHastily: 51,
    playtimeHoursNormally: 103,
    playtimeHoursCompletely: 174,
    genres: ["Role-playing (RPG)", "Adventure"],
    tags: ["Fantasy", "Single player"],
    platforms: ["PC", "PlayStation 5"],
    ...overrides,
  };
}

test("escapes every character that can break out of text or an attribute", () => {
  assert.equal(escapeHtml(`<a href="x">&'`), "&lt;a href=&quot;x&quot;&gt;&amp;&#39;");
});

test("escapes ampersands first, so an escape is never double-encoded", () => {
  // Ordering bug this guards: replacing `<` before `&` turns `<` into `&lt;` and then the
  // later ampersand pass rewrites it to `&amp;lt;`, which renders as literal "&lt;".
  assert.equal(escapeHtml("<"), "&lt;");
  assert.equal(escapeHtml("&lt;"), "&amp;lt;");
});

test("a hostile game name cannot inject markup into the page", () => {
  const page = renderGamePage(
    game({ name: `</title><script>alert(1)</script>`, slug: `"><script>alert(2)</script>` }),
    "pick",
    "https://example.test/g/1942",
  );
  // The only `<script` allowed anywhere in the document is none at all — the page ships no JS
  // of its own, which makes this assertion exact rather than approximate.
  assert.equal(page.includes("<script"), false);
  assert.equal(page.includes("&lt;script&gt;"), true);
});

test("a real title's apostrophes and ampersands survive into the unfurl tags", () => {
  const page = renderGamePage(game({ name: "Ratchet & Clank: Rift Apart" }), "pick", "https://example.test/g/1");
  assert.equal(page.includes("Ratchet &amp; Clank"), true);
  assert.equal(page.includes("Ratchet & Clank"), false);
});

test("prefers landscape key art for og:image, because a cover unfurls badly", () => {
  const page = renderGamePage(game(), "pick", "https://example.test/g/1");
  assert.equal(page.includes(`<meta property="og:image" content="https://images.igdb.com/igdb/image/upload/t_1080p/ar8k3.jpg">`), true);
  assert.equal(page.includes(`name="twitter:card" content="summary_large_image"`), true);
});

test("falls back to the cover when a game has no key art, and to no image at all when it has neither", () => {
  const coverOnly = renderGamePage(game({ backgroundUrl: null }), "pick", "https://example.test/g/1");
  assert.equal(coverOnly.includes("t_cover_big/co1wyy.jpg"), true);

  const bare = renderGamePage(game({ backgroundUrl: null, coverUrl: null }), "pick", "https://example.test/g/1");
  assert.equal(bare.includes("og:image"), false);
  // Without an image, claiming a large-image card gives chat apps a blank rectangle.
  assert.equal(bare.includes(`content="summary_large_image"`), false);
  assert.equal(bare.includes(`content="summary"`), true);
});

test("only https images.igdb.com may become an og:image", () => {
  assert.equal(safeImageUrl("https://images.igdb.com/igdb/image/upload/t_1080p/a.jpg") !== null, true);
  assert.equal(safeImageUrl("http://images.igdb.com/a.jpg"), null, "plain http");
  assert.equal(safeImageUrl("https://images.igdb.com.attacker.example/a.jpg"), null, "suffix host");
  assert.equal(safeImageUrl("https://attacker.example/images.igdb.com/a.jpg"), null, "path, not host");
  assert.equal(safeImageUrl("javascript:alert(1)"), null, "not a fetchable scheme");
  assert.equal(safeImageUrl("not a url at all"), null);
  assert.equal(safeImageUrl(null), null);
});

test("an unrecognised campaign falls back to the neutral wording instead of rendering it", () => {
  assert.equal(sanitizeCampaign("dare"), "dare");
  assert.equal(sanitizeCampaign("cleared"), "cleared");
  assert.equal(sanitizeCampaign(null), "pick");
  assert.equal(sanitizeCampaign("nonsense"), "pick");
  assert.equal(sanitizeCampaign("<script>"), "pick");
});

test("inherited Object properties are not campaigns", () => {
  // The reason the lookup uses hasOwnProperty: a plain `raw in CAMPAIGNS` or `CAMPAIGNS[raw]`
  // check answers yes for these, and `toString` would then be indexed as a campaign and
  // rendered as `[object Object]`-shaped garbage — or worse, a function's source.
  assert.equal(sanitizeCampaign("__proto__"), "pick");
  assert.equal(sanitizeCampaign("constructor"), "pick");
  assert.equal(sanitizeCampaign("toString"), "pick");
});

test("each campaign changes the visible copy", () => {
  const dare = renderGamePage(game(), "dare", "https://example.test/g/1");
  assert.equal(dare.includes("YOU&#39;VE BEEN DARED TO FINISH"), true);
  const cleared = renderGamePage(game(), "cleared", "https://example.test/g/1");
  assert.equal(cleared.includes("SOMEONE JUST FINISHED"), true);
});

test("the page carries IGDB attribution, which is contractually mandatory", () => {
  // CLAUDE.md constraint #3: the exact sentence must appear on any surface showing IGDB data,
  // and this page is such a surface. It is also the only one outside the app.
  const page = renderGamePage(game(), "pick", "https://example.test/g/1");
  assert.equal(page.includes("The data was freely provided by"), true);
  assert.equal(page.includes("IGDB.com"), true);
});

test("the canonical url is the one we pass, not anything from the request", () => {
  const page = renderGamePage(game(), "pick", "https://continue-worker.gamestoplay.workers.dev/g/1942");
  assert.equal(
    page.includes(`<meta property="og:url" content="https://continue-worker.gamestoplay.workers.dev/g/1942">`),
    true,
  );
});

test("asset links group fingerprints by package and claim all urls", () => {
  const body = assetLinksBody() as Array<{
    relation: string[];
    target: { namespace: string; package_name: string; sha256_cert_fingerprints: string[] };
  }>;
  // Two packages: the real one (Play signing + upload key) and the `.debug` suffixed one.
  assert.equal(body.length, 2);
  const release = body.find((s) => s.target.package_name === "com.mikhilnaika.continueapp");
  const debug = body.find((s) => s.target.package_name === "com.mikhilnaika.continueapp.debug");
  assert.notEqual(release, undefined);
  assert.notEqual(debug, undefined);
  assert.equal(release!.relation[0], "delegate_permission/common.handle_all_urls");
  assert.equal(release!.target.namespace, "android_app");
  assert.equal(release!.target.sha256_cert_fingerprints.length, 2, "Play signing key + upload key");
  assert.equal(debug!.target.sha256_cert_fingerprints.length, 1);
});

test("every published fingerprint is a well-formed SHA-256, or is the flagged placeholder", () => {
  const body = assetLinksBody() as Array<{ target: { sha256_cert_fingerprints: string[] } }>;
  for (const statement of body) {
    for (const fingerprint of statement.target.sha256_cert_fingerprints) {
      if (fingerprint.endsWith("PENDING")) continue;
      const bytes = fingerprint.split(":");
      assert.equal(bytes.length, 32, `${fingerprint} should be 32 colon-separated bytes`);
      for (const byte of bytes) {
        assert.equal(/^[0-9A-F]{2}$/.test(byte), true, `${byte} should be two uppercase hex digits`);
      }
    }
  }
});

/**
 * Filled in 2026-09-12, and this assertion flipped with it — working exactly as intended.
 *
 * It stays as a **regression** guard now rather than a reminder. App Links verification fails
 * silently: with a wrong or missing fingerprint Android just opens a browser and nothing
 * anywhere logs a complaint. So if anyone ever reverts this constant to a placeholder while
 * refactoring, the suite says so instead of the feature quietly dying in the field.
 */
test("the Play App Signing fingerprint is filled in", () => {
  assert.equal(
    hasRealPlayFingerprint(),
    true,
    "The Play App Signing SHA-256 is back to a placeholder — shared links will not open the app.",
  );
});

test("the Play signing key and the upload key are different certificates", () => {
  // The trap this guards: pasting the *upload* certificate into the Play App Signing slot.
  // Both are 32 valid hex bytes, both look completely correct, and the result is an app that
  // opens links for a sideloaded build on the bench and for nobody who installed from Play.
  const body = assetLinksBody() as Array<{
    target: { package_name: string; sha256_cert_fingerprints: string[] };
  }>;
  const release = body.find((s) => s.target.package_name === "com.mikhilnaika.continueapp")!;
  const [playSigning, uploadKey] = release.target.sha256_cert_fingerprints;
  assert.notEqual(playSigning, uploadKey);
});
