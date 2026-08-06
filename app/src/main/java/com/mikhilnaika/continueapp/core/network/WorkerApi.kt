package com.mikhilnaika.continueapp.core.network

import com.mikhilnaika.continueapp.core.network.dto.ResolveRequest
import com.mikhilnaika.continueapp.core.network.dto.ResolveResponse
import com.mikhilnaika.continueapp.core.network.dto.SearchResponse
import com.mikhilnaika.continueapp.core.network.dto.SteamOwnedResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    @POST("resolve")
    suspend fun resolve(@Body request: ResolveRequest): ResolveResponse

    @GET("steam/owned")
    suspend fun steamOwned(@Query("vanity") vanity: String): SteamOwnedResponse

    @GET("health")
    suspend fun health(): retrofit2.Response<Unit>
}
