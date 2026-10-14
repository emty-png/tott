package com.tott.app.network

import retrofit2.http.GET

// Stub for future use. V1 is fully offline (ObjectBox). Do not add another client.
interface TottApi {
    @GET("health")
    suspend fun health(): Map<String, String>
}
