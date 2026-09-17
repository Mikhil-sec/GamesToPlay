/**
 * `/p` — the landing page for a shared **pile** (FRIENDS, versionCode 11).
 *
 * The pile itself is in the URL's fragment (`/p#<payload>`), and browsers never send a fragment
 * to a server. So this route receives no data at all: every request for it is byte-for-byte the
 * same, the Worker never learns which pile was opened or by whom, and there is nothing here to
 * cache, look up, or rate-limit on behalf of IGDB. That is the whole privacy design of FRIENDS —
 * see `PileSnapshot.kt` in the app — and it is why this page is static.
 *
 * Only ever *seen* by someone without the app (or whose install hasn't verified App Links): a
 * verified Android install opens `/p` links straight into the app. The page's one dynamic job is
 * done in the browser: a small inline script reads the fragment, checks it is nothing but
 * URL-safe base64, and points "Open in CONTINUE?" at the `continueapp://p/<payload>` fallback.
 * The payload is never written into the document, so there is no injection surface, and the
 * Content-Security-Policy allows exactly that one script by hash.
 */

/** Permanent, per CLAUDE.md — must match `ANDROID_PACKAGE` in gameLink.ts. */
const ANDROID_PACKAGE = "com.mikhilnaika.continueapp";
const PLAY_URL = `https://play.google.com/store/apps/details?id=${ANDROID_PACKAGE}`;

/** Same bound as the app's `PileSnapshotCodec.MAX_PAYLOAD_CHARS`. */
export const MAX_PAYLOAD_CHARS = 4096;

/**
 * The page's only script. Plain ES5, no dependencies, no network. Constants are embedded with
 * `JSON.stringify`, so nothing in them can terminate the string they sit in.
 */
export const PILE_PAGE_SCRIPT = `(function () {
  var payload = location.hash.slice(1);
  var open = document.getElementById("open");
  var note = document.getElementById("note");
  if (!/^[A-Za-z0-9_-]+$/.test(payload) || payload.length > ${MAX_PAYLOAD_CHARS}) {
    note.textContent = "This link looks incomplete. Ask your friend to share their pile again, and open the full link.";
    return;
  }
  if (!/Android/i.test(navigator.userAgent)) {
    note.textContent = "Open this link on an Android phone with CONTINUE? installed to follow their pile.";
    return;
  }
  open.href = "intent://p/" + payload + "#Intent;scheme=continueapp;package=" + ${JSON.stringify(ANDROID_PACKAGE)} +
    ";S.browser_fallback_url=" + ${JSON.stringify(encodeURIComponent(PLAY_URL))} + ";end";
  open.hidden = false;
})();`;

let scriptHash: Promise<string> | null = null;

/** `sha256-…` of [PILE_PAGE_SCRIPT], for the CSP. Computed once per isolate. */
export function pileScriptHash(): Promise<string> {
  scriptHash ??= crypto.subtle
    .digest("SHA-256", new TextEncoder().encode(PILE_PAGE_SCRIPT))
    .then((digest) => `sha256-${btoa(String.fromCharCode(...new Uint8Array(digest)))}`);
  return scriptHash;
}

export function renderPilePage(): string {
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>A friend's game pile — CONTINUE?</title>
<meta name="description" content="Everything they're playing, finished and still putting off. Follow it in CONTINUE?">
<meta property="og:type" content="website">
<meta property="og:site_name" content="CONTINUE?">
<meta property="og:title" content="FOLLOW MY GAME PILE ON CONTINUE?">
<meta property="og:description" content="Everything I'm playing, finished and still putting off. Tap to see it in the app.">
<meta name="twitter:card" content="summary">
<meta name="twitter:title" content="FOLLOW MY GAME PILE ON CONTINUE?">
<meta name="twitter:description" content="Everything I'm playing, finished and still putting off.">
<style>
:root{color-scheme:dark}
*{box-sizing:border-box}
body{margin:0;background:#08090C;color:#F2F4F8;
  font-family:'Chakra Petch',ui-sans-serif,system-ui,-apple-system,'Segoe UI',Roboto,sans-serif;
  display:flex;justify-content:center;padding:24px 16px 40px}
.card{width:100%;max-width:420px}
.wordmark{font-size:14px;letter-spacing:.22em;color:#F7C948;font-weight:700;margin-bottom:28px}
.kicker{margin:0 0 6px;font-size:12px;letter-spacing:.18em;color:#00E5A0;font-weight:700}
h1{margin:0;font-size:30px;line-height:1.15;font-weight:700;letter-spacing:-.01em}
.blurb{margin:14px 0 0;font-size:15px;line-height:1.5;color:#9AA3B2}
ol{margin:22px 0 0;padding:0;list-style:none;counter-reset:step}
li{counter-increment:step;display:flex;gap:12px;align-items:flex-start;margin:0 0 12px;
  font-size:15px;line-height:1.45}
li::before{content:counter(step);flex:none;width:26px;height:26px;border-radius:50%;
  border:2px solid #F7C948;color:#F7C948;font-weight:700;font-size:13px;
  display:flex;align-items:center;justify-content:center}
.cta{display:block;margin-top:22px;padding:16px;border-radius:12px;background:#00E5A0;color:#08090C;
  text-align:center;font-weight:700;letter-spacing:.08em;text-decoration:none;font-size:15px}
.cta.get{background:#F7C948}
.cta[hidden]{display:none}
.note{margin:12px 0 0;font-size:13px;line-height:1.5;color:#9AA3B2;text-align:center;min-height:1em}
.privacy{margin:28px 0 0;padding:14px;border-radius:12px;border:1px solid #242833;background:#101218;
  font-size:13px;line-height:1.55;color:#9AA3B2}
.privacy strong{color:#F2F4F8}
.pitch{margin:24px 0 0;padding-top:20px;border-top:1px solid #242833;font-size:14px;
  line-height:1.6;color:#9AA3B2}
.pitch strong{color:#F2F4F8}
</style>
</head>
<body>
<main class="card">
  <div class="wordmark">CONTINUE?</div>
  <p class="kicker">A FRIEND SHARED THEIR GAME PILE</p>
  <h1>See what they're playing</h1>
  <p class="blurb">Their whole pile — now playing, cleared, still waiting — ready to follow in the app.</p>
  <a id="open" class="cta" href="#" hidden>OPEN IN CONTINUE?</a>
  <p id="note" class="note"></p>
  <ol>
    <li><span>Get CONTINUE? from Google Play.</span></li>
    <li><span>Come back to the chat and tap the link again.</span></li>
    <li><span>Their pile lands in <strong>FRIENDS</strong>, and updates whenever they share it again.</span></li>
  </ol>
  <a class="cta get" href="${PLAY_URL}">GET CONTINUE?</a>
  <p class="privacy"><strong>This page never saw their pile.</strong> The game list travels inside
    the link itself, in the part after the <code>#</code>, which browsers don't send to servers.
    No account, no name, nothing stored.</p>
  <p class="pitch"><strong>CONTINUE?</strong> is a backlog manager that plays like an arcade
    cabinet. Every game you started and never finished, in one pile — then it picks one for you.
    <em>The games you started deserve an ending.</em></p>
</main>
<script>${PILE_PAGE_SCRIPT}</script>
</body>
</html>`;
}

/**
 * Whether a request is for the landing page. Exactly `/p` (what the app mints) or `/p/`, GET
 * only — anything longer under `/p/` is an ordinary unknown path and goes through rate limiting
 * like every other one.
 */
export function isPileLandingRequest(pathname: string, method: string): boolean {
  return method === "GET" && (pathname === "/p" || pathname === "/p/");
}

export async function pileLinkResponse(): Promise<Response> {
  const hash = await pileScriptHash();
  return new Response(renderPilePage(), {
    headers: {
      "Content-Type": "text/html; charset=utf-8",
      "X-Content-Type-Options": "nosniff",
      "Referrer-Policy": "no-referrer",
      // Not an SEO preference: nothing about a friend's pile should be indexable, and a
      // crawler has no business here.
      "X-Robots-Tag": "noindex, nofollow",
      "Content-Security-Policy":
        `default-src 'none'; script-src '${hash}'; style-src 'unsafe-inline'; ` +
        "base-uri 'none'; form-action 'none'; frame-ancestors 'none'",
      // Identical for every visitor, so the edge and unfurl bots can hold it.
      "Cache-Control": "public, max-age=86400",
    },
  });
}
