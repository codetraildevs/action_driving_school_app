package com.drivingschoolrwandaapp.models.request;

public class LoginRequest {
    private String identifier;

    private String password;
    private String deviceId;

    public LoginRequest(String identifier, String password, String deviceId) {
        this.identifier = identifier;
        this.password = password;
        this.deviceId = deviceId;
    }


    public String getIdentifier() { return identifier; }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}