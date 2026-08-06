package com.siren.app

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.sin

class ArrowOverlay : Overlay() {
    var dir = 0.0
    var vel = 0.0
    override fun draw(c: Canvas, mv: MapView, shadow: Boolean) {
        if (shadow || vel <= 0.01) return
        val pj = mv.projection
        val center = pj.toPixels(GeoPoint(SirenNav.pos.value?.latitude ?: return,
            SirenNav.pos.value?.longitude ?: return), null)
        val paint = Paint().apply {
            color = android.graphics.Color.argb(180, 56, 189, 248)
            strokeWidth = 4f; isAntiAlias = true
        }
        val len = (20 + vel * 60).toFloat().coerceAtMost(60f)
        val dx = (sin(Math.toRadians(dir)) * len).toFloat()
        val dy = (-cos(Math.toRadians(dir)) * len).toFloat()
        for (gx in -2..2) {
            for (gy in -2..2) {
                val ox = center.x + gx * 90f
                val oy = center.y + gy * 90f
                c.drawLine(ox, oy, ox + dx, oy + dy, paint)
                c.drawLine(ox + dx, oy + dy, ox + dx * 0.7f - dy * 0.25f, oy + dy * 0.7f + dx * 0.25f, paint)
                c.drawLine(ox + dx, oy + dy, ox + dx * 0.7f + dy * 0.25f, oy + dy * 0.7f - dx * 0.25f, paint)
            }
        }
    }
}

@Composable
fun BoxScope.CurrentArrowsController(mapView: MapView) {
    val overlay = remember { ArrowOverlay() }
    LaunchedEffect(Unit) {
        mapView.overlays.add(1, overlay)
        while (true) {
            val p = SirenNav.pos.value
            if (p != null) {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        val url = URL("https://marine-api.open-meteo.com/v1/marine?latitude=${p.latitude}&longitude=${p.longitude}&current=ocean_current_velocity,ocean_current_direction")
                        val con = url.openConnection() as HttpURLConnection
                        con.connectTimeout = 6000; con.readTimeout = 6000
                        val body = con.inputStream.bufferedReader().use { it.readText() }
                        con.disconnect()
                        val cur = JSONObject(body).optJSONObject("current")
                        (cur?.optDouble("ocean_current_velocity", 0.0) ?: 0.0) to
                            (cur?.optDouble("ocean_current_direction", 0.0) ?: 0.0)
                    }.getOrNull()
                }
                if (res != null) { overlay.vel = res.first; overlay.dir = res.second; mapView.invalidate() }
            }
            delay(300_000)
        }
    }
}
