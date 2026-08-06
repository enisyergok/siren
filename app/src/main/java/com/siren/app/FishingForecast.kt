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
        return s.coerceAtMost(5)
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
    val lat = p?.latitude ?: 40.0
    val lon = p?.longitude ?: 27.0
    val score = FishingCalc.currentScore(lat, lon)
    val label = when {
        score >= 4 -> "BESLENME YUKSEK"
        score == 3 -> "BESLENME ORTA"
        else -> "BESLENME DUSUK"
    }
    val color = when {
        score >= 4 -> SirenGreen
        score == 3 -> SirenTrackYellow
        else -> SirenTextSecondary
    }
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenPanel.copy(alpha = 0.9f))
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text("🎣 $label", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FishingForecastCard(pos: MutableState<GeoPoint?>) {
    val p = pos.value
    val lat = p?.latitude ?: 40.0
    val lon = p?.longitude ?: 27.0
    val age = FishingCalc.moonAge()
    val illum = FishingCalc.illumination(age)
    val slots = remember(p?.latitude?.toInt(), p?.longitude?.toInt()) { FishingCalc.bestSlots(lat, lon) }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎣 AV TAHMINI", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = SirenTextPrimary)
            Spacer(Modifier.weight(1f))
            Text("${FishingCalc.phaseName(age)} · %${(illum * 100).toInt()}",
                color = SirenTextSecondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text("EN IYI SAATLER", fontSize = 10.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            slots.forEach { slot ->
                val h = slot.first
                val s = slot.second
                Column(Modifier.clip(RoundedCornerShape(10.dp)).background(SirenPanel).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("%02d:00".format(h), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("★".repeat(s), color = SirenTrackYellow, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Solunar: safak/alacakaranlik + ay gecisi hesabı", color = SirenTextSecondary, fontSize = 9.sp)
    }
    Spacer(Modifier.height(12.dp))
}
