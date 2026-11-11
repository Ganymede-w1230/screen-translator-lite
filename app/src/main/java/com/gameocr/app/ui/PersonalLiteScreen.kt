package com.gameocr.app.ui

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameocr.app.BuildConfig
import com.gameocr.app.capture.CaptureStartMode
import com.gameocr.app.capture.CaptureStartRequestActivity
import com.gameocr.app.capture.RegionPickerActivity
import com.gameocr.app.data.AsrLanguage
import com.gameocr.app.data.FloatingSkill
import com.gameocr.app.data.FloatingWindowContentMode
import com.gameocr.app.data.Settings
import com.gameocr.app.data.SettingsRepository
import com.gameocr.app.service.CaptureService
import com.gameocr.app.service.CaptureServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@HiltViewModel
class PersonalLiteViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {
    val settings = repository.settings
    var error by mutableStateOf<String?>(null)
        private set

    fun save(onSaved: () -> Unit = {}, transform: (Settings) -> Settings) {
        viewModelScope.launch {
            try {
                repository.update(transform)
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                timber.log.Timber.w(failure, "Could not save personal settings")
                error = "保存失败，请重试。"
            }
        }
    }

    fun actionFailed() { error = "操作未完成，请确认系统授权后重试。" }
    fun dismissError() { error = null }
}

@Composable
internal fun PersonalLiteScreen(
    onLegal: () -> Unit,
    onLogs: () -> Unit,
    viewModel: PersonalLiteViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val stored by viewModel.settings.collectAsState(initial = null)
    val settings = stored ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val running by CaptureServiceState.running.collectAsState()
    val speech = settings.floatingButtonSkill == FloatingSkill.SPEECH
    var expanded by rememberSaveable { mutableStateOf(false) }
    var editKey by remember { mutableStateOf(false) }
    var key by remember { mutableStateOf("") }

    fun perform(action: () -> Unit) {
        runCatching(action).onFailure { viewModel.actionFailed() }
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("屏译 · 轻量版", style = MaterialTheme.typography.headlineMedium)
            Text("字幕与语音 → 简体中文", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("翻译模式", style = MaterialTheme.typography.titleMedium)
                    LiteChoices(
                        listOf(FloatingSkill.LOOP to "画面字幕", FloatingSkill.SPEECH to "视频语音"),
                        settings.floatingButtonSkill,
                    ) { skill ->
                        if (running) perform {
                            context.startService(Intent(context, CaptureService::class.java).apply {
                                action = CaptureService.ACTION_SET_SKILL
                                putExtra(CaptureService.EXTRA_SKILL, skill.name)
                            })
                        } else viewModel.save { it.copy(floatingButtonSkill = skill) }
                    }
                    Text(if (speech) "音频语言" else "字幕语言")
                    if (speech) {
                        LiteChoices(
                            listOf(AsrLanguage.AUTO to "自动", AsrLanguage.EN to "英语", AsrLanguage.RU to "俄语"),
                            settings.asrLanguage,
                        ) { language ->
                            if (running) perform {
                                context.startService(Intent(context, CaptureService::class.java).apply {
                                    action = CaptureService.ACTION_SET_ASR_LANGUAGE
                                    putExtra(CaptureService.EXTRA_ASR_LANGUAGE, language.code)
                                })
                            } else viewModel.save { it.copy(asrLanguage = language) }
                        }
                    } else {
                        LiteChoices(listOf("auto" to "自动", "en" to "英语", "ja" to "日语"), settings.sourceLang) { language ->
                            viewModel.save { it.copy(sourceLang = language) }
                        }
                    }
                    Text(
                        if (running) "悬浮球已开启，回到视频后点击悬浮球开始或暂停。"
                        else "开启悬浮球后，回到视频并点击悬浮球开始翻译。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (speech && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        Text("视频语音识别需要 Android 10 或以上版本。", color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = { perform {
                            if (running) context.startService(CaptureService.stopIntent(context))
                            else context.startActivity(CaptureStartRequestActivity.newIntent(context, CaptureStartMode.SYSTEM))
                        } },
                        enabled = running || !speech || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (running) "关闭悬浮球与翻译" else "开启悬浮球") }
                    OutlinedButton(enabled = running, modifier = Modifier.fillMaxWidth(), onClick = { perform {
                        context.startService(Intent(context, CaptureService::class.java).apply {
                            action = CaptureService.ACTION_SUBTITLE_POSITION
                        })
                    } }) { Text("调整字幕位置") }
                    Text("首次使用请按系统提示允许悬浮窗、屏幕共享和音频权限。", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!speech) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("字幕识别区域", style = MaterialTheme.typography.titleMedium)
                        Text(if (settings.captureRegion == null) "自动寻找字幕区域" else "使用已框选的区域")
                        Text("播放视频时，也可以长按悬浮球选择区域。", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { perform {
                                if (running) context.startService(Intent(context, CaptureService::class.java).apply {
                                    action = CaptureService.ACTION_PICK_REGION
                                }) else context.startActivity(RegionPickerActivity.newIntent(context))
                            } }) { Text("重新选区") }
                            TextButton(
                                enabled = settings.captureRegion != null,
                                onClick = { viewModel.save { it.copy(captureRegion = null) } },
                            ) { Text("恢复自动") }
                        }
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "收起显示与检测设置 ▴" else "显示与检测设置 ▾")
                    }
                    if (expanded) {
                        if (!speech) {
                            Text("字幕检测间隔")
                            LiteChoices(listOf(200L to "200ms", 500L to "500ms", 1000L to "1秒"), settings.captureLoopIntervalMs) { interval ->
                                viewModel.save { it.copy(captureLoopIntervalMs = interval) }
                            }
                        }
                        Text("字幕字号")
                        LiteChoices(listOf(14 to "小", 18 to "中", 22 to "大", 26 to "特大"), settings.overlayTextSizeSp) { size ->
                            viewModel.save { it.copy(overlayTextSizeSp = size) }
                        }
                        Text("字幕内容")
                        LiteChoices(
                            listOf(FloatingWindowContentMode.SRC_AND_DST to "双语", FloatingWindowContentMode.DST_ONLY to "仅译文"),
                            settings.floatingWindowContentMode,
                        ) { mode -> viewModel.save { it.copy(floatingWindowContentMode = mode) } }
                        if (!speech) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("显示选区边框", Modifier.weight(1f))
                            Switch(checked = settings.captureRegionBorderEnabled, onCheckedChange = { enabled ->
                                viewModel.save { it.copy(captureRegionBorderEnabled = enabled) }
                            })
                        }
                    }
                    HorizontalDivider()
                    Text("DeepSeek · ${if (settings.apiKey.isBlank()) "尚未配置" else "已配置"}")
                    Text("识别在本机完成，生成译文需要联网。", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { key = ""; editKey = true }) { Text("更换 API Key") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onLogs) { Text("运行日志") }
                TextButton(onClick = onLegal) { Text("开源许可") }
            }
            Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (editKey) AlertDialog(
        onDismissRequest = { editKey = false; key = "" },
        title = { Text("DeepSeek API Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("当前配置会保留。输入新 Key 后保存即可替换。")
                OutlinedTextField(
                    value = key, onValueChange = { key = it }, singleLine = true,
                    label = { Text("新的 API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = key.isNotBlank(), onClick = {
                val replacement = key.trim()
                viewModel.save(onSaved = { editKey = false; key = "" }) { it.copy(apiKey = replacement) }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = { editKey = false; key = "" }) { Text("取消") } },
    )
    viewModel.error?.let { message ->
        AlertDialog(onDismissRequest = viewModel::dismissError, title = { Text("未完成") },
            text = { Text(message) }, confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("知道了") } })
    }
}

@Composable
private fun <T> LiteChoices(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}
