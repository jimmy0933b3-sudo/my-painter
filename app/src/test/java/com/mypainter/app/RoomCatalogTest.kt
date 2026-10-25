package com.mypainter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validate every bedroom drawing and every fillable interior cell. */
class RoomCatalogTest {
    @Test fun all36RoomItemsAreDistinctAndHaveMatchingDescriptions() {
        assertEquals(36, RoomCatalog.items.size)
        assertEquals(36, RoomCatalog.items.map { it.id }.toSet().size)
        assertEquals(36, RoomCatalog.items.map { it.name }.toSet().size)
        assertEquals(RoomCatalog.drawingIds, RoomCatalog.descriptions.keys)
        assertEquals(RoomCatalog.drawingIds, RoomCatalog.drawings.keys)
        assertEquals(RoomCatalog.drawingIds, ShopCatalog.forCategory("room").map { it.id }.toSet())
        for (item in RoomCatalog.items) {
            assertEquals("room", item.categoryId)
            assertTrue(item.name.isNotBlank())
            assertTrue(item.icon.isNotBlank())
            assertTrue(ItemSpeech.intro(item).contains(item.name))
            assertTrue(ItemSpeech.description(item).length >= 8)
        }
    }

    @Test fun everyPictureHasUniqueRealInnerLinesAndFillableSections() {
        val fingerprints = mutableSetOf<Int>()
        for (item in RoomCatalog.items) {
            val ink = LineArtV2.unpack(item.id)
            assertEquals(144 * 144, ink.size)
            assertTrue("${item.id} missing outline", ink.count { it } > 160)
            assertTrue("${item.id} picture filled with ink", ink.count { it } < ink.size / 2)
            assertTrue("${item.id} reuses another picture", fingerprints.add(ink.contentHashCode()))
            val regions = LineArtRegions(ink, 144)
            assertTrue("${item.id} should have at least two independent coloring sections",
                regions.count >= 2)
            assertTrue("${item.id} primary section too small",
                regions.regionSizes.values.maxOrNull()!! >= 150)
            assertNull(regions.at(0, 0))
            assertNull(regions.at(143, 143))
            val filled = regions.regionSizes.keys.associateWith { 0xFFE53935.toInt() }
            assertEquals(regions.regionSizes.values.sum(), regions.coloredPixels(filled))
            for ((regionId, pixelCount) in regions.regionSizes) {
                assertEquals("${item.id} region ${regionId} not completely filled",
                    pixelCount, regions.coloredPixels(mapOf(regionId to 0xFF2877D4.toInt())))
                val first = regions.ids.indexOfFirst { it == regionId }
                assertTrue(first >= 0)
                assertEquals(regionId, regions.at(first % 144, first / 144))
            }
        }
        assertEquals(36, fingerprints.size)
    }

    @Test fun oldWorldsAreStillAccessibleAndArtworkNamesDoNotCollide() {
        val allIds = ShopCatalog.items.map { it.id }
        assertEquals(102, allIds.size)
        assertEquals(102, allIds.toSet().size)
        assertEquals(36, ShopCatalog.supermarketCategories.sumOf {
            ShopCatalog.forCategory(it.id).size
        })
        assertEquals(30, ShopCatalog.forCategory("bathroom").size)
        assertEquals(36, ShopCatalog.forCategory("room").size)
        assertFalse(RoomCatalog.drawingIds.any { it in BathroomCatalog.drawingIds })
        assertTrue(ShopCatalog.categories.any { it.id == "room" && it.name == "房間" })
    }
}
