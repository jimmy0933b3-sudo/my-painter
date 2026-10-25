package com.mypainter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Compare every announced name with the exact currently shipped V2 drawing ID. */
class ItemSpeechTest {
    @Test fun descriptionsCoverExactlyAll102VisibleDrawings() {
        val catalogIds = ShopCatalog.items.map { it.id }.toSet()
        assertEquals(102, catalogIds.size)
        assertEquals(catalogIds, ItemSpeech.describedIds)
        assertEquals(catalogIds, LineArtV2.ids())
    }

    @Test fun selectionNameAndSpokenNameAreAlwaysTheSame() {
        ShopCatalog.items.forEach { item ->
            val spoken = ItemSpeech.intro(item)
            assertTrue("Wrong spoken item for ${item.id}", spoken.startsWith("這是${item.name}。"))
            assertTrue("Misses current item for ${item.id}", spoken.contains("幫${item.name}塗上"))
            assertTrue("Missing visual description for ${item.id}",
                ItemSpeech.description(item).length >= 8)
            assertFalse(spoken.contains("null"))
            assertFalse(spoken.contains("undefined"))
            assertTrue(ItemSpeech.completed(item).contains(item.name))
        }
    }

    @Test fun previouslyMismatchedPicturesNowHaveAccurateNames() {
        val expected = mapOf(
            "bread" to "法國麵包",
            "juice" to "蘋果汁",
            "yogurt" to "草莓優格",
            "soy" to "葡萄汁",
            "rice" to "白飯",
            "cereal" to "麥片",
            "pasta" to "通心粉",
            "jam" to "草莓果醬",
            "can" to "番茄罐頭",
            "ball" to "足球"
        )
        for ((id, expectedName) in expected) {
            val item = ShopCatalog.items.single { it.id == id }
            assertEquals("Shown artwork and narration do not match for $id",
                expectedName, item.name)
            assertTrue(ItemSpeech.intro(item).contains(expectedName))
            assertTrue(ItemSpeech.description(item).contains(expectedName))
        }
        assertFalse(ShopCatalog.items.any { it.id == "soy" && it.name == "豆漿" })
    }

    @Test fun allDescriptionsUseRealImagesAndHaveAStableCatalogItem() {
        for (item in ShopCatalog.items) {
            val art = LineArtV2.unpack(item.id)
            assertEquals(144 * 144, art.size)
            assertTrue(art.any { it })
            assertFalse(ItemSpeech.intro(item).contains("這是。"))
        }
    }
}
