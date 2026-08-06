package com.siren.app

import org.osmdroid.util.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.asin

object NavMath {
    fun bearingDeg(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun destPoint(from: GeoPoint, bearingDeg: Double, distNm: Double): GeoPoint {
        val d = distNm * 1852.0 / 6371000.0
        val br = Math.toRadians(bearingDeg)
        val lat1 = Math.toRadians(from.latitude)
        val lon1 = Math.toRadians(from.longitude)
        val lat2 = asin(sin(lat1) * cos(d) + cos(lat1) * sin(d) * cos(br))
        val lon2 = lon1 + atan2(sin(br) * sin(d) * cos(lat1), cos(d) - sin(lat1) * sin(lat2))
        return GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }

    // XTE: rota cizgisinden sapma (nm), pozitif = sagda
    fun xteNm(pos: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val d13 = haversineNm(a.latitude, a.longitude, pos.latitude, pos.longitude) * 1852.0 / 6371000.0
        val t13 = Math.toRadians(bearingDeg(a, pos))
        val t12 = Math.toRadians(bearingDeg(a, b))
        return asin(sin(d13) * sin(t13 - t12)) * 6371000.0 / 1852.0
    }

    fun vmgKts(speedKts: Double, courseDeg: Double, targetBearingDeg: Double): Double =
        speedKts * cos(Math.toRadians(courseDeg - targetBearingDeg))

    data class Cpa(val cpaNm: Double, val tcpaMin: Double)

    fun cpaTcpa(ownPos: GeoPoint, ownSog: Double, ownCog: Double,
                tgtPos: GeoPoint, tgtSog: Double, tgtCog: Double): Cpa {
        val k = 0.514444
        val ovx = ownSog * k * sin(Math.toRadians(ownCog)); val ovy = ownSog * k * cos(Math.toRadians(ownCog))
        val tvx = tgtSog * k * sin(Math.toRadians(tgtCog)); val tvy = tgtSog * k * cos(Math.toRadians(tgtCog))
        val rvx = tvx - ovx; val rvy = tvy - ovy
        val dx = (tgtPos.longitude - ownPos.longitude) * 111320.0 * cos(Math.toRadians(ownPos.latitude))
        val dy = (tgtPos.latitude - ownPos.latitude) * 110540.0
        val v2 = rvx * rvx + rvy * rvy
        if (v2 < 1e-6) return Cpa(sqrt(dx * dx + dy * dy) / 1852.0, 0.0)
        val t = -(dx * rvx + dy * rvy) / v2
        val cx = dx + rvx * t; val cy = dy + rvy * t
        return Cpa(sqrt(cx * cx + cy * cy) / 1852.0, t / 60.0)
    }
}
