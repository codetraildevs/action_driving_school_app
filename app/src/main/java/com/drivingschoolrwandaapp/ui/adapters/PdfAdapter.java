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
import java.util.concurrent.RejectedExecutionException;

public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.PdfViewHolder> {

    private final PdfRenderer pdfRenderer;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2); // Reduced threads to save memory with high-res bitmaps
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final int screenWidth;
    private final boolean isLowRamDevice;
    // Lower renderScale (1.5f) for better memory performance on low-RAM devices.
    // Original was 2.0f which creates 4x the pixels of a 1.0f render, consuming significant RAM.
    private static final float RENDER_SCALE_HIGH_RAM = 2.0f;
    private static final float RENDER_SCALE_LOW_RAM = 1.5f;
    // Lower max dimension cap on low-RAM devices to prevent OOM on large displays
    // 4096 pixels @ ARGB_8888 = 64MB per bitmap worst case (unlikely at renderScale 2.0)
    // 2048 pixels @ ARGB_8888 = 16MB per bitmap worst case
    private static final int MAX_DIMENSION_HIGH_RAM = 3072;
    private static final int MAX_DIMENSION_LOW_RAM = 2048;
    private final float renderScale;
    private final int maxDimension;

    /**
     * @param pdfRenderer The PdfRenderer instance
     * @param screenWidth Screen width in pixels for computing render dimensions
     * @param isLowRamDevice Whether the device has limited RAM (<=2GB). Uses lower render scale
     *                       and lower bitmap dimension cap.
     */
    public PdfAdapter(PdfRenderer pdfRenderer, int screenWidth, boolean isLowRamDevice) {
        this.pdfRenderer = pdfRenderer;
        this.screenWidth = screenWidth;
        this.isLowRamDevice = isLowRamDevice;
        this.renderScale = isLowRamDevice ? RENDER_SCALE_LOW_RAM : RENDER_SCALE_HIGH_RAM;
        this.maxDimension = isLowRamDevice ? MAX_DIMENSION_LOW_RAM : MAX_DIMENSION_HIGH_RAM;
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

    private void executeSafely(Runnable runnable) {
        try {
            if (!executorService.isShutdown() && !executorService.isTerminated()) {
                executorService.execute(runnable);
            }
        } catch (RejectedExecutionException e) {
            Log.w("PdfAdapter", "Task rejected, executor is shutting down", e);
        }
    }

    @Override
    public void onViewRecycled(@NonNull PdfViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycleBitmap();
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

        /**
         * Release the bitmap when the view is recycled to free memory immediately
         * instead of waiting for GC. Prevents accumulation of rendered pages
         * when scrolling quickly through a large PDF.
         */
        public void recycleBitmap() {
            if (photoView != null) {
                photoView.setImageBitmap(null);
            }
        }

        public void bind(int position) {
            // Clear the current image immediately so the old bitmap can be GC'd
            // while the new one renders on a background thread.
            photoView.setImageBitmap(null);

            // PhotoView with layout_height="wrap_content" measures its height to
            // the bitmap's full pixel height, but draws the image scaled to fit
            // the view width — leaving a large blank area below every rendered
            // page. Pin the view height to the page's aspect ratio (screen-width
            // based) so the rendered page fills the view exactly.
            synchronized (pdfRenderer) {
                PdfRenderer.Page page = pdfRenderer.openPage(position);
                try {
                    float pageAspect = page.getHeight() / (float) page.getWidth();
                    int viewWidth = photoView.getWidth() > 0 ? photoView.getWidth() : screenWidth;
                    int targetHeight = (int) (viewWidth * pageAspect);
                    ViewGroup.LayoutParams lp = photoView.getLayoutParams();
                    if (lp != null && lp.height != targetHeight) {
                        lp.height = targetHeight;
                        photoView.requestLayout();
                    }
                } finally {
                    page.close();
                }
            }

            executeSafely(() -> {
                synchronized (pdfRenderer) {
                    PdfRenderer.Page page = pdfRenderer.openPage(position);
                    
                    // Render at higher resolution for sharpness
                    int bitmapWidth = (int) (screenWidth * renderScale);
                    int bitmapHeight = (int) (page.getHeight() * ((float) bitmapWidth / page.getWidth()));

                    Bitmap bitmap = null;
                    try {
                        // Cap bitmap dimensions using the per-device max dimension field
                        int cappedWidth = Math.min(bitmapWidth, maxDimension);
                        int cappedHeight = (int) (page.getHeight() * ((float) cappedWidth / page.getWidth()));
                        cappedHeight = Math.min(cappedHeight, maxDimension);

                        bitmap = Bitmap.createBitmap(cappedWidth, cappedHeight, Bitmap.Config.ARGB_8888);
                        // Fill with opaque white BEFORE rendering: PdfRenderer leaves
                        // unpainted/transparent page areas as the bitmap's initial
                        // content, and a transparent bitmap would let the app's
                        // cyan window background show through the page.
                        bitmap.eraseColor(android.graphics.Color.WHITE);
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
