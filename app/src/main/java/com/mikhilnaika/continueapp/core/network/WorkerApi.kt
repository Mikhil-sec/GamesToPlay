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

    @GET("games/trending")
    suspend fun trending(): SearchResponse

    @GET("games/short")
    suspend fun shortAndSweet(): SearchResponse

    @GET("games/new")
    suspend fun newReleases(): SearchResponse

    @GET("games/gems")
    suspend fun hiddenGems(): SearchResponse

    /**
     * Genre rail, addressed by IGDB's own genre *name* — the app already holds those strings on
     * its cached games, so it never has to hardcode IGDB's numeric ids. The Worker resolves the
     * name against a cached genre table and only ever caches by the resolved id.
     */
    @GET("games/genre")
    suspend fun byGenre(@Query("name") name: String): SearchResponse

    @GET("games/{id}")
    suspend fun gameDetail(@Path("id") id: Long): GameDto

    @POST("resolve")
    suspend fun resolve(@Body request: ResolveRequest): ResolveResponse

    @GET("steam/owned")
    suspend fun steamOwned(@Query("vanity") vanity: String): SteamOwnedResponse

    @GET("health")
    suspend fun health(): retrofit2.Response<Unit>
}
