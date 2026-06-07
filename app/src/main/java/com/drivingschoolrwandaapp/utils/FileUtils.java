package com.drivingschoolrwandaapp.utils;

import com.drivingschoolrwandaapp.data.models.LearningMaterial;

public class FileUtils {

    public static String getSafeFileName(LearningMaterial material) {
        // Replace any character that is not a letter, number, dot, or hyphen with an underscore
        String sanitizedTitle = material.getTitle().replaceAll("[^a-zA-Z0-9._-]", "_");
        return sanitizedTitle + getFileExtension(material.getFileType());
    }

    public static String getFileExtension(String mimeType) {
        if (mimeType == null) return "";
        switch (mimeType) {
            case "video/mp4": return ".mp4";
            case "application/vnd.ms-powerpoint": return ".ppt";
            case "application/msword": return ".doc";
            case "image/png": return ".png";
            case "image/jpeg": return ".jpeg";
            case "application/pdf": return ".pdf";
            default: return "";
        }
    }
}
