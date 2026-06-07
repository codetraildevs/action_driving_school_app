package com.drivingschoolrwandaapp.models.request;

import com.google.gson.annotations.SerializedName;

public class VerifyPaymentRequest {
    @SerializedName("transactionId")
    private String transactionId;

    @SerializedName("refId")
    private String refId;

    public VerifyPaymentRequest(String transactionId, String refId) {
        this.transactionId = transactionId;
        this.refId = refId;
    }

    public String getTransactionId() { return transactionId; }
    public String getRefId() { return refId; }
}
