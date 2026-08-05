package com.siren.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController.Visibility
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File

val SirenBackground = Color(0xFF0A101C)
val SirenPanel = Color(0xFF0D1626)
val SirenCard = Color(0xFF111C30)
val SirenPrimary = Color(0xFF1F6FEB)
val SirenGreen = Color(0xFF2ECC71)
val SirenTextPrimary = Color(0xFFEAF1FB)
val SirenTextSecondary = Color(0xFF8CA3C2)
val SirenDivider = Color(0xFF1D2A40)

@Composable
fun SirenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SirenPrimary,
            background = SirenBackground,
            surface = SirenPanel,
            onBackground = SirenTextPrimary,
            onSurface = SirenTextPrimary
        ),
        content = content
    )
}

enum class SirenTab(val title: String, val icon: ImageVector) {
    Harita("Harita", Icons.Filled.Map),
    Rotalar("Rotalar", Icons.Filled.AltRoute),
    Waypointler("Waypoint'ler", Icons.Filled.Place),
    Izler("İzler", Icons.Filled.Timeline),
    HavaDurumu("Hava Durumu", Icons.Filled.Air),
    AisTrafigi("AIS Trafiği", Icons.Filled.DirectionsBoat),
    GelgitAkinti("Gelgit & Akıntı", Icons.Filled.Water),
    Sonar("Sonar", Icons.Filled.Radar),
    Ayarlar("Ayarlar", Icons.Filled.Settings)
}

private val OpenSeaMapSource = XYTileSource(
    "OpenSeaMap", 1, 19, 256, ".png",
    arrayOf("https://tiles.openseamap.org/seamark"),
    "© OpenSeaMap"
)

class GpsTracker(
    context: Context,
    private val onLocation: (Location) -> Unit
) : LocationListener {

    private val manager =
        context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun start() {
        runCatching {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, this)
        }
    }

    fun stop() {
        runCatching { manager.removeUpdates(this) }
    }

    override fun onLocationChanged(location: Location) {
        onLocation(location)
    }
}

fun formatCoords(lat: Double, lon: Double): Pair<String, String> {
    val la = Math.abs(lat)
    val lo = Math.abs(lon)
    val ld = la.toInt()
    val lm = (la - ld) * 60
    val od = lo.toInt()
    val om = (lo - od) * 60
    val latH = if (lat >= 0) "N" else "S"
    val lonH = if (lon >= 0) "E" else "W"
    return ("%02d° %06.3f' %s".format(ld, lm, latH)) to ("%03d° %06.3f' %s".format(od, om, lonH))
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SirenTheme { SirenRoot() } }
    }
}

@Composable
fun SirenRoot() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(SirenTab.Harita) }

    val pos = remember { mutableStateOf<GeoPoint?>(null) }
    val speedKts = remember { mutableStateOf<Float?>(null) }
    val courseDeg = remember { mutableStateOf<Float?>(null) }
    val follow = remember { mutableStateOf(true) }

    var hasLocPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocPerm = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    val tracker = remember {
        GpsTracker(context) { loc ->
            pos.value = GeoPoint(loc.latitude, loc.longitude)
            speedKts.value = loc.speed * 1.94384f
            courseDeg.value = loc.bearing
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocPerm) {
            permLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(hasLocPerm) {
        if (hasLocPerm) tracker.start() else tracker.stop()
    }

    DisposableEffect(Unit) { onDispose { tracker.stop() } }

    Row(Modifier.fillMaxSize().background(SirenBackground)) {
        SideNav(selected = selected, onSelect = { selected = it })
        Box(Modifier.weight(1f)) {
            if (selected == SirenTab.Harita) {
                MapScreen(pos, speedKts, courseDeg, follow)
            } else {
                ComingSoon(selected.title)
            }
        }
        RightPanel(Modifier.width(260.dp).fillMaxHeight(), pos, speedKts, courseDeg)
    }
}

@Composable
fun ComingSoon(title: String) {
    Box(Modifier.fillMaxSize().background(SirenBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Bu modül yakında eklenecek", color = SirenTextSecondary)
        }
    }
}

@Composable
fun SideNav(selected: SirenTab, onSelect: (SirenTab) -> Unit) {
    Column(
        Modifier.width(230.dp).fillMaxHeight().background(SirenPanel).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Anchor, "SİREN", tint = Color.Black, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("SİREN", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = SirenTextPrimary)
                Text("DENİZ NAVİGASYON", fontSize = 9.sp, letterSpacing = 2.sp, color = SirenTextSecondary)
            }
        }
        Spacer(Modifier.height(18.dp))
        SirenTab.entries.forEach { tab ->
            NavItem(tab, tab == selected) { onSelect(tab) }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.weight(1f))
        OfflineBadge()
    }
}

@Composable
private fun NavItem(tab: SirenTab, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SirenPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(tab.icon, tab.title, tint = if (selected) Color.White else SirenTextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(tab.title, color = if (selected) Color.White else SirenTextPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun OfflineBadge() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F2A1D)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = SirenGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text("ÇEVRİMDIŞI MOD", color = SirenGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Tüm Sistemler Çevrimdışı", color = SirenTextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
fun MapScreen(
    pos: MutableState<GeoPoint?>,
    speedKts: MutableState<Float?>,
    courseDeg: MutableState<Float?>,
    follow: MutableState<Boolean>
) {
    val context = LocalContext.current

    val mapView = remember {
        val cfg = Configuration.getInstance()
        cfg.load(context, context.getSharedPreferences("osmdroid", 0))
        cfg.osmdroidBasePath = File(context.filesDir, "osmdroid")
        cfg.osmdroidTileCache = File(context.filesDir, "osmdroid/tiles")
        cfg.userAgentValue = "SIREN/0.3.1"

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setTilesScaledToDpi(true)
            zoomController.setVisibility(Visibility.NEVER)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(36.9582, 27.4428))
            setOnTouchListener { _, e ->
                if (e.action == MotionEvent.ACTION_MOVE) follow.value = false
                false
            }
            runCatching {
                overlays.add(
                    TilesOverlay(MapTileProviderBasic(context, OpenSeaMapSource), context).apply {
                        loadingBackgroundColor = android.graphics.Color.TRANSPARENT
                    }
                )
            }
            invalidate()
        }
    }

    val boatMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(context, R.drawable.boat)
            title = "SİREN"
        }
    }

    LaunchedEffect(pos.value) {
        pos.value?.let { p ->
            if (!mapView.overlays.contains(boatMarker)) {
                mapView.overlays.add(boatMarker)
                mapView.controller.setZoom(15.0)
            }
            boatMarker.position = p
            boatMarker.rotation = courseDeg.value ?: 0f
            if (follow.value) mapView.controller.animateTo(p)
            mapView.invalidate()
        }
    }

    DisposableEffect(Unit) { onDispose { mapView.onDetach() } }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        MapTopBar()
        MapControls(
            onLocate = {
                follow.value = true
                pos.value?.let {
                    mapView.controller.animateTo(it)
                    mapView.invalidate()
                }
            },
            onZoomIn = {
                mapView.controller.zoomIn()
                mapView.invalidate()
            },
            onZoomOut = {
                mapView.controller.zoomOut()
                mapView.invalidate()
            }
        )
        BottomDataBar(speedKts, courseDeg)
        ScaleBar()
    }
}

@Composable
private fun BoxScope.MapTopBar() {
    Row(
        Modifier.align(Alignment.TopStart).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DarkIconButton(Icons.Filled.Menu)
        Spacer(Modifier.width(10.dp))
        Row(
            Modifier.width(240.dp).clip(RoundedCornerShape(10.dp))
                .background(SirenPanel).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = SirenTextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Konum ara", color = SirenTextSecondary, fontSize = 13.sp)
        }
    }
    Box(
        Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
            .width(320.dp).clip(RoundedCornerShape(10.dp))
            .background(SirenPanel).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("B   330   K   30   D", color = SirenTextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
    }
    Box(
        Modifier.align(Alignment.TopEnd).padding(12.dp)
            .clip(RoundedCornerShape(10.dp)).background(SirenPanel)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text("3D", color = SirenTextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BoxScope.MapControls(
    onLocate: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Column(
        Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DarkIconButton(Icons.Filled.MyLocation, onLocate)
        DarkIconButton(Icons.Filled.Add, onZoomIn)
        DarkIconButton(Icons.Filled.Remove, onZoomOut)
        DarkIconButton(Icons.Filled.Layers)
    }
}

@Composable
private fun DarkIconButton(icon: ImageVector, onClick: () -> Unit = {}) {
    Box(
        Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
            .background(SirenPanel).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = SirenTextPrimary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun BoxScope.BottomDataBar(
    speedKts: MutableState<Float?>,
    courseDeg: MutableState<Float?>
) {
    Row(
        Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SirenPanel.copy(alpha = 0.95f))
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        DataCell("SOG", speedKts.value?.let { "%.1f".format(it) } ?: "--", "kts")
        DataCell("COG", courseDeg.value?.let { "%.0f°".format(it) } ?: "--", "T")
        DataCell("DERİNLİK", "42.7", "m")
        DataCell("ETA", "14:35", "")
        Icon(Icons.Filled.KeyboardArrowUp, null, tint = SirenTextPrimary)
    }
}

@Composable
private fun DataCell(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, letterSpacing = 1.5.sp, color = SirenTextSecondary)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(unit, fontSize = 9.sp, color = SirenTextSecondary)
    }
}

@Composable
private fun BoxScope.ScaleBar() {
    Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
        Text("500 m", color = Color.White, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Box(Modifier.width(70.dp).height(2.dp).background(Color.White))
    }
}

@Composable
fun RightPanel(
    modifier: Modifier = Modifier,
    pos: MutableState<GeoPoint?>,
    speedKts: MutableState<Float?>,
    courseDeg: MutableState<Float?>
) {
    Column(
        modifier.background(SirenBackground).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TelemetryCard(pos, speedKts, courseDeg)
        SonarCard()
    }
}

@Composable
private fun TelemetryCard(
    pos: MutableState<GeoPoint?>,
    speedKts: MutableState<Float?>,
    courseDeg: MutableState<Float?>
) {
    val coords = pos.value?.let { formatCoords(it.latitude, it.longitude) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(SirenCard).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TELEMETRİ", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = SirenTextPrimary)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Settings, null, tint = SirenTextSecondary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(10.dp))
        TelemetryRow("HIZ", speedKts.value?.let { "%.1f".format(it) } ?: "--", "kts")
        DividerLine()
        TelemetryRow("YÖN", courseDeg.value?.let { "%.0f°".format(it) } ?: "--", "T")
        DividerLine()
        TelemetryRow("DERİNLİK", "42.7", "m")
        DividerLine()
        Spacer(Modifier.height(10.dp))
        Text("KOORDİNATLAR", fontSize = 10.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(coords?.first ?: "GPS bekleniyor...", fontSize = 13.sp, color = SirenTextPrimary)
        Text(coords?.second ?: "", fontSize = 13.sp, color = SirenTextPrimary)
    }
}

@Composable
private fun TelemetryRow(label: String, value: String, unit: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 10.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.width(4.dp))
            Text(unit, fontSize = 11.sp, color = SirenTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(SirenDivider))
}

@Composable
private fun SonarCard() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(SirenCard).padding(16.dp)
    ) {
        Text("SONAR", fontSize = 12.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("42.7", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.width(4.dp))
            Text("m", fontSize = 12.sp, color = SirenTextSecondary, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().height(170.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF061225), Color(0xFF0B2A55), Color(0xFFB4530A), Color(0xFFE2542A))
                    )
                )
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(SirenPrimary)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Pause, null, tint = Color.White)
        }
    }
}
