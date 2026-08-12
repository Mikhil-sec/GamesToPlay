import type { Env, GameDto, GameProvider } from "../types.ts";
import type { TwitchAuthClient } from "../twitch/TwitchAuthClient.ts";

const IGDB_BASE = "https://api.igdb.com/v4";

const GAME_FIELDS =
  "name, slug, cover.image_id, first_release_date, aggregated_rating, rating, " +
  "total_rating, total_rating_count, genres.name, themes.name, game_modes.name, platforms.name";

/**
 * `game_types` id for a standalone main game (not a DLC, bundle, port, mod, …).
 *
 * IGDB **deprecated the `category` enum in favour of the `game_type` reference** and stopped
 * populating `category`. The nasty part is the failure mode: filtering on a deprecated field
 * is not a syntax error, so `where category = 0` returns `200 OK` with an empty array rather
 * than a 4xx. Every list endpoint silently returned zero games and the Worker looked healthy
 * (`/health` only checks that credentials exist). Don't reintroduce `category` — see
 * docs/08-GAME-DATA.md.
 */
const GAME_TYPE_MAIN_GAME = 0;

/** Excludes DLC/bundles/ports and alternate editions ("Gold Edition") from every list. */
const MAIN_GAMES_ONLY = `game_type = ${GAME_TYPE_MAIN_GAME} & version_parent = null`;

interface IgdbCover {
  image_id: string;
}

interface IgdbNamed {
  name: string;
}

interface IgdbGame {
  id: number;
  slug: string;
  name: string;
  cover?: IgdbCover;
  first_release_date?: number;
  aggregated_rating?: number;
  rating?: number;
  total_rating?: number;
  total_rating_count?: number;
  genres?: IgdbNamed[];
  themes?: IgdbNamed[];
  game_modes?: IgdbNamed[];
  platforms?: IgdbNamed[];
}

/** `/game_time_to_beats` rows — all durations in seconds, keyed by `game_id`. */
interface IgdbTimeToBeat {
  game_id: number;
  hastily?: number;
  normally?: number;
  completely?: number;
  count?: number;
}

/** t_cover_big for grids per docs/08-GAME-DATA.md §Covers. */
function coverUrl(cover?: IgdbCover): string | null {
  return cover ? `https://images.igdb.com/igdb/image/upload/t_cover_big/${cover.image_id}.jpg` : null;
}

function secondsToHours(seconds?: number): number | null {
  return seconds && seconds > 0 ? Math.round(seconds / 3600) : null;
}

function toDto(game: IgdbGame, ttb?: IgdbTimeToBeat): GameDto {
  return {
    id: game.id,
    slug: game.slug,
    name: game.name,
    coverUrl: coverUrl(game.cover),
    backgroundUrl: null,
    released: game.first_release_date
      ? new Date(game.first_release_date * 1000).toISOString().slice(0, 10)
      : null,
    metacritic: null,
    rating: game.total_rating ?? game.rating ?? game.aggregated_rating ?? null,
    playtimeHoursHastily: secondsToHours(ttb?.hastily),
    playtimeHoursNormally: secondsToHours(ttb?.normally),
    playtimeHoursCompletely: secondsToHours(ttb?.completely),
    genres: (game.genres ?? []).map((g) => g.name),
    tags: (game.themes ?? []).map((t) => t.name),
    platforms: (game.platforms ?? []).map((p) => p.name),
  };
}

/** Real IGDB-backed provider — docs/08-GAME-DATA.md. Never called directly by the app. */
export class IgdbGameProvider implements GameProvider {
  constructor(
    private readonly env: Env,
    private readonly authClient: TwitchAuthClient,
  ) {}

  private async query<T>(endpoint: string, body: string): Promise<T[]> {
    const token = await this.authClient.getAccessToken();
    const response = await fetch(`${IGDB_BASE}/${endpoint}`, {
      method: "POST",
      headers: {
        "Client-ID": this.env.TWITCH_CLIENT_ID!,
        Authorization: `Bearer ${token}`,
        "Content-Type": "text/plain",
      },
      body,
    });
    if (response.status === 429) {
      throw new Error("IGDB rate limit hit (429) — retry with backoff");
    }
    if (!response.ok) {
      // IGDB puts the actual apicalypse error (bad field, bad syntax) in the body — surfacing
      // it beats a bare status code when a query silently stops matching, as `category` did.
      const detail = await response.text().catch(() => "");
      throw new Error(`IGDB request failed: ${response.status} ${detail.slice(0, 300)}`);
    }
    return (await response.json()) as T[];
  }

  /**
   * Attaches real how-long-to-beat times to a list in **one** extra IGDB request rather than
   * one per game. Playtime is load-bearing for this app (the time budget and DRAW's
   * time-filter both key off it), so a list without it is only half useful — but the 4 req/sec
   * ceiling in docs/08-GAME-DATA.md rules out fanning out per result. Best-effort: if the
   * lookup fails the games still come back, just with null playtimes.
   */
  private async withPlaytimes(games: IgdbGame[]): Promise<GameDto[]> {
    if (games.length === 0) return [];
    const ids = games.map((g) => g.id).join(",");
    const times = await this.query<IgdbTimeToBeat>(
      "game_time_to_beats",
      `fields game_id, hastily, normally, completely; where game_id = (${ids}); limit ${games.length};`,
    ).catch(() => [] as IgdbTimeToBeat[]);

    const byGameId = new Map(times.map((t) => [t.game_id, t]));
    return games.map((game) => toDto(game, byGameId.get(game.id)));
  }

  async search(query: string): Promise<GameDto[]> {
    const escaped = query.replace(/"/g, '\\"');
    const games = await this.query<IgdbGame>(
      "games",
      `search "${escaped}"; fields ${GAME_FIELDS}; where ${MAIN_GAMES_ONLY}; limit 20;`,
    );
    return this.withPlaytimes(games);
  }

  async detail(id: number): Promise<GameDto | null> {
    const games = await this.query<IgdbGame>("games", `fields ${GAME_FIELDS}; where id = ${id};`);
    if (!games[0]) return null;
    return (await this.withPlaytimes(games))[0] ?? null;
  }

  /**
   * "Trending" for a *backlog* app means games people are actually playing and rating right
   * now — not IGDB's `hypes`, which counts pre-release follows and therefore surfaces
   * unreleased games you can't add to a pile. Scoped to the last ~3 years and ordered by
   * how many people have rated it.
   */
  async trending(): Promise<GameDto[]> {
    const threeYearsAgo = Math.floor(Date.now() / 1000) - 60 * 60 * 24 * 365 * 3;
    const now = Math.floor(Date.now() / 1000);
    const games = await this.query<IgdbGame>(
      "games",
      `fields ${GAME_FIELDS}; where ${MAIN_GAMES_ONLY} & cover != null & ` +
        `first_release_date > ${threeYearsAgo} & first_release_date < ${now} & ` +
        `total_rating_count > 5; sort total_rating_count desc; limit 20;`,
    );
    return this.withPlaytimes(games);
  }

  /**
   * Real "short and sweet": games that genuinely take under 8 hours, sourced from
   * `/game_time_to_beats` rather than approximated by a rating floor (which is what this did
   * before and it wasn't remotely the same thing). Two requests, not N — pick the short games
   * first, then hydrate them.
   */
  async shortAndSweet(): Promise<GameDto[]> {
    const EIGHT_HOURS_SECONDS = 8 * 3600;
    const times = await this.query<IgdbTimeToBeat>(
      "game_time_to_beats",
      `fields game_id, hastily, normally, completely; ` +
        `where normally > 0 & normally < ${EIGHT_HOURS_SECONDS} & count > 5; ` +
        `sort count desc; limit 60;`,
    );
    if (times.length === 0) return [];

    const byGameId = new Map(times.map((t) => [t.game_id, t]));
    const games = await this.query<IgdbGame>(
      "games",
      `fields ${GAME_FIELDS}; where id = (${[...byGameId.keys()].join(",")}) & ` +
        `${MAIN_GAMES_ONLY} & cover != null; sort total_rating_count desc; limit 20;`,
    );
    return games.map((game) => toDto(game, byGameId.get(game.id)));
  }
}
