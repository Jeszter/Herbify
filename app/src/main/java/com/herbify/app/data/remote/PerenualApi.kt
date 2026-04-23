package com.herbify.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PerenualApi {
    @GET("api/v2/species-list")
    suspend fun getSpeciesList(
        @Query("key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("q") query: String? = null
    ): PlantListResponse

    @GET("api/v2/species/details/{id}")
    suspend fun getPlantDetails(
        @Path("id") id: Int,
        @Query("key") apiKey: String
    ): PlantDetailsDto
}