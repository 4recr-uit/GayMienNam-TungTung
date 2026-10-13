package dev.zr0x8.nt118.gaymiennam.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class CityWeatherUi(
    val query: String,
    val displayName: String,
    val tempC: Double,
    val feelsLikeC: Double,
    val condition: String,
    val iconUrl: String,
    val humidity: Int,
    val windKph: Double,
    val localtime: String,
    val isCurrentLocation: Boolean = false
)

class WeatherRepository(
    private val service: WeatherApiService = ApiClient.service,
    private val apiKey: String = ApiClient.apiKey
) {
    private fun requireKey() {
        if (apiKey.isBlank()) {
            throw IllegalStateException(
                "missing api key in .env"
            )
        }
    }

    suspend fun getWeather(query: String): WeatherResponse {
        requireKey()
        return service.getCurrent(apiKey, query.trim())
    }

    suspend fun searchCities(query: String): List<SearchResultDto> {
        requireKey()
        if (query.isBlank()) return emptyList()
        return service.search(apiKey, query.trim())
    }

    suspend fun getWeathers(
        queries: List<String>,
        currentLocationQuery: String? = null
    ): Pair<List<CityWeatherUi>, String?> = coroutineScope {
        requireKey()
        val jobs = queries.map { q ->
            async {
                runCatching { service.getCurrent(apiKey, q) to q }
            }
        }
        val items = mutableListOf<CityWeatherUi>()
        var firstError: String? = null
        jobs.awaitAll().forEach { result ->
            result.onSuccess { (res, q) ->
                items += res.toUi(query = q, isCurrentLocation = (q == currentLocationQuery))
            }.onFailure { e ->
                if (firstError == null) firstError = e.friendlyMessage()
            }
        }
        items.sortByDescending { it.isCurrentLocation }
        items to firstError
    }

    private fun WeatherResponse.toUi(query: String, isCurrentLocation: Boolean) = CityWeatherUi(
        query = query,
        displayName = listOf(location.name, location.country)
            .filter { it.isNotBlank() }.joinToString(", "),
        tempC = current.tempC,
        feelsLikeC = current.feelslikeC,
        condition = current.condition.text,
        iconUrl = current.condition.iconUrl(),
        humidity = current.humidity,
        windKph = current.windKph,
        localtime = location.localtime,
        isCurrentLocation = isCurrentLocation
    )
}

fun Throwable.friendlyMessage(): String {
    val msg = message.orEmpty()
    return when {
        msg.contains("Unable to resolve host", ignoreCase = true) -> "seem like your internet is not available, please check your connection"
        msg.contains("HTTP 400", ignoreCase = true) -> "cannot find requested city, try again"
        msg.contains("HTTP 401", ignoreCase = true) ||
            msg.contains("HTTP 403", ignoreCase = true) -> "seem like your api key is invalid"
        else -> "failed to load: ${msg.ifBlank { this::class.java.simpleName }}"
    }
}
