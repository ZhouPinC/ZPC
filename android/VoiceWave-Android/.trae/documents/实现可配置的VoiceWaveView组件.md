# 第一阶段：组件核心层实现计划

## 目标
完成可配置的 UI 组件 (VoiceWaveView) 及其资源属性 (attrs.xml)，确保 UI 细节完美还原。

## 实现步骤

### 1. 创建属性定义文件 (attrs.xml)
- **文件路径**：`app/src/main/res/values/attrs.xml`
- **内容**：定义 VoiceWaveView 的所有可配置属性，包括：
  - waveColor：波形颜色
  - waveBarWidth：波形条宽度
  - waveGap：波形条间距（核心需求：6dp）
  - waveMinHeight：波形最小高度（核心需求：12dp）
  - waveMaxHeight：波形最大高度（核心需求：58dp）
  - waveCornerRadius：波形圆角半径
  - waveSpeed：波形动画速度

### 2. 实现核心 View 组件 (VoiceWaveView.kt)
- **文件路径**：`app/src/main/java/com/example/test/widget/VoiceWaveView.kt`
- **核心功能**：
  - 属性解析：从 XML 中读取配置属性
  - 绘制逻辑：实现波形条的绘制，确保右向左流动效果
  - 数据管理：使用 LinkedList 存储音量数据，自动清理过期数据
  - 公开接口：addAmplitude() 用于接收外部音量数据，clear() 用于清空波形
  - 性能优化：复用 RectF 对象，限制绘制范围

### 3. 创建布局集成示例 (activity_main.xml)
- **文件路径**：`app/src/main/res/layout/activity_main.xml`
- **内容**：
  - 使用 ConstraintLayout 作为根布局
  - 添加 VoiceWaveView 组件，配置核心属性
  - 设置黑色背景以符合参考图效果

### 4. 验证实现
- 确保所有属性都能正确从 XML 中读取
- 验证波形绘制效果符合设计要求
- 确保右向左流动动画正常工作
- 验证数据清理机制有效

## 技术要点

1. **高内聚低耦合**：组件独立封装，通过属性配置和公开接口与外部交互
2. **可配置性**：所有关键参数通过 attrs.xml 暴露，支持在布局文件中直接调整
3. **性能优化**：
   - 避免在 onDraw 中创建对象
   - 限制绘制范围，超出屏幕的波形不再绘制
   - 使用 LinkedList 高效管理数据
   - 合理使用 postInvalidateOnAnimation() 刷新 UI
4. **代码规范**：遵循 Kotlin 最佳实践，代码结构清晰，注释完整

## 预期效果

- 能够在布局文件中预览 VoiceWaveView
- 运行时能显示从右向左流动的波形动画
- 波形条高度在 12-58dp 之间动态变化
- 波形条间距为 6dp
- 支持通过 XML 属性自定义外观
- 提供清晰的外部接口供后续集成使用