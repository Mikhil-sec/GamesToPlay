import type { Env, GameDto, GameProvider, GenreRef } from "../types.ts";
import type { TwitchAuthClient } from "../twitch/TwitchAuthClient.ts";

const IGDB_BASE = "https://api.igdb.com/v4";

const GAME_FIELDS =
  "name, slug, cover.image_id, artworks.image_id, screenshots.image_id, first_release_date, " +
  "aggregated_rating, rating, total_rating, total_rating_count, genres.name, themes.name, " +
  "game_modes.name, platforms.name";

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

interface IgdbImageRef {
  image_id: string;
}

type IgdbCover = IgdbImageRef;

interface IgdbNamed {
  name: string;
}

interface IgdbGame {
  id: number;
  slug: string;
  name: string;
  cover?: IgdbCover;
  artworks?: IgdbImageRef[];
  screenshots?: IgdbImageRef[];
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

/**
 * t_cover_big for grids per docs/08-GAME-DATA.md §Covers.
 *
 * The size token is the *baseline*; the app re-writes it per surface (`IgdbImage.kt`) because a
 * 264x352 cover blown up to a full-screen hero is visibly blocky. Keep the token here as one
 * path segment so that rewrite stays a simple substitution.
 */
function coverUrl(cover?: IgdbCover): string | null {
  return cover ? `https://images.igdb.com/igdb/image/upload/t_cover_big/${cover.image_id}.jpg` : null;
}

/**
 * Landscape key art for full-bleed backgrounds (the Credits Roll).
 *
 * This was hardcoded `null`, so the Credits Roll fell back to upscaling the 264px cover across
 * a whole phone screen — the source of the "pixelated" key art.
 *
 * **Screenshots first, artworks second**, which is the opposite of what it looks like it should
 * be. Artwork is prettier (promotional key art, no HUD) but its aspect ratio is unconstrained:
 * Elden Ring's first artwork is an ultrawide banner that comes back from `t_1080p` as
 * **1920x295**, and cropping that to fill a portrait phone upscales it ~8x — worse than the
 * cover we were trying to replace. Screenshots are captures, so they're dependably 16:9 at
 * 1920x1080, which crops to portrait without inventing pixels.
 */
function backgroundUrl(game: IgdbGame): string | null {
  const image = game.screenshots?.[0] ?? game.artworks?.[0];
  return image ? `https://images.igdb.com/igdb/image/upload/t_1080p/${image.image_id}.jpg` : null;
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
    backgroundUrl: backgroundUrl(game),
    released: game.first_release_date
      ? new Date(game.first_release_date * 1000).toISOString().slice(0, 10)
      : null,
    metacritic: null,
    rating: game.total_rating ?? game.rating ?? game.aggregated_rating ?? null,
    playtimeHoursHastily: secondsToHours(ttb?.hastily),
    playtimeHoursNormally: secondsToHours(ttb?.normally),
    playtimeHoursCompletely: secondsToHours(ttb?.completely),
    genres: (game.genres ?? []).map((g) => g.name),
    // Themes *and* game modes. `game_modes.name` has been in GAME_FIELDS from the start and
    // was then silently dropped on the floor here, so "Multiplayer"/"Co-operative" reached
    // nothing downstream — see the `tags` doc comment in ../types.ts.
    tags: [
      ...(game.themes ?? []).map((t) => t.name),
      ...(game.game_modes ?? []).map((m) => m.name),
    ],
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
   * Many games in one round trip, for the app's cache refresh.
   *
   * The whole reason this exists rather than N calls to [detail]: a pile of 40 games would be
   * 40 requests against a 4 req/sec ceiling *and* 40 KV writes against a 1,000/day budget.
   * This is two IGDB requests and — because the route deliberately doesn't cache it — zero KV
   * writes. Ids are numbers by the time they reach here (the route parses them), so they can't
   * carry apicalypse syntax into the query.
   */
  async byIds(ids: number[]): Promise<GameDto[]> {
    if (ids.length === 0) return [];
    const games = await this.query<IgdbGame>(
      "games",
      `fields ${GAME_FIELDS}; where id = (${ids.join(",")}); limit ${ids.length};`,
    );
    return this.withPlaytimes(games);
  }

  /**
   * "Trending" for a *backlog* app means games people are actually playing and rating right
   * now — not IGDB's `hypes`, which counts pre-release follows and therefore surfaces
   * unreleased games you can't add to a pile. Scoped to the last ~3 years and ordered by
   * how many people have rated it.
   */
  async trending(offset = 0): Promise<GameDto[]> {
    const threeYearsAgo = Math.floor(Date.now() / 1000) - 60 * 60 * 24 * 365 * 3;
    const now = Math.floor(Date.now() / 1000);
    const games = await this.query<IgdbGame>(
      "games",
      `fields ${GAME_FIELDS}; where ${MAIN_GAMES_ONLY} & cover != null & ` +
        `first_release_date > ${threeYearsAgo} & first_release_date < ${now} & ` +
        `total_rating_count > 5; sort total_rating_count desc; limit 20; offset ${offset};`,
    );
    return this.withPlaytimes(games);
  }

  /**
   * Real "short and sweet": games that genuinely take under 8 hours, sourced from
   * `/game_time_to_beats` rather than approximated by a rating floor (which is what this did
   * before and it wasn't remotely the same thing). Two requests, not N — pick the short games
   * first, then hydrate them.
   */
  async shortAndSweet(offset = 0): Promise<GameDto[]> {
    const EIGHT_HOURS_SECONDS = 8 * 3600;
    const times = await this.query<IgdbTimeToBeat>(
      "game_time_to_beats",
      `fields game_id, hastily, normally, completely; ` +
        `where normally > 0 & normally < ${EIGHT_HOURS_SECONDS} & count > 5; ` +
        `sort count desc; limit 60; offset ${offset * 3};`,
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

  /**
   * NEW RELEASES — the notable games of the last six months, **not** the most recent ones.
   *
   * Sorting by `first_release_date desc` was tried first and produced a rail of games nobody
   * has heard of: the newest thing in IGDB at any moment is whatever indie was catalogued
   * this morning. Ordering the same window by rating count instead surfaces the releases a
   * backlog actually accumulates, which is the point of the rail.
   *
   * `game_status = null` is the "fully released" case — it drops early-access and alpha
   * entries, which are exactly the things you can't sensibly add to a pile yet. (Note it is
   * `game_status`, not the deprecated `status`; see the GAME_TYPE_MAIN_GAME comment above for
   * why filtering on a dead IGDB field fails silently rather than loudly.)
   */
  async newReleases(offset = 0): Promise<GameDto[]> {
    const now = Math.floor(Date.now() / 1000);
    const sixMonthsAgo = now - 60 * 60 * 24 * 180;
    const games = await this.query<IgdbGame>(
      "games",
      `fields ${GAME_FIELDS}; where ${MAIN_GAMES_ONLY} & cover != null & ` +
        `first_release_date > ${sixMonthsAgo} & first_release_date < ${now} & ` +
        `total_rating_count > 3 & game_status = null; sort total_rating_count desc; limit 20; ` +
        `offset ${offset};`,
    );
    return this.withPlaytimes(games);
  }

  /**
   * HIDDEN GEMS — **critic** score, not the blended `total_rating`.
   *
   * Built on `total_rating > 80 & total_rating_count > 5` first, and the result was a rail of
   * meme entries: a handful of user ratings is enough to put a joke listing at 100/100, so the
   * first page came back as *Bubsy 3D* and *PokéOne*. `aggregated_rating` is press coverage,
   * which nothing brigades, and requiring several outlets filters the rest. Capping
   * `total_rating_count` is then what makes it *hidden* rather than merely good.
   */
  async hiddenGems(offset = 0): Promise<GameDto[]> {
    const games = await this.query<IgdbGame>(
      "games",
      `fields ${GAME_FIELDS}; where ${MAIN_GAMES_ONLY} & cover != null & ` +
        `aggregated_rating > 82 & aggregated_rating_count >= 5 & total_rating_count < 200; ` +
        `sort aggregated_rating desc; limit 20; offset ${offset};`,
    );
    return this.withPlaytimes(games);
  }

  async genres(): Promise<GenreRef[]> {
    return this.query<GenreRef>("genres", "fields id, name; limit 50; sort id asc;");
  }

  async byGenre(genreId: number, offset = 0): Promise<GameDto[]> {
    const games = await this.query<IgdbGame>(
      "games",
      `fields ${GAME_FIELDS}; where ${MAIN_GAMES_ONLY} & cover != null & ` +
        `genres = (${genreId}) & total_rating_count > 10; sort total_rating_count desc; ` +
        `limit 20; offset ${offset};`,
    );
    return this.withPlaytimes(games);
  }
}
