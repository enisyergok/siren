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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Icon
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

data class TideSnapshot(
    val currentWaveM: Double?,
    val currentSwellM: Double?,
    val currentCurrentKts: Double?,
    val currentDirDeg: Double?,
    val hourly: List<TideHour>
)

data class TideHour(val timeLabel: String, val waveM: Double, val dirDeg: Double, val currentKts: Double?)

private suspend fun fetchTide(lat: Double, lon: Double): TideSnapshot? = withContext(Dispatchers.IO) {
    runCatching {
        val url = URL(
            "https://marine-api.open-meteo.com/v1/marine?latitude=$lat&longitude=$lon" +
                "&current=wave_height,wind_wave_height,ocean_current_velocity,ocean_current_direction" +
                "&hourly=wave_height,wave_direction,ocean_current_velocity" +
                "&forecast_days=2"
        )
        val con = url.openConnection() as HttpURLConnection
        con.connectTimeout = 8000
        con.readTimeout = 8000
        con.setRequestProperty("User-Agent", "SIREN/0.11.1")
        val body = con.inputStream.bufferedReader().use { it.readText() }
        con.disconnect()
        val j = JSONObject(body)
        val cur = j.optJSONObject("current")
        val hourly = j.optJSONObject("hourly")

        val hours = mutableListOf<TideHour>()
        if (hourly != null) {
            val times = hourly.getJSONArray("time")
            val waves = hourly.getJSONArray("wave_height")
            val dirs = hourly.optJSONArray("wave_direction")
            val currents = hourly.optJSONArray("ocean_current_velocity")
            val n = minOf(times.length(), 24)
            for (i in 0 until n) {
                val t = times.getString(i)
                val label = if (t.length >= 16) t.substring(11, 16) else t
                hours.add(TideHour(
                    label,
                    waves.getDouble(i),
                    dirs?.optDouble(i, 0.0) ?: 0.0,
                    currents?.optDouble(i, Double.NaN).let { if (it == null || it.isNaN()) null else it }
                ))
            }
        }

        TideSnapshot(
            currentWaveM = cur?.optDouble("wave_height", Double.NaN).let { if (it == null || it.isNaN()) null else it },
            currentSwellM = cur?.optDouble("wind_wave_height", Double.NaN).let { if (it == null || it.isNaN()) null else it },
            currentCurrentKts = cur?.optDouble("ocean_current_velocity", Double.NaN).let {
                if (it == null || it.isNaN()) null else it * 1.94384
            },
            currentDirDeg = cur?.optDouble("ocean_current_direction", Double.NaN).let {
                if (it == null || it.isNaN()) null else it
            },
            hourly = hours
        )
    }.getOrNull()
}

@Composable
fun TideScreen(pos: MutableState<GeoPoint?>) {
    var data by remember { mutableStateOf<TideSnapshot?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val refresh: () -> Unit = {
        val p = pos.value
        if (p == null) {
            error = "GPS bekleniyor..."
        } else {
            loading = true
            error = null
            scope.launch {
                val d = fetchTide(p.latitude, p.longitude)
                loading = false
                data = d
                if (d == null) error = "Marine verisi alinamadi"
            }
        }
    }

    LaunchedEffect(pos.value) {
        if (data == null && pos.value != null) refresh()
    }

    Column(Modifier.fillMaxSize().background(SirenBackground).padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Water, null, tint = SirenPrimary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("GELGIT & AKINTI", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
                Text("Open-Meteo Marine API", color = SirenTextSecondary, fontSize = 11.sp)
            }
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(SirenPrimary)
                    .clickable { refresh() }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Icon(Icons.Filled.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(16.dp))

        error?.let { Text(it, color = SirenRed, fontSize = 13.sp); Spacer(Modifier.height(8.dp)) }

        if (data == null && !loading) Text("Yukleniyor...", color = SirenTextSecondary)
        if (loading) Text("Yukleniyor...", color = SirenPrimary)

        data?.let { d ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TideStat("DALGA", d.currentWaveM?.let { "%.1f".format(it) } ?: "--", "m")
                TideStat("SWELL", d.currentSwellM?.let { "%.1f".format(it) } ?: "--", "m")
                TideStat("AKINTI", d.currentCurrentKts?.let { "%.1f".format(it) } ?: "--", "kts")
                TideStat("YON", d.currentDirDeg?.let { "%.0f".format(it) } ?: "--", dirName(d.currentDirDeg ?: 0.0))
            }
            Spacer(Modifier.height(20.dp))
            Text("SONRAKI 24 SAAT", color = SirenTextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            d.hourly.forEach { h ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp))
                    .background(SirenCard).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(h.timeLabel, color = SirenTextPrimary, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(60.dp))
                    Text("%.1f m".format(h.waveM), color = SirenPrimary, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp))
                    Text("%.0f°".format(h.dirDeg), color = SirenTextSecondary,
                        modifier = Modifier.width(60.dp))
                    Text(h.currentKts?.let { "%.1f kts".format(it) } ?: "--",
                        color = SirenTrackYellow, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TideStat(label: String, value: String, unit: String) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(SirenCard).padding(14.dp)) {
        Text(label, fontSize = 9.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.width(3.dp))
            Text(unit, fontSize = 10.sp, color = SirenTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}
