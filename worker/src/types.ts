export interface Env {
  CACHE: KVNamespace;
  TWITCH_CLIENT_ID?: string;
  TWITCH_CLIENT_SECRET?: string;
  REVENUECAT_SECRET_KEY?: string;
  STEAM_API_KEY?: string;
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
}

/** docs/08-GAME-DATA.md — swappable in one file if the provider changes again. */
export interface GameProvider {
  search(query: string): Promise<GameDto[]>;
  detail(id: number): Promise<GameDto | null>;
  trending(): Promise<GameDto[]>;
  shortAndSweet(): Promise<GameDto[]>;
}
