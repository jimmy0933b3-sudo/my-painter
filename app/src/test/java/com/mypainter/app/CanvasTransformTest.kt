package com.mypainter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasTransformTest {
    @Test fun zoomIsAppliedToTheArtwork() {
        assertTrue(CanvasTransform.ZOOM > 1f)
    }

    @Test fun touchCoordinatesMatchTheEnlargedDrawing() {
        val width = 850f
        val height = 850f
        listOf(
            SketchPoint(12f, 16f),
            SketchPoint(30f, 70f),
            SketchPoint(50f, 50f),
            SketchPoint(86f, 84f)
        ).forEach { point ->
            val screen = CanvasTransform.artToScreen(point, width, height)
            val restored = CanvasTransform.screenToArt(screen.x, screen.y, width, height)
            assertEquals(point.x, restored.x, 0.0001f)
            assertEquals(point.y, restored.y, 0.0001f)
        }
    }

    @Test fun mappingWorksOnSmallLandscapeCanvas() {
        val width = 283f
        val height = 283f
        val point = SketchPoint(40f, 72f)
        val screen = CanvasTransform.artToScreen(point, width, height)
        val restored = CanvasTransform.screenToArt(screen.x, screen.y, width, height)
        assertEquals(point.x, restored.x, 0.0001f)
        assertEquals(point.y, restored.y, 0.0001f)
    }

    @Test fun touchInputClampsOutsideTheCanvas() {
        val left = CanvasTransform.screenToArt(-99f, -99f, 280f, 280f)
        val right = CanvasTransform.screenToArt(999f, 999f, 280f, 280f)
        assertEquals(0f, left.x, 0f)
        assertEquals(0f, left.y, 0f)
        assertEquals(100f, right.x, 0f)
        assertEquals(100f, right.y, 0f)
    }

    @Test fun zeroSizeDoesNotDivideByZero() {
        val result = CanvasTransform.screenToArt(0f, 0f, 0f, 0f)
        assertEquals(50f, result.x, 0f)
        assertEquals(50f, result.y, 0f)
    }
}
