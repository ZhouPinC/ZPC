# ZPC — Android 录音机 & 计算器

一款 Android 原生工具应用，集成**实时录音波形显示**与**四则运算计算器**。

## 功能

- 🎙️ 实时麦克风录音，波形条随音量动态变化
- 📜 录音历史列表，可回溯之前的录音记录
- 🔢 完整计算器：加减乘除、小数运算、错误提示
- 🎨 仿 iOS 风格红色波形条 UI

## 技术栈

- 语言：Java
- 平台：Android（最低 API 21）
- 音频：`AudioRecord` PCM 采集 + 自定义 `AudioWaveformView` Canvas 绘制
- 架构：单 Activity 多 Fragment，无第三方依赖

## 运行

```bash
./gradlew assembleDebug
```

## 项目结构

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

## 许可

暂无，欢迎添加。