package com.drivingschoolrwandaapp.models.request;

public class LogoutRequest {
    private String deviceId;

    public LogoutRequest(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}