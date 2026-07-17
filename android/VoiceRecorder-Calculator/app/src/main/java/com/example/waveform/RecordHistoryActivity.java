package com.example.waveform;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecordHistoryActivity extends AppCompatActivity {

    private ListView listView;
    private TextView emptyView;
    private List<File> recordFiles;
    private ArrayAdapter<String> adapter;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 简单的布局构建
        // 为了方便，这里演示动态创建 View，确保能直接运行
        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setBackgroundColor(0xFFFFFFFF);
        listView = new ListView(this);
        emptyView = new TextView(this);
        emptyView.setText("暂无录音记录");
        emptyView.setTextColor(0xFF999999);
        emptyView.setGravity(android.view.Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        
        root.addView(listView);
        root.addView(emptyView);
        setContentView(root);

        setTitle("历史记录");

        loadFiles();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < recordFiles.size()) {
                playAudio(recordFiles.get(position));
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this)
                .setTitle("操作")
                .setItems(new String[]{"删除"}, (dialog, which) -> {
                    if (which == 0) deleteFile(position);
                })
                .show();
            return true;
        });
    }

    private void loadFiles() {
        recordFiles = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();

        try {
            // [关键修复] 在模拟器上 context.getExternalFilesDir 可能返回 null
            File baseDir = getExternalFilesDir(null);
            if (baseDir == null) {
                // 尝试 fallback 到内部存储
                baseDir = getFilesDir();
                Log.d("History", "Using internal storage: " + baseDir.getAbsolutePath());
            } else {
                Log.d("History", "Using external storage: " + baseDir.getAbsolutePath());
            }

            File dir = new File(baseDir, "Recordings");
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    Log.e("History", "Directory creation failed for: " + dir.getAbsolutePath());
                } else {
                    Log.d("History", "Directory created: " + dir.getAbsolutePath());
                }
            }

            Log.d("History", "Checking directory: " + dir.getAbsolutePath());
            File[] files = dir.listFiles();

            if (files != null) {
                Log.d("History", "Found " + files.length + " files in directory");
                // 按时间倒序
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                for (File f : files) {
                    if (f.getName().endsWith(".wav") && f.length() > 0) {
                        recordFiles.add(f);
                        long sizeKb = f.length() / 1024;
                        fileNames.add(f.getName() + "\n" + sizeKb + " KB");
                        Log.d("History", "Added file: " + f.getName() + " (" + sizeKb + " KB)");
                    }
                }
            } else {
                Log.d("History", "No files found or directory not accessible");
            }
        } catch (Exception e) {
            Log.e("History", "Error loading files", e);
            Toast.makeText(this, "加载文件出错: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        Log.d("History", "Total valid files: " + fileNames.size());

        if (fileNames.isEmpty()) {
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
            listView.setAdapter(adapter);
        }
    }

    private void playAudio(File file) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "开始播放", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "无法播放文件", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteFile(int position) {
        try {
            File f = recordFiles.get(position);
            if (f.delete()) {
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                loadFiles(); // 刷新列表
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) mediaPlayer.release();
    }
}