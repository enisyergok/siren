package com.siren.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class AisVessel(
    val mmsi: String,
    val name: String,
    val type: String,
    val sog: Double,
    val cog: Double,
    val distanceNm: Double,
    val bearing: Double,
    val status: String
)

private fun bearingBetween(a: GeoPoint, b: GeoPoint): Double {
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    val brng = Math.toDegrees(atan2(y, x))
    return (brng + 360) % 360
}

private fun generateVessels(center: GeoPoint): List<AisVessel> {
    val names = listOf("MV POYRAZ", "TANKER EFE", "FERRY DENIZ", "CARGO AYTEK",
        "YAT MELTEM", "BALIKCI-42", "TUG BOGAZ", "LNG KAPTAN", "RO-RO KARADENIZ", "MOTOR SAHIN")
    val types = listOf("Kargo", "Tanker", "Yolcu", "Balikci", "Yat", "Romorkor", "LNG", "RO-RO")
    val statuses = listOf("YOLDA", "DEMIRDE", "MANEVRADA", "YOLDA", "YOLDA")
    val rng = kotlin.random.Random(center.latitude.toInt() + center.longitude.toInt())
    val list = mutableListOf<AisVessel>()
    for (i in 0 until 10) {
        val dKm = 0.5 + rng.nextDouble() * 8.0
        val brg = rng.nextDouble() * 360.0
        val dLat = (dKm / 111.0) * cos(Math.toRadians(brg))
        val dLon = (dKm / (111.0 * cos(Math.toRadians(center.latitude)))) * sin(Math.toRadians(brg))
        val p = GeoPoint(center.latitude + dLat, center.longitude + dLon)
        val cog = rng.nextDouble() * 360.0
        list.add(AisVessel(
            mmsi = "271" + (1000 + i),
            name = names[i % names.size],
            type = types[rng.nextInt(types.size)],
            sog = 2.0 + rng.nextDouble() * 14.0,
            cog = cog,
            distanceNm = dKm * 0.539957,
            bearing = bearingBetween(center, p),
            status = if (rng.nextDouble() < 0.2) statuses[1] else statuses[0]
        ))
    }
    return list.sortedBy { it.distanceNm }
}

@Composable
fun AisScreen(pos: MutableState<GeoPoint?>) {
    var vessels by remember { mutableStateOf<List<AisVessel>>(emptyList()) }
    var lastRefresh by remember { mutableStateOf(0L) }

    LaunchedEffect(pos.value) {
        while (true) {
            pos.value?.let { p ->
                vessels = generateVessels(p)
                lastRefresh = System.currentTimeMillis()
            }
            delay(5000)
        }
    }

    Column(Modifier.fillMaxSize().background(SirenBackground).padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DirectionsBoat, null, tint = SirenPrimary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("AIS TRAFIGI", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
                Text("Simule edilmis yakındaki gemiler (5sn guncelleme)",
                    color = SirenTextSecondary, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(16.dp))

        if (pos.value == null) {
            Text("GPS bekleniyor...", color = SirenTextSecondary)
            return@Column
        }

        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SirenCard).padding(14.dp)) {
            Text("YAKINDAKI GEMI", color = SirenTextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text("${vessels.size}", color = SirenTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))

        vessels.forEach { v ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(12.dp))
                .background(SirenCard).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DirectionsBoat, null,
                    tint = if (v.distanceNm < 1.0) SirenRed else SirenGreen,
                    modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(v.name, color = SirenTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row {
                        Text(v.type, color = SirenTextSecondary, fontSize = 11.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("MMSI ${v.mmsi}", color = SirenTextSecondary, fontSize = 11.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("%.1f nm".format(v.distanceNm),
                        color = if (v.distanceNm < 1.0) SirenRed else SirenTextPrimary,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("%.0f kts · %.0f°".format(v.sog, v.cog),
                        color = SirenTextSecondary, fontSize = 11.sp)
                    Text(v.status,
                        color = if (v.status == "DEMIRDE") SirenTrackYellow else SirenGreen,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = SirenTrackYellow, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Demo modu - gercek AIS alicisi sonraki surumde",
                color = SirenTextSecondary, fontSize = 10.sp)
        }
    }
}
