package com.mypainter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class LineArtAssetsTest {
    @Test fun everyCatalogItemHasDistinctIntegratedArtwork() {
        assertEquals(36, ShopCatalog.items.size)
        assertEquals(
            ShopCatalog.items.map { it.id }.toSet(),
            LineArtSources.allItemIds
        )
    }

    @Test fun allThirtySixOriginalLineDrawingsCanBeDecoded() {
        ShopCatalog.items.forEach { item ->
            val ink = LineArtCodec.unpack(LineArtSources.encoded(item.id))
            assertEquals(LineArtCodec.SIDE * LineArtCodec.SIDE, ink.size)
            val linePixels = ink.count { it }
            assertTrue("Blank outline for " + item.id, linePixels > 100)
            assertTrue("Solid block for " + item.id, linePixels < ink.size / 2)
            assertFalse("No inside details for " + item.id, ink.take(100).all { it })
        }
    }

    @Test fun sixCategoriesAllHaveSixPictures() {
        val groups = listOf(
            LineArtFruit.images, LineArtVegetable.images,
            LineArtBakery.images, LineArtDairy.images,
            LineArtGrocery.images, LineArtToys.images
        )
        groups.forEach { assertEquals(6, it.size) }
        assertEquals(36, groups.flatMap { it.keys }.toSet().size)
    }

    @Test(expected = IllegalStateException::class)
    fun unknownItemDoesNotSilentlyFallBackToEmoji() {
        LineArtSources.encoded("no_such_drawing")
    }
}
