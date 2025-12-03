package com.example.waveform;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
    private List<File> recordFiles;
    private ArrayAdapter<String> adapter;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 使用简单的系统列表布局
        listView = new ListView(this);
        listView.setBackgroundColor(0xFF1C1C1E); // Dark BG
        setContentView(listView);

        loadFiles();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            playAudio(recordFiles.get(position));
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteDialog(position);
            return true;
        });
    }

    private void loadFiles() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        recordFiles = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();

        if(dir != null && dir.exists()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".wav"));
            if(files != null) {
                // 按时间倒序排序
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                for (File f : files) {
                    recordFiles.add(f);
                    fileNames.add(f.getName() + "\n" + (f.length()/1024) + " KB");
                }
            }
        }

        // 使用简单的白色文字 Adapter (实际开发建议自定义 Layout)
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
        listView.setAdapter(adapter);
    }

    private void playAudio(File file) {
        if(mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "Playing...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showDeleteDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recording?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = recordFiles.get(position).delete();
                    if (deleted) {
                        loadFiles(); // 刷新列表
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mediaPlayer != null) mediaPlayer.release();
    }
}