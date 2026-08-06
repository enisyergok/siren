package com.siren.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class KnotInfo(
    val name: String,
    val purpose: String,
    val difficulty: Int,
    val strength: String,
    val art: Int,
    val steps: List<String>
)

object KnotDb {
    val knots = listOf(
        KnotInfo("Palomar", "Kanca / sahte bağlama", 1, "%98", 1, listOf(
            "Misineyi katla, halkadan geçir",
            "Halka ile basit düğüm at",
            "Halkayı kancanın üzerinden geçir",
            "Islat ve sıkıştır"
        )),
        KnotInfo("Improved Clinch", "Kanca / sahte bağlama", 1, "%95", 2, listOf(
            "Misineyi halkadan geçir",
            "Ana misinaya 5-6 tur sar",
            "Ucu halkadan geri geçir",
            "Islat ve sıkıştır, ucunu kes"
        )),
        KnotInfo("Uni Knot", "Kanca / halka bağlama", 2, "%90", 3, listOf(
            "Misineyi halkadan geçir",
            "Uçla halka içinde 4-5 tur sar",
            "Halkayı kancaya doğru it",
            "Islat ve sıkıştır"
        )),
        KnotInfo("Blood Knot", "İki misinayı birleştirme", 2, "%85", 4, listOf(
            "İki ucu çaprazla",
            "Her ucu diğerine 5'er tur sar",
            "Uçları ortadan zıt yönlü geçir",
            "Islat ve sıkıştır"
        )),
        KnotInfo("Albright", "İnce-kalın misina ek", 3, "%80", 5, listOf(
            "Kalın misinayla halka yap",
            "İnce ucu halkadan geçir",
            "Halkanın etrafına 10 tur sar",
            "Aynı delikten geri geçir, sıkıştır"
        )),
        KnotInfo("Non-Slip Loop", "Sahteye hareket halkası", 2, "%95", 6, listOf(
            "Halkadan önce küçük düğüm halkası",
            "Ucu kancadan geçir",
            "Düğüm halkasından geçir",
            "4 tur sar, geri geçir, sıkıştır"
        )),
        KnotInfo("Snell", "Kanca sapına bağlama", 2, "%90", 7, listOf(
            "Ucu sap boyunca katla",
            "Halka yapıp sap+misineye 6 tur sar",
            "Ucu halkadan geçir",
            "Sıkıştır, sap boyunca düz durmalı"
        )),
        KnotInfo("Dropper Loop", "Köstek halkası (çoklu iğne)", 3, "%85", 8, listOf(
            "Misinada halka bük",
            "Halkayı birkaç tur kendi etrafında döndür",
            "Ortadan ikinci halkayı çek çıkar",
            "İki uçtan çekerek sabitle"
        )),
        KnotInfo("Köstek Rig (2 İğne)", "Dip avı düzeni", 2, "Rig", 9, listOf(
            "Ana misina → fırdöndü",
            "Dropper loop ile 2 köstek",
            "Uca kurşun (yumurta tip)",
            "Köstekler 20-30 cm aralıklı"
        )),
        KnotInfo("Şamandıra Rig", "Yüzey/orta su avı", 1, "Rig", 10, listOf(
            "Ana misina → şamandıra stoperi",
            "Şamandıra + altına saçma",
            "Uca fırdöndü + köstek",
            "Derinliği stoperle ayarla"
        )),
        KnotInfo("Sahte Rig (Leader)", "Sahte balık avı", 2, "Rig", 11, listOf(
            "Ana misina (örgü) → FG/Albright",
            "Florokarbon leader 60-90 cm",
            "Uca non-slip loop",
            "Sahteyi loop'a tak"
        ))
    )
}

@Composable
fun KnotCanvas(art: Int) {
    Canvas(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A101C))) {
        val w = size.width
        val h = size.height
        fun pt(x: Float, y: Float) = Offset(x * w, y * h)
        val main = Color(0xFFEAF1FB)
        val tag = Color(0xFFF2C94C)
        val metal = Color(0xFF8CA3C2)
        val red = Color(0xFFE5484D)
        fun ln(a: Offset, b: Offset, c: Color = main, sw: Float = 4f) =
            drawLine(c, a, b, sw, cap = StrokeCap.Round)
        fun ci(c: Offset, r: Float, col: Color = main, sw: Float = 4f) =
            drawCircle(col, r, c, style = Stroke(sw))
        fun hook(c: Offset, s: Float, col: Color = metal) {
            val p = Path()
            p.moveTo(c.x, c.y - s)
            p.lineTo(c.x, c.y + s * 0.4f)
            p.quadraticBezierTo(c.x, c.y + s, c.x - s * 0.7f, c.y + s)
            p.quadraticBezierTo(c.x - s * 1.2f, c.y + s, c.x - s * 1.2f, c.y + s * 0.2f)
            p.lineTo(c.x - s, c.y - s * 0.2f)
            drawPath(p, col, style = Stroke(4f, cap = StrokeCap.Round))
        }
        when (art) {
            1 -> { // Palomar
                ci(pt(0.5f, 0.32f), 8f, metal)
                hook(pt(0.5f, 0.55f), h * 0.18f)
                ln(pt(0.47f, 0.05f), pt(0.47f, 0.3f))
                ln(pt(0.53f, 0.05f), pt(0.53f, 0.3f))
                ci(pt(0.5f, 0.1f), 14f, main)
                ln(pt(0.5f, 0.36f), pt(0.5f, 0.45f), tag)
            }
            2 -> { // Clinch
                ci(pt(0.25f, 0.4f), 8f, metal)
                hook(pt(0.25f, 0.62f), h * 0.16f)
                ln(pt(0.28f, 0.4f), pt(0.9f, 0.4f))
                for (i in 0..4) ci(pt(0.45f + i * 0.07f, 0.4f), 7f, main, 3f)
                ln(pt(0.75f, 0.44f), pt(0.3f, 0.5f), tag)
            }
            3 -> { // Uni
                ci(pt(0.25f, 0.4f), 8f, metal)
                ln(pt(0.28f, 0.38f), pt(0.9f, 0.38f))
                ln(pt(0.9f, 0.38f), pt(0.55f, 0.5f), tag)
                for (i in 0..3) ci(pt(0.5f + i * 0.07f, 0.44f), 7f, main, 3f)
            }
            4 -> { // Blood
                ln(pt(0.05f, 0.45f), pt(0.55f, 0.45f))
                ln(pt(0.95f, 0.5f), pt(0.45f, 0.5f))
                for (i in 0..4) ci(pt(0.4f + i * 0.05f, 0.47f), 6f, main, 3f)
                ln(pt(0.5f, 0.45f), pt(0.5f, 0.15f), tag)
                ln(pt(0.52f, 0.5f), pt(0.52f, 0.8f), tag)
            }
            5 -> { // Albright
                drawCircle(Color(0xFF38BDF8), radius = 20f, center = pt(0.35f, 0.45f), style = Stroke(6f))
                for (i in 0..5) ci(pt(0.42f + i * 0.06f, 0.45f), 8f, main, 3f)
                ln(pt(0.8f, 0.45f), pt(0.95f, 0.45f), tag)
                ln(pt(0.42f, 0.35f), pt(0.42f, 0.12f), tag)
            }
            6 -> { // Non-slip loop
                ci(pt(0.3f, 0.3f), 8f, metal)
                ci(pt(0.3f, 0.14f), 12f, main)
                ln(pt(0.33f, 0.32f), pt(0.85f, 0.4f))
                for (i in 0..2) ci(pt(0.45f + i * 0.06f, 0.37f), 6f, main, 3f)
                ln(pt(0.62f, 0.4f), pt(0.35f, 0.2f), tag)
            }
            7 -> { // Snell
                ln(pt(0.5f, 0.1f), pt(0.5f, 0.6f), metal, 5f)
                hook(pt(0.5f, 0.7f), h * 0.15f)
                ln(pt(0.54f, 0.1f), pt(0.54f, 0.5f), main, 3f)
                for (i in 0..3) ci(pt(0.52f, 0.25f + i * 0.08f), 6f, main, 3f)
            }
            8 -> { // Dropper loop
                ln(pt(0.5f, 0.05f), pt(0.5f, 0.95f))
                ci(pt(0.62f, 0.45f), 14f, main)
                ci(pt(0.5f, 0.45f), 6f, tag, 3f)
                ci(pt(0.5f, 0.55f), 6f, tag, 3f)
            }
            9 -> { // Köstek rig
                ln(pt(0.5f, 0.05f), pt(0.5f, 0.8f))
                ci(pt(0.62f, 0.3f), 10f, main)
                hook(pt(0.75f, 0.3f), h * 0.1f)
                ci(pt(0.62f, 0.55f), 10f, main)
                hook(pt(0.75f, 0.55f), h * 0.1f)
                drawCircle(metal, 12f, pt(0.5f, 0.85f))
            }
            10 -> { // Şamandıra
                ln(pt(0.5f, 0.05f), pt(0.5f, 0.9f))
                drawCircle(red, 14f, pt(0.5f, 0.25f))
                ln(pt(0.42f, 0.25f), pt(0.58f, 0.25f), main, 3f)
                drawCircle(metal, 6f, pt(0.5f, 0.6f))
                hook(pt(0.5f, 0.85f), h * 0.1f)
            }
            else -> { // Sahte rig
                ln(pt(0.05f, 0.4f), pt(0.45f, 0.4f), main, 3f)
                ln(pt(0.45f, 0.4f), pt(0.7f, 0.4f), Color(0xFF38BDF8), 4f)
                val p = Path()
                p.moveTo(pt(0.7f, 0.4f).x, pt(0.7f, 0.4f).y)
                p.lineTo(pt(0.9f, 0.32f).x, pt(0.9f, 0.32f).y)
                p.lineTo(pt(0.95f, 0.4f).x, pt(0.95f, 0.4f).y)
                p.lineTo(pt(0.9f, 0.48f).x, pt(0.9f, 0.48f).y)
                p.close()
                drawPath(p, Color(0xFF2ECC71))
                ln(pt(0.9f, 0.4f), pt(0.82f, 0.5f), Color(0xFF2ECC71), 3f)
            }
        }
    }
}

@Composable
fun KnotGuideScreen() {
    Column(Modifier.fillMaxSize().background(SirenBackground).padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("OLTA BAGLARI ANSIKLOPEDISI", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SirenTextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Tum baglar cevrimdisi sema ile - internet gerekmez", color = SirenTextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        KnotDb.knots.forEach { k ->
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(14.dp)).background(SirenCard).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(k.name, color = SirenTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(k.purpose, color = SirenTextSecondary, fontSize = 11.sp)
                    }
                    Text("★".repeat(k.difficulty), color = SirenTrackYellow, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(k.strength, color = SirenGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                KnotCanvas(k.art)
                Spacer(Modifier.height(10.dp))
                k.steps.forEachIndexed { i, s ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text("${i + 1}.", color = SirenRouteBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(s, color = SirenTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
