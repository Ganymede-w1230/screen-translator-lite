package com.gameocr.app.service

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrNoResultFeedbackTest {

    @Test
    fun emptyOcrResult_keepsLogAndOnlyConditionallyShowsTheErrorHint() {
        val source = sourceFile().readText()
        val branchStart = source.indexOf("if (blocks.isEmpty())")
        val branchEnd = source.indexOf("val qualityIssue =", branchStart)
        check(branchStart >= 0 && branchEnd > branchStart) { "empty OCR branch not found" }
        val branch = source.substring(branchStart, branchEnd)

        data class Case(val name: String, val marker: String)
        val required = listOf(
            Case("diagnostic log remains", "R.string.log_msg_ocr_no_result_format"),
            Case("feedback uses the shared loop-aware path", "showOcrNoTextFeedback(settings)"),
        )

        required.forEach { case -> assertTrue(case.name, branch.contains(case.marker)) }
    }

    @Test
    fun noTextFeedback_clearsStaleCaptionsAndUsesTheLoopAwareHintPolicy() {
        val source = sourceFile().readText()
        val start = source.indexOf("private suspend fun showOcrNoTextFeedback(")
        val end = source.indexOf("private fun cropIfNeeded", start)
        val feedback = source.substring(start, end)
        assertTrue(feedback.contains("withContext(Dispatchers.Main.immediate)"))
        assertTrue(feedback.contains("delay(800L)"))
        assertTrue(feedback.contains("hideAutomaticSubtitleForSilence()"))
        assertTrue(!feedback.contains("overlay?.clear()"))
        assertTrue(feedback.contains("DeveloperOcrDebugPolicy.shouldShowNoTextHint("))
        assertTrue(feedback.contains("settings.hideOcrNoTextHint, loopMode"))
        assertTrue(feedback.contains("R.string.toast_ocr_unreliable_result"))
        val pipeline = source.substring(source.indexOf("private suspend fun captureOnce("), start)
        assertTrue("empty, unreliable and end-to-end results share feedback",
            Regex("showOcrNoTextFeedback\\(settings\\)").findAll(pipeline).count() == 3)
    }

    private fun sourceFile(): File =
        listOf(
            File("src/main/java/com/gameocr/app/service/CaptureService.kt"),
            File("app/src/main/java/com/gameocr/app/service/CaptureService.kt"),
        ).firstOrNull(File::isFile) ?: error("CaptureService.kt not found")
}
