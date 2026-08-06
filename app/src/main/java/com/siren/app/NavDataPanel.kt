package com.siren.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun BoxScope.NavDataPanel() {
    val ar by SirenNav.activeRoute
    val p by SirenNav.pos
    val sp by SirenNav.speedKts
    val crs by SirenNav.course
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(5000); tick = System.currentTimeMillis() } }

    val a = ar
    val pos = p
    if (a != null && pos != null && a.leg < a.points.size) {
        val from = a.points[maxOf(0, a.leg - 1)]
        val to = a.points[a.leg]
        val xte = NavMath.xteNm(pos, from, to)
        val btw = NavMath.bearingDeg(pos, to)
        val dtw = haversineNm(pos.latitude, pos.longitude, to.latitude, to.longitude)
        val speed = (sp ?: 0f).toDouble()
        val vmg = NavMath.vmgKts(speed, (crs ?: 0f).toDouble(), btw)
        val ttg = if (vmg > 0.3) dtw / vmg * 60.0 else 0.0

        Column(Modifier.align(Alignment.TopCenter).padding(top = 64.dp)) {
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(SirenPanel.copy(alpha = 0.92f)).padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                NavCell("XTE", if (abs(xte) < 0.01) "0" else "%.0fm %s".format(abs(xte) * 1852, if (xte > 0) "R" else "L"),
                    if (abs(xte) > 0.05) SirenRed else SirenGreen)
                NavCell("VMG", "%.1f".format(vmg), if (vmg > 0.5) SirenGreen else SirenTrackYellow)
                NavCell("BTW", "%.0f°".format(btw), SirenTextPrimary)
                NavCell("DTW", "%.2fnm".format(dtw), SirenTextPrimary)
                NavCell("TTG", if (ttg > 0) "%.0fdk".format(ttg) else "--", SirenTextPrimary)
            }
            // 6) DR - olu hesap (GPS 20sn yoksa)
            val stale = System.currentTimeMillis() - SirenNav.lastFixTime.value > 20000
            val lp = SirenNav.lastPos.value
            if (stale && lp != null) {
                val elapsedH = (System.currentTimeMillis() - SirenNav.lastFixTime.value) / 3600000.0
                val dr = NavMath.destPoint(lp, (crs ?: 0f).toDouble(), speed * elapsedH)
                Box(Modifier.padding(top = 4.dp).clip(RoundedCornerShape(8.dp))
                    .background(SirenTrackYellow.copy(alpha = 0.9f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("DR POZISYON (GPS YOK): %.4f, %.4f".format(dr.latitude, dr.longitude),
                        color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NavCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 8.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
