package com.example.picturereader;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.IOException;

public class PdfViewerActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer; // MediaPlayer 实例
    private String audioFileName; // 当前音频文件名
    private String fileFold = "picture";  //绘本和音频文件存储目录
    private boolean isPlaying = false; // 记录当前播放状态
    private ImageButton btnPlayPause; // 播放/暂停按钮
    private ImageView btnStop,btnReturn; // 停止按钮
    private int position = 0; // 当前播放位置

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        PDFView pdfView = findViewById(R.id.pdfView);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnStop = findViewById(R.id.btnStop);
        btnReturn = findViewById(R.id.btReturn);

        btnReturn.setOnClickListener(view -> finish()); // 返回上一层界面

        releaseMediaPlayer();

// 从 Intent 获取 PDF 文件名
        String pdfFileName = getIntent().getStringExtra("fileName");
        if (pdfFileName == null) {
            Toast.makeText(this, "文件名为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

// 设置音频文件名（替换后缀 .pdf 为 .mp3）
        audioFileName = pdfFileName.replace(".pdf", ".mp3");
        Log.d("AudioDebug", "尝试加载音频文件：" + audioFileName);


// 加载 PDF 文件
        try {
            pdfView.fromStream(getAssets().open(fileFold + "/" +
                    pdfFileName)).load();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "无法加载 PDF 文件",
                    Toast.LENGTH_SHORT).show();
        }



// 设置播放/暂停按钮点击事件

        btnPlayPause.setOnClickListener(view -> {
            if (isPlaying) {
                pauseAudio(btnPlayPause); // 暂停音频
            } else {
                playAudio(btnPlayPause); // 播放音频
            }
        });

// 设置停止按钮点击事件
        btnStop.setOnClickListener(v -> stopAudio());
    }


    private void playAudio(ImageButton btnPlayPause) {
// 每次播放前释放 MediaPlayer
        releaseMediaPlayer();

        try {
            AssetManager assetManager = getAssets();
            mediaPlayer = new MediaPlayer();

// 构建音频路径
            String audioPath = fileFold + "/" + audioFileName;
            Log.d("AudioDebug", "尝试播放音频文件：" + audioPath);

            // 设置音频数据源
            AssetFileDescriptor afd = assetManager.openFd(audioPath);
            mediaPlayer.setDataSource(afd.getFileDescriptor(),
                    afd.getStartOffset(), afd.getLength());
            afd.close();


            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.seekTo(position); // 从暂停处继续播放
// 切换为暂停图标
            this.btnPlayPause.setImageResource(R.drawable.ic_pause1);
            isPlaying = true;

// 音频播放完成监听
            mediaPlayer.setOnCompletionListener(mp -> {
                this.btnPlayPause.setImageResource(R.drawable.ic_play);
                isPlaying = false;
                releaseMediaPlayer(); // 释放资源
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "无法播放音频：" + audioFileName,
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void pauseAudio(ImageButton btnPlayPause) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            position = mediaPlayer.getCurrentPosition(); // 记录当前播放位置
// 切换为播放图标
            btnPlayPause.setImageResource(R.drawable.ic_play);
            isPlaying = false;
        }
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop(); // 停止播放
            releaseMediaPlayer();

// 重置按钮状态
            btnPlayPause.setImageResource(R.drawable.ic_play);
            isPlaying = false;
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release(); // 释放资源
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
// 释放资源以防止内存泄漏
        releaseMediaPlayer();
        super.onDestroy();
    }
}