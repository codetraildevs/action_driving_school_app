package com.drivingschoolrwandaapp.ui.adapters;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drivingschoolrwandaapp.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.PdfViewHolder> {

    private final PdfRenderer pdfRenderer;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2); // Reduced threads to save memory with high-res bitmaps
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final int screenWidth;
    private final float renderScale = 2.0f; // Increase this for even sharper text (e.g., 2.5f or 3.0f), but consumes more RAM

    public PdfAdapter(PdfRenderer pdfRenderer, int screenWidth) {
        this.pdfRenderer = pdfRenderer;
        this.screenWidth = screenWidth;
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return pdfRenderer != null ? pdfRenderer.getPageCount() : 0;
    }

    class PdfViewHolder extends RecyclerView.ViewHolder {
        private final PhotoView photoView;

        public PdfViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.pdf_page_image);
            
            // Set zoom levels for PhotoView
            photoView.setMinimumScale(1.0f);
            photoView.setMediumScale(2.5f);
            photoView.setMaximumScale(5.0f);
        }

        public void bind(int position) {
            photoView.setImageBitmap(null); // Placeholder/Clear
            
            executorService.execute(() -> {
                synchronized (pdfRenderer) {
                    PdfRenderer.Page page = pdfRenderer.openPage(position);
                    
                    // Render at higher resolution for sharpness
                    int bitmapWidth = (int) (screenWidth * renderScale);
                    int bitmapHeight = (int) (page.getHeight() * ((float) bitmapWidth / page.getWidth()));

                    try {
                        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        page.close();

                        mainHandler.post(() -> {
                            photoView.setImageBitmap(bitmap);
                        });
                    } catch (OutOfMemoryError e) {
                        Log.e("PdfAdapter", "Out of memory rendering PDF page " + position, e);
                        page.close();
                    }
                }
            });
        }
    }
}
