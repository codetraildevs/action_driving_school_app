package com.drivingschoolrwandaapp.models.request;

public class FirebaseTokenRequest {
    private String firebaseToken;

    public FirebaseTokenRequest(String firebaseToken) {
        this.firebaseToken = firebaseToken;
    }

    public String getFirebaseToken() {
        return firebaseToken;
    }

    public void setFirebaseToken(String firebaseToken) {
        this.firebaseToken = firebaseToken;
    }
}
