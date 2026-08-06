package com.siren.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.tan
import kotlin.math.cos

data class TileRange(val z: Int, val xMin: Int, val xMax: Int, val yMin: Int, val yMax: Int)

object OfflineMaps {

    fun tileXY(lat: Double, lon: Double, z: Int): Pair<Int, Int> {
        val n = 2.0.pow(z)
        val x = floor((lon + 180.0) / 360.0 * n).toInt()
        val latRad = Math.toRadians(lat)
        val y = floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n).toInt()
        return x to y
    }

    fun rangesForBounds(mapView: MapView, extraZoom: Int): List<TileRange> {
        val bb = mapView.boundingBox
        val z0 = mapView.zoomLevel.toInt().coerceIn(1, 17)
        val out = mutableListOf<TileRange>()
        for (z in z0..min(z0 + extraZoom, 18)) {
            val (xW, yN) = tileXY(bb.latNorth, bb.lonWest, z)
            val (xE, yS) = tileXY(bb.latSouth, bb.lonEast, z)
            out.add(
                TileRange(
                    z,
                    min(xW, xE), max(xW, xE),
                    min(yN, yS), max(yN, yS)
                )
            )
        }
        return out
    }

    fun countOf(ranges: List<TileRange>): Int =
        ranges.sumOf { (it.xMax - it.xMin + 1) * (it.yMax - it.yMin + 1) }

    suspend fun download(ranges: List<TileRange>, onProgress: (Int, Int) -> Unit) {
        val cache = Configuration.getInstance().osmdroidTileCache
        val src = TileSourceFactory.MAPNIK
        val total = countOf(ranges)
        var done = 0
        withContext(Dispatchers.IO) {
            for (r in ranges) {
                for (x in r.xMin..r.xMax) {
                    for (y in r.yMin..r.yMax) {
                        done++
                        val file = File(cache, "Mapnik/${r.z}/$x/$y.png")
                        if (!file.exists()) {
                            runCatching {
                                val con = URL("https://tile.openstreetmap.org/${r.z}/$x/$y.png")
                                    .openConnection() as HttpURLConnection
                                con.connectTimeout = 8000
                                con.readTimeout = 8000
                                con.setRequestProperty("User-Agent", "SIREN/0.7.0")
                                con.inputStream.use { ins ->
                                    file.parentFile?.mkdirs()
                                    FileOutputStream(file).use { outs -> ins.copyTo(outs) }
                                }
                                con.disconnect()
                            }
                        }
                        onProgress(done, total)
                        delay(10)
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadPanel(mapView: MapView, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var extra by remember { mutableStateOf(1) }
    var status by remember { mutableStateOf("Hazir") }
    var running by remember { mutableStateOf(false) }
    val zoomNow = mapView.zoomLevel.toInt()

    val ranges = remember(zoomNow, extra) { OfflineMaps.rangesForBounds(mapView, extra) }
    val count = OfflineMaps.countOf(ranges)
    val tooBig = count > 1500

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
            .clickable { if (!running) onClose() }
    ) {
        Column(
            Modifier.align(Alignment.Center).width(340.dp)
                .clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(20.dp)
        ) {
            Text("CEVRIMDISI HARITA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Gorunen alan (zoom $zoomNow) + secilen seviye indirilecek.",
                color = SirenTextSecondary, fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3).forEach { lv ->
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (extra == lv) SirenPrimary else SirenPanel)
                            .clickable { extra = lv }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("+$lv", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Tahmini karo: $count", color = SirenTextPrimary, fontSize = 14.sp)
            if (tooBig) {
                Text("Cok buyuk! Haritaya yakinlas veya seviyeyi azalt.", color = SirenRed, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(status, color = SirenGreen, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (running || tooBig) SirenPanel else SirenPrimary)
                        .clickable {
                            if (!running && !tooBig) {
                                running = true
                                status = "0 / $count"
                                scope.launch {
                                    OfflineMaps.download(ranges) { d, t -> status = "$d / $t" }
                                    status = "Tamamlandi! Artik cevrimdisi."
                                    running = false
                                }
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(if (running) "INDIRILIYOR" else "INDIR", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(SirenPanel)
                        .clickable { if (!running) onClose() }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("KAPAT", color = SirenTextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
