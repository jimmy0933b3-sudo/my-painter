package com.mypainter.app

import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.zip.InflaterInputStream

/**
 * Second-generation line art generated from the newly redrawn 6x6 coloring sheets.
 * A category is a single zlib blob holding six 144x144 packed ink bitmaps.
 * These are built-in assets; no runtime OCR, screenshots, emoji font or internet.
 */
internal object LineArtV2 {
    private val groups = mapOf(
        "fruit" to (listOf("apple","banana","grape","strawberry","watermelon","orange") to NewFruitArt.pack),
        "vegetable" to (listOf("carrot","tomato","corn","broccoli","cucumber","pumpkin") to NewVegetableArt.pack),
        "bakery" to (listOf("toast","bread","donut","cake","cookie","croissant") to NewBakeryArt.pack),
        "dairy" to (listOf("milk","juice","water","yogurt","cheese","soy") to NewDairyArt.pack),
        "grocery" to (listOf("rice","eggs","cereal","pasta","jam","can") to NewGroceryArt.pack),
        "toys" to (listOf("cart","basket","register","teddy","toycar","ball") to NewToysArt.pack)
    )
    private val unpacked = HashMap<String, ByteArray>()

    fun unpack(id: String): BooleanArray {
        val item = groups.entries.firstOrNull { id in it.value.first }
            ?: error("Unknown coloring item: $id")
        val bits = synchronized(unpacked) {
            unpacked.getOrPut(item.key) {
                val compressed = Base64.getDecoder().decode(item.value.second)
                val expanded = InflaterInputStream(ByteArrayInputStream(compressed))
                    .use { it.readBytes() }
                require(expanded.size == LineArtCodec.PACKED_LENGTH * 6) {
                    "Invalid six-panel source for " + item.key
                }
                expanded
            }
        }
        val index = item.value.first.indexOf(id)
        val from = index * LineArtCodec.PACKED_LENGTH
        return LineArtCodec.unpackPacked(bits.copyOfRange(from, from + LineArtCodec.PACKED_LENGTH))
    }

    fun ids(): Set<String> = groups.values.flatMap { it.first }.toSet()
}
