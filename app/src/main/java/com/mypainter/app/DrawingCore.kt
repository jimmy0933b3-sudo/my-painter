package com.mypainter.app

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Region
import androidx.core.graphics.PathParser
import kotlin.math.hypot

data class SketchPoint(val x: Float, val y: Float)
data class InkStroke(val color: Int, val width: Float, val points: List<SketchPoint>)
data class SavedArtwork(
    val stage: Int = 0,
    val fills: Map<Int, Int> = emptyMap(),
    val marks: List<InkStroke> = emptyList(),
    // Chosen crayon for each finished traced segment. Optional for older saved art.
    val traceColors: Map<Int, Int> = emptyMap(),
    // Source-icon artwork engine; legacy fills/marks are kept for upgrades.
    val rasterFills: Map<Int, Int> = emptyMap(),
    val traceMarks: List<InkStroke> = emptyList()
)

enum class DrawingMode { TRACE, BRUSH, BUCKET }

object DrawingCore {
    fun parse(data: String): Path = PathParser.createPathFromPathData(data) ?: Path()

    fun coverage(data: String, points: List<SketchPoint>, tolerance: Float): Float {
        if (points.size < 2) return 0f
        val measure = PathMeasure(parse(data), false)
        val length = measure.length
        if (length < 1f) return 0f
        val pos = FloatArray(2)
        var visited = 0
        val samples = 64
        for (i in 0 until samples) {
            measure.getPosTan(length * i / (samples - 1), pos, null)
            if (points.any { hypot(it.x - pos[0], it.y - pos[1]) < tolerance }) visited++
        }
        return visited.toFloat() / samples
    }

    fun firstPoint(data: String): SketchPoint? {
        val measure = PathMeasure(parse(data), false)
        val result = FloatArray(2)
        return if (measure.getPosTan(0f, result, null)) SketchPoint(result[0], result[1]) else null
    }

    fun hitRegion(item: DrawingItem, point: SketchPoint): Int? {
        val clip = Region(0, 0, 101, 101)
        // Smallest enclosing region is preferred, allowing the donut's hole to stay unfilled.
        return item.paths.indices.reversed().firstOrNull { index ->
            val path = parse(item.paths[index])
            val region = Region()
            region.setPath(path, clip)
            region.contains(point.x.toInt(), point.y.toInt())
        }
    }

    fun render(
        canvas: Canvas,
        item: DrawingItem,
        saved: SavedArtwork,
        preview: List<SketchPoint> = emptyList(),
        previewColor: Int = android.graphics.Color.BLACK,
        previewWidth: Float = 4f,
        mode: DrawingMode = DrawingMode.TRACE
    ) {
        val paths = item.paths.map(::parse)
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 1.6f
            color = android.graphics.Color.rgb(72, 73, 86)
        }

        paths.forEachIndexed { index, path ->
            saved.fills[index]?.let { color ->
                canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    style = Paint.Style.FILL
                })
            }
        }

        val allDone = saved.stage >= paths.size
        paths.forEachIndexed { index, path ->
            outline.color = when {
                index < saved.stage -> saved.traceColors[index] ?: android.graphics.Color.rgb(51, 160, 120)
                mode != DrawingMode.TRACE || allDone -> android.graphics.Color.rgb(67, 68, 80)
                index == saved.stage -> android.graphics.Color.rgb(242, 138, 57)
                else -> android.graphics.Color.rgb(204, 202, 214)
            }
            outline.strokeWidth = if (index == saved.stage && mode == DrawingMode.TRACE) 2.6f else 1.7f
            outline.pathEffect = if (index == saved.stage && mode == DrawingMode.TRACE)
                DashPathEffect(floatArrayOf(5f, 4f), 0f) else null
            canvas.drawPath(path, outline)
        }

        // Brush strokes are clipped to the overall silhouette, so children cannot color outside.
        if (paths.isNotEmpty()) {
            canvas.save()
            canvas.clipPath(paths[0])
            saved.marks.forEach { drawInk(canvas, it.color, it.width, it.points) }
            if (mode == DrawingMode.BRUSH) drawInk(canvas, previewColor, previewWidth, preview)
            canvas.restore()
        }

        if (mode == DrawingMode.TRACE && !allDone) {
            drawInk(canvas, previewColor, 3.2f, preview)
            firstPoint(item.paths[saved.stage])?.let {
                canvas.drawCircle(it.x, it.y, 3.6f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.rgb(255, 156, 45)
                    style = Paint.Style.FILL
                })
            }
        }

        // Outlines stay legible above the children's coloring.
        if (mode != DrawingMode.TRACE || allDone) {
            outline.apply {
                color = android.graphics.Color.rgb(80, 75, 89)
                strokeWidth = 1.6f
                pathEffect = null
            }
            paths.forEachIndexed { index, path ->
                outline.color = if (index < saved.stage)
                    saved.traceColors[index] ?: android.graphics.Color.rgb(80, 75, 89)
                else android.graphics.Color.rgb(80, 75, 89)
                canvas.drawPath(path, outline)
            }
        }
    }

    private fun drawInk(canvas: Canvas, color: Int, width: Float, points: List<SketchPoint>) {
        if (points.isEmpty()) return
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            if (points.size == 1) lineTo(points[0].x + .03f, points[0].y + .03f)
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.strokeWidth = width
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })
    }

    fun progress(item: DrawingItem, artwork: SavedArtwork): Float =
        if (item.paths.isEmpty()) 0f else artwork.stage.toFloat() / item.paths.size
}
