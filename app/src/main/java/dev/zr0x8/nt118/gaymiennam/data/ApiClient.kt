package dev.zr0x8.nt118.gaymiennam.data

import dev.zr0x8.nt118.gaymiennam.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    const val BASE_URL = "https://api.weatherapi.com/v1/"

    val service: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }

    val apiKey: String get() = BuildConfig.WEATHER_API_KEY
}
