package com.mypainter.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Crayon(val title: String, val color: Int)

object Crayons {
    private val hexes = listOf(
        "#E53935", "#F28C8C", "#FF6F00", "#FFB74D", "#FDD835", "#FFF59D",
        "#7CB342", "#C5E1A5", "#00897B", "#80CBC4", "#1565C0", "#64B5F6",
        "#283593", "#7986CB", "#8E24AA", "#CE93D8", "#6D4C41", "#BCAAA4",
        "#212121", "#9E9E9E", "#FFFFFF", "#F5E6CC", "#D81B60", "#FF80AB",
        "#AD1457", "#4A148C", "#00ACC1", "#B2EBF2", "#33691E", "#DCE775",
        "#BF360C", "#FFCCBC", "#795548", "#FFD700", "#B0BEC5", "#ECEFF1"
    )
    private val names = listOf(
        "紅色", "粉紅色", "橘色", "淺橘色", "黃色", "淺黃色",
        "草綠色", "嫩綠色", "青綠色", "薄荷綠", "藍色", "天空藍",
        "深藍色", "靛藍色", "紫色", "薰衣草色", "棕色", "淺棕色",
        "黑色", "灰色", "白色", "奶油色", "桃紅色", "櫻花粉",
        "酒紅色", "深紫色", "湖藍色", "冰藍色", "墨綠色", "黃綠色",
        "磚紅色", "膚橘色", "巧克力色", "金色", "銀灰色", "珍珠白"
    )
    val colors = hexes.zip(names).map { (hex, title) ->
        Crayon(title, (hex.substring(1).toLong(16) or 0xFF000000L).toInt())
    }
}

@Composable
fun DrawingScreen(
    item: DrawingItem,
    store: ArtworkStore,
    audio: AudioGuide,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val initial = remember(item.id) { store.read(item.id) }
    var step by remember(item.id) { mutableIntStateOf(initial.stage.coerceIn(0, item.paths.size)) }
    val fills = remember(item.id) { mutableStateMapOf<Int, Int>().apply { putAll(initial.fills) } }
    val marks = remember(item.id) { mutableStateListOf<InkStroke>().apply { addAll(initial.marks) } }
    val preview = remember(item.id) { mutableStateListOf<SketchPoint>() }
    var mode by remember(item.id) {
        mutableStateOf(if (initial.stage >= item.paths.size) DrawingMode.BUCKET else DrawingMode.TRACE)
    }
    var colorIndex by remember(item.id) { mutableIntStateOf(0) }
    var brushWidth by remember(item.id) { mutableFloatStateOf(4f) }
    var message by remember(item.id) { mutableStateOf("跟著橘色虛線，慢慢畫一筆吧！") }
    val selectedColor = Crayons.colors[colorIndex]
    val completed = step >= item.paths.size

    fun snapshot(): SavedArtwork = SavedArtwork(step, fills.toMap(), marks.toList())
    fun persist() {
        store.save(item.id, snapshot())
        onSaved()
    }
    fun finishStroke() {
        val points = preview.toList()
        preview.clear()
        if (points.size < 2) return
        when (mode) {
            DrawingMode.TRACE -> {
                if (step >= item.paths.size) return
                val accuracy = DrawingCore.coverage(item.paths[step], points,
                    if (audio.easyMode) 11f else 7f)
                if (accuracy >= if (audio.easyMode) 0.42f else 0.60f) {
                    step++
                    persist()
                    audio.success()
                    if (step >= item.paths.size) {
                        message = "🎉 描線全部完成！選顏色幫" + item.name + "上色吧！"
                        audio.speak("太棒了！" + item.name + "畫好了，現在來上色吧！")
                        mode = DrawingMode.BUCKET
                    } else {
                        message = "⭐ 好棒！再畫下一筆！"
                        audio.speak("畫得真好，再畫下一筆！")
                    }
                } else {
                    message = "再試一次，沿著虛線多畫一些就好！"
                    audio.retry()
                }
            }
            DrawingMode.BRUSH -> {
                marks.add(InkStroke(selectedColor.color, brushWidth, points))
                persist()
                message = "✏️ 畫好了！可以繼續選顏色。"
                audio.click()
            }
            DrawingMode.BUCKET -> Unit
        }
    }
    fun touchStart(point: SketchPoint) {
        preview.clear()
        if (mode == DrawingMode.BUCKET) {
            val region = DrawingCore.hitRegion(item, point)
            if (region != null) {
                fills[region] = selectedColor.color
                persist()
                message = "🌈 " + selectedColor.title + "，真漂亮！"
                audio.click()
            } else {
                message = "點選線條圍起來的地方，就能填上顏色！"
            }
        } else {
            preview.add(point)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeading(item.icon, "畫畫：" + item.name,
            "第 " + step.coerceAtMost(item.paths.size) + " / " + item.paths.size + " 筆描線", onBack)
        LinearProgressIndicator(
            progress = { DrawingCore.progress(item, snapshot()) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(7.dp).clip(CircleShape),
            color = Color(0xFF68B9A0), trackColor = Color(0xFFFFE4C6)
        )
        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(12.dp)
                    .pointerInput(item.id, mode, step) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            fun point(x: Float, y: Float) = SketchPoint(
                                (x / size.width * 100f).coerceIn(0f, 100f),
                                (y / size.height * 100f).coerceIn(0f, 100f))
                            touchStart(point(down.position.x, down.position.y))
                            var active = true
                            while (active) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) {
                                    active = false
                                    if (mode != DrawingMode.BUCKET) finishStroke()
                                } else {
                                    if (mode != DrawingMode.BUCKET) {
                                        preview.add(point(change.position.x, change.position.y))
                                    }
                                    change.consume()
                                }
                            }
                        }
                    }
            ) {
                val native = drawContext.canvas.nativeCanvas
                native.save()
                native.scale(size.width / 100f, size.height / 100f)
                DrawingCore.render(native, item, snapshot(), preview.toList(),
                    selectedColor.color, brushWidth, mode)
                native.restore()
            }
        }
        Text(message, modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
            textAlign = TextAlign.Center, fontSize = 15.sp,
            color = Color(0xFF896042), fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolChoice("✍️ 描線", mode == DrawingMode.TRACE && !completed, !completed) {
                mode = DrawingMode.TRACE
                message = "沿著虛線畫畫，從小圓點開始！"
            }
            ToolChoice("🖌️ 畫筆", mode == DrawingMode.BRUSH, true) {
                mode = DrawingMode.BRUSH
                message = "試著在圖案裡自由上色！"
            }
            ToolChoice("🪣 填色", mode == DrawingMode.BUCKET, true) {
                mode = DrawingMode.BUCKET
                message = "點圖案內部就能填上顏色！"
            }
            ToolChoice("↩ 復原", false, marks.isNotEmpty() || fills.isNotEmpty()) {
                if (marks.isNotEmpty()) marks.removeAt(marks.lastIndex)
                else if (fills.isNotEmpty()) fills.remove(fills.keys.last())
                persist()
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("🎨 選擇顏色（36 色）", fontWeight = FontWeight.Bold,
            fontSize = 17.sp, modifier = Modifier.padding(horizontal = 18.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Crayons.colors.forEachIndexed { index, crayon ->
                Box(Modifier.size(if (colorIndex == index) 49.dp else 41.dp)
                    .clip(CircleShape)
                    .background(Color(crayon.color))
                    .border(if (colorIndex == index) 4.dp else 1.dp,
                        if (colorIndex == index) Color(0xFF3D4358)
                        else Color(0xFFB9BBC4), CircleShape)
                    .clickable {
                        colorIndex = index
                        audio.click()
                        audio.speak(crayon.title)
                    })
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("目前：" + selectedColor.title, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("畫筆粗細", fontSize = 13.sp)
            Slider(value = brushWidth, onValueChange = { brushWidth = it }, valueRange = 2f..11f,
                modifier = Modifier.width(135.dp))
        }
        if (completed) {
            Text("🏆 你完成了" + item.name + "的描線！",
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                color = Color(0xFF4A997C), textAlign = TextAlign.Center,
                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ToolChoice(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, enabled = enabled,
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 14.dp)) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled,
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 14.dp)) { Text(text) }
    }
}
