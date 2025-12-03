package com.example.waveform;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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
    private static final int BUFFER_SIZE = Math.max(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT), 4096);

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private volatile boolean isRecording = false;
    private OnAmplitudeListener amplitudeListener;
    private File currentPcmFile;
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
            // 创建临时文件
            currentPcmFile = new File(context.getExternalCacheDir(), "temp_raw.pcm");
            
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) return;

            isRecording = true;
            recordingThread = new Thread(new RecordingRunnable());
            recordingThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            isRecording = false;
        }
    }

    /**
     * 停止录音并保存为 WAV
     * @return 最终 WAV 文件的路径
     */
    public String stopRecording() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) { e.printStackTrace(); }
            audioRecord = null;
        }

        // 将 PCM 转为 WAV 并保存到正式目录
        if (currentPcmFile != null && currentPcmFile.exists()) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File dir = new File(context.getExternalFilesDir(null), "Recordings");
            if (!dir.exists()) dir.mkdirs();
            File wavFile = new File(dir, "录音_" + timeStamp + ".wav");
            
            pcmToWav(currentPcmFile, wavFile);
            currentPcmFile.delete(); // 删除临时文件
            return wavFile.getAbsolutePath();
        }
        return null;
    }

    private class RecordingRunnable implements Runnable {
        @Override
        public void run() {
            if (audioRecord == null) return;
            audioRecord.startRecording();
            byte[] buffer = new byte[BUFFER_SIZE];
            FileOutputStream os = null;

            try {
                os = new FileOutputStream(currentPcmFile);
                long lastUiUpdate = 0;
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        os.write(buffer, 0, read);
                        // 节流更新 UI，防止卡顿
                        long now = System.currentTimeMillis();
                        if (now - lastUiUpdate > 40 && amplitudeListener != null) {
                            float amplitude = calculateAmplitude(buffer, read);
                            amplitudeListener.onAmplitudeUpdate(amplitude);
                            lastUiUpdate = now;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { if (os != null) os.close(); } catch (IOException e) {}
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

    // --- WAV Header 转换逻辑 ---
    private void pcmToWav(File pcmFile, File wavFile) {
        try (FileInputStream in = new FileInputStream(pcmFile);
             FileOutputStream out = new FileOutputStream(wavFile)) {
            long totalAudioLen = in.getChannel().size();
            long totalDataLen = totalAudioLen + 36;
            long longSampleRate = SAMPLE_RATE;
            int channels = 1;
            long byteRate = 16 * SAMPLE_RATE * channels / 8;

            writeWavHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);

            byte[] data = new byte[BUFFER_SIZE];
            int count;
            while ((count = in.read(data)) != -1) {
                out.write(data, 0, count);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void writeWavHeader(FileOutputStream out, long totalAudioLen, long totalDataLen,
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
}