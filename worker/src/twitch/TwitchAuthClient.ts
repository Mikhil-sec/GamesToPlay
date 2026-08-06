import type { Env } from "../types.ts";
import { CacheTtl } from "../kv.ts";

export interface TwitchAuthClient {
  getAccessToken(): Promise<string>;
}

interface TwitchTokenResponse {
  access_token: string;
  expires_in: number;
  token_type: string;
}

const TOKEN_CACHE_KEY = "twitch:access_token";

/**
 * OAuth client-credentials flow — docs/08-GAME-DATA.md §Auth. Tokens last ~60 days; caching
 * in KV means we hit Twitch's token endpoint roughly once every couple of months, not once
 * per IGDB request (that would burn through the 4 req/sec ceiling instantly).
 */
export class LiveTwitchAuthClient implements TwitchAuthClient {
  constructor(private readonly env: Env) {}

  async getAccessToken(): Promise<string> {
    const cached = await this.env.CACHE.get(TOKEN_CACHE_KEY);
    if (cached) return cached;

    const clientId = this.env.TWITCH_CLIENT_ID;
    const clientSecret = this.env.TWITCH_CLIENT_SECRET;
    if (!clientId || !clientSecret) {
      throw new Error("TWITCH_CLIENT_ID/TWITCH_CLIENT_SECRET not configured");
    }

    const params = new URLSearchParams({
      client_id: clientId,
      client_secret: clientSecret,
      grant_type: "client_credentials",
    });
    const response = await fetch(`https://id.twitch.tv/oauth2/token?${params}`, { method: "POST" });
    if (!response.ok) {
      throw new Error(`Twitch token request failed: ${response.status}`);
    }
    const data = (await response.json()) as TwitchTokenResponse;

    await this.env.CACHE.put(TOKEN_CACHE_KEY, data.access_token, {
      expirationTtl: Math.min(data.expires_in - 60 * 60 * 24, CacheTtl.TWITCH_TOKEN),
    });
    return data.access_token;
  }
}
