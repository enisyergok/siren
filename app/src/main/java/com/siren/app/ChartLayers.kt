package com.siren.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tilesource.TileSourceFactory
import org.osmdroid.tilesource.XYTileSource
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import java.net.HttpURLConnection
import java.net.URL

object ChartLayers {
    val ESRI_OCEAN = XYTileSource("EsriOcean", 0, 16, 256, ".png",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/Ocean/Ocean_Basemap/MapServer/tile/"))
    val ESRI_SAT = XYTileSource("EsriImagery", 0, 18, 256, ".png",
        arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"))

    fun seamarkSource() = XYTileSource("OpenSeaMap", 0, 18, 256, ".png",
        arrayOf("https://tiles.openseamap.org/seamark/"))

    fun makeOverlay(ctx: Context, src: XYTileSource): TilesOverlay {
        val provider = MapTileProviderBasic(ctx)
        provider.tileSource = src
        val ov = TilesOverlay(provider, ctx)
        ov.loadingBackgroundColor = android.graphics.Color.TRANSPARENT
        return ov
    }
}

@Composable
fun BoxScope.LayerPanelButton(mapView: MapView) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var base by remember { mutableStateOf(0) }
    var seamark by remember { mutableStateOf(true) }
    var radar by remember { mutableStateOf(false) }
    var seamarkOv by remember { mutableStateOf<TilesOverlay?>(null) }
    var radarOv by remember { mutableStateOf<TilesOverlay?>(null) }

    fun applyBase(i: Int) {
        base = i
        mapView.setTileSource(when (i) {
            0 -> ChartLayers.ESRI_OCEAN
            1 -> ChartLayers.ESRI_SAT
            else -> TileSourceFactory.MAPNIK
        })
        mapView.invalidate()
    }

    fun setSeamark(on: Boolean) {
        seamark = on
        seamarkOv?.let { mapView.overlays.remove(it); seamarkOv = null }
        if (on) {
            val ov = ChartLayers.makeOverlay(context, ChartLayers.seamarkSource())
            mapView.overlays.add(0, ov)
            seamarkOv = ov
        }
        mapView.invalidate()
    }

    LaunchedEffect(radar) {
        radarOv?.let { mapView.overlays.remove(it); radarOv = null }
        if (!radar) { mapView.invalidate(); return@LaunchedEffect }
        while (radar) {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val con = URL("https://api.rainviewer.com/public/weather-maps.json")
                        .openConnection() as HttpURLConnection
                    con.connectTimeout = 6000; con.readTimeout = 6000
                    val body = con.inputStream.bufferedReader().use { it.readText() }
                    con.disconnect()
                    val arr = JSONObject(body).getJSONObject("radar").getJSONArray("past")
                    arr.getJSONObject(arr.length() - 1).getString("path")
                }.getOrNull()
            }
            if (path != null) {
                radarOv?.let { mapView.overlays.remove(it) }
                val src = XYTileSource("Radar", 0, 18, 256, "/2/1_1.png",
                    arrayOf("https://tilecache.rainviewer.com$path/256/"))
                val ov = ChartLayers.makeOverlay(context, src)
                mapView.overlays.add(0, ov)
                radarOv = ov
                mapView.invalidate()
            }
            delay(600_000)
        }
    }

    LaunchedEffect(Unit) { setSeamark(true); applyBase(0) }

    Column(Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 44.dp)) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(SirenPanel.copy(alpha = 0.9f))
            .clickable { open = !open }, contentAlignment = Alignment.Center) { Text("🗺️", fontSize = 18.sp) }

        if (open) {
            Column(Modifier.padding(top = 6.dp).clip(RoundedCornerShape(10.dp))
                .background(SirenPanel.copy(alpha = 0.95f)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LayerChip("🌊", base == 0) { applyBase(0) }
                    LayerChip("🛰️", base == 1) { applyBase(1) }
                    LayerChip("🗾", base == 2) { applyBase(2) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LayerChip("⚓️", seamark) { setSeamark(!seamark) }
                    LayerChip("📡", radar) { radar = !radar }
                }
            }
        }
    }
}

@Composable
private fun LayerChip(icon: String, active: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(8.dp))
        .background(if (active) SirenPrimary else Color(0xFF22303F))
        .clickable { onClick() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(icon, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
