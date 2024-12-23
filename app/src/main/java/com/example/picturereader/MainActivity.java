package com.example.picturereader;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private String fileFold = "picture";  //绘本和音频文件存储目录
//    private ImageView btnPlayAudio;
    private MediaPlayer mediaPlayer;
    private String selectedFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);


// 加载 assets/picture 文件夹中的文件
        List<String> fileList = getAssetFiles(fileFold);
        FileAdapter adapter = new FileAdapter(this,fileList, file -> {
            selectedFile = file;
            openPdf(file);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);


    }

    // 获取 assets 下指定目录的文件列表
    private List<String> getAssetFiles(String folderName) {
        List<String> files = new ArrayList<>();
        try {
            AssetManager assetManager = getAssets();
            String[] fileNames = assetManager.list(folderName);
            if (fileNames != null) {
                for (String fileName : fileNames) {
                    if (fileName.endsWith(".pdf")) {
                        files.add(fileName);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }


    private void openPdf(String fileName) {
        Intent intent = new Intent(this, PdfViewerActivity.class);
        intent.putExtra("fileName", fileName);
        startActivity(intent);
    }




    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        super.onDestroy();
    }
}