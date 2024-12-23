package com.example.picturereader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.pdf.PdfRenderer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.FileOutputStream;
import java.util.Stack;

public class PdfCanvasView extends View {

    private PdfRenderer.Page pdfPage;
    private Paint paint;
    private float scaleFactor = 1.0f; // 缩放因子
    private ScaleGestureDetector scaleGestureDetector;
    private float offsetX = 0f, offsetY = 0f; // 偏移量，用于平移
    private float startX = 0f, startY = 0f; // 手指按下时的位置

    private Bitmap drawingBitmap; // 笔记层
    private Canvas drawingCanvas;
    private Paint drawingPaint;
    private Path currentPath;
    private Stack<Path> undoStack; // 用于撤销功能

    public PdfCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());

// 初始化笔记绘制相关
        drawingPaint = new Paint();
        drawingPaint.setColor(Color.RED);
        drawingPaint.setStyle(Paint.Style.STROKE);
        drawingPaint.setStrokeWidth(5);
        drawingPaint.setAntiAlias(true);

        currentPath = new Path();
        undoStack = new Stack<>();
    }

    public void setPdfPage(PdfRenderer.Page page) {
        this.pdfPage = page;

// 初始化笔记层
        if (page != null) {
            drawingBitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            drawingCanvas = new Canvas(drawingBitmap);
        }

        invalidate(); // 触发重新绘制
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pdfPage != null) {
// 保存画布状态
            canvas.save();

// 计算初始缩放比例
            float screenWidth = getWidth();
            float screenHeight = getHeight();
            float pdfWidth = pdfPage.getWidth();
            float pdfHeight = pdfPage.getHeight();
            float initialScale = Math.min(screenWidth / pdfWidth, screenHeight / pdfHeight);

// 平移和缩放
            canvas.translate(offsetX, offsetY);
            canvas.scale(initialScale * scaleFactor, initialScale * scaleFactor);

// 绘制 PDF 页面
            Bitmap pdfBitmap = Bitmap.createBitmap(pdfPage.getWidth(), pdfPage.getHeight(), Bitmap.Config.ARGB_8888);

            pdfPage.render(pdfBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

// 绘制笔记层
            canvas.drawBitmap(drawingBitmap, 0, 0, null);

// 恢复画布状态
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
// 处理缩放手势
        scaleGestureDetector.onTouchEvent(event);

// 处理绘制笔记
        if (event.getPointerCount() == 1) {
            float x = (event.getX() - offsetX) / scaleFactor;
            float y = (event.getY() - offsetY) / scaleFactor;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    currentPath.moveTo(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    currentPath.lineTo(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    drawingCanvas.drawPath(currentPath, drawingPaint);
                    undoStack.push(new Path(currentPath)); // 保存到撤销栈
                    currentPath.reset();
                    break;
            }
            invalidate(); // 触发重绘
        }

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 5.0f)); // 限制缩放范围
            invalidate(); // 触发重绘
            return true;
        }
    }

    // 撤销功能
    public void undo() {
        if (!undoStack.isEmpty()) {
            undoStack.pop();
            redrawNotes();
        }
    }

    private void redrawNotes() {
        if (drawingCanvas != null) {
            drawingCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // 清空笔记层
            for (Path path : undoStack) {
                drawingCanvas.drawPath(path, drawingPaint);
            }
            invalidate();
        }
    }

    // 保存笔记
    public void saveNotes(String filePath) {
        if (drawingBitmap != null) {
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                drawingBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}