package com.siren.app

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

data class ActiveRouteInfo(val id: String, val name: String, val points: List<GeoPoint>, val leg: Int)

object SirenNav {
    val pos = mutableStateOf<GeoPoint?>(null)
    val speedKts = mutableStateOf<Float?>(null)
    val accuracy = mutableStateOf<Float?>(null)
    val course = mutableStateOf<Float?>(null)
    val activeRoute = mutableStateOf<ActiveRouteInfo?>(null)
    var routeDao: RouteDao? = null
    var catchDao: CatchDao? = null

    val heading = mutableStateOf<Float?>(null)
    val headingUp = mutableStateOf(false)
    val nightMode = mutableStateOf(false)
    val mob = mutableStateOf<GeoPoint?>(null)
    val anchorPos = mutableStateOf<GeoPoint?>(null)
    val anchorRadiusM = mutableStateOf(0.0)
    val measureA = mutableStateOf<GeoPoint?>(null)
    val measureB = mutableStateOf<GeoPoint?>(null)

    val lastFixTime = mutableStateOf(0L)
    val lastPos = mutableStateOf<GeoPoint?>(null)

    val tripStart = mutableStateOf<Long?>(null)
    val tripDistNm = mutableStateOf(0.0)
    val maxSpeed = mutableStateOf(0f)
    val pressureMsl = mutableStateOf(Double.NaN)
    val pressureHistory = mutableStateOf<List<Pair<Long, Double>>>(emptyList())
    val weatherData = mutableStateOf<WeatherData?>(null)
    private var prevPos: GeoPoint? = null

    fun onLocation(p: GeoPoint, speed: Float?, bearing: Float?) {
        pos.value = p
        speedKts.value = speed
        course.value = bearing
        lastFixTime.value = System.currentTimeMillis()
        lastPos.value = p
        if (speed != null && speed > maxSpeed.value) maxSpeed.value = speed
        prevPos?.let { lp ->
            val d = haversineNm(lp.latitude, lp.longitude, p.latitude, p.longitude)
            if (d in 0.0001..0.5) tripDistNm.value = tripDistNm.value + d
        }
        if (tripStart.value == null && (speed ?: 0f) > 1f) tripStart.value = System.currentTimeMillis()
        prevPos = p
    }

    fun followRoute(r: RouteEntity) {
        val dao = routeDao ?: return
        CoroutineScope(Dispatchers.Main).launch {
            val pts = dao.getPointsForRoute(r.id).map { GeoPoint(it.lat, it.lon) }
            if (pts.isNotEmpty()) activeRoute.value = ActiveRouteInfo(r.id, r.name, pts, 0)
        }
    }
}
