# Waveform Android App

一个使用原生 Android View 实现录音、实时音频波形、录音历史和计算器功能的实验应用。实际 Android 工程位于 `Android/` 目录。

## 功能

- 请求麦克风权限并采集音频。
- 实时绘制音频振幅波形。
- 保存并浏览录音历史。
- 提供独立计算器页面。
- 使用弹窗和 Bottom Sheet 展示录音控件。

## 技术栈

- Java 11
- Android SDK 33，最低支持 Android 5.0（API 21）
- AndroidX AppCompat、ConstraintLayout 与 Material Components
- Gradle Wrapper

## 目录结构

```text
ZPC/
`-- Android/
    |-- app/src/main/java/com/example/waveform/  # 业务代码与自定义 View
    |-- app/src/main/res/                         # 布局、图标与样式
    |-- app/build.gradle                          # 应用构建配置
    `-- gradlew / gradlew.bat                     # Gradle Wrapper
```

## 本地运行

1. 使用 Android Studio 打开 `Android/`。
2. 等待 Gradle 同步完成，并连接 Android 5.0 或更高版本的设备/模拟器。
3. 运行 `app` 配置；首次录音时授予麦克风权限。

也可在 `Android/` 目录执行：

```powershell
.\gradlew.bat assembleDebug
```

## 注意事项

- 录音功能依赖 `RECORD_AUDIO` 运行时权限。
- Android 10 以下的文件写入场景可能需要存储权限。
- 仓库当前包含 IDE、Gradle 缓存和构建产物；后续整理时应补充 `.gitignore` 并清理可再生成文件。

## 状态

实验性原型，适合继续验证音频波形与录音交互，不建议未经测试直接用于生产环境。
