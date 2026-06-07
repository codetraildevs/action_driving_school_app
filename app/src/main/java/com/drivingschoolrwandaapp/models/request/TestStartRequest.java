package com.drivingschoolrwandaapp.models.request;

public class TestStartRequest {
    private int testId;

    public TestStartRequest(int testId) {
        this.testId = testId;
    }

    public int getTestId() { return testId; }
    public void setTestId(int testId) { this.testId = testId; }
}