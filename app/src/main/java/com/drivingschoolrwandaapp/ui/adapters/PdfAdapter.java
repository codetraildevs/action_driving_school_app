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
    // Lower renderScale (1.5f) for better memory performance on low-RAM devices.
    // Original was 2.0f which creates 4x the pixels of a 1.0f render, consuming significant RAM.
    private static final float RENDER_SCALE_HIGH_RAM = 2.0f;
    private static final float RENDER_SCALE_LOW_RAM = 1.5f;
    private final float renderScale;

    /**
     * @param pdfRenderer The PdfRenderer instance
     * @param screenWidth Screen width in pixels for computing render dimensions
     * @param isLowRamDevice Whether the device has limited RAM (<=2GB). Uses lower render scale.
     */
    public PdfAdapter(PdfRenderer pdfRenderer, int screenWidth, boolean isLowRamDevice) {
        this.pdfRenderer = pdfRenderer;
        this.screenWidth = screenWidth;
        this.renderScale = isLowRamDevice ? RENDER_SCALE_LOW_RAM : RENDER_SCALE_HIGH_RAM;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        executorService.shutdownNow();
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

                    Bitmap bitmap = null;
                    try {
                        // Cap bitmap dimensions to prevent OOM on low-RAM devices
                        int maxDimension = 4096; // Safety cap
                        int cappedWidth = Math.min(bitmapWidth, maxDimension);
                        int cappedHeight = (int) (page.getHeight() * ((float) cappedWidth / page.getWidth()));
                        cappedHeight = Math.min(cappedHeight, maxDimension);

                        bitmap = Bitmap.createBitmap(cappedWidth, cappedHeight, Bitmap.Config.ARGB_8888);
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                        final Bitmap finalBitmap = bitmap;
                        mainHandler.post(() -> {
                            // Recycle any previous bitmap to free memory immediately
                            photoView.setImageBitmap(null);
                            System.gc();
                            photoView.setImageBitmap(finalBitmap);
                        });
                    } catch (OutOfMemoryError e) {
                        Log.e("PdfAdapter", "Out of memory rendering PDF page " + position + " (size=" + bitmapWidth + "x" + bitmapHeight + ")", e);
                        if (bitmap != null) {
                            bitmap.recycle();
                        }
                        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                    } catch (Exception e) {
                        Log.e("PdfAdapter", "Error rendering PDF page " + position, e);
                        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
                    } finally {
                        page.close();
                    }
                }
            });
        }
    }
}
