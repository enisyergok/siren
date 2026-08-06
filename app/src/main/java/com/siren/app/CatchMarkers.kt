package com.siren.app

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun BoxScope.CatchMarkersLayer(mapView: MapView) {
    val dao = SirenNav.catchDao
    val empty: Flow<List<CatchEntity>> = flowOf(emptyList())
    val catches by (dao?.observeAll() ?: empty).collectAsState(initial = emptyList())
    val markers = remember { mutableListOf<Marker>() }

    LaunchedEffect(catches) {
        markers.forEach { mapView.overlays.remove(it) }
        markers.clear()
        catches.forEach { c ->
            val m = Marker(mapView).apply {
                position = GeoPoint(c.lat, c.lon)
                title = "🐟 ${c.species}"
                snippet = buildString {
                    c.lengthCm?.let { append("%.0f cm ".format(it)) }
                    c.weightKg?.let { append("%.1f kg".format(it)) }
                    if (isEmpty()) append("Av kaydi")
                }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            markers.add(m)
            mapView.overlays.add(m)
        }
        mapView.invalidate()
    }
}
