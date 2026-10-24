package com.mypainter.app

/** Built-in kid-friendly line drawings sourced from six original printable sheets. */
internal object LineArtSources {
    fun encoded(id: String): String =
        LineArtFruit.images[id]
            ?: LineArtVegetable.images[id]
            ?: LineArtBakery.images[id]
            ?: LineArtDairy.images[id]
            ?: LineArtGrocery.images[id]
            ?: LineArtToys.images[id]
            ?: error("Missing source coloring sheet for $id")

    val allItemIds: Set<String> by lazy {
        LineArtFruit.images.keys + LineArtVegetable.images.keys +
            LineArtBakery.images.keys + LineArtDairy.images.keys +
            LineArtGrocery.images.keys + LineArtToys.images.keys
    }
}
