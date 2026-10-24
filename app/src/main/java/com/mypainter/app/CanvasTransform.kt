package com.mypainter.app

/**
 * Zoom the artwork inside the landscape canvas without breaking touch coordinates.
 *
 * The source artwork lives in a 100 x 100 coordinate system. The Android Canvas
 * performs a ZOOM scale around its center; incoming finger coordinates must use
 * the inverse mapping so brush, trace and bucket hit the same visible position.
 */
object CanvasTransform {
    const val ZOOM = 1.12f

    fun screenToArt(x: Float, y: Float, width: Float, height: Float): SketchPoint {
        if (width <= 0f || height <= 0f) return SketchPoint(50f, 50f)
        return SketchPoint(
            ((x / width * 100f - 50f) / ZOOM + 50f).coerceIn(0f, 100f),
            ((y / height * 100f - 50f) / ZOOM + 50f).coerceIn(0f, 100f)
        )
    }

    fun artToScreen(art: SketchPoint, width: Float, height: Float): SketchPoint =
        SketchPoint(
            ((art.x - 50f) * ZOOM + 50f) / 100f * width,
            ((art.y - 50f) * ZOOM + 50f) / 100f * height
        )
}
