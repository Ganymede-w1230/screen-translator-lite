package com.gameocr.app.data

import com.gameocr.app.BuildConfig
import com.gameocr.app.overlay.MenuItemRegistry
import org.junit.Assert.*
import org.junit.Test

class PersonalLitePolicyTest {
    @Test fun upgradeKeepsCredentialsAndUsefulPreferences() {
        val original = Settings(
            apiKey = "existing-private-key", sourceLang = "ja", asrLanguage = AsrLanguage.RU,
            floatingButtonSkill = FloatingSkill.SPEECH, captureLoopIntervalMs = 500L,
            overlayTextSizeSp = 22, floatingWindowContentMode = FloatingWindowContentMode.DST_ONLY,
            captureRegionBorderEnabled = false,
        )
        val actual = PersonalLitePolicy.normalize(original)
        assertEquals(original.apiKey, actual.apiKey)
        assertEquals(original.sourceLang, actual.sourceLang)
        assertEquals(original.asrLanguage, actual.asrLanguage)
        assertEquals(original.floatingButtonSkill, actual.floatingButtonSkill)
        assertEquals(original.captureLoopIntervalMs, actual.captureLoopIntervalMs)
        assertEquals(original.overlayTextSizeSp, actual.overlayTextSizeSp)
        assertEquals(original.floatingWindowContentMode, actual.floatingWindowContentMode)
        assertFalse(actual.captureRegionBorderEnabled)
        assertEquals(actual, PersonalLitePolicy.normalize(actual))
    }

    @Test fun unsupportedModesAndLanguagesReturnToTheVideoDefaults() {
        val actual = PersonalLitePolicy.normalize(Settings(
            floatingButtonSkill = FloatingSkill.WORD_SELECT, sourceLang = "ko", targetLang = "en",
            asrLanguage = AsrLanguage.JA, ttsEnabled = true, dictionaryTapLookupEnabled = true,
            gestureGlobalEnabled = true, quickPanelEnabled = true,
            openAiRequestOptions = OpenAiRequestOptions(sendScreenImage = true, thinkingModeEnabled = true),
        ))
        assertEquals(FloatingSkill.LOOP, actual.floatingButtonSkill)
        assertEquals("auto", actual.sourceLang)
        assertEquals("zh-CN", actual.targetLang)
        assertEquals(AsrLanguage.AUTO, actual.asrLanguage)
        assertEquals(AsrEngineKind.WHISPER, actual.asrEngine)
        assertEquals(AsrWhisperVariant.BASE, actual.asrWhisperVariant)
        assertEquals("deepseek-flash", actual.model)
        assertFalse(actual.ttsEnabled || actual.dictionaryTapLookupEnabled || actual.gestureGlobalEnabled || actual.quickPanelEnabled)
        assertFalse(actual.openAiRequestOptions.sendScreenImage || actual.openAiRequestOptions.thinkingModeEnabled)
    }

    @Test fun menuActionsNeedNoPageTurnAndKeepOnlyTheTwoSupportedModes() {
        assertEquals(6, PersonalLitePolicy.menu.size)
        assertEquals(6, PersonalLitePolicy.menu.distinct().size)
        assertTrue(PersonalLitePolicy.menu.size <= FloatingMenu.MAX_PAGE_SIZE)
        assertTrue(MenuItemId.SUBTITLE_POSITION in PersonalLitePolicy.menu)
        assertEquals(listOf(MenuItemId.LOOP, MenuItemId.SPEECH_SKILL), PersonalLitePolicy.menu.filter {
            it == MenuItemId.LOOP || it == MenuItemId.SPEECH_SKILL || it == MenuItemId.FULL_SCREEN_SKILL ||
                it == MenuItemId.INPUT_TRANSLATE_SKILL || it == MenuItemId.GESTURE_SKILL
        })
        if (BuildConfig.PERSONAL_LITE) {
            assertEquals(FloatingSkill.LOOP, MenuItemRegistry.targetSkill(MenuItemId.LOOP, FloatingSkill.LOOP))
            assertEquals(FloatingSkill.SPEECH, MenuItemRegistry.targetSkill(MenuItemId.SPEECH_SKILL, FloatingSkill.SPEECH))
        }
    }
}
