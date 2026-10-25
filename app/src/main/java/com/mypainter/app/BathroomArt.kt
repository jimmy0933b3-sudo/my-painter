package com.mypainter.app

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Vector-to-ink rasterizer for offline bathroom coloring pages. */
internal object BathroomArt {
    fun unpack(id: String): BooleanArray {
        val script = BathroomFixtures.scripts[id]
            ?: BathroomWashing.scripts[id]
            ?: BathroomTextiles.scripts[id]
            ?: BathroomTools.scripts[id]
            ?: BathroomExtras.scripts[id]
            ?: error("Unknown bathroom image: $id")
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
            val steps = (3 * max(abs(x2-x1),abs(y2-y1))).toInt().coerceAtLeast(1)
            for (i in 0..steps) {
                val t = i.toDouble() / steps
                dot(x1 + (x2-x1)*t, y1 + (y2-y1)*t)
            }
        }
        script.split(';').forEach { shape ->
            val parts = shape.trim().split(Regex("\\s+"))
            if (parts.isEmpty()) return@forEach
            val v = parts.drop(1).map { it.toDouble() }
            when (parts[0]) {
                "R" -> {
                    val (x1,y1,x2,y2) = v
                    stroke(x1,y1,x2,y1)
                    stroke(x2,y1,x2,y2)
                    stroke(x2,y2,x1,y2)
                    stroke(x1,y2,x1,y1)
                }
                "E" -> {
                    val (x1,y1,x2,y2) = v
                    val cx=(x1+x2)/2
                    val cy=(y1+y2)/2
                    val rx=(x2-x1)/2
                    val ry=(y2-y1)/2
                    for (i in 0..360) {
                        val a=i*Math.PI*2/360
                        dot(cx+rx*cos(a),cy+ry*sin(a))
                    }
                }
                "L", "P" -> {
                    for (j in 0 until v.size/2-1)
                        stroke(v[j*2],v[j*2+1],v[j*2+2],v[j*2+3])
                    if(parts[0]=="P") stroke(v[v.size-2],v.last(),v[0],v[1])
                }
                else -> error("Unknown shape: " + parts[0])
            }
        }
        return mask
    }
}
