package com.drivingschoolrwandaapp.models.request;

public class FirebaseTokenUpdateRequest {
    private String firebaseToken;
    private String deviceId;

    public FirebaseTokenUpdateRequest(String firebaseToken, String deviceId) {
        this.firebaseToken = firebaseToken;
        this.deviceId = deviceId;
    }

    public String getFirebaseToken() {
        return firebaseToken;
    }

    public void setFirebaseToken(String firebaseToken) {
        this.firebaseToken = firebaseToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
