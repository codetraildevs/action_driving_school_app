package com.drivingschoolrwandaapp.models.request;

public class PaymentRequest {
    private int subscriptionId;
    private String method;
    private String refId;

    public PaymentRequest(int subscriptionId, String method, String refId) {
        this.subscriptionId = subscriptionId;
        this.method = method;
        this.refId = refId;
    }

    public int getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(int subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }
}