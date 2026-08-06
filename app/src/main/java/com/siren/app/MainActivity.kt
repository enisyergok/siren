package com.siren.app

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.FiberManualRecord
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController.Visibility as OsmVisibility
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

val SirenBackground = Color(0xFF0A101C)
val SirenPanel = Color(0xFF0D1626)
val SirenCard = Color(0xFF111C30)
val SirenPrimary = Color(0xFF1F6FEB)
val SirenGreen = Color(0xFF2ECC71)
val SirenRed = Color(0xFFE5484D)
val SirenTrackYellow = Color(0xFFF2C94C)
val SirenRouteBlue = Color(0xFF38BDF8)
val SirenTextPrimary = Color(0xFFEAF1FB)
val SirenTextSecondary = Color(0xFF8CA3C2)
val SirenDivider = Color(0xFF1D2A40)

@Composable
fun SirenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = SirenPrimary, background = SirenBackground, surface = SirenPanel,
        onBackground = SirenTextPrimary, onSurface = SirenTextPrimary
    ), content = content)
}

enum class SirenTab(val title: String, val icon: ImageVector) {
    Harita("Harita", Icons.Filled.Map),
    Rotalar("Rotalar", Icons.Filled.AltRoute),
    Waypointler("Waypoint'ler", Icons.Filled.Place),
    Izler("Izler", Icons.Filled.Timeline),
    HavaDurumu("Hava Durumu", Icons.Filled.Air),
    AisTrafigi("AIS Trafigi", Icons.Filled.DirectionsBoat),
    GelgitAkinti("Gelgit ve Akinti", Icons.Filled.Water),
    Sonar("Sonar", Icons.Filled.Radar),
    Balog("Balik Gunlugu", Icons.Filled.DirectionsBoat),
    AdvancedFishing("Ileri Balikci", Icons.Filled.Timeline),
    Ayarlar("Ayarlar", Icons.Filled.Settings)
}

val CartoVoyager = XYTileSource(
    "CartoVoyager", 1, 20, 256, ".png",
    arrayOf("https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/"),
    "© OSM © CARTO"
)

val EsriImagery = object : OnlineTileSourceBase("EsriImagery", 1, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/" +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex) + ".jpg"
    }
}

private val OpenSeaMapSource = XYTileSource(
    "OpenSeaMap", 1, 19, 256, ".png",
    arrayOf("https://tiles.openseamap.org/seamark"),
    "OpenSeaMap"
)

fun tileSourceFor(style: MapStyle) = when (style) {
    MapStyle.OSM -> TileSourceFactory.MAPNIK
    MapStyle.CARTO -> CartoVoyager
    MapStyle.SATELLITE -> EsriImagery
}

class GpsTracker(context: Context, private val onLocation: (Location) -> Unit) : LocationListener {
    private val manager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    fun start() {
        runCatching {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, this)
        }
    }
    fun stop() { runCatching { manager.removeUpdates(this) } }
    override fun onLocationChanged(location: Location) { onLocation(location) }
}

fun formatCoords(lat: Double, lon: Double): Pair<String, String> {
    val la = Math.abs(lat); val lo = Math.abs(lon)
    val ld = la.toInt(); val lm = (la - ld) * 60
    val od = lo.toInt(); val om = (lo - od) * 60
    val latH = if (lat >= 0) "N" else "S"
    val lonH = if (lon >= 0) "E" else "W"
    return ("%02d %06.3f %s".format(ld, lm, latH)) to ("%03d %06.3f %s".format(od, om, lonH))
}

fun haversineNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 3440.065
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2) * sin(dLon / 2)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun routeTotalNm(points: List<GeoPoint>): Double {
    if (points.size < 2) return 0.0
    var total = 0.0
    for (i in 0 until points.lastIndex) {
        total += haversineNm(points[i].latitude, points[i].longitude,
            points[i + 1].latitude, points[i + 1].longitude)
    }
    return total
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
    val settings = remember { SirenSettings(context) }
    var selected by remember { mutableStateOf(SirenTab.Harita) }
    val pos = remember { mutableStateOf<GeoPoint?>(null) }
    val speedKts = remember { mutableStateOf<Float?>(null) }
    val courseDeg = remember { mutableStateOf<Float?>(null) }
    val follow = remember { mutableStateOf(true) }
    val db = remember { AppDatabase.getInstance(context) }
    val dao = db.trackDao()
    val wpDao = db.waypointDao()
    val routeDao = db.routeDao()
    SirenNav.routeDao = routeDao
    SirenNav.catchDao = db.catchDao()
    val scope = rememberCoroutineScope()
    val recording = remember { mutableStateOf(false) }
    val currentTrackId = remember { mutableStateOf<String?>(null) }
    val trackPoints = remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    val waypoints by wpDao.observeAll().collectAsState(initial = emptyList())
    val routes by routeDao.observeAll().collectAsState(initial = emptyList())

    var hasLocPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> hasLocPerm = result[Manifest.permission.ACCESS_FINE_LOCATION] == true }

    val tracker = remember {
        GpsTracker(context) { loc ->
            pos.value = GeoPoint(loc.latitude, loc.longitude)
            speedKts.value = loc.speed * 1.94384f
            courseDeg.value = loc.bearing
            SirenNav.onLocation(GeoPoint(loc.latitude, loc.longitude), loc.speed * 1.94384f)
            SirenNav.accuracy.value = loc.accuracy
            if (recording.value) {
                val tid = currentTrackId.value
                if (tid != null) {
                    trackPoints.value = trackPoints.value + GeoPoint(loc.latitude, loc.longitude)
                    scope.launch {
                        dao.insertPoint(TrackPointEntity(
                            trackId = tid, lat = loc.latitude, lon = loc.longitude,
                            time = System.currentTimeMillis(),
                            speedKnots = (loc.speed * 1.94384).toDouble(),
                            heading = loc.bearing.toDouble()
                        Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Anchor, "SIREN", tint = Color.Black, modifier = Modifier.size(26.dp))
            })
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        if (!hasLocPerm) permLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            "android.permission.POST_NOTIFICATIONS"))
    }
    LaunchedEffect(hasLocPerm) { if (hasLocPerm) tracker.start() else tracker.stop() }
    LaunchedEffect(Unit) {
        ProximityAlarm.monitor(context, pos) { wpDao.getAllOnce() }
    }
    DisposableEffect(Unit) { onDispose { tracker.stop() } }

    val onRecordToggle: () -> Unit = {
        if (!recording.value) {
            val id = UUID.randomUUID().toString()
            val name = "Seyir " + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())
            currentTrackId.value = id
            trackPoints.value = emptyList()
            recording.value = true
            scope.launch { dao.insertTrack(TrackEntity(id, name, System.currentTimeMillis(), null, true)) }
        } else {
            currentTrackId.value?.let { id -> scope.launch { dao.finishTrack(id, System.currentTimeMillis()) } }
            recording.value = false
        }
    }

    val onAddWaypoint: (GeoPoint) -> Unit = { p ->
        scope.launch {
            wpDao.insert(WaypointEntity(
                id = UUID.randomUUID().toString(),
                name = "WP " + SimpleDateFormat("HH:mm", Locale("tr")).format(Date()),
                lat = p.latitude, lon = p.longitude,
                createdAt = System.currentTimeMillis()
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Anchor, "SIREN", tint = Color.Black, modifier = Modifier.size(26.dp))
            })
        }
    }

    Box(Modifier.fillMaxSize().background(SirenBackground)) {
        Row(Modifier.fillMaxSize()) {
            Spacer(Modifier.width(230.dp))
            Box(Modifier.weight(1f).clipToBounds()) {
                when (selected) {
                    SirenTab.Harita -> MapScreen(pos, speedKts, courseDeg, follow, trackPoints,
                        recording, onRecordToggle, waypoints, onAddWaypoint, routes, routeDao, wpDao, settings)
                    SirenTab.Rotalar -> RoutesScreen(routeDao)
                    SirenTab.Waypointler -> WaypointsScreen(wpDao)
                    SirenTab.Izler -> TracksScreen(dao)
                    SirenTab.HavaDurumu -> WeatherScreen(pos)
                    SirenTab.Ayarlar -> SettingsScreen(settings)
                    SirenTab.AisTrafigi -> AisScreen(pos)
                    SirenTab.GelgitAkinti -> TideScreen(pos)
                    SirenTab.Balog -> CatchLogScreen(db.catchDao(), pos)
                    SirenTab.AdvancedFishing -> AdvancedFishingScreen(pos, db.catchDao())
                    else -> ComingSoon(selected.title)
                }
            }
            if (!settings.fullscreen) if (!settings.fullscreen) RightPanel(Modifier.width(260.dp).fillMaxHeight(), pos, speedKts, courseDeg, onSettings = { selected = SirenTab.Ayarlar })
        }
        SideNav(selected = selected, onSelect = { selected = it })
    }
}

@Composable
fun ComingSoon(title: String) {
    Box(Modifier.fillMaxSize().background(SirenBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Bu modul yakinda eklenecek", color = SirenTextSecondary)
        }
    }
}

@Composable
fun SettingsScreen(settings: SirenSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var style by remember { mutableStateOf(settings.mapStyle) }
    var unit by remember { mutableStateOf(settings.speedUnit) }
    Column(Modifier.fillMaxSize().background(SirenBackground).padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("AYARLAR", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
        Spacer(Modifier.height(20.dp))
        Text("HARITA KAYNAGI", color = SirenTextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        MapStyle.values().forEach { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(10.dp))
                .background(if (style == s) SirenPrimary else SirenCard)
                .clickable { style = s; settings.mapStyle = s }
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(s.label, color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (style == s) Icon(Icons.Filled.CheckCircle, null, tint = SirenGreen, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("HIZ BIRIMI", color = SirenTextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        SpeedUnit.values().forEach { u ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(10.dp))
                .background(if (unit == u) SirenPrimary else SirenCard)
                .clickable { unit = u; settings.speedUnit = u }
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${u.label} (${u.key})", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (unit == u) Icon(Icons.Filled.CheckCircle, null, tint = SirenGreen, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(30.dp))
        Text("VERI YEDEKLEME", color = SirenTextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SirenPrimary)
            .clickable {
                scope.launch {
                    val db = AppDatabase.getInstance(context)
                    val f = BackupRestore.exportAll(context, db)
                    BackupRestore.shareBackup(context, f)
                }
            }
            .padding(14.dp), contentAlignment = Alignment.Center) {
            Text("TUM VERILERI DISA AKTAR (JSON)", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text("GORUNUM", color = SirenTextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        var fs by remember { mutableStateOf(settings.fullscreen) }
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(10.dp))
            .background(SirenCard)
            .clickable { fs = !fs; settings.fullscreen = fs }
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Tam Ekran Harita (sag paneli gizle)", color = SirenTextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (fs) Icon(Icons.Filled.CheckCircle, null, tint = SirenGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("IZ RENGİ", color = SirenTextSecondary, fontSize = 12.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        var tc by remember { mutableStateOf(settings.trackColor) }
        TrackColor.values().forEach { c ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(10.dp))
                .background(if (tc == c) SirenPrimary else SirenCard)
                .clickable { tc = c; settings.trackColor = c }
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(c.hex))))
                Spacer(Modifier.width(10.dp))
                Text(c.label, color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (tc == c) Icon(Icons.Filled.CheckCircle, null, tint = SirenGreen, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("SIREN v0.11.0", color = SirenTextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun WaypointsScreen(dao: WaypointDao) {
    val context = LocalContext.current
    val wps by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(SirenBackground).padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("WAYPOINT'LER", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
            }
            if (wps.isNotEmpty()) {
                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(SirenPrimary)
                    .clickable { scope.launch { shareGpx(context, waypointsToGpx(dao.getAllOnce()), "siren-waypoints.gpx") } }
                    .padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("GPX", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (wps.isEmpty()) Text("Henuz waypoint yok.", color = SirenTextSecondary)
        wps.forEach { wp ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp))
                .background(SirenCard).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, null, tint = SirenTrackYellow)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(wp.name, color = SirenTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("%.5f, %.5f".format(wp.lat, wp.lon), color = SirenTextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.Delete, null, tint = SirenRed,
                    modifier = Modifier.size(20.dp).clickable { scope.launch { dao.delete(wp.id) } })
            }
        }
    }
}

@Composable
fun RoutesScreen(dao: RouteDao) {
    val context = LocalContext.current
    val routes by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<Map<String, Pair<Int, Double>>>(emptyMap()) }
    LaunchedEffect(routes) {
        val map = mutableMapOf<String, Pair<Int, Double>>()
        routes.forEach { r ->
            val pts = dao.getPointsForRoute(r.id).map { GeoPoint(it.lat, it.lon) }
            map[r.id] = pts.size to routeTotalNm(pts)
        }
        stats = map
    }
    Column(Modifier.fillMaxSize().background(SirenBackground).padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("ROTALAR", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
        Spacer(Modifier.height(16.dp))
        if (routes.isEmpty()) Text("Henuz rota yok.", color = SirenTextSecondary)
        routes.forEach { r ->
            val st = stats[r.id]
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp))
                .background(SirenCard).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AltRoute, null, tint = SirenRouteBlue)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(r.name, color = SirenTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date(r.createdAt)),
                        color = SirenTextSecondary, fontSize = 12.sp)
                    if (st != null) {
                        Row {
                            Text("${st.first} nokta", color = SirenTextSecondary, fontSize = 11.sp)
                            Spacer(Modifier.width(10.dp))
                            Text("%.1f nm".format(st.second), color = SirenRouteBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Text("~%.0f dk".format(st.second / 5.0 * 60), color = SirenTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.Share, null, tint = SirenPrimary,
                    modifier = Modifier.size(20.dp).clickable {
                        scope.launch {
                            val pts = dao.getPointsForRoute(r.id)
                            if (pts.isNotEmpty()) shareGpx(context, routeToGpx(r, pts), "siren-route.gpx")
                        }
                    })
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Filled.Delete, null, tint = SirenRed,
                    modifier = Modifier.size(20.dp).clickable { scope.launch { dao.delete(r.id) } })
            }
        }
    }
}

@Composable
fun TracksScreen(dao: TrackDao) {
    val context = LocalContext.current
    val tracks by dao.observeTracks().collectAsState(initial = emptyList())
    val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")) }
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<Map<String, Triple<Int, Long, Double?>>>(emptyMap()) }
    LaunchedEffect(tracks) {
        val map = mutableMapOf<String, Triple<Int, Long, Double?>>()
        tracks.forEach { t ->
            val count = dao.countPointsForTrack(t.id)
            val avg = dao.avgSpeedForTrack(t.id)
            val durationMs = ((t.endTime ?: System.currentTimeMillis()) - t.startTime).coerceAtLeast(0L)
            map[t.id] = Triple(count, durationMs, avg)
        }
        stats = map
    }
    Column(Modifier.fillMaxSize().background(SirenBackground).padding(24.dp).verticalScroll(rememberScrollState())) {
        TripComputerCard()
        Text("IZLER", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
        Spacer(Modifier.height(16.dp))
        if (tracks.isEmpty()) Text("Henuz kayitli iz yok.", color = SirenTextSecondary)
        tracks.forEach { t ->
            val s = stats[t.id]
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp))
                .background(SirenCard).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timeline, null, tint = SirenGreen)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(t.name, color = SirenTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(fmt.format(Date(t.startTime)), color = SirenTextSecondary, fontSize = 12.sp)
                    if (s != null) {
                        val durMin = s.second / 60000L
                        val avgStr = s.third?.let { "%.1f".format(it) } ?: "--"
                        Row {
                            Text("${s.first} nokta", color = SirenTextSecondary, fontSize = 11.sp)
                            Spacer(Modifier.width(10.dp))
                            Text("${durMin} dk", color = SirenTrackYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Text("ort $avgStr kts", color = SirenTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (!t.isRecording) {
                    Icon(Icons.Filled.Share, null, tint = SirenPrimary,
                        modifier = Modifier.size(20.dp).clickable {
                            scope.launch {
                                val tr = dao.getTrackById(t.id)
                                val pts = dao.getPointsForTrack(t.id)
                                if (tr != null && pts.isNotEmpty()) shareGpx(context, trackToGpx(tr, pts), "siren-track.gpx")
                            }
                        })
                    Spacer(Modifier.width(12.dp))
                }
                Text(if (t.isRecording) "KAYITTA" else "BITTI",
                    color = if (t.isRecording) SirenRed else SirenTextSecondary,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun shareGpx(context: Context, gpxContent: String, filename: String) {
    val gpxDir = File(context.cacheDir, "gpx"); gpxDir.mkdirs()
    val gpxFile = File(gpxDir, filename); gpxFile.writeText(gpxContent)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", gpxFile)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/gpx+xml"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "GPX Paylas"))
}

@Composable
fun SideNav(selected: SirenTab, onSelect: (SirenTab) -> Unit) {
    Column(Modifier.width(230.dp).fillMaxHeight().background(SirenPanel).padding(14.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Anchor, "SIREN", tint = Color.Black, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("SIREN", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = SirenTextPrimary)
                Text("DENIZ NAVIGASYON", fontSize = 9.sp, letterSpacing = 2.sp, color = SirenTextSecondary)
            }
        }
        Spacer(Modifier.height(18.dp))
        SirenTab.entries.forEach { tab ->
            NavItem(tab, tab == selected) { onSelect(tab) }; Spacer(Modifier.height(4.dp))
        }
        OfflineBadge()
    }
}

@Composable
private fun NavItem(tab: SirenTab, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
        .background(if (selected) SirenPrimary else Color.Transparent)
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(tab.icon, tab.title, tint = if (selected) Color.White else SirenTextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(tab.title, color = if (selected) Color.White else SirenTextPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun OfflineBadge() {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF0F2A1D)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, null, tint = SirenGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text("CEVRIMDISI MOD", color = SirenGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Tum Sistemler Cevrimdisi", color = SirenTextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
fun MapScreen(
    pos: MutableState<GeoPoint?>,
    speedKts: MutableState<Float?>,
    courseDeg: MutableState<Float?>,
    follow: MutableState<Boolean>,
    trackPoints: MutableState<List<GeoPoint>>,
    recording: MutableState<Boolean>,
    onRecordToggle: () -> Unit,
    waypoints: List<WaypointEntity>,
    onAddWaypoint: (GeoPoint) -> Unit,
    routes: List<RouteEntity>,
    routeDao: RouteDao,
    wpDao: WaypointDao,
    settings: SirenSettings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDownload by remember { mutableStateOf(false) }
    var showLayers by remember { mutableStateOf(false) }
    val routePlanner = remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var planningMode by remember { mutableStateOf(false) }
    var mapStyle by remember { mutableStateOf(settings.mapStyle) }
    val speedUnit by remember { mutableStateOf(settings.speedUnit) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var searchResultsWp by remember { mutableStateOf<List<WaypointEntity>>(emptyList()) }
    var searchResultsRt by remember { mutableStateOf<List<RouteEntity>>(emptyList()) }
    var focusTarget by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            searchResultsWp = wpDao.search(searchQuery)
            searchResultsRt = routeDao.search(searchQuery)
            showSearch = true
        } else {
            searchResultsWp = emptyList()
            searchResultsRt = emptyList()
            showSearch = false
        }
    }

    val mapView = remember {
        val cfg = Configuration.getInstance()
        cfg.load(context, context.getSharedPreferences("osmdroid", 0))
        cfg.osmdroidBasePath = File(context.filesDir, "osmdroid")
        cfg.osmdroidTileCache = File(context.filesDir, "osmdroid/tiles")
        cfg.userAgentValue = "SIREN/0.10.0"
        MapView(context).apply {
            setTileSource(tileSourceFor(settings.mapStyle))
            setMultiTouchControls(true)
            zoomController.setVisibility(OsmVisibility.NEVER)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(36.9582, 27.4428))
            setOnTouchListener { _, e ->
                if (e.action == MotionEvent.ACTION_MOVE) follow.value = false
                false
            }
            runCatching {
                overlays.add(TilesOverlay(MapTileProviderBasic(context, OpenSeaMapSource), context).apply {
                    loadingBackgroundColor = android.graphics.Color.TRANSPARENT
                })
            }
            invalidate()
        }
    }

    LaunchedEffect(mapStyle) {
        mapView.setTileSource(tileSourceFor(mapStyle))
        settings.mapStyle = mapStyle
        mapView.invalidate()
    }

    LaunchedEffect(focusTarget) {
        focusTarget?.let { p ->
            mapView.controller.animateTo(p, 15.0, 500L)
            mapView.invalidate()
        }
    }

    LaunchedEffect(Unit) {
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                if (planningMode) { routePlanner.value = routePlanner.value + p; return true }
                return false
            }
            override fun longPressHelper(p: GeoPoint): Boolean {
                if (!planningMode) onAddWaypoint(p); return true
            }
        }
        mapView.overlays.add(0, MapEventsOverlay(receiver))
        mapView.invalidate()
    }

    val trackLine = remember { Polyline(mapView).apply {
        outlinePaint.color = android.graphics.Color.parseColor(settings.trackColor.hex)
        outlinePaint.strokeWidth = 8f
    }}
    val routePlanLine = remember { Polyline(mapView).apply {
        outlinePaint.color = android.graphics.Color.parseColor("#38BDF8")
        outlinePaint.strokeWidth = 6f
    }}
    val savedRouteLines = remember { mutableListOf<Polyline>() }

    LaunchedEffect(Unit) { mapView.overlays.add(trackLine); mapView.overlays.add(routePlanLine); mapView.invalidate() }
    LaunchedEffect(trackPoints.value, settings.trackColor) {
        trackLine.outlinePaint.color = android.graphics.Color.parseColor(settings.trackColor.hex)
        trackLine.setPoints(trackPoints.value); mapView.invalidate()
    }
    LaunchedEffect(routePlanner.value) { routePlanLine.setPoints(routePlanner.value); mapView.invalidate() }

    val boatMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(context, R.drawable.boat)
            title = "SIREN"
        }
    }
    LaunchedEffect(pos.value) {
        pos.value?.let { p ->
            if (!mapView.overlays.contains(boatMarker)) {
                mapView.overlays.add(boatMarker); mapView.controller.setZoom(15.0)
            }
            boatMarker.position = p
            boatMarker.rotation = courseDeg.value ?: 0f
            if (follow.value) mapView.controller.animateTo(p)
            mapView.invalidate()
        }
    }

    val wpMarkers = remember { mutableListOf<Marker>() }
    LaunchedEffect(waypoints) {
        wpMarkers.forEach { mapView.overlays.remove(it) }
        wpMarkers.clear()
        waypoints.forEach { wp ->
            val m = Marker(mapView).apply {
                position = GeoPoint(wp.lat, wp.lon)
                title = wp.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            wpMarkers.add(m); mapView.overlays.add(m)
        }
        mapView.invalidate()
    }

    LaunchedEffect(routes) {
        savedRouteLines.forEach { mapView.overlays.remove(it) }
        savedRouteLines.clear()
        routes.forEach { r ->
            val pts = routeDao.getPointsForRoute(r.id).map { GeoPoint(it.lat, it.lon) }
            if (pts.isNotEmpty()) {
                val line = Polyline(mapView).apply {
                    outlinePaint.color = android.graphics.Color.parseColor(r.colorHex)
                    outlinePaint.strokeWidth = 5f
                    setPoints(pts)
                }
                savedRouteLines.add(line); mapView.overlays.add(line)
            }
        }
        mapView.invalidate()
    }

    DisposableEffect(Unit) { onDispose { mapView.onDetach() } }

    val displaySpeed: Float? = speedKts.value?.let { (it * speedUnit.factor / 1.94384).toFloat() }
    val displayCourse: Float? = courseDeg.value

    Box(Modifier.fillMaxSize().clipToBounds()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        MapTopBar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            showSearch = showSearch,
            searchResultsWp = searchResultsWp,
            searchResultsRt = searchResultsRt,
            onFocus = { p -> focusTarget = p; follow.value = false }
        )
        MapControls(
            onLocate = { follow.value = true; pos.value?.let { mapView.controller.animateTo(it); mapView.invalidate() } },
            onZoomIn = { mapView.controller.zoomIn(); mapView.invalidate() },
            onZoomOut = { mapView.controller.zoomOut(); mapView.invalidate() },
            onLayers = { showLayers = !showLayers },
            onPlanRoute = {
                if (planningMode && routePlanner.value.size >= 2) {
                    val id = UUID.randomUUID().toString()
                    val name = "Rota " + SimpleDateFormat("dd.MM HH:mm", Locale("tr")).format(Date())
                    val pts = routePlanner.value
                    scope.launch {
                        routeDao.insertRoute(RouteEntity(id, name, System.currentTimeMillis(), "#38BDF8"))
                        pts.forEachIndexed { idx, p ->
                            routeDao.insertPoint(RoutePointEntity(0, id, p.latitude, p.longitude, idx, null))
                        }
                    }
                    routePlanner.value = emptyList(); planningMode = false
                } else {
                    planningMode = !planningMode
                    if (!planningMode) routePlanner.value = emptyList()
                }
            },
            planningMode = planningMode,
            onCancelPlan = { planningMode = false; routePlanner.value = emptyList() }
        )
        BottomDataBar(displaySpeed, displayCourse, speedUnit)
        ScaleBar()
        RecordButton(recording, onRecordToggle)
        CatchButton()
        if (showLayers) LayersPanel(currentStyle = mapStyle, onSelectStyle = { mapStyle = it },
            onDownload = { showDownload = true; showLayers = false },
            onClose = { showLayers = false })
        if (showDownload) DownloadPanel(mapView) { showDownload = false }
        if (planningMode) RoutePlanOverlay(routePlanner.value)
        NavBadgeColumn()
    }
}

@Composable
private fun BoxScope.LayersPanel(
    currentStyle: MapStyle,
    onSelectStyle: (MapStyle) -> Unit,
    onDownload: () -> Unit,
    onClose: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable { onClose() }) {
        Column(Modifier.align(Alignment.Center).width(320.dp)
            .clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(20.dp)) {
            Text("KATMANLAR", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
            Spacer(Modifier.height(14.dp))
            Text("HARITA KAYNAGI", color = SirenTextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            MapStyle.values().forEach { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (currentStyle == s) SirenPrimary else SirenPanel)
                    .clickable { onSelectStyle(s) }
                    .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Map, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(s.label, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SirenGreen)
                .clickable { onDownload() }.padding(12.dp), contentAlignment = Alignment.Center) {
                Text("CEVRIMDISI HARITA INDIR", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BoxScope.MapTopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    showSearch: Boolean,
    searchResultsWp: List<WaypointEntity>,
    searchResultsRt: List<RouteEntity>,
    onFocus: (GeoPoint) -> Unit
) {
    Row(Modifier.align(Alignment.TopStart).padding(12.dp), verticalAlignment = Alignment.Top) {
        DarkIconButton(Icons.Filled.Menu)
        Spacer(Modifier.width(10.dp))
        Column {
            Row(Modifier.width(280.dp).clip(RoundedCornerShape(10.dp)).background(SirenPanel)
                .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, null, tint = SirenTextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = SirenTextPrimary, fontSize = 13.sp)
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Anchor, "SIREN", tint = Color.Black, modifier = Modifier.size(26.dp))
            }
            }
            if (showSearch && (searchResultsWp.isNotEmpty() || searchResultsRt.isNotEmpty())) {
                Spacer(Modifier.height(6.dp))
                Column(Modifier.width(280.dp).clip(RoundedCornerShape(10.dp)).background(SirenCard).padding(6.dp)) {
                    searchResultsWp.forEach { wp ->
                        Row(Modifier.fillMaxWidth().clickable { onFocus(GeoPoint(wp.lat, wp.lon)); onSearchChange("") }
                            .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Place, null, tint = SirenTrackYellow, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(wp.name, color = SirenTextPrimary, fontSize = 12.sp)
                        }
                    }
                    searchResultsRt.forEach { r ->
                        Row(Modifier.fillMaxWidth().clickable { SirenNav.followRoute(r); onSearchChange("") }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AltRoute, null, tint = SirenRouteBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(r.name, color = SirenTextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
    Box(Modifier.align(Alignment.TopCenter).padding(top = 12.dp).width(320.dp).clip(RoundedCornerShape(10.dp))
        .background(SirenPanel).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text("B   330   K   30   D", color = SirenTextSecondary, fontSize = 11.sp, letterSpacing = 2.sp)
    }
    Box(Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(10.dp))
        .background(SirenPanel).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text("3D", color = SirenTextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BoxScope.RoutePlanOverlay(points: List<GeoPoint>) {
    val nm = routeTotalNm(points)
    val etaMin = if (nm > 0) (nm / 5.0) * 60.0 else 0.0
    Box(Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
        .clip(RoundedCornerShape(12.dp)).background(SirenPrimary.copy(alpha = 0.95f))
        .padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ROTA PLANLA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.width(16.dp))
            Text("${points.size} nokta", color = Color.White, fontSize = 12.sp)
            Spacer(Modifier.width(16.dp))
            Text("%.1f nm".format(nm), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            Text("~%.0f dk".format(etaMin), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BoxScope.RecordButton(recording: MutableState<Boolean>, onClick: () -> Unit) {
    Box(Modifier.align(Alignment.BottomEnd).padding(16.dp).size(56.dp)
        .clip(CircleShape).background(if (recording.value) SirenRed else SirenPrimary)
        .clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(if (recording.value) Icons.Filled.Stop else Icons.Filled.FiberManualRecord, "Seyir kaydi", tint = Color.White)
    }
}

@Composable
private fun BoxScope.MapControls(
    onLocate: () -> Unit, onZoomIn: () -> Unit, onZoomOut: () -> Unit,
    onLayers: () -> Unit, onPlanRoute: () -> Unit, planningMode: Boolean, onCancelPlan: () -> Unit
) {
    Column(Modifier.align(Alignment.CenterStart).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DarkIconButton(Icons.Filled.MyLocation, onLocate)
        DarkIconButton(Icons.Filled.Add, onZoomIn)
        DarkIconButton(Icons.Filled.Remove, onZoomOut)
        DarkIconButton(Icons.Filled.Layers, onLayers)
        DarkIconButton(Icons.Filled.AltRoute, onPlanRoute,
            tint = if (planningMode) Color.White else SirenTextPrimary,
            bg = if (planningMode) SirenRouteBlue else SirenPanel)
        if (planningMode) {
            DarkIconButton(Icons.Filled.Close, onCancelPlan, tint = Color.White, bg = SirenRed)
        }
    }
}

@Composable
private fun DarkIconButton(icon: ImageVector, onClick: () -> Unit = {},
    tint: Color = SirenTextPrimary, bg: Color = SirenPanel) {
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(bg).clickable { onClick() },
        contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun BoxScope.BottomDataBar(speedVal: Float?, courseVal: Float?, unit: SpeedUnit) {
    Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).clip(RoundedCornerShape(14.dp))
        .background(SirenPanel.copy(alpha = 0.95f)).padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
        DataCell("HIZ", speedVal?.let { "%.1f".format(it) } ?: "--", unit.key)
        DataCell("COG", courseVal?.let { "%.0f".format(it) } ?: "--", "T")
        DataCell("DERINLIK", "42.7", "m")
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
fun RightPanel(modifier: Modifier = Modifier, pos: MutableState<GeoPoint?>, speedKts: MutableState<Float?>, courseDeg: MutableState<Float?>, onSettings: () -> Unit = {}) {
    Column(modifier.background(SirenBackground).padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TelemetryCard(pos, speedKts, courseDeg, onSettings)
        WeatherCard(pos)
        SonarCard()
    }
}

@Composable
private fun TelemetryCard(pos: MutableState<GeoPoint?>, speedKts: MutableState<Float?>, courseDeg: MutableState<Float?>, onSettings: () -> Unit = {}) {
    val coords = pos.value?.let { formatCoords(it.latitude, it.longitude) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TELEMETRI", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = SirenTextPrimary)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Settings, null, tint = SirenTextSecondary, modifier = Modifier.size(16.dp).clickable { onSettings() })
        }
        Spacer(Modifier.height(10.dp))
        TelemetryRow("HIZ", speedKts.value?.let { "%.1f".format(it) } ?: "--", "kts")
        DividerLine()
        TelemetryRow("YON", courseDeg.value?.let { "%.0f".format(it) } ?: "--", "T")
        DividerLine()
        TelemetryRow("DERINLIK", "42.7", "m")
        DividerLine()
        Spacer(Modifier.height(10.dp))
        Text("KOORDINATLAR", fontSize = 10.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(coords?.first ?: "GPS bekleniyor...", fontSize = 13.sp, color = SirenTextPrimary)
        Text(coords?.second ?: "", fontSize = 13.sp, color = SirenTextPrimary)
    }
}

@Composable
private fun WeatherCard(pos: MutableState<GeoPoint?>) {
    var data by remember { mutableStateOf<WeatherData?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pos.value) {
        pos.value?.let { p ->
            val now = System.currentTimeMillis()
            val needRefresh = data == null || (now - data!!.fetchedAt) > 30 * 60 * 1000L
            if (needRefresh) {
                data = Weather.fetch(p.latitude, p.longitude)
                if (data == null) errorMsg = "Hava verisi alinamadi"
            }
        }
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Air, null, tint = SirenPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("HAVA DURUMU", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = SirenTextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        if (data == null && errorMsg == null) Text("Veri yukleniyor...", color = SirenTextSecondary, fontSize = 12.sp)
        errorMsg?.let { Text(it, color = SirenRed, fontSize = 12.sp) }
        data?.let { w ->
            Text("RUZGAR", fontSize = 10.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.0f".format(w.windSpeedKnots), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("kts", fontSize = 11.sp, color = SirenTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                Spacer(Modifier.width(12.dp))
                Text("%.0f°".format(w.windDirectionDeg), fontSize = 14.sp, color = SirenTextPrimary, modifier = Modifier.padding(bottom = 6.dp))
            }
            DividerLine()
            Text("DALGA", fontSize = 10.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(w.waveHeightMeters?.let { "%.1f".format(it) } ?: "--", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("m", fontSize = 11.sp, color = SirenTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text("Open-Meteo (canli)", color = SirenTextSecondary, fontSize = 9.sp)
        }
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
private fun DividerLine() { Box(Modifier.fillMaxWidth().height(1.dp).background(SirenDivider)) }

@Composable
private fun SonarCard() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(16.dp)) {
        Text("SONAR", fontSize = 12.sp, letterSpacing = 1.sp, color = SirenTextSecondary)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("42.7", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.width(4.dp))
            Text("m", fontSize = 12.sp, color = SirenTextSecondary, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(10.dp)).background(
            Brush.verticalGradient(listOf(Color(0xFF061225), Color(0xFF0B2A55), Color(0xFFB4530A), Color(0xFFE2542A)))))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.size(44.dp).clip(CircleShape).background(SirenPrimary).align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Pause, null, tint = Color.White)
        }
    }
}
