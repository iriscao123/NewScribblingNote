package com.example.mypdfplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.pdf.PdfRenderer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Stack;

public class PdfCanvasView extends View {

    private PdfRenderer.Page pdfPage;
    private Bitmap drawingBitmap;// 用于存储笔记的位图
    private Canvas drawingCanvas;// 用于绘制笔记的画布
    Paint drawingPaint;// 笔记画笔
    Paint eraserPaint;//橡皮擦画笔
    private Path currentPath;// 当前笔记路径
    private Stack<Bitmap> undoStack = new Stack<>();// 撤销栈

    private boolean isDrawingMode = false; // 是否启用画笔模式
    private boolean isEraserMode = false; // 是否启用橡皮擦模式

    private float offsetX = 0, offsetY = 0;// 偏移量
    private float scaleFactor = 1.0f;// 用户缩放比例
    private float initialScale = 1.0f;// 初始缩放比例
    private ScaleGestureDetector scaleGestureDetector;

    public PdfCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);

// 缩放手势检测器
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 3.0f));// 限制缩放范围
                invalidate();
                return true;
            }
        });

// 初始化画笔
        drawingPaint = new Paint();
        drawingPaint.setColor(Color.BLACK); // 默认颜色
        drawingPaint.setStyle(Paint.Style.STROKE);//设置为描边模式
        drawingPaint.setStrokeWidth(5f); // 默认画笔粗细
        drawingPaint.setAntiAlias(true);// 抗锯齿

// 初始化橡皮擦
        eraserPaint = new Paint();
        eraserPaint.setStyle(Paint.Style.STROKE);
        eraserPaint.setStrokeWidth(10f); // 默认橡皮擦粗细
        eraserPaint.setColor(Color.TRANSPARENT);
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        currentPath = new Path();
    }




    public void setPdfPage(PdfRenderer.Page page) {
        this.pdfPage = page;
        invalidate();// 重新绘制
    }
    public void setPaintColor(int color) {
        drawingPaint.setColor(color);
    }

    public void setPaintWidth(float width) {
        drawingPaint.setStrokeWidth(width);
    }

    public void setEraserWidth(float width) {
        eraserPaint.setStrokeWidth(width);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
// 创建笔记层
        if (drawingBitmap == null) {
            drawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            drawingCanvas = new Canvas(drawingBitmap);
        }
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
            initialScale = Math.min(screenWidth / pdfWidth, screenHeight / pdfHeight);
        // 平移和缩放
            canvas.translate(offsetX, offsetY);
            canvas.scale(initialScale * scaleFactor, initialScale * scaleFactor);
        // 渲染 PDF 页面到 Bitmap
            Bitmap pdfBitmap = Bitmap.createBitmap(pdfPage.getWidth(), pdfPage.getHeight(), Bitmap.Config.ARGB_8888);
            pdfPage.render(pdfBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // 绘制 PDF 页面
            canvas.drawBitmap(pdfBitmap, 0, 0, null);
        // 绘制笔记层
            canvas.drawBitmap(drawingBitmap, 0, 0, null);
            canvas.drawPath(currentPath, drawingPaint);
        // 恢复画布状态
            canvas.restore();
        // 回收 Bitmap
            pdfBitmap.recycle();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("PdfCanvasView", "触摸事件: " + event.getAction());

//        return super.onTouchEvent(event);
//     处理缩放手势
        scaleGestureDetector.onTouchEvent(event);
        Log.d("PdfCanvasView", "当前模式: " + (isDrawingMode ? "绘图" : "非绘图"));
        Log.d("PdfCanvasView", "画笔颜色: " + drawingPaint.getColor());
        Log.d("PdfCanvasView", "当前路径: " + currentPath.isEmpty());
        Log.d("PdfCanvasView", "当前模式: " + (isEraserMode ? "橡皮擦" : "非橡皮擦"));
// 仅在绘图模式或橡皮擦模式时处理绘图逻辑
            if (isDrawingMode || isEraserMode) {
                float x = (event.getX() - offsetX) / (scaleFactor * initialScale);
                float y = (event.getY() - offsetY) / (scaleFactor * initialScale);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        saveCurrentStateToUndoStack(); // 保存状态
                        currentPath.moveTo(x, y);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        currentPath.lineTo(x, y);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isDrawingMode) {
//
                            drawingCanvas.drawPath(currentPath, drawingPaint);
                        } else if (isEraserMode) {
                            drawingCanvas.drawPath(currentPath, eraserPaint);
                        }
//                        undoStack.push(new Path(currentPath)); // 保存撤销轨迹
                        currentPath.reset();
                        break;
                }
                invalidate(); // 触发重绘
                return true;
            }
            return super.onTouchEvent(event);
        }

    public void enableDrawingMode(boolean enabled) {
        isDrawingMode = enabled;
        isEraserMode = false; // 确保橡皮擦模式关闭
        Log.d("PdfCanvasView", "触发: " + (isDrawingMode ? "绘图模式" : "非绘图"));
        invalidate(); // 强制重新绘制
    }

    public void enableEraserMode(boolean enabled) {
        isEraserMode = enabled;
        isDrawingMode = false; // 确保画笔模式关闭
        Log.d("PdfCanvasView", "触发: " + (isEraserMode ? "橡皮擦模式" : "非橡皮擦"));
        invalidate(); // 强制重新绘制
    }

    public void setDrawingMode(boolean isDrawing, boolean isErasing) {
        isDrawingMode = isDrawing;
        isEraserMode = isErasing;
        invalidate(); // 强制重绘
        Log.d("PdfCanvasView", "当前模式: " + (isDrawingMode ? "绘图" : isEraserMode ? "橡皮擦" : "无"));
    }

    private void saveCurrentStateToUndoStack() {
        Bitmap snapshot = Bitmap.createBitmap(drawingBitmap);
        undoStack.push(snapshot);
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Bitmap lastState = undoStack.pop();
            drawingBitmap = lastState.copy(Bitmap.Config.ARGB_8888, true);
            drawingCanvas = new Canvas(drawingBitmap);
            invalidate();
        }
    }

    public void saveNotes(String filePath) {
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            drawingBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//
    public void setDrawingColor(int color) {
        drawingPaint.setColor(color);
    }

    public void setDrawingStrokeWidth(float strokeWidth) {
        drawingPaint.setStrokeWidth(strokeWidth);
    }

    public void setEraserStrokeWidth(float strokeWidth) {
        eraserPaint.setStrokeWidth(strokeWidth);
    }

    public void zoomIn() {
        scaleFactor *= 1.1f;
        scaleFactor = Math.min(scaleFactor, 3.0f); // 最大缩放 3x
        invalidate();
    }

    public void zoomOut() {
        scaleFactor *= 0.9f;
        scaleFactor = Math.max(scaleFactor, 0.5f); // 最小缩放 0.5x
        invalidate();
    }
}