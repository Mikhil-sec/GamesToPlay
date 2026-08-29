import type { GameDto, GameProvider, GenreRef } from "../types.ts";
// Reuses the exact same offline seed set the Android app bundles (tools/generate_seed.mjs
// at the repo root) — one source of truth instead of two copies drifting apart.
import seedGamesRaw from "../../../app/src/main/assets/seed_games.json";

interface SeedGame {
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

const seedGames = seedGamesRaw as SeedGame[];

function toDto(game: SeedGame): GameDto {
  return {
    id: game.id,
    slug: game.slug,
    name: game.name,
    coverUrl: game.coverUrl,
    backgroundUrl: game.backgroundUrl,
    released: game.released,
    metacritic: game.metacritic,
    rating: game.rating,
    playtimeHoursHastily: game.playtimeHoursHastily,
    playtimeHoursNormally: game.playtimeHoursNormally,
    playtimeHoursCompletely: game.playtimeHoursCompletely,
    genres: game.genres,
    tags: game.tags,
    platforms: game.platforms,
  };
}

/**
 * Fallback provider used whenever Twitch/IGDB credentials aren't configured yet
 * (docs/09-PENDING-INPUTS.md) — selected automatically in src/index.ts. Keeps every route
 * fully testable and demoable before real credentials land.
 */
export class SeedGameProvider implements GameProvider {
  async search(query: string): Promise<GameDto[]> {
    const q = query.trim().toLowerCase();
    if (!q) return seedGames.slice(0, 20).map(toDto);
    return seedGames
      .filter((g) => g.name.toLowerCase().includes(q))
      .slice(0, 20)
      .map(toDto);
  }

  async detail(id: number): Promise<GameDto | null> {
    const game = seedGames.find((g) => g.id === id);
    return game ? toDto(game) : null;
  }

  async byIds(ids: number[]): Promise<GameDto[]> {
    const wanted = new Set(ids);
    return seedGames.filter((g) => wanted.has(g.id)).map(toDto);
  }

  async trending(offset = 0): Promise<GameDto[]> {
    // No real popularity signal in the seed set; a stable pseudo-random sample reads better
    // in a demo than always showing the same first 20 alphabetically-adjacent entries.
    return page(sample(seedGames, 20 + offset), offset).map(toDto);
  }

  async shortAndSweet(offset = 0): Promise<GameDto[]> {
    return page(
      seedGames.filter((g) => (g.playtimeHoursNormally ?? Infinity) < 8),
      offset,
    ).map(toDto);
  }

  /** Newest first by release date — the seed set carries `released` as an ISO date string. */
  async newReleases(offset = 0): Promise<GameDto[]> {
    return page(
      seedGames
        .filter((g) => g.released !== null)
        .sort((a, b) => (b.released ?? "").localeCompare(a.released ?? "")),
      offset,
    ).map(toDto);
  }

  /** Well-rated but not famous — the seed set's only popularity signal is `metacritic`. */
  async hiddenGems(offset = 0): Promise<GameDto[]> {
    return page(
      seedGames
        .filter((g) => (g.metacritic ?? 0) >= 80)
        .sort((a, b) => (b.metacritic ?? 0) - (a.metacritic ?? 0)),
      offset,
    ).map(toDto);
  }

  /**
   * Synthetic ids, assigned by position in the sorted distinct-name list. They only have to be
   * stable within one deploy, because [byGenre] is the sole consumer and the route always
   * resolves a name through [genres] first — no id is ever persisted or sent by a client.
   */
  async genres(): Promise<GenreRef[]> {
    return seedGenreNames().map((name, index) => ({ id: index + 1, name }));
  }

  async byGenre(genreId: number, offset = 0): Promise<GameDto[]> {
    const name = seedGenreNames()[genreId - 1];
    if (!name) return [];
    return page(seedGames.filter((g) => g.genres.includes(name)), offset).map(toDto);
  }
}

/** One rail page of 20, so SHOW MORE behaves the same offline as it does against IGDB. */
function page<T>(items: T[], offset: number): T[] {
  return items.slice(offset, offset + 20);
}

function seedGenreNames(): string[] {
  return [...new Set(seedGames.flatMap((g) => g.genres))].sort();
}

function sample<T>(items: T[], count: number): T[] {
  const copy = [...items];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(seededRandom(i) * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy.slice(0, count);
}

// Deterministic per-request-process shuffle is fine here — this only feeds a "trending" rail
// with no correctness requirement, and avoids pulling in a real RNG/seed dependency.
function seededRandom(seed: number): number {
  const x = Math.sin(seed + Date.now() / 1e10) * 10000;
  return x - Math.floor(x);
}
