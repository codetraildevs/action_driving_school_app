package com.drivingschoolrwandaapp.data.models;

import com.google.gson.annotations.SerializedName;

public class LearningMaterial {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("filePath")
    private String filePath;

    @SerializedName("fileType")
    private String fileType;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName("isPublic")
    private boolean isPublic;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    private boolean isDownloaded = false;
    private long hoursUntilExpiration = -1;
    private long fileSize = -1; // in bytes

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public long getHoursUntilExpiration() {
        return hoursUntilExpiration;
    }

    public void setHoursUntilExpiration(long hoursUntilExpiration) {
        this.hoursUntilExpiration = hoursUntilExpiration;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
