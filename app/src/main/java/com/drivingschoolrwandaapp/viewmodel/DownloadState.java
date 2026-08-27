package com.drivingschoolrwandaapp.viewmodel;

import androidx.annotation.Nullable;

public class DownloadState {

    public enum Status {
        IDLE,
        DOWNLOADING,
        SUCCESS,
        FAILURE
    }

    private final Status status;
    @Nullable
    private final Integer materialId;
    @Nullable
    private final String message;

    public DownloadState(Status status, @Nullable Integer materialId) {
        this(status, materialId, null);
    }

    public DownloadState(Status status, @Nullable Integer materialId, @Nullable String message) {
        this.status = status;
        this.materialId = materialId;
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    @Nullable
    public Integer getMaterialId() {
        return materialId;
    }

    /**
     * Optional user-facing reason for a FAILURE state (server error message,
     * network error, etc.). Null for IDLE/DOWNLOADING/SUCCESS states.
     */
    @Nullable
    public String getMessage() {
        return message;
    }
}
