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

    public DownloadState(Status status, @Nullable Integer materialId) {
        this.status = status;
        this.materialId = materialId;
    }

    public Status getStatus() {
        return status;
    }

    @Nullable
    public Integer getMaterialId() {
        return materialId;
    }
}
