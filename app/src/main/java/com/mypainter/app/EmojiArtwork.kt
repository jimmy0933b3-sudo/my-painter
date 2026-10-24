package com.mypainter.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.hypot

/**
 * The icon shown in the catalog IS the source for the drawing. No independently
 * invented Bézier paths: outline, region map and brush mask share this bitmap.
 *
 * Emoji glyphs are supplied by Android's own color emoji font, so the artwork
 * appearance is consistent with the very same item icon shown in the picker.
 */
class EmojiArtwork private constructor(
    val original: Bitmap,
    private val outlines: Bitmap,
    private val regionIds: IntArray,
    private val alphaMask: Bitmap,
    private val boundaryPoints: List<SketchPoint>
) {
    private var lastFills: Map<Int, Int>? = null
    private var fillCache: Bitmap? = null

    fun regionAt(point: SketchPoint): Int? {
        val px = ((point.x / 100f) * SIZE).toInt().coerceIn(0, SIZE - 1)
        val py = ((point.y / 100f) * SIZE).toInt().coerceIn(0, SIZE - 1)
        val region = regionIds[py * SIZE + px]
        return if (region >= 0) region else null
    }

    fun traceCoverage(traces: List<InkStroke>, tolerance: Float = 3.3f): Float {
        if (boundaryPoints.isEmpty() || traces.isEmpty()) return 0f
        val samples = traces.flatMap { it.points }
        if (samples.size < 2) return 0f
        var covered = 0
        for (point in boundaryPoints) {
            if (samples.any { hypot(it.x - point.x, it.y - point.y) <= tolerance }) covered++
        }
        return covered.toFloat() / boundaryPoints.size
    }

    fun render(
        canvas: Canvas,
        rasterFills: Map<Int, Int>,
        brushMarks: List<InkStroke>,
        traceMarks: List<InkStroke>,
        liveStroke: List<SketchPoint>,
        brushColor: Int,
        brushSize: Float,
        mode: DrawingMode,
        showOriginal: Boolean
    ) {
        val dst = RectF(0f, 0f, 100f, 100f)
        val smooth = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // The enlarged original remains visible as a quiet drawing reference;
        // "原圖" temporarily restores its full color for comparison.
        smooth.alpha = if (showOriginal) 255 else 42
        canvas.drawBitmap(original, null, dst, smooth)
        if (!showOriginal) {
            val fillBitmap = fillBitmapFor(rasterFills)
            canvas.drawBitmap(fillBitmap, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))

            // DST_IN masks all brush strokes to the source glyph alpha, including
            // transparent holes and separated parts. No more bleeding to other cells.
            canvas.saveLayer(dst, null)
            brushMarks.forEach { drawStroke(canvas, it.color, it.width, it.points) }
            if (mode == DrawingMode.BRUSH) drawStroke(canvas, brushColor, brushSize, liveStroke)
            val maskPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            canvas.drawBitmap(alphaMask, null, dst, maskPaint)
            canvas.restore()

            // Every outline pixel is derived from the ACTUAL item image, rather
            // than guessing its shape with manually written paths.
            canvas.drawBitmap(outlines, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            traceMarks.forEach { drawStroke(canvas, it.color, it.width, it.points) }
            if (mode == DrawingMode.TRACE) drawStroke(canvas, brushColor, 1.1f, liveStroke)
        }
    }

    private fun fillBitmapFor(fills: Map<Int, Int>): Bitmap {
        if (lastFills == fills && fillCache != null) return fillCache!!
        val source = IntArray(SIZE * SIZE)
        original.getPixels(source, 0, SIZE, 0, 0, SIZE, SIZE)
        for (i in source.indices) {
            val color = fills[regionIds[i]]
            val alpha = source[i] ushr 24
            source[i] = if (color != null && alpha > 12)
                (color and 0x00FFFFFF) or (alpha shl 24)
            else Color.TRANSPARENT
        }
        val target = Bitmap.createBitmap(source, SIZE, SIZE, Bitmap.Config.ARGB_8888)
        fillCache?.recycle()
        fillCache = target
        lastFills = fills.toMap()
        return target
    }

    companion object {
        const val SIZE = 384

        /**
         * Group nearby original color families, so "填色" can color areas like
         * a fruit's red body and green leaf independently. Key is deterministic
         * and derived from the icon pixel's RGB, never from an invented path.
         */
        fun regionKey(rgb: Int): Int {
            val r = (rgb ushr 16) and 255
            val g = (rgb ushr 8) and 255
            val b = rgb and 255
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min
            if (delta < 24 || max == 0) return 100 + (max / 64)
            val hue = when (max) {
                r -> (60f * (g - b) / delta + 360f) % 360f
                g -> 60f * (b - r) / delta + 120f
                else -> 60f * (r - g) / delta + 240f
            }
            // Hue and brightness group; retain dark outlines as a separate area.
            return (hue / 30f).toInt().coerceIn(0, 11) * 2 + if (max < 115) 1 else 0
        }

        fun create(icon: String): EmojiArtwork {
            // Render at high resolution, crop to the actual glyph's nontransparent
            // pixels, and center/scale into a common artwork coordinate space.
            val rawSize = 768
            val raw = Bitmap.createBitmap(rawSize, rawSize, Bitmap.Config.ARGB_8888)
            val rawCanvas = Canvas(raw)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                textSize = 540f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
                color = Color.BLACK
            }
            val metrics = paint.fontMetrics
            rawCanvas.drawText(icon, rawSize / 2f,
                rawSize / 2f - (metrics.ascent + metrics.descent) / 2f, paint)
            val rawPixels = IntArray(rawSize * rawSize)
            raw.getPixels(rawPixels, 0, rawSize, 0, 0, rawSize, rawSize)
            var left = rawSize
            var top = rawSize
            var right = 0
            var bottom = 0
            for (y in 0 until rawSize) {
                for (x in 0 until rawSize) {
                    if (rawPixels[y * rawSize + x] ushr 24 > 16) {
                        left = minOf(left, x)
                        right = maxOf(right, x)
                        top = minOf(top, y)
                        bottom = maxOf(bottom, y)
                    }
                }
            }
            val image = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            val artCanvas = Canvas(image)
            if (right > left && bottom > top) {
                val side = maxOf(right - left + 1, bottom - top + 1).toFloat()
                val extent = (SIZE - 24).toFloat()
                val destWidth = extent * (right - left + 1f) / side
                val destHeight = extent * (bottom - top + 1f) / side
                artCanvas.drawBitmap(
                    raw,
                    Rect(left, top, right + 1, bottom + 1),
                    RectF(
                        (SIZE - destWidth) / 2f, (SIZE - destHeight) / 2f,
                        (SIZE + destWidth) / 2f, (SIZE + destHeight) / 2f
                    ),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                )
            } else {
                // Older devices without color emoji support still get the glyph
                // fallback (and a drawable mask), not the inaccurate old paths.
                paint.textSize = 260f
                artCanvas.drawText(icon, SIZE / 2f, SIZE * .73f, paint)
            }
            raw.recycle()

            val pixels = IntArray(SIZE * SIZE)
            image.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
            val ids = IntArray(pixels.size) { i ->
                if (pixels[i] ushr 24 > 28) regionKey(pixels[i]) else -1
            }
            val edges = IntArray(pixels.size)
            val maskPixels = IntArray(pixels.size)
            val boundary = ArrayList<SketchPoint>()
            for (y in 1 until SIZE - 1) {
                for (x in 1 until SIZE - 1) {
                    val i = y * SIZE + x
                    val a = pixels[i] ushr 24
                    if (a < 32) continue
                    maskPixels[i] = (a shl 24) or 0x00FFFFFF
                    val boundaryPixel = listOf(i - 1, i + 1, i - SIZE, i + SIZE)
                        .any { pixels[it] ushr 24 < 60 }
                    val insideDetail = !boundaryPixel && a > 160 && (
                        abs((pixels[i] and 255) - (pixels[i + 3] and 255)) > 100 ||
                        abs(((pixels[i] ushr 8) and 255) -
                            ((pixels[i + 3] ushr 8) and 255)) > 110
                    )
                    if (boundaryPixel || insideDetail) {
                        edges[i] = if (boundaryPixel) 0xFF3B4559.toInt()
                            else 0x9A596072.toInt()
                        if (boundaryPixel && (x + y * 7) % 9 == 0) {
                            boundary.add(SketchPoint(x * 100f / SIZE, y * 100f / SIZE))
                        }
                    }
                }
            }
            return EmojiArtwork(
                image,
                Bitmap.createBitmap(edges, SIZE, SIZE, Bitmap.Config.ARGB_8888),
                ids,
                Bitmap.createBitmap(maskPixels, SIZE, SIZE, Bitmap.Config.ARGB_8888),
                boundary
            )
        }

        private fun drawStroke(canvas: Canvas, color: Int, width: Float, points: List<SketchPoint>) {
            if (points.isEmpty()) return
            val path = android.graphics.Path().apply {
                moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                if (points.size == 1) lineTo(points[0].x + .05f, points[0].y + .05f)
            }
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.color = color
                strokeWidth = width
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            })
        }
    }
}
