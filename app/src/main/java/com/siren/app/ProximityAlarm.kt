package com.siren.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint

object ProximityAlarm {
    private const val CHANNEL_ID = "siren_proximity"
    private const val NOTIF_ID_BASE = 9100
    const val RADIUS_METERS = 200.0

    private var lastNotified: MutableMap<String, Long> = mutableMapOf()
    private val COOLDOWN_MS = 60_000L

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Waypoint Yaklasikligi",
                NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Bir waypoint'e 200m icinde yaklastiginda uyari"
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
    }

    suspend fun monitor(ctx: Context, posState: MutableState<GeoPoint?>, waypoints: suspend () -> List<WaypointEntity>) {
        ensureChannel(ctx)
        while (true) {
            val p = posState.value
            if (p != null) {
                val wps = waypoints()
                for (wp in wps) {
                    val d = distanceM(p.latitude, p.longitude, wp.lat, wp.lon)
                    if (d < RADIUS_METERS) {
                        val now = System.currentTimeMillis()
                        val last = lastNotified[wp.id] ?: 0L
                        if (now - last > COOLDOWN_MS) {
                            fire(ctx, wp.name, d)
                            lastNotified[wp.id] = now
                        }
                    }
                }
            }
            delay(5000)
        }
    }

    private fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun fire(ctx: Context, wpName: String, distM: Double) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Waypoint yakinda!")
            .setContentText("$wpName - %.0fm kaldi".format(distM))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(NOTIF_ID_BASE + wpName.hashCode() % 100, n) }
    }
}
