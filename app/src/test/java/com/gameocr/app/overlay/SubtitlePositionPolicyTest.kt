package com.gameocr.app.overlay

import org.junit.Assert.*
import org.junit.Test

class SubtitlePositionPolicyTest {
    @Test fun savedPositionRoundTripsAndStaysRelativeAfterRotation() {
        val (x, y) = subtitleWindowAnchor(16, 1800, 1080, 2400, 1048, 252, 80)
        assertEquals(16 to 1800, subtitleWindowPosition(x, y, 1080, 2400, 1048, 252, 80, 0, 0))
        val landscape = subtitleWindowPosition(x, y, 2400, 1080, 1470, 252, 30, 0, 0)
        assertEquals(465, landscape.first)
        assertTrue(landscape.second in 30..828)
        val restored = subtitleWindowAnchor(landscape.first, landscape.second, 2400, 1080, 1470, 252, 30)
        assertEquals(x, restored.first, 0.002f)
        assertEquals(y, restored.second, 0.002f)
    }

    @Test fun unsetAndInvalidPositionsKeepTheDefaultAndOffscreenValuesAreClamped() {
        assertEquals(16 to 100, subtitleWindowPosition(-1f, -1f, 1080, 2400, 1048, 252, 80, 16, 100))
        assertEquals(16 to 100, subtitleWindowPosition(Float.NaN, Float.POSITIVE_INFINITY, 1080, 2400, 1048, 252, 80, 16, 100))
        assertEquals(32 to 2148, subtitleWindowPosition(9f, 9f, 1080, 2400, 1048, 252, 80, 0, 0))
        assertEquals(0 to 0, subtitleWindowPosition(1f, 1f, 100, 100, 200, 200, 80, 0, 0))
    }

    @Test fun theSameSavedAnchorFitsBothCompactAndSpeechWindows() {
        assertEquals(16 to 2148, subtitleWindowPosition(0.5f, 1f, 1080, 2400, 1048, 252, 80, 0, 0))
        assertEquals(120 to 1920, subtitleWindowPosition(0.5f, 1f, 1080, 2400, 840, 480, 80, 0, 0))
    }

    @Test fun movingNearOriginalCaptionsChoosesTheNearestClearPosition() {
        assertEquals(1300, subtitleTopAvoidingSource(1500, 200, 2400, 80, listOf(1500 to 1700)))
        assertEquals(1700, subtitleTopAvoidingSource(1650, 200, 2400, 80, listOf(1500 to 1700)))
        assertEquals(500, subtitleTopAvoidingSource(500, 200, 2400, 80, listOf(1500 to 1700)))
        assertEquals(800, subtitleTopAvoidingSource(900, 200, 1200, 30, listOf(1000 to 1200)))
        assertEquals(1700, subtitleTopAvoidingSource(1550, 200, 2400, 80, listOf(1500 to 1600, 1600 to 1700)))
    }
}
