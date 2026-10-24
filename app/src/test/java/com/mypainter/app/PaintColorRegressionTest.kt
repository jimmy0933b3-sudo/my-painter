package com.mypainter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaintColorRegressionTest {
    @Test fun bucketUsesEverySelectedCrayonAndChangesWhenReSelected() {
        val fills = mutableMapOf<Int, Int>()
        Crayons.colors.forEachIndexed { index, crayon ->
            PaintActions.fillRegion(fills, 0, crayon.color)
            assertEquals("bucket color index $index", crayon.color, fills[0])
        }
        PaintActions.fillRegion(fills, 1, Crayons.colors[2].color)
        assertEquals(Crayons.colors[2].color, fills[1])
        assertEquals(Crayons.colors.last().color, fills[0])
    }

    @Test fun brushUsesEverySelectedCrayonAndBrushSize() {
        val points = listOf(SketchPoint(20f, 30f), SketchPoint(40f, 45f))
        val strokes = Crayons.colors.mapIndexed { index, crayon ->
            PaintActions.brushStroke(crayon.color, 2f + index / 5f, points)
        }
        strokes.forEachIndexed { index, stroke ->
            assertEquals("brush color index $index", Crayons.colors[index].color, stroke.color)
            assertEquals(2f + index / 5f, stroke.width)
            assertEquals(points, stroke.points)
        }
        assertEquals(36, strokes.map { it.color }.distinct().size)
    }

    @Test fun tracedColorsRemainDistinctInSavedArtwork() {
        val red = Crayons.colors[0].color
        val blue = Crayons.colors[10].color
        val green = Crayons.colors[6].color
        val saved = SavedArtwork(
            stage = 3,
            traceColors = mapOf(0 to red, 1 to blue, 2 to green)
        )
        assertEquals(red, saved.traceColors[0])
        assertEquals(blue, saved.traceColors[1])
        assertEquals(green, saved.traceColors[2])
        assertNotEquals(saved.traceColors[0], saved.traceColors[1])
    }

    @Test fun legacyArtworksWithoutTraceColorsStillLoad() {
        val old = SavedArtwork(stage = 2)
        assertTrue(old.traceColors.isEmpty())
        assertEquals(2, old.stage)
    }

    @Test fun paletteContainsOpaqueDistinctColorsIncludingWhite() {
        assertEquals(36, Crayons.colors.size)
        assertEquals(36, Crayons.colors.map { it.color }.distinct().size)
        Crayons.colors.forEach { assertEquals(255, it.color ushr 24) }
        assertEquals(0xFFFFFFFF.toInt(), Crayons.colors[20].color)
        assertEquals(0xFFE53935.toInt(), Crayons.colors[0].color)
        assertEquals(0xFF1565C0.toInt(), Crayons.colors[10].color)
    }
}
