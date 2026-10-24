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
            val strokes = json.optJSONArray("marks") ?: JSONArray()
            val marks = (0 until strokes.length()).map { idx ->
                val value = strokes.getJSONObject(idx)
                val points = value.getJSONArray("points")
                InkStroke(value.getInt("color"), value.getDouble("width").toFloat(),
                    (0 until points.length()).map { i ->
                        val array = points.getJSONArray(i)
                        SketchPoint(array.getDouble(0).toFloat(), array.getDouble(1).toFloat())
                    })
            }
            SavedArtwork(json.optInt("stage", 0), fills, marks)
        }.getOrDefault(SavedArtwork())
    }

    fun save(id: String, artwork: SavedArtwork) {
        require(id.matches(Regex("[a-z0-9_-]+")))
        val json = JSONObject()
        json.put("stage", artwork.stage)
        json.put("fills", JSONObject().apply {
            artwork.fills.forEach { (key, color) -> put(key.toString(), color) }
        })
        json.put("marks", JSONArray().apply {
            artwork.marks.forEach { mark ->
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
        })
        val target = File(directory, "$id.json")
        val temporary = File(directory, "$id.json.tmp")
        temporary.writeText(json.toString())
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }

    fun completedCount(categoryId: String): Int =
        ShopCatalog.forCategory(categoryId).count { read(it.id).stage >= it.paths.size }

    fun hasWork(id: String): Boolean = File(directory, "$id.json").exists()
}
