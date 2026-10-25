package com.mypainter.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Only two world icons at the top level. All six original shops stay inside 超市. */
@Composable
fun WorldHomeScreen(
    store: ArtworkStore,
    progressTick: Int,
    onSupermarket: () -> Unit,
    onBathroom: () -> Unit,
    onGallery: () -> Unit,
    onSettings: () -> Unit
) {
    val supermarketCount = ShopCatalog.items.count {
        it.categoryId != "bathroom" && store.hasWork(it.id)
    }
    val bathroomCount = ShopCatalog.forCategory("bathroom").count {
        store.hasWork(it.id)
    }
    Row(
        Modifier.fillMaxSize().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            Modifier.weight(.35f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🎨 小小畫家", fontSize = 23.sp,
                fontWeight = FontWeight.Black, color = Color(0xFF37415C))
            Text("今天想去哪裡畫畫？", fontSize = 14.sp)
            Text("⭐ 已玩過：${supermarketCount + bathroomCount} / 66",
                fontSize = 15.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
                Text("🖼️ 我的作品")
            }
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                Text("🔊 家長設定")
            }
        }
        WorldTile(
            title = "超市",
            subtitle = "水果、蔬菜、點心和玩具",
            emoji = "🛒",
            progress = "$supermarketCount / 36",
            background = Color(0xFFFFE6BC),
            modifier = Modifier.weight(.33f).fillMaxHeight(),
            onClick = onSupermarket
        )
        WorldTile(
            title = "廁所",
            subtitle = "洗手、洗澡和清潔用品",
            emoji = "🚽",
            progress = "$bathroomCount / 30",
            background = Color(0xFFD9F3F9),
            modifier = Modifier.weight(.33f).fillMaxHeight(),
            onClick = onBathroom
        )
    }
}

@Composable
private fun WorldTile(
    title: String,
    subtitle: String,
    emoji: String,
    progress: String,
    background: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 59.sp, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF37415C))
            Text(subtitle, fontSize = 12.sp, textAlign = TextAlign.Center,
                maxLines = 2, lineHeight = 16.sp)
            Spacer(Modifier.height(10.dp))
            Text("⭐ 已玩過 $progress", fontSize = 14.sp,
                fontWeight = FontWeight.Bold)
        }
    }
}
