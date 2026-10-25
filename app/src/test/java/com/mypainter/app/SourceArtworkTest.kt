package com.mypainter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceArtworkTest {
    @Test fun everySupermarketItemUsesItsActualCatalogIconAsSource() {
        val supermarket = ShopCatalog.items.filter { it.categoryId != "bathroom" }
        assertEquals(36, supermarket.size)
        supermarket.forEach { item ->
            assertTrue("Missing source picture", item.icon.isNotBlank())
        }
        assertEquals(36, supermarket.map { it.icon }.distinct().size)
    }

    @Test fun regionsAreDerivedFromOriginalPixelColors() {
        assertNotEquals(
            EmojiArtwork.regionKey(0xFFE53935.toInt()),
            EmojiArtwork.regionKey(0xFF42B548.toInt())
        )
        assertNotEquals(
            EmojiArtwork.regionKey(0xFF42B548.toInt()),
            EmojiArtwork.regionKey(0xFF2877D4.toInt())
        )
        assertEquals(
            EmojiArtwork.regionKey(0xFFFF0900.toInt()),
            EmojiArtwork.regionKey(0xFFE82013.toInt())
        )
    }

    @Test fun chosenFillCanTargetDifferentOriginalImageRegions() {
        val colors = mutableMapOf<Int, Int>()
        val fruit = EmojiArtwork.regionKey(0xFFE43C35.toInt())
        val leaf = EmojiArtwork.regionKey(0xFF42B548.toInt())
        PaintActions.fillRegion(colors, fruit, 0xFF2877D4.toInt())
        PaintActions.fillRegion(colors, leaf, 0xFFFFFF00.toInt())
        assertEquals(0xFF2877D4.toInt(), colors[fruit])
        assertEquals(0xFFFFFF00.toInt(), colors[leaf])
    }

    @Test fun oldDrawingsStayReadableWithAdditionalRasterFields() {
        val saved = SavedArtwork(
            stage = 2,
            fills = mapOf(0 to 0xFF119911.toInt()),
            marks = listOf(InkStroke(0xFFFF0000.toInt(), 4f,
                listOf(SketchPoint(10f, 10f), SketchPoint(20f, 20f))))
        )
        assertEquals(2, saved.stage)
        assertTrue(saved.rasterFills.isEmpty())
        assertTrue(saved.traceMarks.isEmpty())
        assertFalse(saved.marks.isEmpty())
        assertEquals(1, saved.fills.size)
    }

    @Test fun allThirtySixSelectedCrayonValuesAreDistinct() {
        assertEquals(36, Crayons.colors.map { it.color }.distinct().size)
    }
}
