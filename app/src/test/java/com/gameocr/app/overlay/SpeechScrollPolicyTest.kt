package com.gameocr.app.overlay

import com.gameocr.app.data.FloatingWindowContentMode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechScrollPolicyTest {
    @Test fun speechUpdatesWaitForScrollingAndPublishOnlyTheLatestSnapshot() {
        val updates = SpeechScrollUpdates()
        val shown = mutableListOf<String>()
        assertFalse(updates.defer { shown += "idle" })
        updates.begin()
        assertTrue(updates.defer { shown += "partial" })
        assertTrue(updates.defer { shown += "translated" })
        assertTrue(shown.isEmpty())
        updates.finish()
        assertEquals(listOf("translated"), shown)
        assertFalse(updates.isScrolling)
        updates.finish()
        assertEquals("a pending snapshot is applied once", 1, shown.size)
    }

    @Test fun closingTheWindowDiscardsPendingSpeechUpdates() {
        val updates = SpeechScrollUpdates()
        updates.begin()
        updates.defer { error("a closed window must not be reopened by a stale snapshot") }
        updates.reset()
        updates.finish()
        assertFalse(updates.isScrolling)
    }

    @Test fun scrollingAgainKeepsThePendingSnapshotUntilTheNewGestureEnds() {
        val updates = SpeechScrollUpdates()
        var shown = 0
        updates.begin()
        updates.defer { shown = 1 }
        updates.begin()
        updates.defer { shown = 2 }
        assertEquals(0, shown)
        updates.finish()
        assertEquals(2, shown)
    }

    @Test fun speechRenderingDefersReplacementAndDiscardsStaleScrollCallbacks() {
        val source = listOf(
            java.io.File("src/main/java/com/gameocr/app/overlay/OverlayManager.kt"),
            java.io.File("app/src/main/java/com/gameocr/app/overlay/OverlayManager.kt"),
        ).first(java.io.File::isFile).readText()
        val start = source.indexOf("internal fun showSpeechRecords(")
        val replacement = source.indexOf("target.text =", start)
        val defer = source.indexOf("deferContentUpdateUntilScrollIdle", start)
        assertTrue("drag/fling must finish before text replacement", defer in start until replacement)
        assertTrue("identical snapshots must not trigger another layout", source.contains("ids == previousIds && pairs == previousPairs"))
        assertTrue("old automatic scroll callbacks must be cancelled", source.contains("pendingSpeechScroll?.removeListener()"))
        assertTrue("scheduled scroll must yield to a new gesture", source.contains("floatingWindow.isContentScrollActive()"))
        val settings = source.substring(source.indexOf("fun syncFloatingWindowFromSettings("), start)
        assertTrue("appearance changes must retain the record being read", settings.contains("captureSpeechAnchor(") && settings.contains("restoreSpeechAnchor("))
    }

    @Test fun followsOnlyWhileTheReaderIsAtTheBottom_tableDriven() {
        data class Case(val name: String, val scrollY: Int, val viewport: Int, val content: Int, val atBottom: Boolean)
        listOf(
            Case("content fits", 0, 500, 300, true),
            Case("exactly at bottom", 500, 500, 1000, true),
            Case("within slop", 470, 500, 1000, true),
            Case("reading older records", 200, 500, 1000, false),
            Case("at top of long history", 0, 500, 5000, false),
        ).forEach { case ->
            assertEquals(case.name, case.atBottom, SpeechScrollPolicy.isAtBottom(case.scrollY, case.viewport, case.content, slopPx = 40))
        }
    }

    @Test fun recordAtOffset_tableDriven() {
        val starts = intArrayOf(0, 10, 25)
        listOf(0 to 0, 9 to 0, 10 to 1, 24 to 1, 25 to 2, 400 to 2).forEach { (offset, index) ->
            assertEquals("offset $offset", index, SpeechScrollPolicy.recordIndexAt(starts, offset))
        }
    }

    @Test fun anchorSurvivesTranslationsAndFallsBackWhenTrimmed_tableDriven() {
        data class Case(val name: String, val ids: List<Long>, val anchor: Long, val expected: Pair<Int, Int>?)
        listOf(
            Case("still displayed", listOf(4, 5, 6), 5, 1 to 12),
            Case("trimmed off the top", listOf(7, 8), 5, 0 to 0),
            Case("nothing left", emptyList(), 5, null),
        ).forEach { case ->
            assertEquals(case.name, case.expected, SpeechScrollPolicy.resolveAnchor(case.ids, case.anchor, 12))
        }
    }

    @Test fun pairStartsMatchTheRenderedText_tableDriven() {
        val pairs = listOf("ab" to "AB", "c" to "…", "d" to "")
        data class Case(val mode: FloatingWindowContentMode, val expected: IntArray)
        listOf(
            // "・ab\n" + "AB" + "\n\n" | "・c\n" + "…" + "\n\n" | "・d\n" + ""
            Case(FloatingWindowContentMode.SRC_AND_DST, intArrayOf(0, 8, 14)),
            // "AB" + "\n\n" | "…" + "\n\n" | ""
            Case(FloatingWindowContentMode.DST_ONLY, intArrayOf(0, 4, 7)),
        ).forEach { case ->
            val starts = floatingWindowPairStartOffsets(pairs, case.mode)
            assertArrayEquals(case.mode.name, case.expected, starts)
            val text = floatingWindowTextSegments(pairs, case.mode).joinToString("") { it.text }
            starts.forEachIndexed { index, start ->
                val segment = floatingWindowTextSegments(pairs, case.mode).first { it.pairIndex == index }
                assertEquals(case.mode.name, segment.text, text.substring(start, start + segment.text.length))
            }
        }
    }
}
