package com.mypainter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val cream = Color(0xFFFFF8EC)
private val navy = Color(0xFF37415C)
private val peach = Color(0xFFFFDCA8)
private val mint = Color(0xFFD3F4E4)
private val lilac = Color(0xFFE6DDFD)

class MainActivity : ComponentActivity() {
    private lateinit var audio: AudioGuide
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ArtworkStore(this)
        audio = AudioGuide(store, this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFFBB6F35),
                secondary = Color(0xFF428B71),
                background = cream,
                surface = Color.White,
                onBackground = navy,
                onSurface = navy
            )) { PainterApp(store, audio) }
        }
    }
    override fun onDestroy() {
        audio.close()
        super.onDestroy()
    }
}

@Composable
private fun PainterApp(store: ArtworkStore, audio: AudioGuide) {
    var page by remember { mutableStateOf("home") }
    var group by remember { mutableStateOf("fruit") }
    var selected by remember { mutableStateOf(ShopCatalog.items.first()) }
    var progressTick by remember { mutableIntStateOf(0) }
    fun back() {
        page = when (page) {
            "draw" -> "category"
            "category" -> if (group == "bathroom" || group == "room") "home" else "supermarket"
            else -> "home"
        }
    }
    BackHandler(page != "home") { back() }
    Surface(modifier = Modifier.fillMaxSize(), color = cream) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (page) {
                "home" -> WorldHomeScreen(
                    store, progressTick,
                    onSupermarket = { page = "supermarket"; audio.click() },
                    onBathroom = { group = "bathroom"; page = "category"; audio.click() },
                    onRoom = { group = "room"; page = "category"; audio.click() },
                    onGallery = { page = "gallery"; audio.click() },
                    onSettings = { page = "settings"; audio.click() })
                "supermarket" -> HomeScreen(store, progressTick,
                    onCategory = { group = it; page = "category"; audio.click() },
                    onGallery = { page = "gallery"; audio.click() },
                    onSettings = { page = "settings"; audio.click() })
                "category" -> CategoryScreen(group, store, progressTick, onBack = ::back,
                    onItem = {
                        selected = it
                        // First visit counts as played, even without finishing a trace.
                        if (!store.hasWork(it.id)) store.save(it.id, store.read(it.id))
                        progressTick++
                        page = "draw"
                        audio.click()
                        audio.speakItem(it)
                    })
                "draw" -> DrawingScreen(selected, store, audio, onBack = ::back,
                    onSaved = { progressTick++ })
                "gallery" -> GalleryScreen(store, progressTick, onBack = ::back,
                    onOpen = {
                        selected = it
                        group = it.categoryId
                        page = "draw"
                        audio.speakItem(it)
                    })
                "settings" -> SettingsScreen(audio, onBack = ::back)
            }
        }
    }
}

@Composable
fun PageHeading(icon: String, title: String, subtitle: String, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Text("❮", modifier = Modifier.clickable(onClick = onBack)
                .padding(end = 16.dp, top = 7.dp, bottom = 7.dp), fontSize = 24.sp, color = navy)
        }
        Column(Modifier.weight(1f)) {
            Text(icon + "  " + title, fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold, color = navy)
            Text(subtitle, fontSize = 13.sp, color = Color(0xFF7B8192))
        }
    }
}

@Composable
private fun HomeScreen(
    store: ArtworkStore, progressTick: Int,
    onCategory: (String) -> Unit, onGallery: () -> Unit, onSettings: () -> Unit
) {
    val completed = remember(progressTick) {
        ShopCatalog.items.count { it.categoryId != "bathroom" && it.categoryId != "room" && store.hasWork(it.id) }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compactLandscape = maxWidth < 760.dp
        Row(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Overview and navigation live on the left in landscape.
            Column(
                Modifier.fillMaxWidth(if (compactLandscape) 0.36f else 0.29f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🎨 小小畫家", fontWeight = FontWeight.ExtraBold,
                    fontSize = if (compactLandscape) 21.sp else 27.sp,
                    color = navy)
                Card(
                    colors = CardDefaults.cardColors(containerColor = peach),
                    shape = RoundedCornerShape(19.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("🛒 超市樂園", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("36 樣商品等你來畫！", fontSize = 14.sp)
                        Spacer(Modifier.height(7.dp))
                        Text("⭐ 完成 $completed / 36", fontSize = 15.sp,
                            fontWeight = FontWeight.Bold)
                    }
                }
                NavigationTile("🖼️", "我的作品", lilac,
                    Modifier.fillMaxWidth(), onGallery)
                NavigationTile("🔊", "家長設定", mint,
                    Modifier.fillMaxWidth(), onSettings)
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "挑選一個超市區域吧！",
                        fontSize = if (compactLandscape) 18.sp else 22.sp,
                        fontWeight = FontWeight.ExtraBold, color = navy
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (compactLandscape) 2 else 3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gridItems(ShopCatalog.supermarketCategories) { category ->
                        val count = remember(progressTick) { store.completedCount(category.id) }
                        Card(
                            onClick = { onCategory(category.id) },
                            shape = RoundedCornerShape(19.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                                .height(if (compactLandscape) 126.dp else 144.dp)
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(category.icon, fontSize = if (compactLandscape) 30.sp else 37.sp)
                                Text(category.name, fontWeight = FontWeight.Bold,
                                    fontSize = if (compactLandscape) 14.sp else 16.sp,
                                    maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { count / 6f },
                                    modifier = Modifier.fillMaxWidth().height(5.dp),
                                    color = Color(0xFF65B894),
                                    trackColor = Color(0xFFE7E7E7)
                                )
                                Text("$count / 6", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationTile(icon: String, label: String, color: Color,
                           modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 28.sp)
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CategoryScreen(
    group: String, store: ArtworkStore, progressTick: Int,
    onBack: () -> Unit, onItem: (DrawingItem) -> Unit
) {
    val category = ShopCatalog.categories.first { it.id == group }
    val products = ShopCatalog.forCategory(group)
    Column(Modifier.fillMaxSize()) {
        PageHeading(category.icon, category.name, "點一個物品，開始學畫畫", onBack)
        LazyVerticalGrid(columns = GridCells.Adaptive(152.dp),
            modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            gridItems(products) { item ->
                val work = remember(progressTick) { store.read(item.id) }
                Card(onClick = { onItem(item) }, shape = RoundedCornerShape(23.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        LineArtThumbnail(item, 112.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        val status = when {
                            store.hasWork(item.id) -> "✅ 已玩過"
                                            else -> "✏️ 開始畫"
                        }
                        Text(status, color = Color(0xFF8F807B), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryScreen(
    store: ArtworkStore, progressTick: Int,
    onBack: () -> Unit, onOpen: (DrawingItem) -> Unit
) {
    val artwork = remember(progressTick) { ShopCatalog.items.filter { store.hasWork(it.id) } }
    Column(Modifier.fillMaxSize()) {
        PageHeading("🖼️", "我的作品", "畫畫會自動保存，隨時都能繼續", onBack)
        if (artwork.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🌈\n還沒有作品\n先挑一個主題畫第一幅圖吧！",
                    lineHeight = 32.sp, textAlign = TextAlign.Center, fontSize = 19.sp)
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                gridItems(artwork) { item ->
                    Card(onClick = { onOpen(item) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            LineArtThumbnail(item, 108.dp)
                            Text(item.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("點擊繼續編輯", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(audio: AudioGuide, onBack: () -> Unit) {
    var voice by remember { mutableStateOf(audio.voiceEnabled) }
    var sound by remember { mutableStateOf(audio.soundEnabled) }
    var easy by remember { mutableStateOf(audio.easyMode) }
    Column(Modifier.fillMaxSize()) {
        PageHeading("⚙️", "家長設定", "調整適合孩子的畫畫方式", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingLine("🔊 語音說明", "朗讀物品與畫畫提示", voice) {
                voice = it; audio.voiceEnabled = it; if (it) audio.speak("歡迎來畫畫")
            }
            SettingLine("🎶 操作音效", "按鍵、描線成功、提示聲", sound) {
                sound = it; audio.soundEnabled = it
            }
            SettingLine("🧸 初學者模式（4～5 歲）", "描線容錯較寬，容易完成", easy) {
                easy = it; audio.easyMode = it
            }
            Text("🔒 無廣告、不收集個人資訊，畫畫資料只保存在此裝置。" +
                "若系統沒有安裝離線中文語音，部分朗讀功能可能無法使用。",
                color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SettingLine(title: String, hint: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(hint, color = Color.Gray, fontSize = 12.sp)
            }
            Switch(checked = value, onCheckedChange = onChange)
        }
    }
}

/** Pickers and saved gallery share the actual in-app coloring picture. */
@Composable
private fun LineArtThumbnail(item: DrawingItem, side: Dp) {
    val image = remember(item.id) { LineArtwork.create(item.id).preview.asImageBitmap() }
    Image(
        bitmap = image,
        contentDescription = item.name + "的著色線稿",
        modifier = Modifier.size(side),
        contentScale = ContentScale.Fit
    )
}
