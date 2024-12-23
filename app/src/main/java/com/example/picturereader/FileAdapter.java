package com.example.picturereader;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private final List<String> files; // PDF 文件列表

    private final OnFileClickListener listener;
    private final Context context; // 上下文，用于访问资源
    private String fileFold = "picture";

    public FileAdapter(Context context,List<String> files, OnFileClickListener listener) {
        this.context = context;
        this.files = files;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 使用自定义布局 item_pdf 封面显示

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        String fileName = files.get(position);

        // 设置文件名
        holder.textView.setText(fileName);
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> listener.onFileClick(fileName));

        // 加载 PDF 封面
        try {
            //将文件拷贝到缓存
            File pdfFile = copyPdfToCache(context,fileName);
// 从 assets 获取 PDF 文件
            AssetManager assetManager = context.getAssets();
//            ParcelFileDescriptor fileDescriptor = assetManager.openFd(fileFold + "/" + fileName).getParcelFileDescriptor();
            ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
// 初始化 PdfRenderer
            PdfRenderer renderer = new PdfRenderer(fileDescriptor);
            PdfRenderer.Page page = renderer.openPage(0);

// 创建位图
            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

// 将渲染的封面设置到 ImageView
            holder.imageView.setImageBitmap(bitmap);

// 关闭资源
            page.close();
            renderer.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("PDF_DEBUG", "加载封面失败: " + e.getMessage());

// 如果失败，显示默认图片
            holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            // 如果加载失败，则显示默认占位图片
            holder.imageView.setImageResource(android.R.drawable.ic_menu_report_image); // 加载失败时显示默认图片
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView; // PDF 封面
        TextView textView;// PDF 名称

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.pdfCoverImage); // 封面图片
            textView = itemView.findViewById(R.id.pdfTitle);// 文件名
        }
    }

    public interface OnFileClickListener {
        void onFileClick(String file);
    }

    //拷贝指定目录下的文件至缓存文件
    private File copyPdfToCache(Context context, String fileName) throws Exception {
        File cacheFile = new File(context.getCacheDir(), fileName);
        if (!cacheFile.exists()) {



            InputStream inputStream = context.getAssets().open(fileFold + "/" + fileName);
            FileOutputStream outputStream = new FileOutputStream(cacheFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();
        }
        return cacheFile;
    }


}