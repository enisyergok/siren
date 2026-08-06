package com.siren.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun CatchLogScreen(dao: CatchDao, pos: MutableState<GeoPoint?>) {
    val catches by dao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("dd.MM HH:mm", Locale("tr")) }

    val totalCount = catches.sumOf { it.count }
    val biggest = catches.maxByOrNull { it.weightKg ?: 0.0 }
    val topSpecies = catches.groupBy { it.species }
        .mapValues { it.value.sumOf { c -> c.count } }
        .maxByOrNull { it.value }

    Column(Modifier.fillMaxSize().background(SirenBackground).padding(24.dp).verticalScroll(rememberScrollState())) {
        FishingForecastCard(pos)

        Text("BALIK GUNLUGU", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Toplam: $totalCount", color = SirenGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (biggest?.weightKg != null)
                Text("En buyuk: ${biggest.species} %.1fkg".format(biggest.weightKg),
                    color = SirenTrackYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (topSpecies != null)
                Text("Favori: ${topSpecies.key}", color = SirenRouteBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))

        if (catches.isEmpty()) {
            Text("Henuz av kaydi yok. Haritadaki 🎣 butonuyla ekle.", color = SirenTextSecondary)
        }

        catches.forEach { c ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp))
                .background(SirenCard).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🐟", fontSize = 22.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.species, color = SirenTextPrimary, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(fmt.format(Date(c.time)), color = SirenTextSecondary, fontSize = 11.sp)
                        if (c.lengthCm != null) Text("%.0fcm".format(c.lengthCm), color = SirenTextSecondary, fontSize = 11.sp)
                        if (c.weightKg != null) Text("%.1fkg".format(c.weightKg), color = SirenTextSecondary, fontSize = 11.sp)
                    }
                    Text("%.4f, %.4f".format(c.lat, c.lon), color = SirenTextSecondary, fontSize = 10.sp)
                }
                Text("x${c.count}", color = SirenGreen, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                Text("SIL", color = SirenRed, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { scope.launch { dao.delete(c.id) } })
            }
        }
    }
}

@Composable
fun BoxScope.CatchButton() {
    var showDialog by remember { mutableStateOf(false) }
    Box(
        Modifier.align(Alignment.BottomEnd).padding(end = 90.dp, bottom = 16.dp)
            .size(56.dp).clip(CircleShape).background(SirenGreen)
            .clickable { showDialog = true },
        contentAlignment = Alignment.Center
    ) {
        Text("🎣", fontSize = 24.sp)
    }
    if (showDialog) CatchDialog(onDismiss = { showDialog = false })
}

@Composable
fun CatchDialog(onDismiss: () -> Unit) {
    var species by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Balik Kaydi", color = SirenTextPrimary) },
        text = {
            Column {
                TextField(
                    value = species, onValueChange = { species = it },
                    label = { Text("Tur (örn. Levrek, Cipura)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = length, onValueChange = { length = it },
                        label = { Text("Boy cm") }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextField(
                        value = weight, onValueChange = { weight = it },
                        label = { Text("Kilo") }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Text("KAYDET", color = SirenGreen, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    val p = SirenNav.pos.value
                    if (species.isNotBlank() && p != null) {
                        scope.launch {
                            SirenNav.catchDao?.insert(
                                CatchEntity(
                                    id = UUID.randomUUID().toString(),
                                    species = species.trim(),
                                    lengthCm = length.toDoubleOrNull(),
                                    weightKg = weight.toDoubleOrNull(),
                                    count = 1,
                                    note = null,
                                    lat = p.latitude,
                                    lon = p.longitude,
                                    time = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    onDismiss()
                })
        },
        dismissButton = {
            Text("IPTAL", color = SirenTextSecondary, modifier = Modifier.clickable { onDismiss() })
        }
    )
}
