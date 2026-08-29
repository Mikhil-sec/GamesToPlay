package com.mikhilnaika.continueapp.core.network

import com.mikhilnaika.continueapp.core.network.dto.GameDto
import com.mikhilnaika.continueapp.core.network.dto.ResolveRequest
import com.mikhilnaika.continueapp.core.network.dto.ResolveResponse
import com.mikhilnaika.continueapp.core.network.dto.SearchResponse
import com.mikhilnaika.continueapp.core.network.dto.SteamOwnedResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit surface for the Cloudflare Worker (docs/05-TECH-ARCHITECTURE.md §Endpoints).
 * The app never calls IGDB directly — IGDB's 4 req/sec limit and the need to keep Twitch
 * credentials out of a public APK both force every call through here.
 */
interface WorkerApi {
    @GET("games/search")
    suspend fun searchGames(@Query("q") query: String): SearchResponse

    // Every rail takes a page — what "SHOW MORE" spends. The Worker clamps it and keys its
    // cache on it, so page 0 stays exactly as cheap as it always was. No Kotlin default value
    // on any parameter here: a default on a Retrofit interface method makes the compiler emit a
    // synthetic bridge that the dynamic proxy has no business meeting. Defaults belong on
    // [GameDataSource], which is an ordinary interface.
    @GET("games/trending")
    suspend fun trending(@Query("page") page: Int): SearchResponse

    @GET("games/short")
    suspend fun shortAndSweet(@Query("page") page: Int): SearchResponse

    @GET("games/new")
    suspend fun newReleases(@Query("page") page: Int): SearchResponse

    @GET("games/gems")
    suspend fun hiddenGems(@Query("page") page: Int): SearchResponse

    /**
     * Refresh many already-known games in one round trip.
     *
     * Exists so that re-reading a whole pile costs one request rather than one per game — see
     * [GameDataSource.byIds]. The Worker deliberately doesn't cache this in KV (the id set is
     * user-shaped, so a cache key would burn the 1,000 writes/day budget), which is also why
     * the app only calls it when it genuinely holds stale rows.
     */
    @GET("games/batch")
    suspend fun gamesBatch(@Query("ids") ids: String): SearchResponse

    /**
     * Genre rail, addressed by IGDB's own genre *name* — the app already holds those strings on
     * its cached games, so it never has to hardcode IGDB's numeric ids. The Worker resolves the
     * name against a cached genre table and only ever caches by the resolved id.
     */
    @GET("games/genre")
    suspend fun byGenre(@Query("name") name: String, @Query("page") page: Int): SearchResponse

    @GET("games/{id}")
    suspend fun gameDetail(@Path("id") id: Long): GameDto

    @POST("resolve")
    suspend fun resolve(@Body request: ResolveRequest): ResolveResponse

    @GET("steam/owned")
    suspend fun steamOwned(@Query("vanity") vanity: String): SteamOwnedResponse

    @GET("health")
    suspend fun health(): retrofit2.Response<Unit>
}
