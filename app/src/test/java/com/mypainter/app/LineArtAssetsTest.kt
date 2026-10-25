package com.mypainter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression validation for EVERY line-art page and EVERY flood-fill region.
 * These tests would have caught the old unfillable-body bug: an open outer
 * contour was treated as the background, so the body had no region ID.
 */
class LineArtAssetsTest {
    @Test fun eachOfThe36CatalogItemsHasARedrawnPicture() {
        assertEquals(36, ShopCatalog.items.size)
        assertEquals(ShopCatalog.items.map { it.id }.toSet(), LineArtV2.ids())
    }

    @Test fun everyPageHasClosedTouchableRegionsAndBackgroundOutside() {
        ShopCatalog.items.forEach { item ->
            val ink = LineArtV2.unpack(item.id)
            assertEquals(LineArtCodec.SIDE * LineArtCodec.SIDE, ink.size)
            val strokePixels = ink.count { it }
            assertTrue(item.id + " has no drawn lines", strokePixels > 160)
            assertTrue(item.id + " has no white paper", strokePixels < ink.size / 2)
            val region = LineArtRegions(ink, LineArtCodec.SIDE)
            assertTrue(item.id + " needs at least two distinct enclosed sections",
                region.count >= 2)
            assertNull(item.id + " background must NOT be fillable",
                region.at(0, 0))
            assertNull(item.id + " background must NOT be fillable",
                region.at(143, 143))
            assertTrue(item.id + " main body cannot be a tiny inaccessible speck",
                region.regionSizes.values.maxOrNull()!! >= 150)
            assertTrue(item.id + " fillable area too small",
                region.regionSizes.values.sum() >= 300)
        }
    }

    @Test fun everyClosedSectionCanBeColoredExactlyAndIndependently() {
        ShopCatalog.items.forEach { item ->
            val region = LineArtRegions(LineArtV2.unpack(item.id), LineArtCodec.SIDE)
            val all = region.regionSizes.keys.associateWith { 0xFF2877D4.toInt() }
            assertEquals(
                item.id + " all colored sections must cover every fillable pixel",
                region.regionSizes.values.sum(),
                region.coloredPixels(all)
            )
            for ((id, pixels) in region.regionSizes) {
                assertEquals(
                    item.id + " region " + id + " cannot be filled completely",
                    pixels,
                    region.coloredPixels(mapOf(id to 0xFFE53935.toInt()))
                )
                val pos = region.ids.indexOfFirst { it == id }
                assertTrue(pos >= 0)
                assertEquals(
                    item.id + " clicking the actual pixel must select its own region",
                    id,
                    region.at(pos % LineArtCodec.SIDE, pos / LineArtCodec.SIDE)
                )
            }
        }
    }

    @Test fun regionsDoNotOverlapAndDoNotRecolorOtherSections() {
        ShopCatalog.items.forEach { item ->
            val region = LineArtRegions(LineArtV2.unpack(item.id), LineArtCodec.SIDE)
            val ids = region.regionSizes.keys.sorted()
            assertTrue(ids.size >= 2)
            val firstColor = mapOf(ids[0] to 0xFF00AA11.toInt())
            val secondColor = mapOf(ids[1] to 0xFF3377EE.toInt())
            assertEquals(region.regionSizes.getValue(ids[0]),region.coloredPixels(firstColor))
            assertEquals(region.regionSizes.getValue(ids[1]),region.coloredPixels(secondColor))
            assertFalse(region.ids.any { it == -1 && firstColor.containsKey(it) })
        }
    }

    @Test fun missingPictureFailsInsteadOfFallingBackToWrongEmoji() {
        try {
            LineArtV2.unpack("no_such_drawing")
            throw AssertionError("Expected rejection of unknown artwork")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("Unknown"))
        }
    }
}
