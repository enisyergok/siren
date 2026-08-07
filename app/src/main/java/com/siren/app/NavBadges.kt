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
fun BoxScope.NavBadgeColumn() {
    Row(
        Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GpsQualityBadge()
        AnchorWatchBadge()
        FishingBadge()
        FollowBadge()
    }
}

@Composable
fun GpsQualityBadge() {
    val acc by SirenNav.accuracy
    val a = acc ?: return
    val (label, color) = when {
        a < 15f -> "GPS MUHTEM" to SirenGreen
        a < 30f -> "GPS ORTA" to SirenTrackYellow
        else -> "GPS ZAYIF" to SirenRed
    }
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenPanel.copy(alpha = 0.9f))
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text("$label · %.0fm".format(a), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnchorWatchBadge() {
    var anchored by remember { mutableStateOf<GeoPoint?>(null) }
    var alarm by remember { mutableStateOf(false) }
    var slowSince by remember { mutableStateOf<Long?>(null) }
    val p by SirenNav.pos

    LaunchedEffect(p) {
        val cur = p ?: return@LaunchedEffect
        val sp = SirenNav.speedKts.value ?: 5f
        val now = System.currentTimeMillis()
        if (sp < 0.8f) {
            val since = slowSince ?: now
            if (slowSince == null) slowSince = now
            if (anchored == null && now - since > 15000) { anchored = cur; alarm = false }
        } else {
            slowSince = null
        }
        anchored?.let { a ->
            val d = haversineNm(a.latitude, a.longitude, cur.latitude, cur.longitude) * 1852.0
            if (d > 50) alarm = true
        }
    }

    val a = anchored
    if (a == null) return

    val cur = p
    val distM = if (cur != null)
        haversineNm(a.latitude, a.longitude, cur.latitude, cur.longitude) * 1852.0 else 0.0

    Box(Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (alarm) SirenRed else SirenPanel.copy(alpha = 0.9f))
        .clickable { anchored = null; alarm = false; slowSince = null }
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(
            if (alarm) "DEMIR TARAR! %.0fm".format(distM) else "DEMIR IZI · %.0fm".format(distM),
            color = if (alarm) Color.White else SirenGreen,
            fontSize = 11.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FollowBadge() {
    val ar by SirenNav.activeRoute
    val p by SirenNav.pos
    val a = ar ?: return

    LaunchedEffect(p) {
        val cur = p ?: return@LaunchedEffect
        val info = SirenNav.activeRoute.value ?: return@LaunchedEffect
        val target = info.points[info.leg]
        val d = haversineNm(cur.latitude, cur.longitude, target.latitude, target.longitude)
        if (d < 0.03 && info.leg < info.points.lastIndex) {
            SirenNav.activeRoute.value = info.copy(leg = info.leg + 1)
        }
    }

    val cur = p
    val remain = if (cur != null) remainingNmFrom(a, cur) else 0.0
    val etaMin = remain / 5.0 * 60.0

    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenRouteBlue.copy(alpha = 0.9f))
        .clickable { SirenNav.activeRoute.value = null }
        .padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text("ROTA ${a.name} · ${a.leg + 1}/${a.points.size} · %.2f nm · ~%.0f dk".format(remain, etaMin),
            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
fun TripComputerCard() {
    val start by SirenNav.tripStart
    val dist by SirenNav.tripDistNm
    val maxSp by SirenNav.maxSpeed
    val s = start
    val elapsedMin = if (s != null) (System.currentTimeMillis() - s) / 60000.0 else 0.0
    val avg = if (elapsedMin > 1) dist / (elapsedMin / 60.0) else 0.0
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SirenCard).padding(16.dp)) {
        Text("SEYIR BILGISAYARI", fontSize = 12.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
        Spacer(Modifier.height(8.dp))
        Row {
            TripCell("MESAFE", "%.2f".format(dist), "nm")
            Spacer(Modifier.width(18.dp))
            TripCell("ORT HIZ", "%.1f".format(avg), "kts")
            Spacer(Modifier.width(18.dp))
            TripCell("MAX HIZ", "%.1f".format(maxSp), "kts")
            Spacer(Modifier.width(18.dp))
            TripCell("SURE", "%.0f".format(elapsedMin), "dk")
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun TripCell(label: String, value: String, unit: String) {
    Column {
        Text(label, fontSize = 9.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.width(3.dp))
            Text(unit, fontSize = 9.sp, color = SirenTextSecondary, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
fun SunCard(pos: MutableState<GeoPoint?>) {
    val p = pos.value ?: return
    val key = "${p.latitude.toInt()}_${p.longitude.toInt()}"
    val times = remember(key) { sunTimesLocal(p.latitude, p.longitude) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(16.dp)) {
        Text("GUNES", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = SirenTextPrimary)
        Spacer(Modifier.height(8.dp))
        Row {
            Column {
                Text("GUNDOGUMU", fontSize = 9.sp, color = SirenTextSecondary)
                Text(times.first, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(24.dp))
            Column {
                Text("GUNBATIMI", fontSize = 9.sp, color = SirenTextSecondary)
                Text(times.second, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
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
