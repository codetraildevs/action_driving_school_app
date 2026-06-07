package com.drivingschoolrwandaapp.models.response;

public class AuthResponse {
    private boolean success;
    private String message;
    private LoginResponse data;

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LoginResponse getData() { return data; }
    public void setData(LoginResponse data) { this.data = data; }
}