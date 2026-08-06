package com.siren.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherData(
    val windSpeedKnots: Double,
    val windDirectionDeg: Double,
    val waveHeightMeters: Double?,
    val temperatureC: Double,
    val fetchedAt: Long
)

object Weather {
    suspend fun fetch(lat: Double, lon: Double): WeatherData? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(
                "https://marine-api.open-meteo.com/v1/marine" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=wind_wave_height,wind_speed_10m,wind_direction_10m" +
                    "&wind_speed_unit=kn"
            )
            val con = url.openConnection() as HttpURLConnection
            con.connectTimeout = 8000
            con.readTimeout = 8000
            con.setRequestProperty("User-Agent", "SIREN/0.9.0")
            val body = con.inputStream.bufferedReader().use { it.readText() }
            con.disconnect()
            val j = JSONObject(body)
            val cur = j.getJSONObject("current")
            WeatherData(
                windSpeedKnots = cur.getDouble("wind_speed_10m"),
                windDirectionDeg = cur.getDouble("wind_direction_10m"),
                waveHeightMeters = runCatching { cur.getDouble("wind_wave_height") }.getOrNull(),
                temperatureC = 22.0,
                fetchedAt = System.currentTimeMillis()
            )
        }.getOrNull()
    }
}
