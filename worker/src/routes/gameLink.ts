import type { Env, GameDto, GameProvider } from "../types.ts";
import { cached, CacheTtl } from "../kv.ts";

/**
 * The friend loop: a shareable `https://…/g/<igdbId>` link that unfurls with real key art in
 * WhatsApp/Discord/iMessage, and that Android hands **straight to the app** when it's installed.
 *
 * Two routes live here and they are a matched pair — neither works without the other:
 *
 *   `/.well-known/assetlinks.json` — Digital Asset Links. Android fetches this once at install
 *     time to decide whether this origin may claim `/g/*` links. If it 404s, redirects, or the
 *     fingerprints don't match the signing cert, verification fails **silently** and every
 *     shared link opens a browser instead of the app.
 *   `/g/<igdbId>` — the landing page. Only ever *seen* by someone without the app (or on
 *     desktop/iOS), because a verified Android install intercepts the URL before a browser is
 *     ever involved. So its job is not to be a game-details page — it's to make the game look
 *     worth installing for.
 *
 * Cost note (docs/12-SECURITY.md — KV writes are the binding quota at 1,000/day): this route
 * reuses the **exact same `detail:<id>` cache key** as `GET /games/<id>`. A game a friend has
 * already opened in the app is already cached, so the overwhelmingly common case is a share
 * link that costs zero KV writes. That is deliberate, not incidental — do not give this route
 * its own key space.
 */

/** Permanent, per CLAUDE.md. Changing this invalidates every link ever shared. */
const ANDROID_PACKAGE = "com.mikhilnaika.continueapp";

const PLAY_URL = `https://play.google.com/store/apps/details?id=${ANDROID_PACKAGE}`;

/**
 * SHA-256 fingerprints of every certificate allowed to claim this origin's links.
 *
 * These are **public by design** — they are literally published at a well-known URL for anyone
 * to fetch. They are not secrets and do not belong in `wrangler secret`.
 *
 * All three are listed because the same build gets signed three different ways depending on
 * where it came from, and a fingerprint that isn't here means links silently stop opening the
 * app for that flavour of install:
 *
 *   1. **Play App Signing** — what actually reaches real users. Google re-signs every upload
 *      with its own key, so the upload certificate below is *not* what's on a Play install.
 *      Read it from Play Console → Test and release → Setup → App integrity → App signing key
 *      certificate → SHA-256. **This is the one that matters in production.**
 *   2. **Upload key** — `app/keystore/continue-release.jks`. Covers a release APK sideloaded
 *      straight off this machine, which is how App Links get tested before a store release.
 *   3. **Debug key** — `~/.android/debug.keystore`. Covers `./gradlew installDebug`. Note debug
 *      builds carry `applicationIdSuffix .debug`, hence the separate package name below.
 */
const SIGNING_FINGERPRINTS: ReadonlyArray<{ pkg: string; sha256: string; note: string }> = [
  {
    pkg: ANDROID_PACKAGE,
    // TODO(mikhil): replace with the real Play App Signing SHA-256 — see (1) above. Until this
    // is the genuine value, links will NOT open the app for anyone who installed from Play.
    // The placeholder is harmless (it simply matches no certificate) but it is not optional.
    sha256: "PLAY_APP_SIGNING_SHA256_PENDING",
    note: "Play App Signing key — the certificate real users' installs carry",
  },
  {
    pkg: ANDROID_PACKAGE,
    sha256: "95:4D:DB:CC:C0:35:7E:F1:A0:B4:46:A6:52:AF:EC:77:9B:3F:53:ED:C8:7B:2F:A7:4A:1C:CB:06:4B:FD:99:BF",
    note: "Upload key (app/keystore/continue-release.jks) — sideloaded release builds",
  },
  {
    pkg: `${ANDROID_PACKAGE}.debug`,
    sha256: "EA:52:DD:A6:47:D1:74:A1:E8:5D:77:EA:A8:A5:3D:70:68:1E:FB:74:A0:07:CC:A6:5D:BA:5F:9D:10:84:A7:8D",
    note: "Debug key — ./gradlew installDebug",
  },
];

/** True once the Play App Signing fingerprint has actually been filled in. */
export function hasRealPlayFingerprint(): boolean {
  return !SIGNING_FINGERPRINTS.some((f) => f.sha256.endsWith("PENDING"));
}

/**
 * Digital Asset Links, grouped by package so the debug build gets its own statement.
 *
 * Served with no cache headers of our own: Android re-verifies on install and on app update,
 * and a stale CDN copy is exactly how a fingerprint fix fails to take effect.
 */
export function assetLinksBody(): unknown {
  const byPackage = new Map<string, string[]>();
  for (const f of SIGNING_FINGERPRINTS) {
    const list = byPackage.get(f.pkg) ?? [];
    list.push(f.sha256);
    byPackage.set(f.pkg, list);
  }
  return [...byPackage.entries()].map(([pkg, fingerprints]) => ({
    relation: ["delegate_permission/common.handle_all_urls"],
    target: {
      namespace: "android_app",
      package_name: pkg,
      sha256_cert_fingerprints: fingerprints,
    },
  }));
}

/**
 * What kind of share this link came from, which changes only the wording.
 *
 * Deliberately a closed enum resolved through a lookup, never free text. The alternative —
 * letting the sharer put their name or a message in the query string — would mean rendering
 * attacker-controlled text into a public HTML page (an XSS surface) *and* putting a real
 * person's name into a URL that gets forwarded around group chats. The personal half of the
 * message belongs in the chat message itself, where the sender's name is already attached by
 * the messenger and nothing of ours has to escape it.
 */
const CAMPAIGNS = {
  pick: {
    kicker: "SOMEONE THINKS YOU SHOULD PLAY",
    blurb: "They picked this out of their backlog for you.",
  },
  dare: {
    kicker: "YOU'VE BEEN DARED TO FINISH",
    blurb: "They think you'll bounce off it. Prove them wrong.",
  },
  cleared: {
    kicker: "SOMEONE JUST FINISHED",
    blurb: "Cleared, credits rolled, off the pile. Your move.",
  },
} as const;

export type CampaignKey = keyof typeof CAMPAIGNS;

const DEFAULT_CAMPAIGN: CampaignKey = "pick";

/** Unknown / absent / hostile values all collapse to the neutral wording. */
export function sanitizeCampaign(raw: string | null): CampaignKey {
  return raw !== null && Object.prototype.hasOwnProperty.call(CAMPAIGNS, raw)
    ? (raw as CampaignKey)
    : DEFAULT_CAMPAIGN;
}

/**
 * Escapes for both element text and double-quoted attribute values.
 *
 * Every dynamic value on the page goes through this, including the ones that come from IGDB
 * rather than from the client — game titles genuinely contain `&` and `'` (`Assassin's
 * Creed`, `Ratchet & Clank`), and an unescaped apostrophe in an `og:title` truncates the
 * unfurl at best. Written with `replaceAll` on literal strings rather than a regex so there
 * are no escape sequences in this file to get mangled in transit.
 */
export function escapeHtml(raw: string): string {
  return raw
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

/**
 * Only `https://images.igdb.com/...` may become an `og:image`.
 *
 * The DTO is ours, so this looks redundant today. It isn't: `og:image` is a URL we ask other
 * people's servers (WhatsApp, Discord, Slack) to fetch on our behalf, which makes anything
 * that reaches it a request-forgery primitive pointed at third parties. Same reasoning as
 * `hostMatches()` in security.ts — the check belongs at the boundary, not at the one call site
 * that happens to be safe right now.
 */
export function safeImageUrl(raw: string | null): string | null {
  if (raw === null) return null;
  let parsed: URL;
  try {
    parsed = new URL(raw);
  } catch {
    return null;
  }
  const ok = parsed.protocol === "https:" && parsed.hostname === "images.igdb.com";
  return ok ? parsed.toString() : null;
}

/** "2017 · Action, Adventure · about 30h" — whichever parts exist. */
function subtitle(game: GameDto): string {
  const parts: string[] = [];
  const year = game.released?.slice(0, 4);
  if (year && /^\d{4}$/.test(year)) parts.push(year);
  if (game.genres.length > 0) parts.push(game.genres.slice(0, 2).join(", "));
  const hours = game.playtimeHoursNormally ?? game.playtimeHoursHastily;
  if (hours !== null && hours !== undefined && hours > 0 && hours <= 300) parts.push(`about ${hours}h`);
  return parts.join(" · ");
}

/**
 * The landing page.
 *
 * Single self-contained document — no external CSS, no JS, no web fonts. A link unfurling in a
 * chat app is fetched by a bot on someone else's infrastructure with its own timeout, and a
 * page that needs a second round trip to look right is a page that sometimes unfurls blank.
 */
export function renderGamePage(game: GameDto, campaign: CampaignKey, canonicalUrl: string): string {
  const copy = CAMPAIGNS[campaign];
  const name = escapeHtml(game.name);
  const meta = escapeHtml(subtitle(game));
  const headline = `${copy.kicker} ${game.name.toUpperCase()}`;
  const art = safeImageUrl(game.backgroundUrl) ?? safeImageUrl(game.coverUrl);
  const cover = safeImageUrl(game.coverUrl);

  // `intent://` is the escape hatch for the case where App Links verification did not take —
  // an older Android, a sideload, or a fingerprint that hasn't been updated yet. Its
  // `S.browser_fallback_url` sends anyone without the app to Play, so it is safe to show
  // unconditionally.
  const intentUrl =
    `intent://g/${game.id}#Intent;scheme=continueapp;package=${ANDROID_PACKAGE};` +
    `S.browser_fallback_url=${encodeURIComponent(PLAY_URL)};end`;

  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>${name} — CONTINUE?</title>
<meta name="description" content="${escapeHtml(copy.blurb)}">
<link rel="canonical" href="${escapeHtml(canonicalUrl)}">
<meta property="og:type" content="website">
<meta property="og:site_name" content="CONTINUE?">
<meta property="og:title" content="${escapeHtml(headline)}">
<meta property="og:description" content="${escapeHtml(copy.blurb)}">
<meta property="og:url" content="${escapeHtml(canonicalUrl)}">
${art ? `<meta property="og:image" content="${escapeHtml(art)}">
<meta property="og:image:alt" content="${name} key art">
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:image" content="${escapeHtml(art)}">` : `<meta name="twitter:card" content="summary">`}
<meta name="twitter:title" content="${escapeHtml(headline)}">
<meta name="twitter:description" content="${escapeHtml(copy.blurb)}">
<style>
:root{color-scheme:dark}
*{box-sizing:border-box}
body{margin:0;background:#08090C;color:#F2F4F8;
  font-family:'Chakra Petch',ui-sans-serif,system-ui,-apple-system,'Segoe UI',Roboto,sans-serif;
  display:flex;justify-content:center;padding:24px 16px 40px}
.card{width:100%;max-width:420px}
.wordmark{font-size:14px;letter-spacing:.22em;color:#F7C948;font-weight:700;margin-bottom:24px}
.art{position:relative;border-radius:16px;overflow:hidden;border:1px solid #242833;background:#101218;
  aspect-ratio:16/9;display:flex;align-items:center;justify-content:center}
.art img{width:100%;height:100%;object-fit:cover;display:block}
.cover{width:96px;border-radius:8px;box-shadow:0 8px 28px rgba(0,0,0,.6)}
.kicker{margin:24px 0 6px;font-size:12px;letter-spacing:.18em;color:#00E5A0;font-weight:700}
h1{margin:0;font-size:30px;line-height:1.15;font-weight:700;letter-spacing:-.01em}
.meta{margin:10px 0 0;font-size:14px;color:#9AA3B2}
.blurb{margin:18px 0 0;font-size:15px;line-height:1.5;color:#F2F4F8}
.cta{display:block;margin-top:26px;padding:16px;border-radius:12px;background:#F7C948;color:#08090C;
  text-align:center;font-weight:700;letter-spacing:.08em;text-decoration:none;font-size:15px}
.alt{display:block;margin-top:12px;padding:14px;border-radius:12px;border:1px solid #242833;
  color:#9AA3B2;text-align:center;text-decoration:none;font-size:14px;letter-spacing:.04em}
.pitch{margin:30px 0 0;padding-top:22px;border-top:1px solid #242833;font-size:14px;
  line-height:1.6;color:#9AA3B2}
.pitch strong{color:#F2F4F8;font-weight:700}
.attr{margin:26px 0 0;font-size:11px;line-height:1.5;color:#5A6373}
.attr a{color:#5A6373}
</style>
</head>
<body>
<main class="card">
  <div class="wordmark">CONTINUE?</div>
  <div class="art">
    ${art ? `<img src="${escapeHtml(art)}" alt="${name} key art">`
      : cover ? `<img class="cover" src="${escapeHtml(cover)}" alt="${name} cover art">`
      : ""}
  </div>
  <p class="kicker">${escapeHtml(copy.kicker)}</p>
  <h1>${name}</h1>
  ${meta ? `<p class="meta">${meta}</p>` : ""}
  <p class="blurb">${escapeHtml(copy.blurb)}</p>
  <a class="cta" href="${escapeHtml(PLAY_URL)}">ADD IT TO MY PILE</a>
  <!-- "Already have CONTINUE?" would render a double question mark, the brand's and the
       sentence's. The app's name ends in punctuation and that has to be written around. -->
  <a class="alt" href="${escapeHtml(intentUrl)}">Already have the app? Open it there</a>
  <p class="pitch"><strong>CONTINUE?</strong> is a backlog manager that plays like an arcade
    cabinet. Every game you started and never finished, in one pile — then it picks one for you.
    <em>The games you started deserve an ending.</em></p>
  <p class="attr">The data was freely provided by
    <a href="https://www.igdb.com/games/${escapeHtml(game.slug)}">IGDB.com</a>.</p>
</main>
</body>
</html>`;
}

/** Fetches the game through the shared detail cache and renders the page. */
export async function gameLinkResponse(
  env: Env,
  provider: GameProvider,
  id: number,
  campaign: CampaignKey,
  canonicalUrl: string,
): Promise<Response> {
  const game = await cached(env, `detail:${id}`, CacheTtl.DETAIL, () => provider.detail(id));
  if (!game) {
    return new Response(notFoundPage(), {
      status: 404,
      headers: htmlHeaders(0),
    });
  }
  // A game's key art and title effectively never change, and this page is fetched once per
  // recipient of a forwarded message — so let the edge and the chat-app unfurl bots hold it.
  return new Response(renderGamePage(game, campaign, canonicalUrl), { headers: htmlHeaders(3600) });
}

function htmlHeaders(maxAge: number): HeadersInit {
  return {
    "Content-Type": "text/html; charset=utf-8",
    "X-Content-Type-Options": "nosniff",
    "Referrer-Policy": "no-referrer",
    /**
     * Keep search engines out of this route entirely.
     *
     * Not an SEO preference — a quota defence. `/g/<id>` is the first endpoint in this project
     * whose URLs are meant to be **posted in public**, and a cache miss costs one IGDB request
     * plus one KV write against a 1,000 writes/day budget (docs/12-SECURITY.md). A crawler that
     * discovers one shared link on a public page and walks the id space would spend that budget
     * in minutes, with entirely innocent intent and from addresses the per-IP limiter treats as
     * well-behaved because each request is slow and polite.
     *
     * `nofollow` matters as much as `noindex`: without it a crawler still follows the links it
     * finds here, and `noindex` only stops the result being *listed*, not fetched.
     *
     * Malicious enumeration is a different problem and is handled elsewhere — this route is not
     * exempt from either rate limiter, and the account-wide one is what stops a distributed
     * walk. See `checkRateLimits` in security.ts.
     */
    "X-Robots-Tag": "noindex, nofollow",
    "Cache-Control": maxAge > 0 ? `public, max-age=${maxAge}` : "no-store",
  };
}

/** An unknown id is still a link someone tapped — send them somewhere, not to a blank 404. */
function notFoundPage(): string {
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>CONTINUE?</title>
<meta property="og:title" content="CONTINUE?">
<meta property="og:description" content="The games you started deserve an ending.">
<style>
body{margin:0;background:#08090C;color:#F2F4F8;font-family:ui-sans-serif,system-ui,sans-serif;
  display:flex;align-items:center;justify-content:center;min-height:100vh;padding:24px;text-align:center}
.wordmark{font-size:14px;letter-spacing:.22em;color:#F7C948;font-weight:700}
p{color:#9AA3B2;line-height:1.6}
a{display:inline-block;margin-top:20px;padding:16px 22px;border-radius:12px;background:#F7C948;
  color:#08090C;font-weight:700;letter-spacing:.08em;text-decoration:none}
</style>
</head>
<body>
<main>
  <div class="wordmark">CONTINUE?</div>
  <p>That game link has expired or never existed.</p>
  <a href="${PLAY_URL}">GET CONTINUE?</a>
</main>
</body>
</html>`;
}
