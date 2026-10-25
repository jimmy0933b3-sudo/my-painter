package com.mypainter.app
import org.junit.Assert.*
import org.junit.Test
class ShopCatalogTest {
    @Test fun categoriesAndDrawings() {
        assertEquals(8, ShopCatalog.categories.size)
        assertEquals(102, ShopCatalog.items.size)
        assertEquals(102, ShopCatalog.items.map { it.id }.distinct().size)
        ShopCatalog.supermarketCategories.forEach { assertEquals(6, ShopCatalog.forCategory(it.id).size) }
        assertEquals(30, ShopCatalog.forCategory("bathroom").size)
        assertEquals(36, ShopCatalog.forCategory("room").size)
    }
    @Test fun allItemsHaveVectorOutlines() {
        ShopCatalog.items.forEach {
            assertTrue(it.paths.size >= 2)
            assertTrue(it.paths.all { path -> path.startsWith("M ") && path.length >= 12 })
        }
    }
    @Test fun has36UniqueColors() {
        assertEquals(36, Crayons.colors.size)
        assertEquals(36, Crayons.colors.map { it.color }.distinct().size)
    }
}
