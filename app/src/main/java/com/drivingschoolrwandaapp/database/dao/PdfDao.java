package com.drivingschoolrwandaapp.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.drivingschoolrwandaapp.database.entities.Bookmark;
import com.drivingschoolrwandaapp.database.entities.BookmarkEntity;

import java.util.List;

@Dao
public interface PdfDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBookmark(BookmarkEntity bookmark);

    @Query("SELECT id, pdfId, page as pageNumber, name FROM bookmarks WHERE pdfId = :pdfId ORDER BY page ASC")
    LiveData<List<Bookmark>> getBookmarksByPdfId(int pdfId);

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    void deleteBookmark(int bookmarkId);

}
