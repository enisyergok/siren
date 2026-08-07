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
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

// WMS -> XYZ köprüsü: her tile için bbox hesaplar, WMS isteği üretir
class WmsTileSource(
    name: String,
    minZoom: Int,
    maxZoom: Int,
    private val wmsUrl: String,
    private val layers: String,
    private val styles: String = ""
) : OnlineTileSourceBase(name, minZoom, maxZoom, 256, ".png", arrayOf(wmsUrl)) {

    override fun getTileURLString(pMapTileIndex: Long): String {
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        val z = MapTileIndex.getZoom(pMapTileIndex)

        val n = Math.pow(2.0, z.toDouble())
        val minLon = x / n * 360.0 - 180.0
        val maxLon = (x + 1) / n * 360.0 - 180.0

        val minLatRad = Math.atan(Math.sinh(Math.PI * (1.0 - 2.0 * (y + 1) / n)))
        val maxLatRad = Math.atan(Math.sinh(Math.PI * (1.0 - 2.0 * y / n)))
        val minLat = Math.toDegrees(minLatRad)
        val maxLat = Math.toDegrees(maxLatRad)

        val bbox = "$minLon,$minLat,$maxLon,$maxLat"

        return "$wmsUrl" +
            "?SERVICE=WMS" +
            "&VERSION=1.1.1" +
            "&REQUEST=GetMap" +
            "&LAYERS=$layers" +
            "&STYLES=$styles" +
            "&SRS=EPSG:4326" +
            "&BBOX=$bbox" +
            "&WIDTH=256&HEIGHT=256" +
            "&FORMAT=image/png" +
            "&TRANSPARENT=TRUE"
    }
}

object BathymetryLayers {
    val EMODNET = WmsTileSource(
        name = "EMODnet",
        minZoom = 1,
        maxZoom = 14,
        wmsUrl = "https://tiles.emodnet-bathymetry.eu/baselayer/wms",
        layers = "emodnet_bathymetry"
    )

    val GEBCO = WmsTileSource(
        name = "GEBCO",
        minZoom = 1,
        maxZoom = 14,
        wmsUrl = "https://www.gebco.net/data_and_products/gebco_web_services/nf_3d/wms",
        layers = "GEBCO_LATEST"
    )

    fun makeOverlay(ctx: Context, src: WmsTileSource): TilesOverlay {
        val provider = MapTileProviderBasic(ctx)
        provider.tileSource = src
        val ov = TilesOverlay(provider, ctx)
        ov.loadingBackgroundColor = android.graphics.Color.TRANSPARENT
        return ov
    }
}

@Composable
fun BoxScope.BathymetryPanel(mapView: MapView) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var emodnetOn by remember { mutableStateOf(false) }
    var gebcoOn by remember { mutableStateOf(false) }
    var emodnetOv by remember { mutableStateOf<TilesOverlay?>(null) }
    var gebcoOv by remember { mutableStateOf<TilesOverlay?>(null) }

    fun toggleEmodnet() {
        emodnetOn = !emodnetOn
        emodnetOv?.let { mapView.overlays.remove(it); emodnetOv = null }
        if (emodnetOn) {
            val ov = BathymetryLayers.makeOverlay(context, BathymetryLayers.EMODNET)
            mapView.overlays.add(0, ov)
            emodnetOv = ov
        }
        mapView.invalidate()
    }

    fun toggleGebco() {
        gebcoOn = !gebcoOn
        gebcoOv?.let { mapView.overlays.remove(it); gebcoOv = null }
        if (gebcoOn) {
            val ov = BathymetryLayers.makeOverlay(context, BathymetryLayers.GEBCO)
            mapView.overlays.add(0, ov)
            gebcoOv = ov
        }
        mapView.invalidate()
    }

    Column(Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 92.dp)) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                .background(SirenPanel.copy(alpha = 0.9f))
                .clickable { open = !open },
            contentAlignment = Alignment.Center
        ) {
            Text("🌊", fontSize = 18.sp)
        }

        if (open) {
            Column(
                Modifier.padding(top = 6.dp).clip(RoundedCornerShape(10.dp))
                    .background(SirenPanel.copy(alpha = 0.95f)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("DERINLIK KONTURLARI", color = SirenTextSecondary,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BathChip("EMODnet", emodnetOn) { toggleEmodnet() }
                    BathChip("GEBCO", gebcoOn) { toggleGebco() }
                }
            }
        }
    }
}

@Composable
private fun BathChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (active) SirenPrimary else Color(0xFF22303F))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = if (active) Color.White else SirenTextSecondary)
    }
}
