package dev.zr0x8.nt118.gaymiennam.data

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * docs: https://www.weatherapi.com/docs/
 * base url: https://api.weatherapi.com/v1/
 */
interface WeatherApiService {

    @GET("current.json")
    suspend fun getCurrent(
        @Query("key") key: String,
        @Query("q") query: String,
        @Query("aqi") aqi: String = "no"
    ): WeatherResponse

    @GET("search.json")
    suspend fun search(
        @Query("key") key: String,
        @Query("q") query: String
    ): List<SearchResultDto>
}
