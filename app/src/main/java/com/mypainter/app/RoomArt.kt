package com.mypainter.app

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Deterministic 144px line-art renderer for the 36 room drawings.
 * The same black ink bitmap drives both visible outlines and enclosed fill cells.
 * R=closed rectangle, E=closed ellipse, L=connected lines, P=closed polygon.
 */
internal object RoomArt {
    fun unpack(id: String): BooleanArray {
        val script = RoomCatalog.drawings[id]
            ?: error("Unknown room drawing: $id")
        val side = LineArtCodec.SIDE
        val mask = BooleanArray(side * side)

        fun dot(x: Double, y: Double) {
            val cx = x.toInt()
            val cy = y.toInt()
            for (dy in -1..1) for (dx in -1..1) {
                val xx = cx + dx
                val yy = cy + dy
                if (xx in 0 until side && yy in 0 until side) mask[yy * side + xx] = true
            }
        }
        fun stroke(x1: Double, y1: Double, x2: Double, y2: Double) {
            val steps = (3 * max(abs(x2 - x1), abs(y2 - y1))).toInt().coerceAtLeast(1)
            for (i in 0..steps) {
                val t = i.toDouble() / steps
                dot(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t)
            }
        }
        script.split(';').forEach { shape ->
            val parts = shape.trim().split(Regex("\\s+"))
            val values = parts.drop(1).map { it.toDouble() }
            when (parts.first()) {
                "R" -> {
                    require(values.size == 4) { "Bad room rectangle: $id" }
                    val (x1, y1, x2, y2) = values
                    stroke(x1, y1, x2, y1)
                    stroke(x2, y1, x2, y2)
                    stroke(x2, y2, x1, y2)
                    stroke(x1, y2, x1, y1)
                }
                "E" -> {
                    require(values.size == 4) { "Bad room ellipse: $id" }
                    val (x1, y1, x2, y2) = values
                    val cx = (x1 + x2) / 2
                    val cy = (y1 + y2) / 2
                    val rx = (x2 - x1) / 2
                    val ry = (y2 - y1) / 2
                    for (i in 0..360) {
                        val angle = i * Math.PI * 2 / 360
                        dot(cx + rx * cos(angle), cy + ry * sin(angle))
                    }
                }
                "L", "P" -> {
                    require(values.size >= 4 && values.size % 2 == 0) {
                        "Bad room line points: $id"
                    }
                    for (j in 0 until values.size / 2 - 1) {
                        stroke(values[j*2], values[j*2+1],
                            values[j*2+2], values[j*2+3])
                    }
                    if (parts.first() == "P") {
                        stroke(values[values.size-2], values.last(), values[0], values[1])
                    }
                }
                else -> error("Unknown room drawing shape: $id / ${parts.first()}")
            }
        }
        return mask
    }
}
