package dev.zr0x8.nt118.gaymiennam.data

import android.content.Context
import androidx.core.content.edit

class CityStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadCities(): MutableList<String> {
        val raw = prefs.getStringSet(KEY_CITIES, null)
        if (raw.isNullOrEmpty()) return mutableListOf(DEFAULT_CITY)
        return raw.toMutableList()
    }

    fun saveCities(cities: List<String>) {
        prefs.edit { putStringSet(KEY_CITIES, cities.toSet()) }
    }

    fun addCity(query: String): Boolean {
        val cities = loadCities()
        val q = query.trim()
        if (q.isBlank()) return false
        if (cities.any { it.equals(q, ignoreCase = true) }) return false
        cities.add(q)
        saveCities(cities)
        return true
    }

    fun removeCity(query: String) {
        val cities = loadCities()
        cities.removeAll { it.equals(query, ignoreCase = true) }
        saveCities(cities)
    }

    companion object {
        private const val PREFS = "weather_prefs"
        private const val KEY_CITIES = "cities"
        const val DEFAULT_CITY = "Ho Chi Minh"
    }
}
