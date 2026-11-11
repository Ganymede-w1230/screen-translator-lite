package com.gameocr.app.data

import android.content.Context
import com.gameocr.app.asr.AsrImportResult
import com.gameocr.app.asr.AsrImportSource
import com.gameocr.app.asr.AsrModelId
import com.gameocr.app.asr.AsrModelInstaller
import com.gameocr.app.onboarding.OnboardingPrefs
import com.gameocr.app.onboarding.OnboardingUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Optional private build asset; ordinary builds retain their existing setup flow. */
@Singleton
class PersonalSetup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val asrInstaller: AsrModelInstaller,
) {
    private val lock = Mutex()

    suspend fun prepareLite() {
        settingsRepository.update(PersonalLitePolicy::normalize)
        com.gameocr.app.overlay.FloatingMenuTourPrefs.markCompleted(context)
    }

    suspend fun prepare() = lock.withLock {
        withContext(Dispatchers.IO) {
            if (context.assets.list("")?.contains("personal_setup.json") != true) return@withContext
            val config = context.assets.open("personal_setup.json").bufferedReader().use {
                JSONObject(it.readText())
            }
            val profileId = config.getString("profileId")
            val prefs = context.getSharedPreferences("personal_setup", Context.MODE_PRIVATE)
            if (prefs.getBoolean(profileId, false)) return@withContext

            val model = AsrModelId.WHISPER_BASE
            if (asrInstaller.checkInstalled(model) == null) {
                val sources = model.files.map { spec ->
                    AsrImportSource(spec.name, spec.bytes) {
                        context.assets.open("asr/whisper_base/${model.revision}/${spec.name}")
                    }
                }
                when (val result = asrInstaller.import(model, sources)) {
                    AsrImportResult.Imported -> Unit
                    is AsrImportResult.Failed -> throw result.error
                    else -> throw IOException("Bundled speech model verification failed")
                }
            }

            settingsRepository.update { current ->
                current.copy(
                    baseUrl = config.getString("baseUrl"),
                    apiKey = config.getString("apiKey"),
                    model = config.getString("model"),
                    translatorEngine = TranslatorEngine.OPENAI,
                    openAiProvider = OpenAiProvider.CUSTOM,
                    openAiRequestOptions = OpenAiRequestOptions(),
                    sourceLang = "auto",
                    targetLang = "zh-CN",
                    ocrEngine = OcrEngineKind.ML_KIT_AUTO,
                    autoOcr = AutoOcrSettings(routes = mapOf(
                        "en" to AutoOcrRoute(OcrEngineKind.ML_KIT_LATIN),
                        "ja" to AutoOcrRoute(OcrEngineKind.ML_KIT_JAPANESE),
                        "zh" to AutoOcrRoute(OcrEngineKind.ML_KIT_CHINESE),
                    )),
                    asrEngine = AsrEngineKind.WHISPER,
                    asrWhisperVariant = AsrWhisperVariant.BASE,
                    asrLanguage = AsrLanguage.AUTO,
                    captureLoopIntervalMs = 200L,
                    loopTriggerMode = LoopTriggerMode.FIXED_INTERVAL,
                    loopCaptureVibrationEnabled = false,
                    hideOcrNoTextHint = true,
                    streamingTranslate = true,
                    ttsEnabled = false,
                )
            }
            val configured = settingsRepository.get()
            settingsRepository.saveTranslationPreset(
                TranslationPresetCatalog.fromSettings(
                    id = "personal-video",
                    name = "个人视频翻译 · DeepSeek",
                    shortName = "视频翻译",
                    settings = configured,
                ),
                source = configured,
            )
            OnboardingPrefs.rememberUsage(context, OnboardingUsage.DAILY)
            OnboardingPrefs.markCompleted(context)
            check(prefs.edit().putBoolean(profileId, true).commit()) { "Could not save bundled setup" }
        }
    }
}
