package com.siren.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun AdaptiveRoot() {
    val cfg = LocalConfiguration.current
    if (cfg.screenWidthDp < 600) MobileRoot() else SirenRoot()
}

private fun pal(night: Boolean) = if (night)
    listOf(Color(0xFF050505), Color(0xFF1A1208), Color(0xFFFFB000), Color(0xFFFFE0B0), Color(0xFF806040))
else
    listOf(SirenBackground, SirenPanel, SirenPrimary, SirenTextPrimary, SirenTextSecondary)

@Composable
fun MobileRoot() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    LaunchedEffect(Unit) {
        SirenNav.routeDao = db.routeDao()
        SirenNav.catchDao = db.catchDao()
    }

    val pos = remember { mutableStateOf<GeoPoint?>(null) }
    val speed = remember { mutableStateOf<Float?>(null) }
    val course = remember { mutableStateOf<Float?>(null) }
    var tab by remember { mutableStateOf(0) }
    var subPage by remember { mutableStateOf("") }
    val night by SirenNav.nightMode
    val C = pal(night)

    LaunchedEffect(Unit) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(l: Location) {
                val p = GeoPoint(l.latitude, l.longitude)
                pos.value = p
                speed.value = l.speed * 1.94384f
                course.value = l.bearing
                SirenNav.onLocation(p, l.speed * 1.94384f, l.bearing)
            }
            override fun onStatusChanged(s: String?, i: Int, b: Bundle?) {}
            override fun onProviderEnabled(s: String) {}
            override fun onProviderDisabled(s: String) {}
        }
        runCatching {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener)
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 0f, listener)
        }
    }

    var autoNight by remember { mutableStateOf(true) }
    LaunchedEffect(autoNight) {
        while (autoNight) {
            val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            SirenNav.nightMode.value = (h >= 19 || h < 6)
            delay(60000)
        }
    }

    Box(Modifier.fillMaxSize().background(C[0])) {
        when (tab) {
            0 -> MobileMap(pos, speed, course, C)
            1 -> RoutesScreen(db.routeDao())
            2 -> FishTabs(pos, db.catchDao())
            3 -> WeatherScreen(pos)
            4 -> MenuPage(db, pos, C) { subPage = it }
        }

        if (subPage.isNotEmpty()) {
            SubPage(subPage, db, pos, C) { subPage = "" }
        }

        Row(
            Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .background(C[1].copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val acc = SirenNav.accuracy.value
            Box(Modifier.clip(RoundedCornerShape(6.dp))
                .background(if (acc != null && acc < 15) SirenGreen else if (acc != null && acc < 40) SirenTrackYellow else SirenRed)
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("GPS ${acc?.toInt() ?: "--"}m", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Text(SimpleDateFormat("HH:mm", Locale("tr")).format(Date()), color = C[3], fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            val alarms = mobileAlarms(pos)
            Box(Modifier.clickable { subPage = "ALARMLAR" }.padding(4.dp)) {
                Text(if (alarms.isEmpty()) "🔕" else "🔔(${alarms.size})", fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clickable {
                autoNight = false
                SirenNav.nightMode.value = !night
            }.padding(4.dp)) {
                Text(if (night) "🌙" else "☀️", fontSize = 14.sp)
            }
        }

        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(C[1]).height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab("🗺️", "Harita", tab == 0, C) { tab = 0 }
            NavTab("➿", "Rotalar", tab == 1, C) { tab = 1 }
            NavTab("🐟", "Balık", tab == 2, C) { tab = 2 }
            NavTab("🌬️", "Hava", tab == 3, C) { tab = 3 }
            NavTab("☰", "Menü", tab == 4, C) { tab = 4 }
        }
    }
}

private fun mobileAlarms(pos: androidx.compose.runtime.State<GeoPoint?>): List<String> {
    val out = mutableListOf<String>()
    if (SirenNav.mob.value != null) out.add("KRITIK: MOB aktif")
    val ar = SirenNav.activeRoute.value
    val p = pos.value
    if (ar != null && p != null && ar.leg < ar.points.size) {
        val xte = NavMath.xteNm(p, ar.points[maxOf(0, ar.leg - 1)], ar.points[ar.leg])
        if (abs(xte) > 0.05) out.add("UYARI: Rota sapmasi %.0fm".format(abs(xte) * 1852))
    }
    val ap = SirenNav.anchorPos.value
    if (ap != null && p != null) {
        val d = haversineNm(ap.latitude, ap.longitude, p.latitude, p.longitude) * 1852
        if (d > SirenNav.anchorRadiusM.value) out.add("UYARI: Demir tarandi")
    }
    return out
}

@Composable
private fun NavTab(icon: String, label: String, active: Boolean, C: List<Color>, onClick: () -> Unit) {
    Column(
        Modifier.weight(1f).fillMaxWidth().clickable { onClick() }.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 16.sp)
        Text(label, color = if (active) C[2] else C[4], fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

class BoatOverlay : Overlay() {
    override fun draw(c: Canvas, mv: MapView, shadow: Boolean) {
        if (shadow) return
        val p = SirenNav.pos.value ?: return
        val pt = mv.projection.toPixels(p, null)
        val hdg = Math.toRadians((SirenNav.course.value ?: 0f).toDouble())
        val paint = Paint().apply { color = android.graphics.Color.parseColor("#2E6BFF"); isAntiAlias = true }
        val s = 22f
        val x = pt.x.toFloat(); val y = pt.y.toFloat()
        val tipX = x + (Math.sin(hdg) * s).toFloat(); val tipY = y - (Math.cos(hdg) * s).toFloat()
        val lX = x + (Math.sin(hdg + 2.5) * s * 0.7).toFloat(); val lY = y - (Math.cos(hdg + 2.5) * s * 0.7).toFloat()
        val rX = x + (Math.sin(hdg - 2.5) * s * 0.7).toFloat(); val rY = y - (Math.cos(hdg - 2.5) * s * 0.7).toFloat()
        val path = android.graphics.Path()
        path.moveTo(tipX, tipY); path.lineTo(lX, lY); path.lineTo(rX, rY); path.close()
        c.drawPath(path, paint)
    }
}

@Composable
private fun MobileMap(pos: androidx.compose.runtime.State<GeoPoint?>,
                      speed: androidx.compose.runtime.State<Float?>,
                      course: androidx.compose.runtime.State<Float?>,
                      C: List<Color>) {
    val context = LocalContext.current
    var locked by remember { mutableStateOf(true) }
    // Harita kaynagi sabit
    val cursor = remember { mutableStateOf<GeoPoint?>(null) }
    var showCatchDlg by remember { mutableStateOf(false) }
    var infoTxt by remember { mutableStateOf("") }
    val tripPoints = remember { mutableListOf<GeoPoint>() }
    var recording by remember { mutableStateOf(false) }
    val tripLine = remember { Polyline() }
    val measLine = remember { Polyline() }

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            overlays.add(BoatOverlay())
        }
    }

    LaunchedEffect(pos.value) {
        pos.value?.let { p ->
            if (locked) mapView.controller.animateTo(p)
            if (recording) {
                tripPoints.add(p)
                tripLine.setPoints(tripPoints.toList())
                tripLine.outlinePaint.color = android.graphics.Color.parseColor("#2ECC71")
                tripLine.outlinePaint.strokeWidth = 6f
                if (!mapView.overlays.contains(tripLine)) mapView.overlays.add(tripLine)
                mapView.invalidate()
            }
        }
    }

    LaunchedEffect(Unit) {
        mapView.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = true
            override fun longPressHelper(p: GeoPoint?): Boolean {
                cursor.value = p
                return true
            }
        }))
        mapView.overlays.add(BoatOverlay())
    }

    LaunchedEffect(SirenNav.measureA.value, SirenNav.measureB.value) {
        val a = SirenNav.measureA.value; val b = SirenNav.measureB.value
        if (a != null && b != null) {
            measLine.setPoints(listOf(a, b))
            measLine.outlinePaint.color = android.graphics.Color.CYAN
            measLine.outlinePaint.strokeWidth = 5f
            if (!mapView.overlays.contains(measLine)) mapView.overlays.add(measLine)
            mapView.invalidate()
        }
    }

    Box(Modifier.fillMaxSize().padding(bottom = 56.dp)) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        CatchMarkersLayer(mapView)
        CurrentArrowsController(mapView)
        HeadingUpController(mapView)
        ToolsColumn(mapView)
        RegionFishPanel()

        Column(
            Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RailBtn("🎯") { locked = true; pos.value?.let { mapView.controller.animateTo(it) } }
            RailBtn("＋") { mapView.controller.zoomIn() }
            RailBtn("－") { mapView.controller.zoomOut() }
            RailBtn("🗂️") {
                // Tek kaynak kullaniliyor
                // Tek harita kaynagi (OSM)
                    
                mapView.setTileSource(TileSourceFactory.MAPNIK)
                mapView.invalidate()
            }
        }

        Row(
            Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(SirenRed)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = {
                        SirenNav.mob.value = SirenNav.pos.value
                    })
                }, contentAlignment = Alignment.Center) { Text("SOS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Box(Modifier.size(52.dp).clip(CircleShape).background(SirenGreen)
                .clickable { showCatchDlg = true }, contentAlignment = Alignment.Center) { Text("🎣", fontSize = 20.sp) }
            Box(Modifier.size(52.dp).clip(CircleShape).background(if (recording) SirenRed else SirenPrimary)
                .clickable { recording = !recording }, contentAlignment = Alignment.Center) { Text("●", color = Color.White, fontSize = 20.sp) }
        }

        if (cursor.value != null) {
            val cp = cursor.value!!
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp)).background(C[1].copy(alpha = 0.95f)).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CursorBtn("🚩") {
                    val mk = Marker(mapView).apply { position = cp; title = "Isaret" }
                    mapView.overlays.add(mk); mapView.invalidate()
                    cursor.value = null
                }
                CursorBtn("➿") {
                    val p0 = SirenNav.pos.value
                    if (p0 != null) SirenNav.activeRoute.value = ActiveRouteInfo("anlik", "Anlik Rota", listOf(p0, cp), 1)
                    cursor.value = null
                }
                CursorBtn("📏") {
                    val p0 = SirenNav.pos.value
                    if (p0 != null) {
                        SirenNav.measureA.value = p0
                        SirenNav.measureB.value = cp
                        infoTxt = "%.2f nm · %.0f°".format(
                            haversineNm(p0.latitude, p0.longitude, cp.latitude, cp.longitude),
                            NavMath.bearingDeg(p0, cp))
                    }
                    cursor.value = null
                }
                CursorBtn("🐟") { showCatchDlg = true; cursor.value = null }
                CursorBtn("ℹ️") {
                    infoTxt = "%.4f, %.4f".format(cp.latitude, cp.longitude)
                    cursor.value = null
                }
                CursorBtn("✕") {
                    cursor.value = null
                    SirenNav.measureA.value = null; SirenNav.measureB.value = null
                    mapView.overlays.remove(measLine); infoTxt = ""
                    mapView.invalidate()
                }
            }
        }

        if (infoTxt.isNotEmpty()) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 40.dp)
                .clip(RoundedCornerShape(8.dp)).background(C[1]).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(infoTxt, color = C[2], fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        InstrumentBar(pos, speed, course, C)
    }

    if (showCatchDlg) CatchDialog(onDismiss = { showCatchDlg = false })
}

@Composable
private fun RailBtn(icon: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(SirenPanel.copy(alpha = 0.9f))
        .clickable { onClick() }, contentAlignment = Alignment.Center) { Text(icon, fontSize = 16.sp) }
}

@Composable
private fun CursorBtn(icon: String, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(SirenPrimary.copy(alpha = 0.3f))
        .clickable { onClick() }, contentAlignment = Alignment.Center) { Text(icon, fontSize = 16.sp) }
}

@Composable
private fun BoxScope.InstrumentBar(pos: androidx.compose.runtime.State<GeoPoint?>,
                                   speed: androidx.compose.runtime.State<Float?>,
                                   course: androidx.compose.runtime.State<Float?>,
                                   C: List<Color>) {
    var cells by remember { mutableStateOf(listOf("SOG", "COG", "XTE", "VMG", "TRIP", "ETA")) }
    var editSlot by remember { mutableStateOf(-1) }
    val pool = listOf("SOG", "COG", "HDG", "XTE", "VMG", "BTW", "DTW", "TTG", "ETA", "TRIP", "MAX", "MOD")

    val p = pos.value
    val ar = SirenNav.activeRoute.value
    var xte = 0.0; var vmg = 0.0; var btw = 0.0; var dtw = 0.0
    if (ar != null && p != null && ar.leg < ar.points.size) {
        val from = ar.points[maxOf(0, ar.leg - 1)]; val to = ar.points[ar.leg]
        xte = NavMath.xteNm(p, from, to)
        btw = NavMath.bearingDeg(p, to)
        dtw = haversineNm(p.latitude, p.longitude, to.latitude, to.longitude)
        vmg = NavMath.vmgKts((speed.value ?: 0f).toDouble(), (course.value ?: 0f).toDouble(), btw)
    }

    fun valOf(key: String): Pair<String, Color> = when (key) {
        "SOG" -> Pair("%.1f".format(speed.value ?: 0f), C[3])
        "COG" -> Pair("%.0f°".format(course.value ?: 0f), C[3])
        "HDG" -> Pair("%.0f°".format(SirenNav.heading.value ?: 0f), C[3])
        "XTE" -> if (abs(xte) > 0.05) Pair("%.0fm".format(abs(xte) * 1852), SirenRed) else Pair("%.0fm".format(abs(xte) * 1852), SirenGreen)
        "VMG" -> Pair("%.1f".format(vmg), if (vmg > 0.5) SirenGreen else SirenTrackYellow)
        "BTW" -> Pair("%.0f°".format(btw), C[3])
        "DTW" -> Pair("%.2f".format(dtw), C[3])
        "TTG" -> if (vmg > 0.3) Pair("%.0fdk".format(dtw / vmg * 60), C[3]) else Pair("--", C[3])
        "ETA" -> {
            val t = if (vmg > 0.3) System.currentTimeMillis() + (dtw / vmg * 3600000).toLong() else 0L
            if (t > 0) Pair(SimpleDateFormat("HH:mm", Locale("tr")).format(Date(t)), C[3]) else Pair("--", C[3])
        }
        "TRIP" -> Pair("%.1f".format(SirenNav.tripDistNm.value), C[3])
        "MAX" -> Pair("%.1f".format(SirenNav.maxSpeed.value), C[3])
        else -> Pair(if (ar != null) "SEYIR" else "BOS", C[2])
    }

    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().background(C[1].copy(alpha = 0.92f)).padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            cells.forEachIndexed { i, key ->
                val v = valOf(key)
                Column(
                    Modifier.weight(1f).clickable { }
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = { editSlot = i })
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(key, color = C[4], fontSize = 8.sp, letterSpacing = 1.sp)
                    Text(v.first, color = v.second, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (editSlot >= 0) {
            Row(
                Modifier.fillMaxWidth().background(C[1]).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                pool.forEach { k ->
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(SirenPrimary.copy(alpha = 0.3f))
                        .clickable {
                            cells = cells.toMutableList().also { it[editSlot] = k }
                            editSlot = -1
                        }.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text(k, color = C[3], fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FishTabs(pos: androidx.compose.runtime.State<GeoPoint?>, dao: CatchDao) {
    var sub by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(bottom = 56.dp)) {
        Row(Modifier.fillMaxWidth().background(SirenPanel).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Günlük", "Analiz", "Bağlar").forEachIndexed { i, t ->
                Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (sub == i) SirenPrimary else Color.Transparent)
                    .clickable { sub = i }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                    Text(t, color = if (sub == i) Color.White else SirenTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        when (sub) {
            0 -> CatchLogScreen(dao, pos as androidx.compose.runtime.MutableState<GeoPoint?>)
            1 -> AdvancedFishingScreen(pos as androidx.compose.runtime.MutableState<GeoPoint?>, dao)
            2 -> KnotGuideScreen()
        }
    }
}

@Composable
private fun MenuPage(db: AppDatabase, pos: androidx.compose.runtime.State<GeoPoint?>, C: List<Color>, open: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(bottom = 56.dp).padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("MENÜ", color = C[3], fontSize = 20.sp, fontWeight = FontWeight.Bold)
        MenuBtn("📍 Waypoint'ler") { open("WP") }
        MenuBtn("〰️ İzler") { open("TRACKS") }
        MenuBtn("🚢 AIS Trafiği") { open("AIS") }
        MenuBtn("🌊 Gelgit ve Akıntı") { open("TIDE") }
        MenuBtn("📡 Sonar") { open("SONAR") }
        MenuBtn("🚨 Alarmlar") { open("ALARMLAR") }
    }
}

@Composable
private fun MenuBtn(label: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SirenCard)
        .clickable { onClick() }.padding(16.dp)) {
        Text(label, color = SirenTextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SubPage(name: String, db: AppDatabase, pos: androidx.compose.runtime.State<GeoPoint?>, C: List<Color>, back: () -> Unit) {
    Column(Modifier.fillMaxSize().background(C[0]).padding(bottom = 56.dp)) {
        Row(Modifier.fillMaxWidth().background(C[1]).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = C[2], fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { back() }.padding(end = 12.dp))
            Text(name, color = C[3], fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        when (name) {
            "WP" -> WaypointsScreen(db.waypointDao())
            "TRACKS" -> TracksScreen(db.trackDao())
            "AIS" -> AisScreen(pos as androidx.compose.runtime.MutableState<GeoPoint?>)
            "TIDE" -> TideScreen(pos as androidx.compose.runtime.MutableState<GeoPoint?>)
            "SONAR" -> SonarScreen()
            "ALARMLAR" -> Column(Modifier.padding(16.dp)) {
                val alarms = mobileAlarms(pos)
                if (alarms.isEmpty()) Text("Aktif alarm yok", color = C[4])
                alarms.forEach { a ->
                    Text(a, color = if (a.startsWith("KRITIK")) SirenRed else SirenTrackYellow,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
