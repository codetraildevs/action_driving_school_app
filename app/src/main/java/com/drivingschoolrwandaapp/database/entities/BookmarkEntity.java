package com.drivingschoolrwandaapp.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bookmarks")
public class BookmarkEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int pdfId;
    public int page;
    public String name;

    public BookmarkEntity(int pdfId, int page, String name) {
        this.pdfId = pdfId;
        this.page = page;
        this.name = name;
    }

    // Required by Room
    public BookmarkEntity() {}
}
