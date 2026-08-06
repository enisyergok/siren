package com.siren.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.osmdroid.util.GeoPoint

enum class SeaRegion(val label: String) {
    KARADENIZ("Karadeniz"),
    MARMARA("Marmara"),
    EGE("Ege"),
    AKDENIZ("Akdeniz")
}

data class RegionFish(val name: String, val season: String, val bait: String, val zone: String)

object RegionDb {
    fun detect(lat: Double, lon: Double): SeaRegion = when {
        lat > 40.9 -> SeaRegion.KARADENIZ
        lat > 39.7 && lon < 30.5 -> SeaRegion.MARMARA
        lon < 27.8 -> SeaRegion.EGE
        else -> SeaRegion.AKDENIZ
    }

    fun fishFor(r: SeaRegion): List<RegionFish> = when (r) {
        SeaRegion.KARADENIZ -> listOf(
            RegionFish("Hamsi", "Ekim-Mart", "Kurt, tüy", "Yüzey, sürü"),
            RegionFish("Lüfer", "Eylül-Ocak", "Sardalya", "Orta su"),
            RegionFish("Palamut", "Eylül-Mart", "Sardalya, sahte", "Orta su"),
            RegionFish("İstavrit", "Yıl boyu", "Kurt, tüy", "Kıyı"),
            RegionFish("Mezgit", "Yıl boyu", "Kurt, karides", "Dip"),
            RegionFish("Barbun", "Mart-Kasım", "Kurt", "Kumlu dip"),
            RegionFish("Kalkan", "Mayıs-Ağustos", "Canlı yem", "Dip"),
            RegionFish("Levrek", "Mart-Kasım", "Sahte", "Kıyı, kaya")
        )
        SeaRegion.MARMARA -> listOf(
            RegionFish("Levrek", "Mart-Kasım", "Sahte, karides", "Kıyı, kaya"),
            RegionFish("Lüfer", "Eylül-Ocak", "Sardalya", "Orta su"),
            RegionFish("Çipura", "Mayıs-Ekim", "Kurt, midye", "Kumlu dip"),
            RegionFish("Karagöz", "Mart-Aralık", "Kurt, midye", "Kaya"),
            RegionFish("Barbun", "Mart-Kasım", "Kurt", "Kumlu dip"),
            RegionFish("İstavrit", "Yıl boyu", "Kurt, tüy", "Kıyı"),
            RegionFish("Sardalya", "Yıl boyu", "Kurt, ekmek", "Yüzey"),
            RegionFish("Mırmır", "Nisan-Ekim", "Kurt", "Kumlu kıyı")
        )
        SeaRegion.EGE -> listOf(
            RegionFish("Çipura", "Mayıs-Ekim", "Kurt, midye", "Kumlu dip"),
            RegionFish("Levrek", "Mart-Kasım", "Sahte", "Kıyı, kaya"),
            RegionFish("Mercan", "Nisan-Ekim", "Karides", "Kaya, kum"),
            RegionFish("Karagöz", "Mart-Aralık", "Kurt, midye", "Kaya"),
            RegionFish("Akya", "Haziran-Ekim", "Sahte, canlı", "Açık deniz"),
            RegionFish("Lahoz", "Mayıs-Aralık", "Sahte, canlı", "Kaya, batık"),
            RegionFish("İskorpit", "Mart-Kasım", "Karides", "Kaya, batık"),
            RegionFish("Sargos", "Mart-Aralık", "Kurt, ekmek", "Kum, kıyı")
        )
        SeaRegion.AKDENIZ -> listOf(
            RegionFish("Lahoz", "Mayıs-Aralık", "Sahte, canlı", "Kaya, batık"),
            RegionFish("Akya", "Haziran-Ekim", "Sahte, canlı", "Açık deniz"),
            RegionFish("Mercan", "Nisan-Ekim", "Karides", "Kaya, kum"),
            RegionFish("Sinarit", "Mayıs-Ekim", "Sahte, canlı", "Kaya"),
            RegionFish("Çipura", "Mayıs-Ekim", "Kurt, midye", "Kumlu dip"),
            RegionFish("Trança", "Yıl boyu", "Sahte, canlı", "Derin kaya"),
            RegionFish("İskorpit", "Mart-Kasım", "Karides", "Kaya, batık"),
            RegionFish("Orkinos", "Temmuz-Ocak", "Sardalya", "Açık deniz")
        )
    }
}

@Composable
fun BoxScope.RegionFishPanel() {
    val p by SirenNav.pos
    var region by remember { mutableStateOf<SeaRegion?>(null) }
    var open by remember { mutableStateOf(false) }

    LaunchedEffect(p) {
        val cur = p?.let { RegionDb.detect(it.latitude, it.longitude) }
        if (cur != null && cur != region) {
            region = cur
            open = true
        }
    }

    val r = region
    if (!open || r == null) return

    Box(
        Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
            .fillMaxWidth(0.94f).height(320.dp)
            .clip(RoundedCornerShape(14.dp)).background(SirenCard.copy(alpha = 0.97f)).padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌊 ${r.label} — Bölge Balıkları",
                    color = SirenTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text("OTOMATIK", color = SirenGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text("✕", color = SirenRed, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { open = false })
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.verticalScroll(rememberScrollState())) {
                RegionDb.fishFor(r).forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(8.dp)).background(SirenPanel.copy(alpha = 0.6f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🐟", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, color = SirenTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${f.zone} · ${f.bait}", color = SirenTextSecondary, fontSize = 10.sp)
                        }
                        Text(f.season, color = SirenTrackYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
