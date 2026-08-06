package com.siren.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BoxScope.MobButton() {
    val mob by SirenNav.mob
    val p by SirenNav.pos

    Box(
        Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 64.dp)
            .size(56.dp).clip(CircleShape).background(if (mob != null) SirenRed else SirenPanel)
            .clickable { SirenNav.mob.value = if (mob != null) null else p },
        contentAlignment = Alignment.Center
    ) {
        Text("🆘", fontSize = 22.sp)
    }

    if (mob != null && p != null) {
        val brg = NavMath.bearingDeg(p!!, mob!!)
        val dst = haversineNm(p!!.latitude, p!!.longitude, mob!!.latitude, mob!!.longitude)
        Column(Modifier.align(Alignment.TopCenter).padding(top = 110.dp)) {
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(SirenRed)
                .clickable { SirenNav.mob.value = null }
                .padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text("🆘 MOB! KERTEZIZ %.0f° · %.2f nm — SONA ERDIR".format(brg, dst),
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
