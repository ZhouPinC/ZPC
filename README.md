# ZPC — 项目合集

ZPC 是 ZhouPinC 的 GitHub 主仓库，用作对外展示的项目合集索引。本仓库本身也是一个 **Android 录音机 & 计算器** 应用（见下方 [Android 应用](#android-应用) 分类中的 ZPC 条目）。

---

## 📂 项目分类

### 🤖 AI 应用

| 仓库 | 简介 |
|------|------|
| [XiaoZhou-AI](https://github.com/ZhouPinC/XiaoZhou-AI) | 仿 DeepSeek 风格的 AI 对话应用，基于 Gemini API，多模型+联网搜索 |

### 📱 Android 应用

| 仓库 | 简介 |
|------|------|
| **ZPC**（本仓库） | Android 录音机 & 计算器一体应用，实时波形显示 + 四则运算 |
| [VoiceWave-Android](https://github.com/ZhouPinC/VoiceWave-Android) | Kotlin 语音波形可视化组件测试原型 |

### 🎨 Web 工具

| 仓库 | 简介 |
|------|------|
| [Resume-Editor](https://github.com/ZhouPinC/Resume-Editor) | 在线简历编辑器，双模板、多主题、JSON 备份、PDF 导出 |

### 🕹️ 全栈项目

| 仓库 | 简介 |
|------|------|
| [GuessNumber-Game](https://github.com/ZhouPinC/GuessNumber-Game) | 三端猜数字游戏平台（Web/桌面/小程序），Spring Boot + WebSocket |

### 🧩 Flutter 组件

| 仓库 | 简介 |
|------|------|
| [VoiceWave-Flutter](https://github.com/ZhouPinC/VoiceWave-Flutter) | Flutter 录音波形可视化组件，对标 iPhone 语音备忘录 |

### 🛩️ 嵌入式 & FPV

| 仓库 | 简介 |
|------|------|
| [hx-esp32-cam-fpv](https://github.com/ZhouPinC/hx-esp32-cam-fpv) | ESP32 开源数字 FPV 图传系统（Fork） |

---

## Android 应用 — ZPC（本仓库）

一款 Android 原生工具应用，集成**实时录音波形显示**与**四则运算计算器**。

### 功能

- 🎙️ 实时麦克风录音，波形条随音量动态变化
- 📜 录音历史列表，可回溯之前的录音记录
- 🔢 完整计算器：加减乘除、小数运算、错误提示
- 🎨 仿 iOS 风格红色波形条 UI

### 技术栈

- 语言：Java
- 平台：Android（最低 API 21）
- 音频：`AudioRecord` PCM 采集 + 自定义 `AudioWaveformView` Canvas 绘制
- 架构：单 Activity 多 Fragment，无第三方依赖

### 运行

```bash
./gradlew assembleDebug
```

### 项目结构

```
ZPC/Android/
├── app/src/main/java/com/example/waveform/
│   ├── MainActivity.java              # 主界面入口
│   ├── RecorderBottomSheetFragment.java # 录音底部弹窗
│   ├── AudioWaveformView.java         # 波形条自定义 View
│   ├── AudioRecorder.java             # PCM 录音引擎
│   ├── RecordHistoryActivity.java     # 录音历史
│   └── CalculatorActivity.java        # 计算器
└── ...
```