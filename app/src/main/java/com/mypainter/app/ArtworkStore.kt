package com.mypainter.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ArtworkStore(context: Context) {
    private val directory = File(context.filesDir, "drawings").apply { mkdirs() }
    val preferences = context.getSharedPreferences("my_painter_settings", Context.MODE_PRIVATE)

    fun read(id: String): SavedArtwork {
        return runCatching {
            val file = File(directory, "$id.json")
            if (!file.exists()) return SavedArtwork()
            val json = JSONObject(file.readText())
            val fillData = json.optJSONObject("fills") ?: JSONObject()
            val fills = buildMap<Int, Int> {
                fillData.keys().forEach { key -> put(key.toInt(), fillData.getInt(key)) }
            }
            val traceData = json.optJSONObject("traceColors") ?: JSONObject()
            val traceColors = buildMap<Int, Int> {
                traceData.keys().forEach { key -> put(key.toInt(), traceData.getInt(key)) }
            }
            val raster = json.optJSONObject("rasterFills") ?: JSONObject()
            val rasterFills = buildMap<Int, Int> {
                raster.keys().forEach { key -> put(key.toInt(), raster.getInt(key)) }
            }
            fun decodeStrokes(array: JSONArray): List<InkStroke> =
                (0 until array.length()).map { idx ->
                val value = array.getJSONObject(idx)
                val points = value.getJSONArray("points")
                InkStroke(value.getInt("color"), value.getDouble("width").toFloat(),
                    (0 until points.length()).map { i ->
                        val array = points.getJSONArray(i)
                        SketchPoint(array.getDouble(0).toFloat(), array.getDouble(1).toFloat())
                    })
            }
            val marks = decodeStrokes(json.optJSONArray("marks") ?: JSONArray())
            val traceMarks = decodeStrokes(json.optJSONArray("traceMarks") ?: JSONArray())
            SavedArtwork(json.optInt("stage", 0), fills, marks, traceColors,
                rasterFills, traceMarks)
        }.getOrDefault(SavedArtwork())
    }

    fun save(id: String, artwork: SavedArtwork) {
        require(id.matches(Regex("[a-z0-9_-]+")))
        val json = JSONObject()
        json.put("stage", artwork.stage)
        json.put("fills", JSONObject().apply {
            artwork.fills.forEach { (key, color) -> put(key.toString(), color) }
        })
        json.put("traceColors", JSONObject().apply {
            artwork.traceColors.forEach { (key, color) -> put(key.toString(), color) }
        })
        json.put("rasterFills", JSONObject().apply {
            artwork.rasterFills.forEach { (region, color) -> put(region.toString(), color) }
        })
        fun encodeStrokes(strokes: List<InkStroke>): JSONArray = JSONArray().apply {
            strokes.forEach { mark ->
                put(JSONObject().apply {
                    put("color", mark.color)
                    put("width", mark.width)
                    put("points", JSONArray().apply {
                        mark.points.forEach { point ->
                            put(JSONArray().put(point.x.toDouble()).put(point.y.toDouble()))
                        }
                    })
                })
            }
        }
        json.put("marks", encodeStrokes(artwork.marks))
        json.put("traceMarks", encodeStrokes(artwork.traceMarks))
        val target = File(directory, "$id.json")
        val temporary = File(directory, "$id.json.tmp")
        temporary.writeText(json.toString())
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }

    fun completedCount(categoryId: String): Int =
        ShopCatalog.forCategory(categoryId).count { hasWork(it.id) }

    fun hasWork(id: String): Boolean = File(directory, "$id.json").exists()
}
