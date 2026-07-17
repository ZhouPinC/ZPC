import 'dart:math';
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter01/widgets/voice_wave_widget.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Voice Wave Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const VoiceWaveDemoPage(),
    );
  }
}

class VoiceWaveDemoPage extends StatefulWidget {
  const VoiceWaveDemoPage({super.key});

  @override
  State<VoiceWaveDemoPage> createState() => _VoiceWaveDemoPageState();
}

class _VoiceWaveDemoPageState extends State<VoiceWaveDemoPage> {
  final VoiceWaveController _waveController = VoiceWaveController();
  late Timer _timer;
  bool _isRecording = false;

  // 模拟录音数据生成
  void _startSimulation() {
    setState(() {
      _isRecording = true;
    });
    
    // 每秒生成 50 个随机样本，模拟实时录音数据
    _timer = Timer.periodic(const Duration(milliseconds: 20), (timer) {
      // 生成 0.0 到 1.0 之间的随机值，模拟音量变化
      // 添加一些随机性，使波形看起来更自然
      final random = Random();
      double value = random.nextDouble() * 0.8 + 0.2; // 0.2 到 1.0 之间
      
      // 偶尔生成一个峰值，模拟说话时的音量变化
      if (random.nextDouble() > 0.9) {
        value = random.nextDouble() * 0.5 + 0.5; // 0.5 到 1.0 之间
      }
      
      _waveController.addSample(value);
    });
  }

  void _stopSimulation() {
    setState(() {
      _isRecording = false;
    });
    _timer.cancel();
  }

  @override
  void dispose() {
    _timer.cancel();
    _waveController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: const Text('Voice Wave Demo'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const SizedBox(height: 50),
            // 语音波形组件
            VoiceWaveWidget(
              controller: _waveController,
              barColor: Colors.redAccent,
              height: 100,
              barWidth: 4,
              spacing: 4,
            ),
            const SizedBox(height: 50),
            // 控制按钮
            ElevatedButton(
              onPressed: _isRecording ? _stopSimulation : _startSimulation,
              style: ElevatedButton.styleFrom(
                backgroundColor: _isRecording ? Colors.red : Colors.green,
                padding: const EdgeInsets.symmetric(horizontal: 40, vertical: 15),
                textStyle: const TextStyle(fontSize: 18),
              ),
              child: Text(
                _isRecording ? '停止模拟' : '开始模拟',
                style: const TextStyle(color: Colors.white),
              ),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: () {
                _waveController.clear();
              },
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 40, vertical: 15),
                textStyle: const TextStyle(fontSize: 18),
              ),
              child: const Text('清空波形'),
            ),
          ],
        ),
      ),
    );
  }
}
