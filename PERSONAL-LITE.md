# 屏译轻量版公开源码

基于 [ciddwd/overlay-translator](https://github.com/ciddwd/overlay-translator) v0.4.8（上游提交 `64a7aa2c9605c88dd35bdeea42ab5c0f1c18ce5e`）修改，保留原项目的 LICENSE、NOTICE 和模型许可。

当前版本：0.4.8-personal-lite.2。保留英/日画面字幕、英/俄语音识别、DeepSeek 翻译、字幕选区、字幕位置调整，以及多行合并、忽略标点去重、防闪烁和手势穿透修复。

## 使用

公开源码不包含个人 API Key。安装后自动准备内置模型，在首页点击“更换 API Key”，填写你自己的 DeepSeek 官方 Key 即可使用联网翻译。请在应用内填写，不要把真实 Key 提交到仓库。

`personal-assets/personal_setup.json` 中的 Key 保持为空，其余内容用于自动安装模型和建立默认设置。语音模型位于 `personal-assets/asr/whisper_base/`。大文件通过 Git LFS 保存。

## 获取与构建

安装 Git LFS 后，在克隆的仓库目录执行：

```sh
git lfs install
git lfs pull
git submodule update --init --recursive
```

准备 JDK 17 和 Android SDK 35，详细环境要求见原项目 README。默认构建轻量版。Windows 可复用已包含的原生库：

```powershell
.\gradlew.bat -I windows-build.init.gradle :app:assembleDebug
```

`prebuilt/` 包含对应官方原生库与语音运行库。Linux/macOS 可使用相同预编译库配置，或按原项目说明编译原生模块。

输出：`app/build/outputs/apk/debug/app-debug.apk`。第一次打开自动准备模型；系统权限需本人确认。
