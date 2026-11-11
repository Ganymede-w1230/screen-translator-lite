package com.gameocr.app.data

/** The personal video build keeps one translator and two operation-ball modes. */
internal object PersonalLitePolicy {
    val textLanguages = setOf("auto", "en", "ja")
    val speechLanguages = setOf("auto", "en", "ru")
    val menu = listOf(MenuItemId.LOOP, MenuItemId.SPEECH_SKILL, MenuItemId.REGION,
        MenuItemId.SUBTITLE_POSITION, MenuItemId.LANGUAGE_PAIR, MenuItemId.SETTINGS)

    fun normalize(settings: Settings): Settings = settings.copy(
        translatorEngine = TranslatorEngine.OPENAI,
        openAiProvider = OpenAiProvider.CUSTOM,
        baseUrl = "https://api.deepseek.com/v1/",
        model = "deepseek-flash",
        openAiRequestOptions = settings.openAiRequestOptions.copy(sendScreenImage = false, thinkingModeEnabled = false),
        sourceLang = settings.sourceLang.takeIf { it in textLanguages } ?: "auto",
        targetLang = "zh-CN",
        ocrEngine = OcrEngineKind.ML_KIT_AUTO,
        asrEngine = AsrEngineKind.WHISPER,
        asrWhisperVariant = AsrWhisperVariant.BASE,
        asrLanguage = settings.asrLanguage.takeIf { it.code in speechLanguages } ?: AsrLanguage.AUTO,
        floatingButtonSkill = settings.floatingButtonSkill.takeIf {
            it == FloatingSkill.LOOP || it == FloatingSkill.SPEECH
        } ?: FloatingSkill.LOOP,
        renderMode = RenderMode.FLOATING_WINDOW,
        loopTriggerMode = LoopTriggerMode.FIXED_INTERVAL,
        loopCaptureVibrationEnabled = false,
        hideOcrNoTextHint = true,
        shareEntireScreen = true,
        quickPanelEnabled = false,
        arcMenuPageSize = menu.size,
        ttsEnabled = false,
        dictionaryTapLookupEnabled = false,
        translationGlossaryEnabled = false,
        gestureGlobalEnabled = false,
        gestureEdgeEnabled = false,
        gestureTwistEnabled = false,
        gestureTiltEnabled = false,
        a11yVolumeTrigger = false,
        ocrRedBoxModeEnabled = false,
        performanceOverlayEnabled = false,
        ocrScreenshotSavingEnabled = false,
    )
}
