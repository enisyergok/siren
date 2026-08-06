package com.siren.app

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint

data class AisVessel(
    val mmsi: String, val name: String, val type: String,
    val sog: Double, val cog: Double, val distanceNm: Double,
    val bearing: Double, val status: String
)

@Composable
fun AisScreen(pos: MutableState<GeoPoint?>) {
    var vessels by remember { mutableStateOf<List<AisVessel>>(emptyList()) }

    LaunchedEffect(pos.value) {
        while (true) {
            pos.value?.let { center ->
                val rng = kotlin.random.Random(center.latitude.toInt() + center.longitude.toInt())
                val names = listOf("MV POYRAZ", "TANKER EFE", "FERRY DENIZ", "CARGO AYTEK",
                    "YAT MELTEM", "BALIKCI-42", "TUG BOGAZ", "LNG KAPTAN")
                val list = mutableListOf<AisVessel>()
                for (i in 0 until 8) {
                    val dKm = 0.5 + rng.nextDouble() * 8.0
                    val brg = rng.nextDouble() * 360.0
                    list.add(AisVessel(
                        mmsi = "271" + (1000 + i), name = names[i % names.size],
                        type = if (i % 3 == 0) "Tanker" else "Kargo",
                        sog = 4.0 + rng.nextDouble() * 12.0, cog = rng.nextDouble() * 360.0,
                        distanceNm = dKm * 0.539957, bearing = brg,
                        status = if (rng.nextDouble() < 0.2) "DEMIRDE" else "YOLDA"
                    ))
                }
                vessels = list.sortedBy { it.distanceNm }
            }
            delay(5000)
        }
    }

    val own = pos.value
    val ownSog = (SirenNav.speedKts.value ?: 0f).toDouble()
    val ownCog = (SirenNav.course.value ?: 0f).toDouble()
    val risks = if (own != null) vessels.mapNotNull { v ->
        val vp = NavMath.destPoint(own, v.bearing, v.distanceNm)
        val c = NavMath.cpaTcpa(own, ownSog, ownCog, vp, v.sog, v.cog)
        if (c.cpaNm < 0.5 && c.tcpaMin in 0.0..15.0) v.name to c else null
    } else emptyList()

    Column(Modifier.fillMaxSize().background(SirenBackground).padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DirectionsBoat, null, tint = SirenPrimary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text("AIS TRAFIGI + CPA/TCPA", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
        }
        Spacer(Modifier.height(12.dp))

        if (risks.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SirenRed).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("CARPISMA RISKI: ${risks.joinToString(", ") { it.first }}",
                    color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }

        vessels.forEach { v ->
            val vp = if (own != null) NavMath.destPoint(own, v.bearing, v.distanceNm) else null
            val c = if (own != null && vp != null) NavMath.cpaTcpa(own, ownSog, ownCog, vp, v.sog, v.cog) else null
            val danger = c != null && c.cpaNm < 0.5 && c.tcpaMin in 0.0..15.0

            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(12.dp))
                .background(if (danger) SirenRed.copy(alpha = 0.3f) else SirenCard).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DirectionsBoat, null,
                    tint = if (danger) SirenRed else SirenGreen, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(v.name, color = SirenTextPrimary, fontWeight = FontWeight.Bold)
                    Text("%.1f kts · %.0f° · %.1f nm".format(v.sog, v.cog, v.distanceNm),
                        color = SirenTextSecondary, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (c != null) {
                        Text("CPA %.2fnm".format(c.cpaNm),
                            color = if (danger) SirenRed else SirenTextPrimary, fontWeight = FontWeight.Bold)
                        Text("TCPA %.0fdk".format(c.tcpaMin),
                            color = if (danger) SirenRed else SirenTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Demo hedefler - gercek AIS sonraki surumde", color = SirenTextSecondary, fontSize = 10.sp)
    }
}
