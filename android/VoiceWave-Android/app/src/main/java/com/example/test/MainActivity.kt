package com.example.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.test.util.AudioRecorderHelper
import com.example.test.widget.VoiceWaveView
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    private lateinit var voiceWaveView: VoiceWaveView
    private val audioHelper = AudioRecorderHelper()
    
    // 用于定时刷新的 Handler
    private val handler = Handler(Looper.getMainLooper())
    private val REQUEST_AUDIO_PERMISSION_CODE = 200
    
    // 测试模式开关
    private var isTestMode = true // [开关] 设为 true 开启正弦波测试，false 开启录音
    private var sineWaveTime = 0.0

    // 注意：我们将刷新率从 100ms 提高到 20ms，提高数据密度
    // 这不会加快波形滚动的速度（滚动速度由 View 内部的 speedPxPerMs 决定）
    // 这只会让波形的形状更细致
    private val DATA_REFRESH_INTERVAL = 20L

    // 刷新任务：读取音量 -> 更新 UI -> 再次调度
    private val updateTask = object : Runnable {
        override fun run() {
            if (isTestMode) {
                // 每次增加的角度变小，因为刷新变快了，保证波形频率正常
                sineWaveTime += 0.05
                // sin 的结果是 -1~1，我们转换到 0~1
                val amplitude = (sin(sineWaveTime) + 1) / 2.0
                voiceWaveView.addAmplitude(amplitude.toFloat())
            } else {
                // --- 场景 B：真实录音 ---
                val maxAmplitude = audioHelper.getCurrentAmplitude()
                val ratio = maxAmplitude / 32767f * 1.5f
                voiceWaveView.addAmplitude(ratio)
            }

            // 保持生产数据的频率与动画消耗的频率一致
            handler.postDelayed(this, DATA_REFRESH_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        voiceWaveView = findViewById(R.id.voiceWaveView)

        // 检查并申请权限
        if (checkPermissions()) {
            startAudioVisualizer()
        } else {
            requestPermissions()
        }
    }

    private fun startAudioVisualizer() {
        if (!isTestMode) {
            audioHelper.startRecord(cacheDir)
        }
        handler.post(updateTask)
    }

    private fun stopAudioVisualizer() {
        // 停止 UI 刷新
        handler.removeCallbacks(updateTask)
        // 停止录音
        if (!isTestMode) {
            audioHelper.stopRecord()
        }
        // 清空画布 (可选)
        // voiceWaveView.clear()
    }

    // --- 生命周期管理 (关键) ---
    override fun onResume() {
        super.onResume()
        if (checkPermissions()) {
            startAudioVisualizer()
        }
    }

    override fun onPause() {
        super.onPause()
        stopAudioVisualizer() // 切后台时必须停止，否则会在后台占用麦克风
    }

    // --- 权限相关样板代码 ---
    private fun checkPermissions(): Boolean {
        return if (isTestMode) {
            true // 测试模式不需要权限
        } else {
            val result = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
            result == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (!isTestMode) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_AUDIO_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAudioVisualizer()
            } else {
                Toast.makeText(this, "需要录音权限才能展示波形", Toast.LENGTH_LONG).show()
            }
        }
    }
}