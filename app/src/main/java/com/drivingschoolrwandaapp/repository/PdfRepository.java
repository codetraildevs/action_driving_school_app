package com.drivingschoolrwandaapp.repository;

import androidx.lifecycle.LiveData;

import com.drivingschoolrwandaapp.database.dao.PdfDao;
import com.drivingschoolrwandaapp.database.entities.Bookmark;
import com.drivingschoolrwandaapp.database.entities.BookmarkEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        executorService.execute(() -> {
            BookmarkEntity bookmark = new BookmarkEntity(pdfId, page, name);
            pdfDao.insertBookmark(bookmark);
        });
    }

    public LiveData<List<Bookmark>> getBookmarks(int pdfId) {
        return pdfDao.getBookmarksByPdfId(pdfId);
    }

    public void deleteBookmark(Bookmark bookmark) {
        executorService.execute(() -> {
            pdfDao.deleteBookmark(bookmark.id);
        });
    }
}
