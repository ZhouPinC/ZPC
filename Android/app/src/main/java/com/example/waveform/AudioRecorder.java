package com.example.waveform;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Process;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioRecorder {

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    // 安全获取缓冲区大小，避免模拟器上可能出现的负数错误
    private static final int BUFFER_SIZE = Math.max(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT),
            4096 // 默认最小 fallback 值
    );

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private volatile boolean isRecording = false;
    private OnAmplitudeListener amplitudeListener;
    private File currentRawFile;
    private Context context;

    public interface OnAmplitudeListener {
        void onAmplitudeUpdate(float amplitude);
    }

    public AudioRecorder(Context context) {
        this.context = context;
    }

    public void setAmplitudeListener(OnAmplitudeListener listener) {
        this.amplitudeListener = listener;
    }

    public void startRecording() {
        if (isRecording) return;
        try {
            // 创建文件
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Recordings");
            if (!dir.exists()) dir.mkdirs();
            currentRawFile = new File(dir, "REC_" + timeStamp + ".pcm"); // 暂存PCM

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord initialize failed");
                return;
            }

            isRecording = true;
            recordingThread = new Thread(new RecordingRunnable(), "AudioRecorderThread");
            recordingThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            isRecording = false;
        }
    }

    /**
     * 停止录音并保存为 WAV
     * @return 最终的 WAV 文件路径
     */
    public String stopRecording() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            audioRecord = null;
        }
        // 线程会通过 isRecording 标志自然退出
        recordingThread = null;

        // 转换 PCM 到 WAV
        if (currentRawFile != null && currentRawFile.exists()) {
            File wavFile = new File(currentRawFile.getParent(), currentRawFile.getName().replace(".pcm", ".wav"));
            rawToWave(currentRawFile, wavFile);
            currentRawFile.delete(); // 删除原始 PCM
            return wavFile.getAbsolutePath();
        }
        return null;
    }

    private class RecordingRunnable implements Runnable {
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

            if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                return;
            }

            audioRecord.startRecording();
            byte[] buffer = new byte[BUFFER_SIZE];
            FileOutputStream os = null;
            
            // [关键修复] 限制UI更新频率为每40ms一次 (25fps)，防止ANR崩溃
            long lastUiUpdate = 0;
            final long UPDATE_INTERVAL_MS = 40;

            try {
                os = new FileOutputStream(currentRawFile);
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        os.write(buffer, 0, read);
                        
                        if (amplitudeListener != null) {
                            long now = System.currentTimeMillis();
                            if (now - lastUiUpdate > UPDATE_INTERVAL_MS) {
                                float amplitude = calculateAmplitude(buffer, read);
                                amplitudeListener.onAmplitudeUpdate(amplitude);
                                lastUiUpdate = now;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { if (os != null) os.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }

        private float calculateAmplitude(byte[] buffer, int read) {
            long sum = 0;
            for (int i = 0; i < read; i += 2) {
                short sample = (short) ((buffer[i] & 0xFF) | (buffer[i + 1] << 8));
                sum += Math.abs(sample);
            }
            return read > 0 ? (float) sum / (read / 2) : 0;
        }
    }

    // --- WAV Header Helper ---
    private void rawToWave(File rawFile, File waveFile) {
        try (FileInputStream in = new FileInputStream(rawFile);
             FileOutputStream out = new FileOutputStream(waveFile)) {
            long totalAudioLen = in.getChannel().size();
            long totalDataLen = totalAudioLen + 36;
            long longSampleRate = SAMPLE_RATE;
            int channels = 1;
            long byteRate = 16 * SAMPLE_RATE * channels / 8;

            writeWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);

            byte[] data = new byte[BUFFER_SIZE];
            while (in.read(data) != -1) {
                out.write(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen,
                                     long longSampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff); header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff); header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0; header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff); header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff); header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff); header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff); header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); header[33] = 0; header[34] = 16; header[35] = 0;
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff); header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff); header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    public boolean isRecording() {
        return isRecording;
    }
}