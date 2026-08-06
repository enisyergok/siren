package com.siren.app

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline

class SwingCircleOverlay : Overlay() {
    override fun draw(c: Canvas, mv: MapView, shadow: Boolean) {
        if (shadow) return
        val ap = SirenNav.anchorPos.value ?: return
        val r = SirenNav.anchorRadiusM.value
        val pj = mv.projection
        val center = pj.toPixels(ap, null)
        val edge = pj.toPixels(
            GeoPoint(ap.latitude + r / 111320.0, ap.longitude), null)
        val radiusPx = Math.hypot((edge.x - center.x).toDouble(), (edge.y - center.y).toDouble()).toFloat()
        val paint = Paint().apply {
            color = android.graphics.Color.argb(90, 242, 201, 76)
            style = Paint.Style.STROKE; strokeWidth = 5f; isAntiAlias = true
        }
        c.drawCircle(center.x.toFloat(), center.y.toFloat(), radiusPx, paint)
        val boat = SirenNav.pos.value
        if (boat != null) {
            val bp = pj.toPixels(boat, null)
            val line = Paint().apply {
                color = android.graphics.Color.argb(160, 229, 72, 77)
                strokeWidth = 3f; isAntiAlias = true
            }
            c.drawLine(center.x.toFloat(), center.y.toFloat(), bp.x.toFloat(), bp.y.toFloat(), line)
        }
    }
}

@Composable
fun BoxScope.ToolsColumn(mapView: MapView) {
    var showMeasure by remember { mutableStateOf(false) }
    var showAnchor by remember { mutableStateOf(false) }
    val night by SirenNav.nightMode
    val up by SirenNav.headingUp
    val line = remember { Polyline() }
    var lineAdded by remember { mutableStateOf(false) }

    Column(
        Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolBtn("📏") { showMeasure = !showMeasure }
        ToolBtn("⚓") { showAnchor = true }
        ToolBtn(if (up) "🧭" else "🧭") { SirenNav.headingUp.value = !up }
        ToolBtn(if (night) "🌙" else "☀️") { SirenNav.nightMode.value = !night }
    }

    if (showMeasure) {
        val a by SirenNav.measureA
        val b by SirenNav.measureB
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 60.dp)
            .clip(RoundedCornerShape(10.dp)).background(SirenPanel.copy(alpha = 0.95f)).padding(12.dp)) {
            Text("EBL/VRM OLCER", color = SirenTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Text("A AL", color = SirenGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        val c = mapView.mapCenter
                        SirenNav.measureA.value = GeoPoint(c.latitude, c.longitude)
                        SirenNav.measureB.value = null
                    })
                Text("B AL", color = SirenTrackYellow, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        val c = mapView.mapCenter
                        SirenNav.measureB.value = GeoPoint(c.latitude, c.longitude)
                    })
                Text("KAPAT", color = SirenRed, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        showMeasure = false
                        SirenNav.measureA.value = null
                        SirenNav.measureB.value = null
                        if (lineAdded) { mapView.overlays.remove(line); lineAdded = false }
                    })
            }
            if (a != null && b != null) {
                val d = haversineNm(a!!.latitude, a!!.longitude, b!!.latitude, b!!.longitude)
                val brg = NavMath.bearingDeg(a!!, b!!)
                Text("%.2f nm · %.0f°".format(d, brg), color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                LaunchedEffect(a, b) {
                    line.setPoints(listOf(a!!, b!!))
                    line.outlinePaint.color = android.graphics.Color.CYAN
                    line.outlinePaint.strokeWidth = 5f
                    if (!lineAdded) { mapView.overlays.add(line); lineAdded = true }
                    mapView.invalidate()
                }
            }
        }
    }

    if (showAnchor) {
        var depth by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAnchor = false },
            title = { Text("Demir Hesaplayici", color = SirenTextPrimary) },
            text = {
                Column {
                    TextField(value = depth, onValueChange = { depth = it },
                        label = { Text("Derinlik (m)") }, singleLine = true)
                    val d = depth.toDoubleOrNull()
                    if (d != null) {
                        Spacer(Modifier.width(0.dp))
                        Text("Sakin: %.0f m zincir (x5)".format(d * 5), color = SirenGreen, fontSize = 12.sp)
                        Text("Firtina: %.0f m zincir (x7)".format(d * 7), color = SirenTrackYellow, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Text("DAIRE KUR", color = SirenGreen, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        val d = depth.toDoubleOrNull() ?: 5.0
                        val p = SirenNav.pos.value
                        if (p != null) {
                            SirenNav.anchorPos.value = p
                            SirenNav.anchorRadiusM.value = d * 5 + 10
                        }
                        showAnchor = false
                    })
            },
            dismissButton = {
                Text("KALDIR", color = SirenRed, modifier = Modifier.clickable {
                    SirenNav.anchorPos.value = null
                    showAnchor = false
                })
            }
        )
    }

    LaunchedEffect(Unit) {
        mapView.overlays.add(1, SwingCircleOverlay())
        mapView.invalidate()
    }
}

@Composable
private fun ToolBtn(icon: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(SirenPanel)
        .clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(icon, fontSize = 18.sp)
    }
}

@Composable
fun BoxScope.NightFilter() {
    val night by SirenNav.nightMode
    if (night) {
        Box(Modifier.matchParentSize().background(Color(0x55FF2222)))
    }
}
