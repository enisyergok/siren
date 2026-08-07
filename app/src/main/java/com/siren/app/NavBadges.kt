package com.siren.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.osmdroid.util.GeoPoint
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

@Composable
fun FishingBadge() {
    val p by SirenNav.pos
    val lat = p?.latitude ?: 40.0
    val lon = p?.longitude ?: 27.0
    val score = FishingCalc.currentScore(lat, lon)
    if (score < 4) return
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenPanel.copy(alpha = 0.9f))
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text("🎣 BESLENME YUKSEK", color = SirenGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FishingBadge() {
    val p by SirenNav.pos
    val lat = p?.latitude ?: 40.0
    val lon = p?.longitude ?: 27.0
    val score = FishingCalc.currentScore(lat, lon)
    if (score < 4) return
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenPanel.copy(alpha = 0.9f))
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text("🎣 BESLENME YUKSEK", color = SirenGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FishingBadge() {
    val p by SirenNav.pos
    val lat = p?.latitude ?: 40.0
    val lon = p?.longitude ?: 27.0
    val score = FishingCalc.currentScore(lat, lon)
    if (score < 4) return
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenPanel.copy(alpha = 0.9f))
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text("🎣 BESLENME YUKSEK", color = SirenGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FishingBadge() {
    val p by SirenNav.pos
    val lat = p?.latitude ?: 40.0
    val lon = p?.longitude ?: 27.0
    val score = FishingCalc.currentScore(lat, lon)
    if (score < 4) return
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenPanel.copy(alpha = 0.9f))
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text("🎣 BESLENME YUKSEK", color = SirenGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

fun remainingNmFrom(a: ActiveRouteInfo, cur: GeoPoint): Double {
    if (a.leg >= a.points.size) return 0.0
    var total = haversineNm(cur.latitude, cur.longitude, a.points[a.leg].latitude, a.points[a.leg].longitude)
    for (i in a.leg until a.points.lastIndex) {
        total += haversineNm(a.points[i].latitude, a.points[i].longitude,
            a.points[i + 1].latitude, a.points[i + 1].longitude)
    }
    return total
}

@Composable
fun FishingBadge() {
    val p by SirenNav.pos
    val lat = p?.latitude ?: 40.0
    val lon = p?.longitude ?: 27.0
    val score = FishingCalc.currentScore(lat, lon)
    if (score < 4) return
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenPanel.copy(alpha = 0.9f))
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text("🎣 BESLENME YUKSEK", color = SirenGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FishingBadge() {
    val p by SirenNav.pos
    val lat = p?.latitude ?: 40.0
    val lon = p?.longitude ?: 27.0
    val score = FishingCalc.currentScore(lat, lon)
    if (score < 4) return
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenPanel.copy(alpha = 0.9f))
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text("🎣 BESLENME YUKSEK", color = SirenGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FishingBadge() {
    val p by SirenNav.pos
    val lat = p?.latitude ?: 40.0
    val lon = p?.longitude ?: 27.0
    val score = FishingCalc.currentScore(lat, lon)
    if (score < 4) return
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenPanel.copy(alpha = 0.9f))
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text("🎣 BESLENME YUKSEK", color = SirenGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun rad(d: Double) = Math.toRadians(d)
private fun deg(r: Double) = Math.toDegrees(r)
private fun norm(v: Double, m: Double): Double { var x = v % m; if (x < 0) x += m; return x }

private fun sunTimesLocal(lat: Double, lon: Double): Pair<String, String> {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val doy = cal.get(Calendar.DAY_OF_YEAR)
    val lnghour = lon / 15.0
    fun calc(rise: Boolean): Double {
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
        val Hdeg = if (rise) 360 - deg(acos(cosH)) else deg(acos(cosH))
        val H = Hdeg / 15.0
        val T = H + RA - 0.06571 * t - 6.622
        return norm(T - lnghour, 24.0)
    }
    val offH = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0
    fun fmt(ut: Double): String {
        if (ut.isNaN()) return "--:--"
        val local = norm(ut + offH, 24.0)
        val h = local.toInt()
        val m = ((local - h) * 60).toInt()
        return "%02d:%02d".format(h, m)
    }
    return fmt(calc(true)) to fmt(calc(false))
}
