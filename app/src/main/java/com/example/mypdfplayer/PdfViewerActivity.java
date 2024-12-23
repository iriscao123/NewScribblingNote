package com.example.mypdfplayer;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;

public class PdfViewerActivity extends AppCompatActivity {
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;
    private PdfRenderer.Page currentPage;
    private PdfCanvasView pdfCanvasView;

    private int currentPageIndex = 0;

    private ImageButton btnNext, btnPrevious, btnZoomIn, btnZoomOut;
    private ImageButton btnPen, btnEraser, btnUndo, btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        pdfCanvasView = findViewById(R.id.pdfCanvasView);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnPen = findViewById(R.id.btnPen);
        btnEraser = findViewById(R.id.btnEraser);
        btnUndo = findViewById(R.id.btnUndo);
        btnSave = findViewById(R.id.btnSave);

        Spinner penColorSpinner = findViewById(R.id.penColorSpinner);
        Spinner penWidthSpinner = findViewById(R.id.penWidthSpinner);

//  打开PDF 文件
        String filePath = getIntent().getStringExtra("filePath");
        if (filePath == null) {
            Toast.makeText(this, "未指定文件路径", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        openPdf(filePath);
        renderPage();

// 翻页功能
        btnNext.setOnClickListener(v -> {
            if (currentPageIndex < pdfRenderer.getPageCount() - 1) {
                currentPageIndex++;
                renderPage();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                renderPage();
            }
        });

// 缩放功能
        btnZoomIn.setOnClickListener(v -> pdfCanvasView.zoomIn());
        btnZoomOut.setOnClickListener(v -> pdfCanvasView.zoomOut());

// 启动画笔功能
        btnPen.setOnClickListener(v -> {
            pdfCanvasView.setDrawingMode(true, false);
//            showPenOptions();// 选择颜色和粗细
        });

//启动橡皮擦功能
        btnEraser.setOnClickListener(v -> {
        pdfCanvasView.setDrawingMode(false, true);
//            showEraserOptions();
        });

// 撤销功能
        btnUndo.setOnClickListener(v -> pdfCanvasView.undo());

// 保存笔记功能
        btnSave.setOnClickListener(v -> {
            //指定保存到Picture/pdf_notes
//            String notesPath = getExternalFilesDir(null) + "/pdf_notes_" + System.currentTimeMillis() + ".png";
//获取相册目录: 获取Android设备的公共存储目录，并指定为 DIRECTORY_PICTURES，即相册目录。
            File picturesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES);
 //创建子目录: 在相册目录下创建名为 “pdf_notes” 的子目录，用于存放笔记图片。如果该目录不存在，则使用 mkdirs() 方法创建它。
            File pdfNotesDir = new File(picturesDir, "pdf_notes");
            if (!pdfNotesDir.exists()) {
                pdfNotesDir.mkdirs(); // 创建子目录
            }
            String notesPath = pdfNotesDir +
                    "/pdf_notes_" + System.currentTimeMillis() + ".png";

            pdfCanvasView.saveNotes(notesPath);
            Toast.makeText(this, "笔记已保存至: " + notesPath, Toast.LENGTH_SHORT).show();
        });




        // 笔颜色选择下拉菜单
        ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(this,
                R.array.pen_colors, android.R.layout.simple_spinner_item);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        penColorSpinner.setAdapter(colorAdapter);
        penColorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                      @Override
                                                      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                                          switch (position) {
                                                              case 0:
                                                                  pdfCanvasView.setPaintColor(Color.RED);
                                                                  break;
                                                              case 1:
                                                                  pdfCanvasView.setPaintColor(Color.BLUE);
                                                                  break;
                                                              case 2:
                                                                  pdfCanvasView.setPaintColor(Color.GREEN);
                                                                  break;
                                                              case 3:
                                                                  pdfCanvasView.setPaintColor(Color.BLACK);
                                                                  break;

                                                          }
                                                      }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

// 笔宽选择下拉菜单
        ArrayAdapter<CharSequence> widthAdapter = ArrayAdapter.createFromResource(this,
                R.array.pen_widths, android.R.layout.simple_spinner_item);
        widthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        penWidthSpinner.setAdapter(widthAdapter);
        penWidthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        pdfCanvasView.setPaintWidth(5f);
                        pdfCanvasView.setEraserWidth(5f);
                        break;
                    case 1:
                        pdfCanvasView.setPaintWidth(10f);
                        pdfCanvasView.setEraserWidth(10f);
                        break;
                    case 2:
                        pdfCanvasView.setPaintWidth(20f);
                        pdfCanvasView.setEraserWidth(20f);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
//打开pdf文件
    private void openPdf(String filePath) {
        try {
            fileDescriptor = ParcelFileDescriptor.open(new File(filePath), ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "无法打开 PDF 文件", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
//渲染页面
    private void renderPage() {
        if (pdfRenderer == null) return;

        if (currentPage != null) {
            currentPage.close();
        }

        currentPage = pdfRenderer.openPage(currentPageIndex);
        pdfCanvasView.setPdfPage(currentPage);
    }

    private void showPenOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("设置画笔");

// 自定义布局
        final SeekBar thicknessSeekBar = new SeekBar(this);
        thicknessSeekBar.setMax(50);
        thicknessSeekBar.setProgress((int) pdfCanvasView.drawingPaint.getStrokeWidth());

        builder.setView(thicknessSeekBar);

        builder.setPositiveButton("选择颜色", (dialog, which) -> showColorPicker());
        builder.setNegativeButton("确定", (dialog, which) -> {
            float newThickness = thicknessSeekBar.getProgress();
            pdfCanvasView.setDrawingStrokeWidth(newThickness);
        });

        builder.show();
    }



    private void showEraserOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("设置橡皮擦");

// 自定义布局
        final SeekBar thicknessSeekBar = new SeekBar(this);
        thicknessSeekBar.setMax(50);
        thicknessSeekBar.setProgress((int) pdfCanvasView.eraserPaint.getStrokeWidth());

        builder.setView(thicknessSeekBar);

        builder.setNegativeButton("确定", (dialog, which) -> {
            float newThickness = thicknessSeekBar.getProgress();
            pdfCanvasView.setEraserStrokeWidth(newThickness);
        });

        builder.show();
    }

    private void showColorPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择颜色");

        String[] colors = {"红色", "蓝色", "绿色", "黑色"};
        int[] colorValues = {Color.RED, Color.BLUE, Color.GREEN, Color.BLACK};

        builder.setItems(colors, (dialog, which) -> {
            int selectedColor = colorValues[which];
            pdfCanvasView.setDrawingColor(selectedColor);

        });

        builder.show();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentPage != null) currentPage.close();
        if (pdfRenderer != null) {
            pdfRenderer.close();
        }
        if (fileDescriptor != null) try {
            fileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}