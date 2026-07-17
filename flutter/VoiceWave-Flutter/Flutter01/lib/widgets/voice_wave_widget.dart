import 'package:flutter/material.dart';

/// 1. 控制器：负责管理音量数据和状态
/// 对应计划书第 5 节：Controller API
class VoiceWaveController extends ChangeNotifier {
  final List<double> _samples = [];
  int _maxSamples = 100; // 这里的初始值会被 Widget 覆盖

  // 获取当前样本列表（只读视图）
  List<double> get samples => List.unmodifiable(_samples);

  /// 添加单个样本，amplitude 范围 [0, 1]
  /// 对应计划书第 6 节：关键实现要点 - 高度映射输入
  void addSample(double amplitude) {
    // 简单的数值钳制，确保在 0.0 - 1.0 之间
    final value = amplitude.clamp(0.0, 1.0);
    _samples.add(value);

    // 维持队列长度，移除旧数据
    if (_samples.length > _maxSamples) {
      _samples.removeAt(0);
    }
    
    // 通知监听者（主要用于驱动重绘或动画触发）
    notifyListeners();
  }

  /// 清空数据
  void clear() {
    _samples.clear();
    notifyListeners();
  }
  
  // 内部方法：更新最大缓存数量，由 Widget 根据宽度计算后调用
  void _updateMaxSamples(int count) {
    _maxSamples = count;
    while (_samples.length > _maxSamples) {
      _samples.removeAt(0);
    }
  }
}

/// 2. 绘制器：核心渲染逻辑
/// 对应计划书第 6 节：性能优化与绘制策略
class VoiceWavePainter extends CustomPainter {
  final List<double> samples;
  final double animationValue; // 0.0 -> 1.0，用于平滑移动
  final Color barColor;
  final double barWidth;
  final double spacing;
  final double minHeight;
  final double maxHeight;
  final double radius;

  VoiceWavePainter({
    required this.samples,
    required this.animationValue,
    required this.barColor,
    required this.barWidth,
    required this.spacing,
    required this.minHeight,
    required this.maxHeight,
    required this.radius,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = barColor
      ..style = PaintingStyle.fill;

    // 计划书第 6.1 节：高度映射与位置计算
    // 策略：从右向左绘制。最新的数据在最右侧。
    // 每一个条的占用宽度 = barWidth + spacing
    final step = barWidth + spacing;
    
    // 这里的 offset 是动画产生的位移：从 0 移动到 1 个 step 的距离
    // 使得视觉上看起来是连续向左流动的
    final outputOffset = animationValue * step;

    for (int i = 0; i < samples.length; i++) {
      // 倒序读取，samples.last 是最新的
      final indexFromEnd = samples.length - 1 - i;
      final sampleValue = samples[i];

      // 计算高度：线性插值
      final h = minHeight + (maxHeight - minHeight) * sampleValue;

      // 计算 X 坐标：
      // base: 最右侧 (size.width)
      // offset: 根据索引向左偏移
      // animation: 整体向左滑动的微调
      final rightPos = size.width - (indexFromEnd * step) + outputOffset;
      
      // 如果已经滑出左边界，就不绘制了（性能优化）
      if (rightPos + barWidth < 0) continue;

      // 垂直居中
      final top = (size.height - h) / 2;

      final rect = RRect.fromRectAndRadius(
        Rect.fromLTWH(rightPos - barWidth, top, barWidth, h),
        Radius.circular(radius),
      );

      canvas.drawRRect(rect, paint);
    }
  }

  @override
  bool shouldRepaint(covariant VoiceWavePainter oldDelegate) {
    // 只要有动画值变化或数据变化就重绘
    return oldDelegate.animationValue != animationValue ||
           oldDelegate.samples != samples;
  }
}

/// 3. 组件：整合动画与状态
/// 对应计划书第 5 节：Widget 构造器
class VoiceWaveWidget extends StatefulWidget {
  final VoiceWaveController controller;
  final double height; // 容器高度
  final double barWidth;
  final double spacing;
  final double minBarHeight;
  final double maxBarHeight;
  final Color barColor;
  
  const VoiceWaveWidget({
    super.key,
    required this.controller,
    this.height = 60,
    this.barWidth = 6.0,   // 计划书默认值
    this.spacing = 6.0,    // 计划书默认值
    this.minBarHeight = 12.0,
    this.maxBarHeight = 58.0,
    this.barColor = Colors.blueAccent, // 可根据项目调整
  });

  @override
  State<VoiceWaveWidget> createState() => _VoiceWaveState();
}

class _VoiceWaveState extends State<VoiceWaveWidget> with SingleTickerProviderStateMixin {
  late AnimationController _animController;
  
  @override
  void initState() {
    super.initState();
    // 计划书第 6.3 节：动画策略
    // 使用 fastOutSlowIn 或线性动画，让条目移动一个单位（bar + spacing）
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 100), // 可根据采样率调整
    );

    // 监听控制器数据更新
    widget.controller.addListener(_onDataAdded);
  }

  void _onDataAdded() {
    // 每次有新数据，重置动画并播放，模拟“推入”效果
    // 注意：如果数据频率极高（如 60Hz），这里可能需要优化为不重置动画，而是持续滚动
    // 这里采用计划书建议的“新样本入列触发 forward”策略
    if (!_animController.isAnimating) {
       _animController.forward(from: 0.0);
    } else {
       // 如果上一次动画未结束，强制重置以保持同步（或根据需求平滑处理）
       _animController.forward(from: 0.0);
    }
  }

  @override
  void dispose() {
    _animController.dispose();
    widget.controller.removeListener(_onDataAdded);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        // 计划书第 6.2 节：动态计算 barCount
        // barCount = (Total Width) / (Bar Width + Spacing) + buffer
        final width = constraints.maxWidth;
        final step = widget.barWidth + widget.spacing;
        final count = (width / step).ceil() + 2; // +2 这里的 buffer 用于处理动画移出屏幕的部分
        
        // 更新控制器里的最大缓存限制
        widget.controller._updateMaxSamples(count);

        return SizedBox(
          height: widget.height,
          width: double.infinity,
          // 计划书第 6.4 节：性能优化 - RepaintBoundary
          child: RepaintBoundary(
            child: AnimatedBuilder(
              animation: _animController,
              builder: (context, child) {
                return CustomPaint(
                  painter: VoiceWavePainter(
                    samples: widget.controller.samples,
                    animationValue: (1.0 - _animController.value), // 1->0 还是 0->1 取决于你想要的进入方向，这里微调
                    barColor: widget.barColor,
                    barWidth: widget.barWidth,
                    spacing: widget.spacing,
                    minHeight: widget.minBarHeight,
                    maxHeight: widget.maxBarHeight,
                    radius: widget.barWidth / 2,
                  ),
                );
              },
            ),
          ),
        );
      },
    );
  }
}
