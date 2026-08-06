package com.siren.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SonarScreen() {
    var isRunning by remember { mutableStateOf(true) }
    var currentDepth by remember { mutableStateOf(42.7f) }
    var history by remember { mutableStateOf(listOf(42.7f)) }

    val infiniteTransition = rememberInfiniteTransition(label = "sonar")
    val pingProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ping"
    )

    LaunchedEffect(isRunning) {
        while (isRunning) {
            val noise = Random.nextFloat() * 2f - 1f
            currentDepth = (42f + noise + sin(System.currentTimeMillis() / 2000.0) * 3).toFloat()
            history = (history + currentDepth).takeLast(30)
            delay(500)
        }
    }

    Column(Modifier.fillMaxSize().background(SirenBackground).padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Radar, null, tint = SirenPrimary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text("SONAR", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .background(if (isRunning) SirenRed else SirenPrimary)
                    .clickable { isRunning = !isRunning },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White)
            }
        }
        Spacer(Modifier.height(20.dp))

        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(24.dp),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isRunning) "CANLI" else "DURAKLATILDI",
                    color = if (isRunning) SirenGreen else SirenTextSecondary,
                    fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("%.1f".format(currentDepth), fontSize = 64.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("m", fontSize = 20.sp, color = SirenTextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp))
            .background(SirenCard), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val maxRadius = min(cx, cy)
                drawCircle(Color(0xFF1F6FEB), radius = 8f, center = Offset(cx, cy))
                for (i in 1..4) {
                    drawCircle(Color(0xFF1D2A40), radius = maxRadius * i / 4,
                        center = Offset(cx, cy), style = Stroke(1f))
                }
                val pingRadius = maxRadius * pingProgress
                val alpha = (1f - pingProgress) * 0.8f
                drawCircle(Color(0xFF38BDF8).copy(alpha = alpha), radius = pingRadius,
                    center = Offset(cx, cy), style = Stroke(3f))
                val pingProgress2 = (pingProgress + 0.5f) % 1f
                val pingRadius2 = maxRadius * pingProgress2
                val alpha2 = (1f - pingProgress2) * 0.6f
                drawCircle(Color(0xFF1F6FEB).copy(alpha = alpha2), radius = pingRadius2,
                    center = Offset(cx, cy), style = Stroke(2f))
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp))
            .background(SirenCard).padding(16.dp)) {
            Column {
                Text("SON 30 ÖLÇÜM", color = SirenTextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                if (history.size > 1) {
                    val minD = history.minOrNull() ?: 40f
                    val maxD = history.maxOrNull() ?: 50f
                    val range = (maxD - minD).coerceAtLeast(1f)
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stepX = size.width / (history.size - 1).coerceAtLeast(1)
                            for (i in 0 until history.size - 1) {
                                val x1 = stepX * i
                                val x2 = stepX * (i + 1)
                                val y1 = size.height - ((history[i] - minD) / range * size.height)
                                val y2 = size.height - ((history[i + 1] - minD) / range * size.height)
                                drawLine(Color(0xFFF2C94C), Offset(x1, y1), Offset(x2, y2), strokeWidth = 3f)
                            }
                        }
                    }
                } else {
                    Text("Veri bekleniyor...", color = SirenTextSecondary, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Not: Demo verisi. Gercek sonar alicisi gelecek surumlerde.",
            color = SirenTextSecondary, fontSize = 11.sp)
    }
}
