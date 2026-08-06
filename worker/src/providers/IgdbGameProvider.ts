import type { Env, GameDto, GameProvider } from "../types.ts";
import type { TwitchAuthClient } from "../twitch/TwitchAuthClient.ts";

const IGDB_BASE = "https://api.igdb.com/v4";

const GAME_FIELDS =
  "name, slug, cover.image_id, first_release_date, aggregated_rating, rating, " +
  "genres.name, themes.name, game_modes.name, platforms.name";

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
  genres?: IgdbNamed[];
  themes?: IgdbNamed[];
  game_modes?: IgdbNamed[];
  platforms?: IgdbNamed[];
}

/** t_cover_big for grids per docs/08-GAME-DATA.md §Covers. */
function coverUrl(cover?: IgdbCover): string | null {
  return cover ? `https://images.igdb.com/igdb/image/upload/t_cover_big/${cover.image_id}.jpg` : null;
}

function toDto(game: IgdbGame): GameDto {
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
    rating: game.rating ?? game.aggregated_rating ?? null,
    // IGDB's game_time_to_beat needs a separate request per game; Phase-1 search/trending
    // results leave these null rather than fan out N extra requests per list (docs/08-GAME-
    // DATA.md's rate limit is the reason — /games/:id below can fetch it for one game).
    playtimeHoursHastily: null,
    playtimeHoursNormally: null,
    playtimeHoursCompletely: null,
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

  private async query(endpoint: string, body: string): Promise<IgdbGame[]> {
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
      throw new Error(`IGDB request failed: ${response.status}`);
    }
    return (await response.json()) as IgdbGame[];
  }

  async search(query: string): Promise<GameDto[]> {
    const escaped = query.replace(/"/g, '\\"');
    const games = await this.query(
      "games",
      `search "${escaped}"; fields ${GAME_FIELDS}; where category = 0 & version_parent = null; limit 20;`,
    );
    return games.map(toDto);
  }

  async detail(id: number): Promise<GameDto | null> {
    const games = await this.query(
      "games",
      `fields ${GAME_FIELDS}; where id = ${id};`,
    );
    return games[0] ? toDto(games[0]) : null;
  }

  async trending(): Promise<GameDto[]> {
    const games = await this.query(
      "games",
      `fields ${GAME_FIELDS}; where category = 0 & version_parent = null & hypes > 0; sort hypes desc; limit 20;`,
    );
    return games.map(toDto);
  }

  async shortAndSweet(): Promise<GameDto[]> {
    // Time-to-beat lives on a separate IGDB endpoint keyed by game id, which would mean an
    // extra request per candidate under a 4 req/sec ceiling. Phase-1 approximates "short"
    // with a rating floor instead; wiring the real game_time_to_beat join is a follow-up
    // once usage patterns show it's worth the extra IGDB calls.
    const games = await this.query(
      "games",
      `fields ${GAME_FIELDS}; where category = 0 & version_parent = null & aggregated_rating > 75; sort aggregated_rating desc; limit 20;`,
    );
    return games.map(toDto);
  }
}
