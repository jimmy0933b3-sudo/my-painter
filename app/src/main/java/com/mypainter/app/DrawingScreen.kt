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
    val traceColors = remember(item.id) { mutableStateMapOf<Int, Int>().apply { putAll(initial.traceColors) } }
    val preview = remember(item.id) { mutableStateListOf<SketchPoint>() }
    var mode by remember(item.id) {
        mutableStateOf(if (initial.stage >= item.paths.size) DrawingMode.BUCKET else DrawingMode.TRACE)
    }
    var colorIndex by remember(item.id) { mutableIntStateOf(0) }
    var brushWidth by remember(item.id) { mutableFloatStateOf(4f) }
    var message by remember(item.id) { mutableStateOf("跟著橘色虛線，慢慢畫一筆吧！") }
    val selectedColor = Crayons.colors[colorIndex]
    val completed = step >= item.paths.size

    fun snapshot(): SavedArtwork =
        SavedArtwork(step, fills.toMap(), marks.toList(), traceColors.toMap())
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
                    traceColors[step] = selectedColor.color
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
                marks.add(PaintActions.brushStroke(selectedColor.color, brushWidth, points))
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
                PaintActions.fillRegion(fills, region, selectedColor.color)
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

    // pointerInput is long-lived: always dispatch through the newest Compose callback.
    // Without rememberUpdatedState a color change updates the swatch but not the
    // touch handler, so BRUSH/BUCKET repeatedly paints with the initial red color.
    val latestTouchStart = rememberUpdatedState<(SketchPoint) -> Unit> { point ->
        touchStart(point)
    }
    val latestFinishStroke = rememberUpdatedState<() -> Unit> {
        finishStroke()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val narrowLandscape = maxWidth < 760.dp
        val sidebarWidth = if (maxWidth < 670.dp) 224.dp
            else if (maxWidth < 1050.dp) 276.dp else 328.dp
        val swatchSize = if (sidebarWidth < 250.dp) 29.dp
            else if (sidebarWidth < 300.dp) 35.dp else 43.dp

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Keep the illustration on a stationary, square canvas.
            // Only the right-hand controls scroll, so a drawing gesture
            // can never scroll the entire page away from the child.
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "❮",
                        modifier = Modifier.clickable { onBack() }
                            .padding(start = 8.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
                        color = Color(0xFF37415C),
                        fontSize = 25.sp
                    )
                    Text(item.icon, fontSize = 26.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "畫畫：" + item.name,
                            fontSize = if (narrowLandscape) 18.sp else 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Text(
                            "描線 " + step.coerceAtMost(item.paths.size) +
                                " / " + item.paths.size + " 筆",
                            fontSize = 12.sp, color = Color.Gray
                        )
                    }
                    if (completed) {
                        Text("⭐ 完成", color = Color(0xFF3F9878),
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                LinearProgressIndicator(
                    progress = { DrawingCore.progress(item, snapshot()) },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = Color(0xFF65B994),
                    trackColor = Color(0xFFFFE5C9)
                )

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Clamp to BOTH available dimensions: the picture remains
                    // large without being cut off on short landscape phones.
                    val canvasSide = minOf(maxWidth, maxHeight) - 4.dp
                    Card(
                        modifier = Modifier.size(canvasSide.coerceAtLeast(64.dp)),
                        shape = RoundedCornerShape(17.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Canvas(
                            modifier = Modifier.fillMaxSize().padding(5.dp)
                                .pointerInput(item.id, mode, step) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        down.consume()
                                        fun point(x: Float, y: Float): SketchPoint =
                                            CanvasTransform.screenToArt(
                                                x, y, size.width.toFloat(), size.height.toFloat()
                                            )

                                        latestTouchStart.value(
                                            point(down.position.x, down.position.y)
                                        )
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id }
                                            if (change == null || !change.pressed) {
                                                if (mode != DrawingMode.BUCKET)
                                                    latestFinishStroke.value()
                                                break
                                            }
                                            if (mode != DrawingMode.BUCKET) {
                                                preview.add(point(
                                                    change.position.x, change.position.y
                                                ))
                                            }
                                            change.consume()
                                        }
                                    }
                                }
                        ) {
                            val native = drawContext.canvas.nativeCanvas
                            native.save()
                            native.scale(size.width / 100f, size.height / 100f)
                            native.translate(50f, 50f)
                            native.scale(CanvasTransform.ZOOM, CanvasTransform.ZOOM)
                            native.translate(-50f, -50f)
                            DrawingCore.render(
                                native, item, snapshot(), preview.toList(),
                                selectedColor.color, brushWidth, mode
                            )
                            native.restore()
                        }
                    }
                }
                Text(
                    message,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 28.dp),
                    fontSize = if (narrowLandscape) 12.sp else 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    color = Color(0xFF855C3C),
                    fontWeight = FontWeight.Medium
                )
            }

            Card(
                modifier = Modifier.width(sidebarWidth).fillMaxHeight(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDFC))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🖍️ 畫畫工具",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ToolChoice("✍️ 描線", mode == DrawingMode.TRACE && !completed,
                            !completed, Modifier.weight(1f)) {
                            mode = DrawingMode.TRACE
                            message = "沿著虛線畫畫，從小圓點開始！"
                        }
                        ToolChoice("🖌️ 畫筆", mode == DrawingMode.BRUSH,
                            true, Modifier.weight(1f)) {
                            mode = DrawingMode.BRUSH
                            message = "在圖案裡自由上色！"
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ToolChoice("🪣 填色", mode == DrawingMode.BUCKET,
                            true, Modifier.weight(1f)) {
                            mode = DrawingMode.BUCKET
                            message = "點圖案裡面就能填色！"
                        }
                        ToolChoice("↩ 復原", false,
                            marks.isNotEmpty() || fills.isNotEmpty(),
                            Modifier.weight(1f)) {
                            if (marks.isNotEmpty()) marks.removeAt(marks.lastIndex)
                            else if (fills.isNotEmpty()) fills.remove(fills.keys.last())
                            persist()
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "🎨 36 種顏色",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier.size(22.dp).clip(CircleShape)
                                .background(Color(selectedColor.color))
                                .border(1.dp, Color(0xFF848A95), CircleShape)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(selectedColor.title, fontSize = 11.sp, maxLines = 1)
                    }

                    // Six columns with six rows. The independent sidebar scroll
                    // keeps ALL 36 colors reachable without shrinking the canvas.
                    Crayons.colors.chunked(6).forEachIndexed { rowIndex, crayons ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            crayons.forEachIndexed { columnIndex, crayon ->
                                val index = rowIndex * 6 + columnIndex
                                Box(
                                    Modifier.size(swatchSize)
                                        .clip(CircleShape)
                                        .background(Color(crayon.color))
                                        .border(
                                            if (colorIndex == index) 3.dp else 1.dp,
                                            if (colorIndex == index) Color(0xFF3D4358)
                                            else Color(0xFFB4B6BD),
                                            CircleShape
                                        )
                                        .clickable {
                                            colorIndex = index
                                            audio.click()
                                            audio.speak(crayon.title)
                                        }
                                )
                            }
                        }
                    }
                    Text("畫筆粗細", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Slider(
                        value = brushWidth,
                        onValueChange = { brushWidth = it },
                        valueRange = 2f..11f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (completed) {
                        Text(
                            "🏆 " + item.name + "描線完成！",
                            color = Color(0xFF398A69),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolChoice(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick, enabled = enabled,
            modifier = modifier.height(38.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 3.dp)
        ) { Text(text, fontSize = 12.sp, maxLines = 1) }
    } else {
        OutlinedButton(
            onClick = onClick, enabled = enabled,
            modifier = modifier.height(38.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 3.dp)
        ) { Text(text, fontSize = 12.sp, maxLines = 1) }
    }
}
