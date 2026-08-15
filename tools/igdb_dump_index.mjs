#!/usr/bin/env node
/**
 * Builds the offline game index from IGDB **data dumps** — docs/08-GAME-DATA.md §Data dumps.
 *
 * Why this exists
 * ---------------
 * IGDB's `search` operator is near-exact, not fuzzy. One extra word kills a query outright:
 * `Palworld` returns 3 results, `Pocketpair Palworld` returns **0**. That's fine when a human
 * is typing, and useless when the input is a TikTok caption. `worker/src/resolve/candidates.ts`
 * works around it by generating substrings and firing up to 6 searches per share, which is both
 * inaccurate and the single most expensive thing the Worker does (IGDB allows 4 req/sec).
 *
 * Data-dump access (granted for our Client ID on 2026-08-14) removes the problem at the root:
 * with the whole name table on hand we can match locally, against every name *and every
 * alternative name*, with no network call and no rate limit.
 *
 * Where the index lives, and why it is NOT in the Worker
 * -----------------------------------------------------
 * The obvious home is the Worker, and it's the wrong one. **Workers Free allows 10ms of CPU per
 * request** (verified against Cloudflare's limits page, 2026-08-14). Parsing or scanning a
 * multi-megabyte index blows that budget on the first request into every cold isolate. The 3MB
 * compressed script limit is survivable; the CPU ceiling is not.
 *
 * So the index ships as an **app asset**. A phone has real CPU and memory, and putting it there
 * makes matching work with no network at all — which is CLAUDE.md constraint #5 (offline-first)
 * finally being true for the feature that most needs it.
 *
 * Credentials
 * -----------
 * Reads `TWITCH_CLIENT_ID` / `TWITCH_CLIENT_SECRET` from the environment, or from
 * `worker/.dev.vars` (gitignored — the same file wrangler uses for local dev). The secret never
 * enters the repo, the APK, or this file. See docs/12-SECURITY.md.
 *
 * Usage
 * -----
 *   node tools/igdb_dump_index.mjs --list        # what dumps exist and how big they are
 *   node tools/igdb_dump_index.mjs --inspect     # header + first rows of each dump (ranged GET,
 *                                                # a few KB — use this before any full download)
 *   node tools/igdb_dump_index.mjs               # full build -> app/src/main/assets/
 *
 * Note on the S3 URLs: they are presigned and **valid for 5 minutes**. Downloads here resume
 * with HTTP Range and re-request a fresh URL when one expires, because a multi-hundred-MB file
 * will not always finish inside that window.
 */
import { createWriteStream, createReadStream, existsSync, mkdirSync, readFileSync, statSync } from "node:fs";
import { createGzip } from "node:zlib";
import { pipeline } from "node:stream/promises";
import { createInterface } from "node:readline";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO = join(__dirname, "..");
const CACHE_DIR = join(__dirname, ".dump-cache");
const OUT_PATH = join(REPO, "app", "src", "main", "assets", "game_index.tsv.gz");

/** The four tables the index is joined from. `covers` is needed because `games.cover` is a
 *  cover *row id*, not the `image_id` the CDN URL is built from. */
const ENDPOINTS = ["games", "covers", "alternative_names", "game_time_to_beats"];

/**
 * Keep a game only if it clears this. The full table is ~350k rows including DLC, mods, ports
 * and thousands of untitled shovelware; indexing all of it would bloat the APK and *hurt*
 * matching by giving generic words like "Pal" or "Line" something to match against.
 */
const MIN_RATING_COUNT = 3;

/** `game_type` 0 = a standalone main game. Same constant, same reasoning, as the Worker —
 *  see MAIN_GAMES_ONLY in worker/src/providers/IgdbGameProvider.ts. */
const GAME_TYPE_MAIN_GAME = 0;

// ---------------------------------------------------------------------------- credentials

function loadCredentials() {
  let id = process.env.TWITCH_CLIENT_ID;
  let secret = process.env.TWITCH_CLIENT_SECRET;

  const devVars = join(REPO, "worker", ".dev.vars");
  if ((!id || !secret) && existsSync(devVars)) {
    for (const line of readFileSync(devVars, "utf8").split(/\r?\n/)) {
      const match = line.match(/^\s*(TWITCH_CLIENT_ID|TWITCH_CLIENT_SECRET)\s*=\s*"?([^"\s]+)"?\s*$/);
      if (!match) continue;
      if (match[1] === "TWITCH_CLIENT_ID") id ??= match[2];
      else secret ??= match[2];
    }
  }

  if (!id || !secret) {
    console.error(
      [
        "Missing Twitch credentials.",
        "",
        "Put them in worker/.dev.vars (gitignored, never shipped):",
        "  TWITCH_CLIENT_ID=...",
        "  TWITCH_CLIENT_SECRET=...",
        "",
        "or export them into the environment. The Client ID is public; the secret is not —",
        "it lives in `wrangler secret put` for the deployed Worker (docs/12-SECURITY.md).",
      ].join("\n"),
    );
    process.exit(1);
  }
  return { id, secret };
}

async function getAccessToken({ id, secret }) {
  const url = new URL("https://id.twitch.tv/oauth2/token");
  url.searchParams.set("client_id", id);
  url.searchParams.set("client_secret", secret);
  url.searchParams.set("grant_type", "client_credentials");
  const response = await fetch(url, { method: "POST" });
  if (!response.ok) throw new Error(`Twitch auth failed: ${response.status} ${await response.text()}`);
  return (await response.json()).access_token;
}

// ---------------------------------------------------------------------------- dumps API

async function igdb(path, { id }, token) {
  const response = await fetch(`https://api.igdb.com/v4${path}`, {
    headers: { "Client-ID": id, Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error(`IGDB ${path} failed: ${response.status} ${await response.text()}`);
  return response.json();
}

/** Presigned and short-lived, so this is called again whenever a download needs to resume. */
async function freshDumpUrl(endpoint, creds, token) {
  const dump = await igdb(`/dumps/${endpoint}`, creds, token);
  return dump;
}

// ---------------------------------------------------------------------------- download

const MB = 1024 * 1024;

/**
 * Downloads a dump to the local cache, resuming with `Range` across the 5-minute presigned-URL
 * expiry. Skips entirely if a complete copy is already cached — dumps only change daily, and
 * re-pulling hundreds of megabytes to rebuild the same index is pure waste.
 */
async function downloadDump(endpoint, creds, token) {
  mkdirSync(CACHE_DIR, { recursive: true });
  const target = join(CACHE_DIR, `${endpoint}.csv`);

  let meta = await freshDumpUrl(endpoint, creds, token);
  const total = meta.size_bytes;

  if (existsSync(target) && statSync(target).size === total) {
    console.log(`  ${endpoint}: cached (${(total / MB).toFixed(1)} MB), skipping`);
    return target;
  }

  console.log(`  ${endpoint}: ${(total / MB).toFixed(1)} MB, schema v${meta.schema_version}`);
  let downloaded = existsSync(target) ? statSync(target).size : 0;
  if (downloaded > total) downloaded = 0; // stale partial from an older dump — start over

  let attempts = 0;
  while (downloaded < total) {
    if (attempts++ > 40) throw new Error(`${endpoint}: too many resume attempts`);
    // A presigned URL dies after 5 minutes; every resume gets a new one rather than retrying a
    // URL that is now guaranteed to 403.
    if (attempts > 1) meta = await freshDumpUrl(endpoint, creds, token);

    const response = await fetch(meta.s3_url, {
      headers: downloaded > 0 ? { Range: `bytes=${downloaded}-` } : {},
    });
    if (!response.ok && response.status !== 206) {
      throw new Error(`${endpoint}: download failed ${response.status}`);
    }

    try {
      await pipeline(response.body, createWriteStream(target, { flags: downloaded > 0 ? "a" : "w" }));
    } catch (error) {
      // Connection resets partway through are expected on files this size; the bytes already on
      // disk are still good, so just loop and range-resume from wherever we got to.
      console.log(`    resuming after: ${error.message}`);
    }
    const now = existsSync(target) ? statSync(target).size : 0;
    if (now === downloaded) await new Promise((r) => setTimeout(r, 2000)); // no progress — back off
    downloaded = now;
    process.stdout.write(`\r    ${(downloaded / MB).toFixed(1)} / ${(total / MB).toFixed(1)} MB`);
  }
  process.stdout.write("\n");
  return target;
}

// ---------------------------------------------------------------------------- CSV

/**
 * Splits one CSV record. Hand-rolled rather than pulled from npm because every other tool in
 * `tools/` is dependency-free and this is ~20 lines: RFC4180 quoting, `""` as an escaped quote.
 */
function splitCsvLine(line) {
  const fields = [];
  let field = "";
  let quoted = false;
  for (let i = 0; i < line.length; i++) {
    const char = line[i];
    if (quoted) {
      if (char === '"') {
        if (line[i + 1] === '"') { field += '"'; i++; } else quoted = false;
      } else field += char;
    } else if (char === '"') quoted = true;
    else if (char === ",") { fields.push(field); field = ""; }
    else field += char;
  }
  fields.push(field);
  return fields;
}

/**
 * Streams a dump row-by-row as objects keyed by the header.
 *
 * Records can span physical lines (a quoted `summary` containing newlines), so lines are
 * accumulated until the quotes balance rather than assuming one line per row.
 */
async function* readCsv(path) {
  const reader = createInterface({ input: createReadStream(path, { encoding: "utf8" }), crlfDelay: Infinity });
  let header = null;
  let buffer = "";
  for await (const line of reader) {
    buffer = buffer === "" ? line : `${buffer}\n${line}`;
    // An odd number of quote characters means we're inside a quoted field that hasn't closed.
    if ((buffer.match(/"/g) ?? []).length % 2 !== 0) continue;
    const fields = splitCsvLine(buffer);
    buffer = "";
    if (header === null) { header = fields; continue; }
    if (fields.length !== header.length) continue; // malformed row — skip rather than misalign
    const row = {};
    for (let i = 0; i < header.length; i++) row[header[i]] = fields[i];
    yield row;
  }
}

// ---------------------------------------------------------------------------- modes

async function listDumps(creds, token) {
  const dumps = await igdb("/dumps", creds, token);
  const rows = Array.isArray(dumps) ? dumps : dumps.dumps ?? [];
  console.log(`${rows.length} dumps available:\n`);
  for (const dump of rows) {
    const size = dump.size_bytes ? `${(dump.size_bytes / MB).toFixed(1)} MB` : "?";
    console.log(`  ${(dump.endpoint ?? "?").padEnd(28)} ${size.padStart(10)}`);
  }
}

/**
 * Pulls only the first ~64KB of each dump we care about and prints the header plus two rows.
 *
 * Worth doing before any full download: it costs kilobytes instead of gigabytes and answers the
 * questions the docs don't — exactly how array columns are encoded, whether ids or image_ids
 * are in `covers`, what an empty value looks like.
 */
async function inspectDumps(creds, token) {
  for (const endpoint of ENDPOINTS) {
    const meta = await freshDumpUrl(endpoint, creds, token);
    const response = await fetch(meta.s3_url, { headers: { Range: "bytes=0-65535" } });
    const text = await response.text();
    const lines = text.split("\n");
    console.log(`\n=== ${endpoint}  (${(meta.size_bytes / MB).toFixed(1)} MB, schema v${meta.schema_version})`);
    console.log(`columns: ${lines[0]}`);
    console.log(`row 1:   ${lines[1]?.slice(0, 400)}`);
    console.log(`row 2:   ${lines[2]?.slice(0, 400)}`);
  }
}

// ---------------------------------------------------------------------------- build

/**
 * `first_release_date` is a genuine trap between IGDB's two surfaces: the REST API returns a
 * **Unix timestamp** (seconds), which is what `worker/src/providers/IgdbGameProvider.ts`
 * correctly multiplies by 1000 before handing to `Date`. The **CSV dump** returns the same
 * field as an **already-formatted datetime string**, `"2023-08-15 00:00:00"`. Treating the dump
 * value as a timestamp doesn't throw — `Number("2023-08-15 00:00:00")` is `NaN`, `new
 * Date(NaN * 1000)` is an Invalid Date, and `.getUTCFullYear()` silently returns `NaN`, which
 * then serialises into the TSV as the literal string "NaN". Caught by inspecting the real
 * output, not the docs — same lesson as the `category`/`game_type` bug this project already
 * paid for once.
 */
function parseDumpYear(value) {
  if (!value) return "";
  const year = new Date(value).getUTCFullYear();
  return Number.isFinite(year) ? year : "";
}

function toIntOrEmpty(value) {
  const n = Number(value);
  return Number.isFinite(n) && n > 0 ? Math.round(n) : "";
}

async function build(creds, token) {
  console.log("Downloading dumps (cached copies are reused):");
  const paths = {};
  for (const endpoint of ENDPOINTS) paths[endpoint] = await downloadDump(endpoint, creds, token);

  console.log("\nIndexing covers…");
  const coverImageId = new Map();
  for await (const row of readCsv(paths.covers)) {
    if (row.image_id) coverImageId.set(Number(row.id), row.image_id);
  }
  console.log(`  ${coverImageId.size} covers`);

  console.log("Indexing time-to-beat…");
  const hours = new Map();
  for await (const row of readCsv(paths.game_time_to_beats)) {
    const seconds = Number(row.normally);
    if (seconds > 0) hours.set(Number(row.game_id), Math.round(seconds / 3600));
  }
  console.log(`  ${hours.size} playtimes`);

  console.log("Scanning games…");
  const games = new Map();
  let scanned = 0;
  for await (const row of readCsv(paths.games)) {
    scanned++;
    // Mirrors the Worker's list filter so the offline index and the online results agree on
    // what counts as "a game" — a title that can be found offline but not online (or the
    // reverse) is worse than either behaviour on its own.
    if (Number(row.game_type) !== GAME_TYPE_MAIN_GAME) continue;
    if (row.version_parent) continue;
    const ratingCount = Number(row.total_rating_count) || 0;
    if (ratingCount < MIN_RATING_COUNT) continue;
    if (!row.name) continue;

    const id = Number(row.id);
    games.set(id, {
      id,
      name: row.name,
      cover: coverImageId.get(Number(row.cover)) ?? "",
      year: parseDumpYear(row.first_release_date),
      rating: toIntOrEmpty(row.total_rating),
      ratingCount,
      hours: hours.get(id) ?? "",
      alt: [],
    });
  }
  console.log(`  ${games.size} kept of ${scanned} scanned (game_type=0, no version parent, >=${MIN_RATING_COUNT} ratings)`);

  console.log("Attaching alternative names…");
  let altCount = 0;
  for await (const row of readCsv(paths.alternative_names)) {
    const game = games.get(Number(row.game));
    if (!game || !row.name) continue;
    // Alternative names are where "BG3", "FF7" and regional titles live — the exact strings a
    // caption is most likely to use and the API's `search` is least likely to match.
    if (row.name.toLowerCase() === game.name.toLowerCase()) continue;
    if (game.alt.length >= 4) continue; // cap: a handful of games have 30+, all long-tail
    game.alt.push(row.name);
    altCount++;
  }
  console.log(`  ${altCount} alternative names`);

  console.log("\nWriting index…");
  const header = "id\tname\tcover\tyear\trating\tratingCount\thours\talt\n";
  const rows = [...games.values()]
    // Descending popularity: the app's matcher can then break ties toward the better-known
    // game without carrying a separate sort, and a truncated read is still the useful half.
    .sort((a, b) => b.ratingCount - a.ratingCount)
    .map((g) =>
      [g.id, g.name, g.cover, g.year, g.rating, g.ratingCount, g.hours, g.alt.join("|")]
        // Tabs/newlines inside a name would corrupt the row format; there is no escaping in a
        // TSV, so they're normalised to spaces at build time instead.
        .map((v) => String(v).replace(/[\t\r\n]/g, " "))
        .join("\t"),
    )
    .join("\n");

  mkdirSync(dirname(OUT_PATH), { recursive: true });
  await pipeline(
    (async function* () { yield header; yield rows; })(),
    createGzip({ level: 9 }),
    createWriteStream(OUT_PATH),
  );

  const bytes = statSync(OUT_PATH).size;
  console.log(`\n✅ ${OUT_PATH}`);
  console.log(`   ${games.size} games · ${(bytes / MB).toFixed(2)} MB gzipped (this lands in the APK)`);
  if (bytes > 6 * MB) {
    console.log("   ⚠ over 6 MB — raise MIN_RATING_COUNT to trim the long tail before shipping");
  }
}

// ---------------------------------------------------------------------------- main

const creds = loadCredentials();
const token = await getAccessToken(creds);
const mode = process.argv[2];

if (mode === "--list") await listDumps(creds, token);
else if (mode === "--inspect") await inspectDumps(creds, token);
else await build(creds, token);
