package com.mypainter.app

/** Pure painting operations shared by the gesture handlers and their regression tests. */
object PaintActions {
    fun fillRegion(fills: MutableMap<Int, Int>, region: Int, selectedColor: Int) {
        fills[region] = selectedColor
    }

    fun brushStroke(selectedColor: Int, brushWidth: Float, points: List<SketchPoint>): InkStroke =
        InkStroke(selectedColor, brushWidth, points.toList())
}
