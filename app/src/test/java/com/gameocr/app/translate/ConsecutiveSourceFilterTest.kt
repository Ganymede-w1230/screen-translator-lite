package com.gameocr.app.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsecutiveSourceFilterTest {
    @Test fun ocrPunctuationChangesAndPunctuationOnlyFramesDoNotRetranslate() {
        val filter = ConsecutiveSourceFilter(ignoreOcrFormatting = true)
        assertTrue(filter.accept("Wait...\nI am coming!"))
        listOf("Wait… I am coming", "WAIT.. I am coming.", "Wait, I am coming!!", "…", "Wait I am coming")
            .forEach { assertFalse(it, filter.accept(it)) }
        assertTrue(filter.accept("Wait, we are coming!"))
        assertTrue(filter.accept("本当ですか？"))
        assertFalse(filter.accept("本当ですか……"))
        assertTrue(filter.accept("Подожди…"))
        assertFalse(filter.accept("Подожди!"))
        assertTrue(filter.accept("Episode 1..."))
        assertFalse(filter.accept("Episode 1"))
        assertTrue(filter.accept("Episode 2!"))
    }

    @Test fun failedOcrRequestCanRetryAfterPunctuationChanges() {
        val filter = ConsecutiveSourceFilter(ignoreOcrFormatting = true)
        assertTrue(filter.accept("Wait…"))
        filter.forget("Wait...")
        assertTrue(filter.accept("Wait!"))
    }

    @Test fun ocrLineBreaksCaseUnicodeWidthsAndInvisibleCharactersAreNotNewSentences() {
        val filter = ConsecutiveSourceFilter(ignoreOcrFormatting = true)
        assertTrue(filter.accept("We are here."))
        assertFalse(filter.accept("WE ARE\nHERE."))
        assertFalse(filter.accept("We Ａre\u200b Here."))
        assertTrue(filter.accept("We are there."))
        assertTrue(filter.accept("Room 12345"))
        assertTrue(filter.accept("Room 12346"))
    }
    @Test fun skipsOnlyConsecutiveSourcesAndKeepsPreviousAcrossBlankFrames() {
        val filter = ConsecutiveSourceFilter()
        val sources = listOf("Hello world", "Hello world", " Hello\n world\t", "", "  ", "Next", "Next", "Hello world")
        assertEquals(
            listOf(true, false, false, false, false, true, false, true),
            sources.map { filter.accept(it) },
        )
    }

    @Test fun meaningfulTextContextChangesAndNewSessionsAreAccepted() {
        val filter = ConsecutiveSourceFilter()
        assertTrue(filter.accept("Hello", "zh"))
        assertFalse(filter.accept("Hello", "zh"))
        assertTrue(filter.accept("Hello", "ja"))
        assertTrue(filter.accept("hello", "ja"))
        assertTrue(filter.accept("hello!", "ja"))
        filter.reset()
        assertTrue(filter.accept("hello!", "ja"))
    }

    @Test fun failedRequestCanRetryWithoutAnOlderFailureForgettingANewerSource() {
        val filter = ConsecutiveSourceFilter()
        assertTrue(filter.accept("first"))
        assertTrue(filter.accept("second"))
        filter.forget("first")
        assertFalse(filter.accept("second"))
        filter.forget(" second\n")
        assertTrue(filter.accept("second"))
    }
}
