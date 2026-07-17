package com.example.test.util

import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * 录音帮助类
 * 负责调用系统 MediaRecorder 获取环境音量
 */
class AudioRecorderHelper {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    /**
     * 开始录音
     * @param cacheDir用于存放临时文件的目录 (Context.cacheDir)
     */
    fun startRecord(cacheDir: File) {
        if (isRecording) return

        try {
            // 创建临时文件，因为 MediaRecorder 需要一个输出路径，即使我们只想要音量
            // 使用 /dev/null 在部分机型上可能报错，建议使用临时文件
            val tempFile = File.createTempFile("temp_audio", ".m4a", cacheDir)
            
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC) // 麦克风源
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // 输出格式
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC) // 编码格式
                setOutputFile(tempFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            Log.d("AudioRecorder", "Recording started")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("AudioRecorder", "Prepare failed")
        } catch (e: RuntimeException) {
            e.printStackTrace()
            Log.e("AudioRecorder", "Start failed (Permission denied?)")
        }
    }

    /**
     * 获取当前音量最大振幅
     * @return 0 ~ 32767 (MediaRecorder 的标准范围)
     */
    fun getCurrentAmplitude(): Int {
        return try {
            if (isRecording) mediaRecorder?.maxAmplitude ?: 0 else 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 停止录音并释放资源
     */
    fun stopRecord() {
        if (!isRecording) return
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: RuntimeException) {
            // 防止立即 stop 导致的 crash
        } finally {
            mediaRecorder = null
            isRecording = false
            Log.d("AudioRecorder", "Recording stopped")
        }
    }
}