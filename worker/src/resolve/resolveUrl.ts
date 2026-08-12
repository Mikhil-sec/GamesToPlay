/**
 * Stage 1 — URL to text. docs/02-PRODUCT-SPEC.md §2a: most share sheets hand us a bare URL
 * with no title, so this resolves it to *some* text before Stage 2 (titleParser.ts) tries to
 * extract a game name from it. Server-side on purpose: these parsers break whenever a
 * platform changes its markup, and a Worker fix ships in minutes vs. an app fix waiting on
 * Play review (docs/05-TECH-ARCHITECTURE.md).
 */
import { hostMatches } from "../security.ts";

export interface UrlResolution {
  text: string | null;
  source: string;
}

interface YoutubeOembed {
  title: string;
  author_name: string;
}

async function resolveYoutube(url: URL): Promise<UrlResolution> {
  let target = url.toString();
  // Normalize Shorts to the canonical watch URL before hitting oEmbed.
  const shortsMatch = url.pathname.match(/^\/shorts\/([\w-]+)/);
  if (shortsMatch) {
    target = `https://www.youtube.com/watch?v=${shortsMatch[1]}`;
  }
  const oembedUrl = `https://www.youtube.com/oembed?url=${encodeURIComponent(target)}&format=json`;
  const response = await fetch(oembedUrl);
  if (!response.ok) return { text: null, source: "youtube" };
  const data = (await response.json()) as YoutubeOembed;
  return { text: `${data.title} ${data.author_name ?? ""}`.trim(), source: "youtube" };
}

async function resolveTikTok(url: URL): Promise<UrlResolution> {
  // vm.tiktok.com short links redirect to the canonical video URL; oEmbed wants that form.
  let finalUrl = url.toString();
  if (url.hostname.includes("vm.tiktok.com")) {
    const redirect = await fetch(url.toString(), { redirect: "manual" });
    const location = redirect.headers.get("location");
    if (location) finalUrl = location;
  }
  const oembedUrl = `https://www.tiktok.com/oembed?url=${encodeURIComponent(finalUrl)}`;
  const response = await fetch(oembedUrl);
  if (!response.ok) return { text: null, source: "tiktok" };
  const data = (await response.json()) as { title?: string };
  return { text: data.title ?? null, source: "tiktok" };
}

async function resolveReddit(url: URL): Promise<UrlResolution> {
  const jsonUrl = url.toString().replace(/\/?$/, ".json");
  const response = await fetch(jsonUrl, { headers: { "User-Agent": "continue-app-worker/0.1" } });
  if (!response.ok) return { text: null, source: "reddit" };
  const data = (await response.json()) as unknown;
  const title = (data as any)?.[0]?.data?.children?.[0]?.data?.title as string | undefined;
  return { text: title ?? null, source: "reddit" };
}

function resolveSteam(url: URL): UrlResolution {
  // store.steampowered.com/app/<id>/<Slug>/ — un-slug directly, no fetch needed.
  const match = url.pathname.match(/^\/app\/\d+\/([^/]+)/);
  if (!match) return { text: null, source: "steam" };
  const name = decodeURIComponent(match[1]).replace(/_/g, " ");
  return { text: name, source: "steam" };
}

export async function resolveUrlToText(rawUrl: string): Promise<UrlResolution> {
  let url: URL;
  try {
    url = new URL(rawUrl);
  } catch {
    return { text: null, source: "unknown" };
  }

  // Only https, and only hosts we explicitly recognise. This function performs *server-side
  // fetches of client-supplied URLs*, so the allowlist is the SSRF boundary — anything that
  // falls through must never be fetched.
  if (url.protocol !== "https:") {
    return { text: null, source: "unsupported-scheme" };
  }

  const host = url.hostname.replace(/^www\./, "");

  if (hostMatches(host, "youtube.com") || hostMatches(host, "youtu.be")) {
    return resolveYoutube(url);
  }
  if (hostMatches(host, "tiktok.com")) {
    return resolveTikTok(url);
  }
  if (hostMatches(host, "reddit.com")) {
    return resolveReddit(url);
  }
  if (hostMatches(host, "store.steampowered.com")) {
    return resolveSteam(url);
  }
  if (hostMatches(host, "instagram.com")) {
    // Meta removed public oEmbed in April 2025 — structurally unresolvable.
    return { text: null, source: "instagram" };
  }
  // X/Twitter and anything else: unreliable/unsupported today, degrade to manual entry
  // rather than guessing (docs/02-PRODUCT-SPEC.md degradation ladder).
  return { text: null, source: host || "unknown" };
}

export function looksLikeUrl(text: string): boolean {
  return /^https?:\/\//i.test(text.trim());
}
