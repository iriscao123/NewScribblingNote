package com.example.mypdfplayer;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private String fileFold = "paper";

    private MediaPlayer mediaPlayer;
    private String selectedFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);





// 加载 assets/picture 文件夹中的文件
        //模拟PDF文件名列表
        List<String> fileList = getAssetFiles(fileFold);
        FileAdapter adapter = new FileAdapter(this,fileList, file -> {
            // 选择 PDF 文件后打开
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

        // 定义文件的目标路径
        String outputPath = getFilesDir().getAbsolutePath() + "/" + fileName;

// 将文件从 assets 复制到内部存储
        copyAssetToInternalStorage(fileName, outputPath);

        // 创建打开 PDF Viewer 的 Intent
        Intent intent = new Intent(this, PdfViewerActivity.class);

// 将文件完整路径传递给 PdfViewerActivity

        intent.putExtra("filePath", outputPath);
        startActivity(intent);
    }

    private void copyAssetToInternalStorage(String assetName, String outputPath) {
        try (InputStream in = getAssets().open(fileFold + "/" + assetName);
             OutputStream out = new FileOutputStream(outputPath)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        super.onDestroy();
    }
}