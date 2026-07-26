package com.drivingschoolrwandaapp.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.drivingschoolrwandaapp.database.dao.PdfDao;
import com.drivingschoolrwandaapp.database.entities.Bookmark;
import com.drivingschoolrwandaapp.database.entities.BookmarkEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PdfRepository {

    private final PdfDao pdfDao;
    private final ExecutorService executorService;

    @Inject
    public PdfRepository(PdfDao pdfDao) {
        this.pdfDao = pdfDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void addBookmark(int pdfId, int page, String name) {
        executeSafely(() -> {
            BookmarkEntity bookmark = new BookmarkEntity(pdfId, page, name);
            pdfDao.insertBookmark(bookmark);
        });
    }    public LiveData<List<Bookmark>> getBookmarks(int pdfId) {
        return pdfDao.getBookmarksByPdfId(pdfId);
    }

    private void executeSafely(Runnable runnable) {
        try {
            if (!executorService.isShutdown() && !executorService.isTerminated()) {
                executorService.execute(runnable);
            }
        } catch (RejectedExecutionException e) {
            Log.w("PdfRepository", "Task rejected, executor is shutting down", e);
        }
    }

    public void deleteBookmark(Bookmark bookmark) {
        executeSafely(() -> {
            pdfDao.deleteBookmark(bookmark.id);
        });
    }

    /**
     * Cleanly shut down the internal executor to release the background thread.
     * Call from the ViewModel's onCleared() when this repository is no longer needed.
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
