package com.gameocr.app.overlay

import kotlin.math.roundToInt

internal fun subtitleTopAvoidingSource(
    preferredTop: Int, height: Int, screenHeight: Int, minimumTop: Int,
    sourceSpans: List<Pair<Int, Int>>,
): Int {
    val maxTop = (screenHeight - height).coerceAtLeast(0)
    val minTop = minimumTop.coerceIn(0, maxTop)
    val candidates = listOf(preferredTop) + sourceSpans.flatMap { (top, bottom) -> listOf(top - height, bottom) }
    return candidates.filter { candidate ->
        candidate in minTop..maxTop && sourceSpans.none { (top, bottom) -> candidate < bottom && candidate + height > top }
    }.minByOrNull { kotlin.math.abs(it - preferredTop) } ?: preferredTop.coerceIn(minTop, maxTop)
}

/** Keep the same relative placement across orientations and different subtitle window sizes. */
internal fun subtitleWindowPosition(
    anchorX: Float, anchorY: Float,
    screenWidth: Int, screenHeight: Int, width: Int, height: Int, minimumTop: Int,
    defaultX: Int, defaultY: Int,
): Pair<Int, Int> {
    val maxX = (screenWidth - width).coerceAtLeast(0)
    val maxY = (screenHeight - height).coerceAtLeast(0)
    val top = minimumTop.coerceIn(0, maxY)
    if (!anchorX.isFinite() || !anchorY.isFinite() || anchorX < 0f || anchorY < 0f) {
        return defaultX.coerceIn(0, maxX) to defaultY.coerceIn(top, maxY)
    }
    return (anchorX.coerceIn(0f, 1f) * maxX).roundToInt() to
        (top + anchorY.coerceIn(0f, 1f) * (maxY - top)).roundToInt()
}

internal fun subtitleWindowAnchor(
    x: Int, y: Int, screenWidth: Int, screenHeight: Int, width: Int, height: Int, minimumTop: Int,
): Pair<Float, Float> {
    val maxX = (screenWidth - width).coerceAtLeast(0)
    val maxY = (screenHeight - height).coerceAtLeast(0)
    val top = minimumTop.coerceIn(0, maxY)
    return (if (maxX > 0) x.toFloat() / maxX else 0.5f).coerceIn(0f, 1f) to
        (if (maxY > top) (y - top).toFloat() / (maxY - top) else 0f).coerceIn(0f, 1f)
}

internal data class OverlayWindowSize(
    val widthPx: Int,
    val heightPx: Int,
)

internal fun constrainOverlayWindowSize(
    requestedWidthPx: Int,
    requestedHeightPx: Int,
    screenWidthPx: Int,
    screenHeightPx: Int,
    minimumWidthPx: Int,
    minimumHeightPx: Int,
): OverlayWindowSize {
    val maxWidth = screenWidthPx.coerceAtLeast(1)
    val maxHeight = screenHeightPx.coerceAtLeast(1)
    val minWidth = minimumWidthPx.coerceIn(1, maxWidth)
    val minHeight = minimumHeightPx.coerceIn(1, maxHeight)
    return OverlayWindowSize(
        widthPx = requestedWidthPx.coerceIn(minWidth, maxWidth),
        heightPx = requestedHeightPx.coerceIn(minHeight, maxHeight),
    )
}
