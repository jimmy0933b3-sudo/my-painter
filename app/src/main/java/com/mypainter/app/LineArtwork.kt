package com.mypainter.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.zip.InflaterInputStream
import kotlin.math.hypot

/**
 * Child-friendly coloring pages traced from the 36 original six-panel line-art
 * sheets. Each image has INSIDE lines, not just an outside emoji silhouette.
 * All tools share the same 144x144 pixel line mask and flood-fill region map.
 */
object LineArtCodec {
    const val SIDE = 144
    const val PACKED_LENGTH = SIDE * SIDE / 8

    fun unpack(encoded: String): BooleanArray {
        val bytes = InflaterInputStream(
            ByteArrayInputStream(Base64.getDecoder().decode(encoded))
        ).use { it.readBytes() }
        require(bytes.size == PACKED_LENGTH) {
            "Malformed line art mask: ${bytes.size} instead of $PACKED_LENGTH"
        }
        return BooleanArray(SIDE * SIDE) { i ->
            (bytes[i / 8].toInt() and (1 shl (i % 8))) != 0
        }
    }
}

class LineArtwork private constructor(private val ink: BooleanArray) {
    private val side = LineArtCodec.SIDE
    private val regionIds = IntArray(ink.size)
    private val outlineImage: Bitmap
    private val samples: List<SketchPoint>
    private val regionMasks = HashMap<Int, Bitmap>()
    private var fillBitmap: Bitmap? = null
    private var lastFills: Map<Int, Int>? = null
    val preview: Bitmap get() = outlineImage

    init {
        // 4-connected flood fill of the paper's white cells.
        // Region zero is the black stroke; edge-connected white space is -1.
        val queue = IntArray(ink.size)
        var nextId = 1000 // avoids collisions with pre-upgrade emoji rasterFills
        for (p in ink.indices) {
            if (ink[p] || regionIds[p] != 0) continue
            nextId++
            var head = 0
            var tail = 1
            var touchesEdge = false
            queue[0] = p
            regionIds[p] = nextId
            while (head < tail) {
                val index = queue[head++]
                val x = index % side
                val y = index / side
                if (x == 0 || y == 0 || x == side - 1 || y == side - 1)
                    touchesEdge = true
                fun push(n: Int) {
                    if (n in ink.indices && !ink[n] && regionIds[n] == 0) {
                        regionIds[n] = nextId
                        queue[tail++] = n
                    }
                }
                if (x > 0) push(index - 1)
                if (x < side - 1) push(index + 1)
                if (y > 0) push(index - side)
                if (y < side - 1) push(index + side)
            }
            if (touchesEdge) {
                for (i in 0 until tail) regionIds[queue[i]] = -1
            }
        }

        val pixels = IntArray(ink.size) { i ->
            if (ink[i]) Color.rgb(36, 41, 52) else Color.TRANSPARENT
        }
        outlineImage = Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888)
        val trace = ArrayList<SketchPoint>()
        ink.forEachIndexed { i, black ->
            if (black && i % 5 == 0) {
                trace.add(SketchPoint(
                    (i % side + .5f) * 100f / side,
                    (i / side + .5f) * 100f / side
                ))
            }
        }
        samples = trace
    }

    fun regionAt(point: SketchPoint): Int? {
        val x = (point.x / 100f * side).toInt().coerceIn(0, side - 1)
        val y = (point.y / 100f * side).toInt().coerceIn(0, side - 1)
        val hit = regionIds[y * side + x]
        if (hit >= 1000) return hit
        // Finger tips cover much more than a single line pixel.
        for (radius in 1..3) {
            for (dy in -radius..radius) for (dx in -radius..radius) {
                if (dx * dx + dy * dy > radius * radius) continue
                val xx = x + dx
                val yy = y + dy
                if (xx !in 0 until side || yy !in 0 until side) continue
                val candidate = regionIds[yy * side + xx]
                if (candidate >= 1000) return candidate
            }
        }
        return null
    }

    fun traceCoverage(traces: List<InkStroke>, tolerance: Float = 3.3f): Float {
        if (samples.isEmpty() || traces.isEmpty()) return 0f
        val points = traces.flatMap { it.points }
        if (points.size < 2) return 0f
        var count = 0
        for (sample in samples) {
            if (points.any { hypot(it.x - sample.x, it.y - sample.y) <= tolerance })
                count++
        }
        return count.toFloat() / samples.size
    }

    private fun fillImage(fills: Map<Int, Int>): Bitmap {
        if (lastFills == fills && fillBitmap != null) return fillBitmap!!
        val pixels = IntArray(ink.size) { i ->
            val chosen = fills[regionIds[i]]
            if (chosen != null && regionIds[i] >= 1000) chosen or -0x1000000
            else Color.WHITE
        }
        val bitmap = Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888)
        fillBitmap?.recycle()
        fillBitmap = bitmap
        lastFills = fills.toMap()
        return bitmap
    }

    private fun regionMask(id: Int): Bitmap = regionMasks.getOrPut(id) {
        val mask = IntArray(ink.size) { i ->
            if (regionIds[i] == id && id >= 1000) Color.WHITE
            else Color.TRANSPARENT
        }
        Bitmap.createBitmap(mask, side, side, Bitmap.Config.ARGB_8888)
    }

    fun render(
        canvas: Canvas,
        fills: Map<Int, Int>,
        marks: List<InkStroke>,
        traces: List<InkStroke>,
        previewStroke: List<SketchPoint>,
        color: Int,
        brushSize: Float,
        mode: DrawingMode,
        showOriginal: Boolean
    ) {
        val dst = RectF(0f, 0f, 100f, 100f)
        val smooth = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        if (showOriginal) {
            canvas.drawColor(Color.WHITE)
        } else {
            canvas.drawBitmap(fillImage(fills), null, dst, smooth)

            // A stroke belongs to the first enclosed cell it touches.
            // Applying this per-stroke mask prevents brush bleeding into
            // another closed cell, including donut holes and tire details.
            fun drawContained(stroke: InkStroke) {
                val region = stroke.points.firstOrNull()?.let(::regionAt) ?: return
                canvas.saveLayer(dst, null)
                drawInk(canvas, stroke.color, stroke.width, stroke.points)
                val maskPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }
                canvas.drawBitmap(regionMask(region), null, dst, maskPaint)
                canvas.restore()
            }
            marks.forEach(::drawContained)
            if (mode == DrawingMode.BRUSH && previewStroke.isNotEmpty())
                drawContained(InkStroke(color, brushSize, previewStroke))
        }
        // Full line art drawn ABOVE the fills and brush. Internal lines always
        // stay crisp, and all panels are based on the original artwork.
        canvas.drawBitmap(outlineImage, null, dst, Paint(Paint.FILTER_BITMAP_FLAG).apply {
            // Only ink pixels are overlaid. White paper should not hide colors.
            colorFilter = null
        })
        if (!showOriginal) {
            traces.forEach { drawInk(canvas, it.color, it.width, it.points) }
            if (mode == DrawingMode.TRACE)
                drawInk(canvas, color, 1.4f, previewStroke)
        }
    }

    companion object {
        fun create(id: String): LineArtwork =
            LineArtwork(LineArtCodec.unpack(LineArtSources.encoded(id)))

        private fun drawInk(canvas: Canvas, color: Int, width: Float, points: List<SketchPoint>) {
            if (points.isEmpty()) return
            val line = Path().apply {
                moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                if (points.size == 1) lineTo(points[0].x + .03f, points[0].y + .03f)
            }
            canvas.drawPath(line, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                strokeWidth = width
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            })
        }
    }
}
