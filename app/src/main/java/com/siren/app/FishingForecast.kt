package com.siren.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.osmdroid.util.GeoPoint
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

object FishingCalc {
    fun pressureTrend(): String {
        val current = SirenNav.pressureMsl.value
        if (current.isNaN()) return "VERI YOK"
        
        val history = SirenNav.pressureHistory.value
        if (history.size < 2) return "VERI YETERSIZ"
        
        val oldest = history.first()
        val now = System.currentTimeMillis()
        val hoursElapsed = (now - oldest.first) / 3600000.0
        
        if (hoursElapsed < 0.5) return "VERI YETERSIZ"
        
        val change = current - oldest.second
        val changePerHour = change / hoursElapsed
        
        return when {
            changePerHour < -1.0 -> "HIZLA DUSUYOR"
            changePerHour < -0.3 -> "DUSUYOR"
            changePerHour > 1.0 -> "HIZLA YUKSELIYOR"
            changePerHour > 0.3 -> "YUKSELIYOR"
            else -> "SABIT"
        }
    }

    fun pressureScore(): Int {
        return when (pressureTrend()) {
            "HIZLA DUSUYOR" -> 2
            "DUSUYOR" -> 1
            "SABIT" -> 1
            "YUKSELIYOR" -> -1
            "HIZLA YUKSELIYOR" -> -2
            else -> 0
        }
    }

    fun multiFactorScore(pos: GeoPoint?, weather: WeatherData?): Int {
        if (pos == null) return 0
        val lat = pos.latitude
        val lon = pos.longitude
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // 1. Solunar (ay fazi + gun saati) - %30
        val lunarScore = scoreHour(lat, lon, hour)
        val lunarNorm = (lunarScore / 5.0) * 30

        // 2. Basinc trendi - %15
        val pressureS = pressureScore()
        val pressureNorm = ((pressureS + 2) / 4.0) * 15

        // 3. Ruzgar - %15 (hafif iyi, siddetli kotu)
        val windS = when {
            weather == null -> 7.5
            weather.windSpeedKnots < 5 -> 15.0
            weather.windSpeedKnots < 10 -> 12.0
            weather.windSpeedKnots < 15 -> 8.0
            weather.windSpeedKnots < 20 -> 4.0
            else -> 0.0
        }

        // 4. Dalga - %10
        val waveS = when {
            weather == null -> 5.0
            weather.waveHeightMeters == null -> 5.0
            weather.waveHeightMeters!! < 0.5 -> 10.0
            weather.waveHeightMeters!! < 1.0 -> 7.0
            weather.waveHeightMeters!! < 1.5 -> 4.0
            else -> 0.0
        }

        // 5. Altin saat bonusu - %20
        val timeBonus = if (hour in 5..8 || hour in 18..21) 20.0 else 5.0

        // 6. Sicaklik (mevsime uygunluk) - %10
        val tempS = weather?.temperatureC?.let {
            if (it > 15 && it < 28) 10.0 else 3.0
        } ?: 5.0

        return (lunarNorm + pressureNorm + windS + waveS + timeBonus + tempS).toInt().coerceIn(0, 100)
    }

    fun scoreExplanation(score: Int, weather: WeatherData?): String {
        val parts = mutableListOf<String>()
        val trend = pressureTrend()
        when (trend) {
            "HIZLA DUSUYOR" -> parts.add("dusen basinc")
            "DUSUYOR" -> parts.add("azalan basinc")
            "SABIT" -> parts.add("stabil basinc")
        }

        weather?.let { w ->
            if (w.windSpeedKnots < 10) parts.add("hafif ruzgar")
            if (w.waveHeightMeters != null && w.waveHeightMeters!! < 0.5) parts.add("sakin deniz")
        }

        val age = moonAge()
        val phase = when {
            age < 1.5 || age > 28 -> "yeniay"
            age in 6.0..8.5 -> "ilk dordun"
            age in 13.0..16.0 -> "dolunay"
            age in 21.0..23.5 -> "son dordun"
            else -> "kresan/azalan"
        }
        parts.add("$phase donemi")

        return if (parts.isEmpty()) "Analiz yapiliyor" else parts.take(3).joinToString(" + ")
    }
    const val SYNODIC = 29.53058867
    private const val NEW_MOON_EPOCH = 947182440000.0

    fun moonAge(now: Long = System.currentTimeMillis()): Double {
        val days = (now - NEW_MOON_EPOCH) / 86400000.0
        var a = days % SYNODIC
        if (a < 0) a += SYNODIC
        return a
    }

    fun illumination(age: Double = moonAge()): Double =
        (1.0 - cos(2.0 * Math.PI * age / SYNODIC)) / 2.0

    fun phaseName(age: Double = moonAge()): String = when {
        age < 1.84 -> "Yeni Ay"
        age < 5.54 -> "Hilal (buyuyen)"
        age < 9.23 -> "Ilkdordun"
        age < 12.92 -> "Buyuyen Ay"
        age < 16.61 -> "Dolunay"
        age < 20.30 -> "Kuculen Ay"
        age < 23.99 -> "Sondordun"
        age < 27.68 -> "Hilal (kuculen)"
        else -> "Yeni Ay"
    }

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun norm(v: Double, m: Double): Double { var x = v % m; if (x < 0) x += m; return x }

    private fun sunUtc(lat: Double, lon: Double, rise: Boolean): Double {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val doy = cal.get(Calendar.DAY_OF_YEAR)
        val lnghour = lon / 15.0
        val t = doy + ((if (rise) 6.0 else 18.0) - lnghour) / 24.0
        val M = 0.9856 * t - 3.289
        var L = M + 1.916 * sin(rad(M)) + 0.020 * sin(rad(2 * M)) + 282.634
        L = norm(L, 360.0)
        var RA = deg(atan(0.91764 * tan(rad(L))))
        RA = norm(RA, 360.0)
        val lq = (L / 90.0).toInt() * 90.0
        val raq = (RA / 90.0).toInt() * 90.0
        RA = RA + (lq - raq)
        RA = RA / 15.0
        val sinDec = 0.39782 * sin(rad(L))
        val cosDec = cos(asin(sinDec))
        val cosH = (cos(rad(90.833)) - sinDec * sin(rad(lat))) / (cosDec * cos(rad(lat)))
        if (cosH > 1 || cosH < -1) return Double.NaN
        val H = (if (rise) 360 - deg(acos(cosH)) else deg(acos(cosH))) / 15.0
        val T = H + RA - 0.06571 * t - 6.622
        return norm(T - lnghour, 24.0)
    }

    fun scoreHour(lat: Double, lon: Double, hourLocal: Int): Int {
        val offH = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0
        val hUt = norm(hourLocal - offH, 24.0)
        var s = 1
        val sr = sunUtc(lat, lon, true)
        val ss = sunUtc(lat, lon, false)
        if (!sr.isNaN() && abs(hUt - sr) < 1.5) s += 2
        if (!ss.isNaN() && abs(hUt - ss) < 1.5) s += 2
        val age = moonAge()
        val lunar = norm(age * 0.842, 24.0)
        val major1 = lunar
        val major2 = norm(lunar + 12.4, 24.0)
        val minor1 = norm(lunar + 6.2, 24.0)
        val minor2 = norm(lunar + 18.6, 24.0)
        if (abs(hUt - major1) < 1.0 || abs(hUt - major2) < 1.0) s += 2
        if (abs(hUt - minor1) < 1.0 || abs(hUt - minor2) < 1.0) s += 1
        val night = !sr.isNaN() && !ss.isNaN() && (hUt > ss || hUt < sr)
        if (night && illumination(age) > 0.5) s += 1
    val baseScore = s.coerceAtMost(5)
        val pressureModifier = pressureScore()
        return (baseScore + pressureModifier).coerceIn(0, 5)
    }

    fun currentScore(lat: Double, lon: Double): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return scoreHour(lat, lon, hour)
    }

    fun bestSlots(lat: Double, lon: Double): List<Pair<Int, Int>> =
        (0..23).map { it to scoreHour(lat, lon, it) }
            .sortedByDescending { it.second }
            .take(3)
            .sortedBy { it.first }
}

@Composable
fun FishingBadge() {
    val p by SirenNav.pos
    val w by SirenNav.weatherData
    if (p == null) return

    val score = FishingCalc.multiFactorScore(p, w)
    if (score < 70) return

    val (label, color) = when {
        score >= 90 -> "MUKEMMEL" to SirenGreen
        score >= 80 -> "COK IYI" to SirenGreen
        score >= 70 -> "IYI" to SirenTrackYellow
        else -> return
    }

    val explanation = FishingCalc.scoreExplanation(score, w)

    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenPanel.copy(alpha = 0.9f))
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text("🎣 $label · $score · $explanation", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FishingBadge() {
    val p by SirenNav.pos
    val w by SirenNav.weatherData
    if (p == null) return

    val score = FishingCalc.multiFactorScore(p, w)
    if (score < 70) return

    val (label, color) = when {
        score >= 90 -> "MUKEMMEL" to SirenGreen
        score >= 80 -> "COK IYI" to SirenGreen
        score >= 70 -> "IYI" to SirenTrackYellow
        else -> return
    }

    val explanation = FishingCalc.scoreExplanation(score, w)

    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenPanel.copy(alpha = 0.9f))
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text("🎣 $label · $score · $explanation", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
