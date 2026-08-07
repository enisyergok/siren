package com.siren.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL

data class HourWeather(
    val timeLabel: String,
    val tempC: Double,
    val windKts: Double,
    val windDir: Double
)

data class WeatherData(
    val windSpeedKnots: Double,
    val windDirectionDeg: Double,
    val waveHeightMeters: Double?,
    val temperatureC: Double?,
    val hourly: List<HourWeather>,
    val fetchedAt: Long
)

fun dirName(deg: Double): String {
    val dirs = arrayOf("K", "KD", "D", "GD", "G", "GB", "B", "KB")
    val idx = (Math.round(deg / 45.0).toInt() % 8 + 8) % 8
    return dirs[idx]
}

object Weather {

    private fun httpGet(url: String): String? = runCatching {
        val con = URL(url).openConnection() as HttpURLConnection
        con.connectTimeout = 8000
        con.readTimeout = 8000
        con.setRequestProperty("User-Agent", "SIREN/0.9.1")
        val s = con.inputStream.bufferedReader().use { it.readText() }
        con.disconnect()
        s
    }.getOrNull()

    suspend fun fetch(lat: Double, lon: Double): WeatherData? = withContext(Dispatchers.IO) {
        val windBody = httpGet(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,wind_speed_10m,wind_direction_10mcurrent=temperature_2m,wind_speed_10m,wind_direction_10m,pressure_msl" +
                "&hourly=temperature_2m,wind_speed_10m,wind_direction_10m" +
                "&wind_speed_unit=kn"
        ) ?: return@withContext null

        runCatching {
            val j = JSONObject(windBody)
            val cur = j.getJSONObject("current")

            val hours = mutableListOf<HourWeather>()
            val hourly = j.optJSONObject("hourly")
            if (hourly != null) {
                val times = hourly.getJSONArray("time")
                val temps = hourly.getJSONArray("temperature_2m")
                val winds = hourly.getJSONArray("wind_speed_10m")
                val dirs = hourly.getJSONArray("wind_direction_10m")
                val n = minOf(times.length(), 8)
                for (i in 0 until n) {
                    val t = times.getString(i)
                    val label = if (t.length >= 16) t.substring(11, 16) else t
                    hours.add(HourWeather(label, temps.getDouble(i), winds.getDouble(i), dirs.getDouble(i)))
                }
            }

            val wave = runCatching {
                val wb = httpGet(
                    "https://marine-api.open-meteo.com/v1/marine?latitude=$lat&longitude=$lon&current=wave_height"
                ) ?: return@runCatching null
                val v = JSONObject(wb).getJSONObject("current").optDouble("wave_height", Double.NaN)
                if (v.isNaN()) null else v
            }.getOrNull()

            val temp = cur.optDouble("temperature_2m", Double.NaN)

            WeatherData(
                windSpeedKnots = cur.getDouble("wind_speed_10m"),
                windDirectionDeg = cur.getDouble("wind_direction_10m"),
            pressureMsl = cur.optDouble("pressure_msl", Double.NaN),
                waveHeightMeters = wave,
                temperatureC = if (temp.isNaN()) null else temp,
                hourly = hours,
                fetchedAt = System.currentTimeMillis()
            )
        }.getOrNull()
    }
}

@Composable
fun WeatherScreen(pos: MutableState<GeoPoint?>) {
    var data by remember { mutableStateOf<WeatherData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val refresh: () -> Unit = {
        val p = pos.value
        if (p == null) {
            error = "GPS bekleniyor..."
        } else {
            if (!loading) {
                loading = true
                error = null
                scope.launch {
                    val d = Weather.fetch(p.latitude, p.longitude)
                    loading = false
                    data = d
                    if (d == null) error = "Hava verisi alinamadi (internet kontrol)"
                }
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        Modifier.fillMaxSize().background(SirenBackground).padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("HAVA DURUMU", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(SirenPrimary)
                    .clickable { refresh() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(if (loading) "..." else "YENILE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))

        error?.let {
            Text(it, color = SirenRed, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }

        if (data == null && !loading && error == null) {
            Text("Yukleniyor...", color = SirenTextSecondary)
        }

        data?.let { w ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigStat("RUZGAR", "%.0f".format(w.windSpeedKnots), "kts")
                BigStat("YON", "%.0f".format(w.windDirectionDeg), dirName(w.windDirectionDeg))
                BigStat("DALGA", w.waveHeightMeters?.let { "%.1f".format(it) } ?: "--", "m")
                BigStat("SICAKLIK", w.temperatureC?.let { "%.0f".format(it) } ?: "--", "C")
            }
            Spacer(Modifier.height(20.dp))
            SunCard(pos)
            FishingForecastCard(pos)
            Text("SONRAKI 8 SAAT", color = SirenTextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            w.hourly.forEach { h ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(10.dp)).background(SirenCard).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(h.timeLabel, color = SirenTextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(70.dp))
                    Text("%.0f C".format(h.tempC), color = SirenTextSecondary, modifier = Modifier.width(70.dp))
                    Text("%.0f kts".format(h.windKts), color = SirenTextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                    Text("%.0f  %s".format(h.windDir, dirName(h.windDir)), color = SirenTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun BigStat(label: String, value: String, unit: String) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).background(SirenCard).padding(16.dp)) {
        Text(label, fontSize = 10.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.width(4.dp))
            Text(unit, fontSize = 11.sp, color = SirenTextSecondary, modifier = Modifier.padding(bottom = 5.dp))
        }
    }
}
