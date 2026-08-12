import type { Env, GameProvider, SteamOwnedResponse } from "../types.ts";
import { MAX_STEAM_GAMES } from "../security.ts";

interface SteamApp {
  appid: number;
  name: string;
  playtime_forever: number; // minutes
}

interface SteamOwnedGamesResponse {
  response: { games?: SteamApp[] };
}

/**
 * docs/02-PRODUCT-SPEC.md §2b — Steam import. Resolves a vanity name or numeric SteamID64
 * to owned games, then loosely matches each title against the game provider by name.
 * Requires STEAM_API_KEY (docs/09-PENDING-INPUTS.md); returns a clear 501 until it's set,
 * same "fail closed with a clear error" policy as /coins/spend.
 */
export async function getSteamOwnedGames(
  env: Env,
  provider: GameProvider,
  vanityOrId: string,
): Promise<{ status: number; body: SteamOwnedResponse | { error: string } }> {
  if (!env.STEAM_API_KEY) {
    return { status: 501, body: { error: "STEAM_API_KEY not configured (docs/09-PENDING-INPUTS.md)" } };
  }

  let steamId64 = vanityOrId;
  if (!/^\d{17}$/.test(vanityOrId)) {
    const resolveUrl = `https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key=${env.STEAM_API_KEY}&vanityurl=${encodeURIComponent(vanityOrId)}`;
    const resolveResponse = await fetch(resolveUrl);
    const resolveData = (await resolveResponse.json()) as { response: { success: number; steamid?: string } };
    if (resolveData.response.success !== 1 || !resolveData.response.steamid) {
      return { status: 404, body: { error: "Steam profile not found" } };
    }
    steamId64 = resolveData.response.steamid;
  }

  const ownedUrl =
    `https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/` +
    `?key=${env.STEAM_API_KEY}&steamid=${steamId64}&include_appinfo=true&format=json`;
  const ownedResponse = await fetch(ownedUrl);
  if (!ownedResponse.ok) {
    return { status: 502, body: { error: `Steam API request failed: ${ownedResponse.status}` } };
  }
  const ownedData = (await ownedResponse.json()) as SteamOwnedGamesResponse;
  const allSteamGames = ownedData.response.games ?? [];

  /*
   * Capped, and matched sequentially rather than with `Promise.all`.
   *
   * This was by far the worst amplifier in the codebase: an unbounded `Promise.all` over a
   * library that can hold thousands of games turned **one** inbound request into thousands of
   * simultaneous IGDB searches — blowing the 4 req/sec ceiling and the daily KV write budget
   * from a single curl, with no attacker effort at all. Most-played first, so the cap keeps
   * the games the user actually cares about.
   */
  const steamGames = [...allSteamGames]
    .sort((a, b) => b.playtime_forever - a.playtime_forever)
    .slice(0, MAX_STEAM_GAMES);

  const games = [];
  for (const steamApp of steamGames) {
    const matches = await provider.search(steamApp.name).catch(() => []);
    games.push({
      gameId: matches[0]?.id ?? null,
      steamAppId: steamApp.appid,
      name: steamApp.name,
      playtimeHours: Math.round((steamApp.playtime_forever / 60) * 10) / 10,
    });
  }

  return { status: 200, body: { games, truncated: allSteamGames.length > MAX_STEAM_GAMES } };
}
