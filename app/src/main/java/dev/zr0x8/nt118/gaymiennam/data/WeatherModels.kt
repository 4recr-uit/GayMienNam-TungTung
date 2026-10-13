package dev.zr0x8.nt118.gaymiennam.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("location") val location: LocationDto,
    @SerializedName("current") val current: CurrentDto
)

data class LocationDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("region") val region: String = "",
    @SerializedName("country") val country: String = "",
    @SerializedName("lat") val lat: Double = 0.0,
    @SerializedName("lon") val lon: Double = 0.0,
    @SerializedName("localtime") val localtime: String = ""
)

data class CurrentDto(
    @SerializedName("temp_c") val tempC: Double = 0.0,
    @SerializedName("feelslike_c") val feelslikeC: Double = 0.0,
    @SerializedName("condition") val condition: ConditionDto = ConditionDto(),
    @SerializedName("humidity") val humidity: Int = 0,
    @SerializedName("wind_kph") val windKph: Double = 0.0
)

data class ConditionDto(
    @SerializedName("text") val text: String = "",
    @SerializedName("icon") val icon: String = "",
    @SerializedName("code") val code: Int = 0
) {
    fun iconUrl(): String = if (icon.startsWith("http")) icon else "https:$icon"
}

data class SearchResultDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("region") val region: String = "",
    @SerializedName("country") val country: String = "",
    @SerializedName("lat") val lat: Double = 0.0,
    @SerializedName("lon") val lon: Double = 0.0,
    @SerializedName("url") val url: String = ""
) {
    fun displayName(): String =
        listOf(name, region, country).filter { it.isNotBlank() }.joinToString(", ")
}
