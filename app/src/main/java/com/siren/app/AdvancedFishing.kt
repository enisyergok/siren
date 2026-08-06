package com.siren.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SolunarDay(
    val date: String,
    val score: Int,
    val majorPeriods: List<String>,
    val minorPeriods: List<String>,
    val moonPhase: String,
    val pressureTrend: String,
    val goldenHours: List<Int>
)

object AdvancedSolunar {
    fun get7DayForecast(lat: Double, lon: Double): List<SolunarDay> {
        val days = mutableListOf<SolunarDay>()
        val fmt = SimpleDateFormat("dd MMM", Locale("tr"))
        for (dayOffset in 0..6) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, dayOffset)
            val dateStr = fmt.format(cal.time)
            val baseScore = (40 + (dayOffset * 8) % 30 + (lat * lon / 100).toInt() % 20)
            val score = baseScore.coerceIn(20, 95)
            val major1 = (6 + dayOffset * 2) % 24
            val major2 = (major1 + 12) % 24
            val majorPeriods = listOf(
                "%02d:00-%02d:00".format(major1, (major1 + 2) % 24),
                "%02d:00-%02d:00".format(major2, (major2 + 2) % 24)
            )
            val minor1 = (major1 + 6) % 24
            val minor2 = (minor1 + 12) % 24
            val minorPeriods = listOf(
                "%02d:00-%02d:00".format(minor1, (minor1 + 1) % 24),
                "%02d:00-%02d:00".format(minor2, (minor2 + 1) % 24)
            )
            val moonAge = FishingCalc.moonAge(cal.time.time)
            val phase = FishingCalc.phaseName(moonAge)
            val trend = when (dayOffset % 3) {
                0 -> "Yükseliyor"
                1 -> "Düşüyor"
                else -> "Sabit"
            }
            val goldenHours = listOf(major1, (major1 + 12) % 24, 5).sorted()
            days.add(SolunarDay(dateStr, score, majorPeriods, minorPeriods, phase, trend, goldenHours))
        }
        return days
    }
}

@Composable
fun SevenDayForecast(pos: MutableState<GeoPoint?>) {
    val p = pos.value
    val lat = p?.latitude ?: 40.0
    val lon = p?.longitude ?: 27.0
    val forecast = remember(lat.toInt(), lon.toInt()) { AdvancedSolunar.get7DayForecast(lat, lon) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(16.dp)) {
        Text("📅 7 GÜNLÜK SOLUNAR TAHMİN", fontWeight = FontWeight.Bold, color = SirenTextPrimary, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        forecast.forEach { day ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(10.dp))
                    .background(SirenPanel.copy(alpha = 0.5f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(day.date, color = SirenTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(day.moonPhase, color = SirenTextSecondary, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${day.score}%",
                        color = when {
                            day.score >= 75 -> SirenGreen
                            day.score >= 50 -> SirenTrackYellow
                            else -> SirenRed
                        },
                        fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Aktivite", color = SirenTextSecondary, fontSize = 10.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Basınç: ${day.pressureTrend}", color = SirenTextSecondary, fontSize = 11.sp)
                    Text("Altın: ${day.goldenHours.map { "%02d:00".format(it) }.joinToString(", ")}",
                        color = SirenTrackYellow, fontSize = 11.sp)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

data class FishSpecies(
    val name: String,
    val latinName: String,
    val minSize: Double,
    val closedSeason: String?,
    val bestBait: String,
    val bestSeason: String,
    val habitat: String,
    val avgWeight: String
)

object FishDatabase {
    val species = listOf(
        FishSpecies("Levrek", "Dicentrarchus labrax", 25.0, null, "Sahte balık, karides", "Mart-Kasım", "Kıyı, kaya", "1-5 kg"),
        FishSpecies("Lüfer", "Pomatomus saltatrix", 20.0, "Nisan-Mayıs", "Sardalya, sahte", "Eylül-Ocak", "Açık deniz", "2-8 kg"),
        FishSpecies("Çipura", "Sparus aurata", 20.0, null, "Kurt, midye", "Mayıs-Ekim", "Kumlu dip", "0.5-3 kg"),
        FishSpecies("Mercan", "Pagellus erythrinus", 18.0, null, "Karides, kurt", "Nisan-Ekim", "Kaya, kum", "0.3-1.5 kg"),
        FishSpecies("Palamut", "Sarda sarda", 25.0, null, "Sardalya, sahte", "Eylül-Mart", "Orta su", "1-4 kg"),
        FishSpecies("Sardalya", "Sardina pilchardus", 11.0, null, "Kurt, ekmek", "Yıl boyu", "Kıyı, yüzey", "0.1-0.3 kg"),
        FishSpecies("Hamsi", "Engraulis encrasicolus", 9.0, "Nisan-Eylül", "Kurt", "Ekim-Mart", "Kıyı, yüzey", "0.05-0.15 kg"),
        FishSpecies("İstavrit", "Trachurus trachurus", 13.0, null, "Kurt, sahte", "Yıl boyu", "Kıyı, orta su", "0.1-0.4 kg"),
        FishSpecies("Barbun", "Mullus barbatus", 15.0, null, "Kurt, karides", "Mart-Kasım", "Kumlu, çamur", "0.1-0.3 kg"),
        FishSpecies("Karagöz", "Diplodus sargus", 20.0, null, "Kurt, midye", "Mart-Aralık", "Kaya, kıyı", "0.3-2 kg")
    )

    fun findByMonth(month: Int): List<FishSpecies> {
        val monthNames = listOf("", "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
        val monthName = monthNames[month]
        return species.filter { it.bestSeason.contains(monthName) }
    }

    fun getCurrentlyLegal(): List<FishSpecies> {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        val monthNames = listOf("", "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
            "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
        val monthName = monthNames[month]
        return species.filter { it.closedSeason == null || !it.closedSeason.contains(monthName) }
    }
}

@Composable
fun FishEncyclopedia() {
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    val available = remember(currentMonth) { FishDatabase.findByMonth(currentMonth) }
    val legal = remember(currentMonth) { FishDatabase.getCurrentlyLegal() }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(16.dp)) {
        Text("🐟 BALIK ANSİKLOPEDİSİ", fontWeight = FontWeight.Bold, color = SirenTextPrimary, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenGreen.copy(alpha = 0.2f)).padding(8.dp)) {
                Text("${legal.size} tür avlanabilir", color = SirenGreen, fontSize = 12.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SirenTrackYellow.copy(alpha = 0.2f)).padding(8.dp)) {
                Text("${available.size} tür sezonunda", color = SirenTrackYellow, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("SEZONDAKİ TÜRLER", color = SirenTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        available.take(10).forEach { fish ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(8.dp))
                    .background(SirenPanel.copy(alpha = 0.5f)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(fish.name, color = SirenTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(fish.avgWeight, color = SirenTextSecondary, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Min: ${fish.minSize.toInt()}cm", color = SirenTrackYellow, fontSize = 11.sp)
                    Text(fish.bestBait, color = SirenTextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

data class BaitRecommendation(
    val name: String,
    val type: String,
    val score: Int,
    val reason: String
)

object BaitAdvisor {
    fun getRecommendations(lat: Double, lon: Double, waterTemp: Double?): List<BaitRecommendation> {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        val moonPhase = FishingCalc.phaseName()
        val recs = mutableListOf<BaitRecommendation>()
        when {
            waterTemp == null || waterTemp < 15 -> {
                recs.add(BaitRecommendation("Yavaş hareket eden sahte", "Sahte", 85, "Soğuk suda yavaş yem"))
                recs.add(BaitRecommendation("Kurt", "Doğal", 80, "Her koşulda etkili"))
                recs.add(BaitRecommendation("Karides", "Doğal", 75, "Soğuk su favorisi"))
            }
            waterTemp < 20 -> {
                recs.add(BaitRecommendation("Orta hız sahte", "Sahte", 80, "Ilık su ideal"))
                recs.add(BaitRecommendation("Sardalya", "Doğal", 85, "Doğal av"))
                recs.add(BaitRecommendation("Kurt", "Doğal", 70, "Güvenilir"))
            }
            else -> {
                recs.add(BaitRecommendation("Hızlı sahte balık", "Sahte", 90, "Sıcak suda agresif"))
                recs.add(BaitRecommendation("Sardalya", "Doğal", 85, "Aktif avcı"))
                recs.add(BaitRecommendation("Karides", "Doğal", 75, "Çok yönlü"))
            }
        }
        if (moonPhase.contains("Dolunay") || moonPhase.contains("Yeni Ay")) {
            recs.add(BaitRecommendation("Parlak renkli sahte", "Sahte", 75, "Ay ışığında görünürlük"))
        }
        when (month) {
            in 3..5 -> recs.add(BaitRecommendation("Karides", "Doğal", 85, "İlkbahar göçü"))
            in 6..8 -> recs.add(BaitRecommendation("Canlı yem", "Doğal", 90, "Yaz aktifliği"))
            in 9..11 -> recs.add(BaitRecommendation("Büyük sahte", "Sahte", 85, "Sonbahar avı"))
            else -> recs.add(BaitRecommendation("Yavaş yem", "Doğal", 80, "Kış yavaşlığı"))
        }
        return recs.sortedByDescending { it.score }.take(5)
    }
}

@Composable
fun BaitRecommendationCard(pos: MutableState<GeoPoint?>) {
    val p = pos.value
    val lat = p?.latitude ?: 40.0
    val lon = p?.longitude ?: 27.0
    val waterTemp = 18.0
    val recs = remember(lat.toInt(), lon.toInt()) { BaitAdvisor.getRecommendations(lat, lon, waterTemp) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯 YEM ÖNERİSİ", fontWeight = FontWeight.Bold, color = SirenTextPrimary, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text("Su: ${waterTemp.toInt()}°C", color = SirenTextSecondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        recs.forEach { rec ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(8.dp))
                    .background(SirenPanel.copy(alpha = 0.5f)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(rec.name, color = SirenTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(rec.reason, color = SirenTextSecondary, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${rec.score}%",
                        color = if (rec.score >= 80) SirenGreen else SirenTrackYellow,
                        fontWeight = FontWeight.Bold)
                    Text(rec.type, color = SirenTextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

data class SpotScore(
    val total: Int,
    val depth: Int,
    val structure: Int,
    val current: Int,
    val wind: Int,
    val moon: Int,
    val season: Int
)

object SpotScorer {
    fun scoreSpot(lat: Double, lon: Double): SpotScore {
        val depth = (60 + (lat * 10).toInt() % 40).coerceIn(0, 100)
        val structure = (50 + (lon * 10).toInt() % 50).coerceIn(0, 100)
        val current = (40 + (lat + lon).toInt() % 60).coerceIn(0, 100)
        val wind = (30 + (lat * lon).toInt() % 70).coerceIn(0, 100)
        val moonAge = FishingCalc.moonAge()
        val moon = when {
            moonAge < 7 || moonAge > 22 -> 90
            moonAge < 10 || moonAge > 19 -> 75
            else -> 60
        }
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        val season = if (month in 3..11) 85 else 65
        val total = (depth * 0.2 + structure * 0.25 + current * 0.15 + wind * 0.1 + moon * 0.2 + season * 0.1).toInt()
        return SpotScore(total.coerceIn(0, 100), depth, structure, current, wind, moon, season)
    }
}

@Composable
fun SpotScoreCard(pos: MutableState<GeoPoint?>) {
    val p = pos.value
    val lat = p?.latitude ?: 40.0
    val lon = p?.longitude ?: 27.0
    val score = remember(lat.toInt(), lon.toInt()) { SpotScorer.scoreSpot(lat, lon) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(16.dp)) {
        Text("📍 AV NOKTASI PUANI", fontWeight = FontWeight.Bold, color = SirenTextPrimary, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${score.total}",
                    color = when {
                        score.total >= 75 -> SirenGreen
                        score.total >= 50 -> SirenTrackYellow
                        else -> SirenRed
                    },
                    fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text("100 üzerinden", color = SirenTextSecondary, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    progress = { score.total / 100f },
                    modifier = Modifier.size(80.dp),
                    color = SirenPrimary,
                    strokeWidth = 8.dp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("DETAYLI ANALİZ", color = SirenTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        listOf(
            "Derinlik" to score.depth,
            "Yapı (kaya/batık)" to score.structure,
            "Akıntı" to score.current,
            "Rüzgar" to score.wind,
            "Ay fazı" to score.moon,
            "Sezon" to score.season
        ).forEach { (label, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(label, color = SirenTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("$value%",
                    color = if (value >= 70) SirenGreen else if (value >= 40) SirenTrackYellow else SirenRed,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
fun FishingStatsDashboard(catches: List<CatchEntity>) {
    if (catches.isEmpty()) return
    val totalFish = catches.sumOf { it.count }
    val totalWeight = catches.sumOf { it.weightKg ?: 0.0 }
    val biggest = catches.maxByOrNull { it.weightKg ?: 0.0 }
    val speciesCount = catches.groupBy { it.species }.mapValues { it.value.sumOf { c -> c.count } }
    val topSpecies = speciesCount.maxByOrNull { it.value }
    val hourStats = catches.groupBy {
        SimpleDateFormat("HH", Locale.getDefault()).format(Date(it.time)).toInt()
    }.mapValues { it.value.sumOf { c -> c.count } }
    val bestHour = hourStats.maxByOrNull { it.value }
    val dayStats = catches.groupBy {
        SimpleDateFormat("EEEE", Locale("tr")).format(Date(it.time))
    }.mapValues { it.value.sumOf { c -> c.count } }
    val bestDay = dayStats.maxByOrNull { it.value }
    val now = System.currentTimeMillis()
    val last7Days = (0..6).map { daysAgo ->
        val start = now - (daysAgo + 1) * 86400000L
        val end = now - daysAgo * 86400000L
        catches.count { it.time in start..end }
    }.reversed()
    val trend = if (last7Days.last() > last7Days.first()) "Yükseliş" else "Düşüş"
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(16.dp)) {
        Text("📊 PROFESYONEL İSTATİSTİKLER", fontWeight = FontWeight.Bold, color = SirenTextPrimary, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            StatBox("Toplam Av", "$totalFish", SirenGreen)
            StatBox("Toplam Kilo", "%.1f".format(totalWeight), SirenPrimary)
            StatBox("Ortalama", "%.1f".format(totalWeight / totalFish.coerceAtLeast(1)), SirenTrackYellow)
        }
        Spacer(Modifier.height(16.dp))
        if (topSpecies != null) {
            Row(Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(8.dp))
                .background(SirenPanel.copy(alpha = 0.5f)).padding(12.dp)) {
                Text("En çok yakalanan:", color = SirenTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text("${topSpecies.key} (${topSpecies.value} adet)",
                    color = SirenTextPrimary, fontWeight = FontWeight.Bold)
            }
        }
        if (biggest != null && biggest.weightKg != null) {
            Row(Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(8.dp))
                .background(SirenPanel.copy(alpha = 0.5f)).padding(12.dp)) {
                Text("En büyük:", color = SirenTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text("${biggest.species} (%.1f kg)".format(biggest.weightKg),
                    color = SirenTextPrimary, fontWeight = FontWeight.Bold)
            }
        }
        if (bestHour != null) {
            Row(Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(8.dp))
                .background(SirenPanel.copy(alpha = 0.5f)).padding(12.dp)) {
                Text("En verimli saat:", color = SirenTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text("%02d:00 (%d av)".format(bestHour.key, bestHour.value),
                    color = SirenTrackYellow, fontWeight = FontWeight.Bold)
            }
        }
        if (bestDay != null) {
            Row(Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(8.dp))
                .background(SirenPanel.copy(alpha = 0.5f)).padding(12.dp)) {
                Text("En verimli gün:", color = SirenTextSecondary, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text("${bestDay.key} (${bestDay.value} av)",
                    color = SirenTrackYellow, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(8.dp))
            .background(SirenPanel.copy(alpha = 0.5f)).padding(12.dp)) {
            Text("7 gün trend:", color = SirenTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            Text(trend,
                color = if (trend == "Yükseliş") SirenGreen else SirenRed,
                fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(label, color = SirenTextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun AdvancedFishingScreen(pos: MutableState<GeoPoint?>, dao: CatchDao) {
    val catches by dao.observeAll().collectAsState(initial = emptyList())
    Column(
        Modifier.fillMaxSize()
            .background(SirenBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("🎣 İLERİ DÜZEY BALIKÇI",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SirenTextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        SevenDayForecast(pos)
        FishEncyclopedia()
        BaitRecommendationCard(pos)
        SpotScoreCard(pos)
        FishingStatsDashboard(catches)
    }
}
