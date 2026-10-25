package com.mypainter.app

/**
 * A fillable region is a 4-connected, enclosed white area of the coloring page.
 * All calculations use the SAME bitmap coordinates as the printed line art.
 *
 * Previous versions discarded any region connected to the outside. A 1-pixel
 * break in a contour therefore made an entire vegetable or toy unpaintable.
 * Closing only tiny gaps in the flood-fill barrier fixes this without changing
 * the visible illustration.
 */
class LineArtRegions(originalInk: BooleanArray, val side: Int) {
    init {
        require(side >= 16 && originalInk.size == side * side)
    }

    private val barrier = closeSmallGaps(originalInk, side)
    val ids = IntArray(side * side)
    private val sizes = LinkedHashMap<Int, Int>()
    val count: Int get() = sizes.size
    val regionSizes: Map<Int, Int> get() = sizes.toMap()

    init {
        val queue = IntArray(side * side)
        var next = 2000 // previous emoji/lineart saved color keys started at 1001
        for (start in ids.indices) {
            if (barrier[start] || ids[start] != 0) continue
            val id = ++next
            var head = 0
            var end = 1
            queue[0] = start
            ids[start] = id
            var outside = false
            while (head < end) {
                val pos = queue[head++]
                val x = pos % side
                val y = pos / side
                if (x == 0 || y == 0 || x == side - 1 || y == side - 1) {
                    outside = true
                }
                fun visit(candidate: Int) {
                    if (!barrier[candidate] && ids[candidate] == 0) {
                        ids[candidate] = id
                        queue[end++] = candidate
                    }
                }
                if (x > 0) visit(pos - 1)
                if (x + 1 < side) visit(pos + 1)
                if (y > 0) visit(pos - side)
                if (y + 1 < side) visit(pos + side)
            }
            if (outside) {
                for (i in 0 until end) ids[queue[i]] = -1
                // Keep the allocated ID: stable IDs across future runs.
            } else {
                sizes[id] = end
            }
        }
        // Sealing a pinhole must not create a new unpaintable white pixel.
        // Assign artificial barriers (not the original visible black ink)
        // to their closest already-labeled enclosed region.
        for (y in 1 until side - 1) for (x in 1 until side - 1) {
            val pos = y * side + x
            if (!originalInk[pos] && barrier[pos] && ids[pos] == 0) {
                var winner = 0
                var distance = Int.MAX_VALUE
                for (dy in -2..2) for (dx in -2..2) {
                    val xx = x + dx
                    val yy = y + dy
                    if (xx !in 0 until side || yy !in 0 until side) continue
                    val candidate = ids[yy * side + xx]
                    val d = dx * dx + dy * dy
                    if (sizes.containsKey(candidate) && d < distance) {
                        winner = candidate
                        distance = d
                    }
                }
                if (winner > 0) {
                    ids[pos] = winner
                    sizes[winner] = sizes.getValue(winner) + 1
                }
            }
        }
    }

    fun at(x: Int, y: Int): Int? {
        if (x !in 0 until side || y !in 0 until side) return null
        val direct = ids[y * side + x]
        if (sizes.containsKey(direct)) return direct
        // Large finger targets can land on a black line. Find the closest
        // actual enclosed region, never the edge-connected background.
        for (radius in 1..5) {
            var best: Int? = null
            var bestDistance = Int.MAX_VALUE
            for (dy in -radius..radius) for (dx in -radius..radius) {
                val xx = x + dx
                val yy = y + dy
                if (xx !in 0 until side || yy !in 0 until side) continue
                val distance = dx * dx + dy * dy
                if (distance > radius * radius || distance >= bestDistance) continue
                val candidate = ids[yy * side + xx]
                if (sizes.containsKey(candidate)) {
                    best = candidate
                    bestDistance = distance
                }
            }
            if (best != null) return best
        }
        return null
    }

    fun coloredPixels(fills: Map<Int, Int>): Int =
        ids.count { fills.containsKey(it) && sizes.containsKey(it) }

    /**
     * 3x3 morphological closing, with an untouched 1px edge to prevent
     * accidentally enclosing the outside/background of the page.
     * This adds no visible strokes, only seals tiny gaps for flood fill.
     */
    private fun closeSmallGaps(ink: BooleanArray, size: Int): BooleanArray {
        val expanded = BooleanArray(ink.size)
        for (y in 1 until size - 1) for (x in 1 until size - 1) {
            val i = y * size + x
            expanded[i] = (-1..1).any { dy ->
                (-1..1).any { dx -> ink[i + dy * size + dx] }
            }
        }
        val closed = ink.copyOf()
        for (y in 2 until size - 2) for (x in 2 until size - 2) {
            val i = y * size + x
            if (!ink[i]) {
                closed[i] = (-1..1).all { dy ->
                    (-1..1).all { dx -> expanded[i + dy * size + dx] }
                }
            }
        }
        return closed
    }
}
