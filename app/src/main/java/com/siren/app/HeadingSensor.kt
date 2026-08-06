package com.siren.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.osmdroid.views.MapView

@Composable
fun HeadingSensor() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sm.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        val listener = object : SensorEventListener {
            val m = FloatArray(9)
            val o = FloatArray(3)
            override fun onSensorChanged(e: SensorEvent?) {
                if (e == null) return
                if (e.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromRotationVector(m, e.values)
                    SensorManager.getOrientation(m, o)
                    val az = (Math.toDegrees(o[0].toDouble()) + 360.0) % 360.0
                    SirenNav.heading.value = az.toFloat()
                } else {
                    SirenNav.heading.value = (e.values[0] + 360f) % 360f
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    }
}

@Composable
fun BoxScope.HeadingUpController(mapView: MapView) {
    val up by SirenNav.headingUp
    val hdg by SirenNav.heading

    LaunchedEffect(up, hdg) {
        mapView.setMapOrientation(if (up) (hdg ?: 0f) else 0f)
    }

    // Kompas gulu
    Box(
        Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 120.dp)
            .size(64.dp).clip(CircleShape).background(SirenPanel.copy(alpha = 0.9f))
            .clickable { SirenNav.headingUp.value = !up },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(56.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.width / 2 - 4
            drawCircle(Color(0xFF3A4A63), radius = r, center = c, style = Stroke(2f))
            val rot = Math.toRadians((hdg ?: 0f).toDouble())
            val nx = c.x - (sin(rot) * r).toFloat()
            val ny = c.y + (cos(rot) * r).toFloat()
            drawLine(Color(0xFFE5484D), c, Offset(nx, ny), strokeWidth = 3f)
            drawCircle(Color.White, radius = 3f, center = c)
        }
        Text(if (up) "HDG" else "N", color = SirenTrackYellow, fontSize = 9.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 40.dp))
    }
}
