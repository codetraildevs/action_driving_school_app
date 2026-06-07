package com.drivingschoolrwandaapp.models.request;

import com.google.gson.annotations.SerializedName;

public class UpdateSessionRequest {
    @SerializedName("currentPage")
    private int currentPage;

    public UpdateSessionRequest(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getCurrentPage() { return currentPage; }
}