export interface Env {
  CACHE: KVNamespace;
  TWITCH_CLIENT_ID?: string;
  TWITCH_CLIENT_SECRET?: string;
  REVENUECAT_SECRET_KEY?: string;
  STEAM_API_KEY?: string;

  // Rate limiters — see src/security.ts. Optional so the Worker still boots (fail-open) if a
  // deploy is missing the binding, rather than 500ing every request.
  /** Per-IP budget for ordinary reads. */
  API_LIMITER?: RateLimit;
  /** Per-IP budget for `/resolve`, which fans one request out to several IGDB searches. */
  RESOLVE_LIMITER?: RateLimit;
  /** Account-wide ceiling on IGDB-touching work, so many IPs can't drain the shared quota. */
  GLOBAL_LIMITER?: RateLimit;
}

/** Mirrors app/.../core/network/dto/GameDto.kt — keep the two in sync by hand. */
export interface GameDto {
  id: number;
  slug: string;
  name: string;
  coverUrl: string | null;
  backgroundUrl: string | null;
  released: string | null;
  metacritic: number | null;
  rating: number | null;
  playtimeHoursHastily: number | null;
  playtimeHoursNormally: number | null;
  playtimeHoursCompletely: number | null;
  genres: string[];
  /**
   * IGDB **themes plus game modes**, in that order — "Horror", "Stealth", "Multiplayer",
   * "Co-operative". `game_modes` was being requested from IGDB and then thrown away, which is
   * why nothing in the app could ever tell a multiplayer game from a single-player one (and
   * why `MoodMapper`'s CHAOS rule, which looks for "multiplayer" in exactly this list, had
   * never once matched). Folded into `tags` rather than added as a fourth array so the app's
   * Room schema doesn't need a migration to carry it.
   */
  tags: string[];
  platforms: string[];
}

export interface SearchResponse {
  results: GameDto[];
}

export interface ResolveCandidate {
  id: number;
  name: string;
  confidence: number;
  coverUrl: string | null;
}

export interface ResolveResponse {
  resolvedTitle: string | null;
  source: string;
  candidates: ResolveCandidate[];
  needsManualEntry: boolean;
  /**
   * Cleanest available guess at a *game name*, for the app to prefill its manual-entry field
   * with. Null when there is nothing better than a bare link — an empty field beats a field
   * the user has to clear first. Never contains a URL.
   */
  suggestion: string | null;
}

export interface SteamOwnedGame {
  gameId: number | null;
  steamAppId: number;
  name: string;
  playtimeHours: number;
}

export interface SteamOwnedResponse {
  games: SteamOwnedGame[];
  /** True when the library exceeded the import cap and only the most-played games are here. */
  truncated: boolean;
}

/** docs/08-GAME-DATA.md — swappable in one file if the provider changes again. */
export interface GameProvider {
  search(query: string): Promise<GameDto[]>;
  detail(id: number): Promise<GameDto | null>;
  /**
   * Several games by id in one round trip — what the app's cache refresh uses so a 40-game
   * pile costs one request rather than forty. Order and completeness are not guaranteed:
   * unknown ids are simply absent from the result.
   */
  byIds(ids: number[]): Promise<GameDto[]>;
  /**
   * The browsable rails. [offset] is what "SHOW MORE" spends — every rail returns 20 at a
   * time, and the route caps how deep a client may page (see PAGE_SIZE/MAX_PAGE in index.ts)
   * so paging can never mint unbounded KV keys.
   */
  trending(offset?: number): Promise<GameDto[]>;
  shortAndSweet(offset?: number): Promise<GameDto[]>;
  newReleases(offset?: number): Promise<GameDto[]>;
  hiddenGems(offset?: number): Promise<GameDto[]>;
  /**
   * The genre table, so a caller can turn a genre *name* into an id. Kept separate from
   * [byGenre] on purpose: the route caches this whole table under one key and resolves names
   * in memory, which is what stops arbitrary client strings from minting unbounded KV keys.
   */
  genres(): Promise<GenreRef[]>;
  byGenre(genreId: number, offset?: number): Promise<GameDto[]>;
}

export interface GenreRef {
  id: number;
  name: string;
}
