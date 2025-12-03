package com.example.waveform;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;

public class AudioRecorder {

    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;
    private OnAmplitudeListener amplitudeListener;

    public interface OnAmplitudeListener {
        void onAmplitudeUpdate(float amplitude);
    }

    public void setAmplitudeListener(OnAmplitudeListener listener) {
        this.amplitudeListener = listener;
    }

    public void startRecording() {
        if (isRecording) return;

        try {
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("AudioRecord初始化失败");
            }

            isRecording = true;
            recordingThread = new Thread(new RecordingRunnable(), "AudioRecorderThread");
            recordingThread.start();

        } catch (Exception e) {
            e.printStackTrace();
            isRecording = false;
        }
    }

    public void stopRecording() {
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

        if (recordingThread != null) {
            try {
                recordingThread.interrupt();
                recordingThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recordingThread = null;
        }
    }

    private class RecordingRunnable implements Runnable {
        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            if (audioRecord != null) {
                audioRecord.startRecording();

                short[] buffer = new short[BUFFER_SIZE / 2];

                while (isRecording && !Thread.currentThread().isInterrupted()) {
                    int readSize = audioRecord.read(buffer, 0, buffer.length);

                    if (readSize > 0 && amplitudeListener != null) {
                        // 计算音频振幅
                        float amplitude = calculateAmplitude(buffer, readSize);
                        amplitudeListener.onAmplitudeUpdate(amplitude);
                    }
                }
            }
        }

        private float calculateAmplitude(short[] buffer, int length) {
            long sum = 0;
            for (int i = 0; i < length; i++) {
                sum += Math.abs(buffer[i]);
            }
            return (float) sum / length;
        }
    }

    public boolean isRecording() {
        return isRecording;
    }
}