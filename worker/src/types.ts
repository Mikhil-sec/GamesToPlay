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
  trending(): Promise<GameDto[]>;
  shortAndSweet(): Promise<GameDto[]>;
}
