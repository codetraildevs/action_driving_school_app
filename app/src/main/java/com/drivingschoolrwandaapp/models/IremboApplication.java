package com.drivingschoolrwandaapp.models;

import java.io.Serializable;

public class IremboApplication implements Serializable {
    private String title;
    private String reference;
    private String status;
    private String date;
   private String message;
   private int completionPercentage;
   private String currentStep;

    public IremboApplication(String title, String reference, String status, String date, String message, int completionPercentage, String currentStep) {
        this.title = title;
        this.reference = reference;
        this.status = status;
        this.date = date;
        this.message = message;
        this.currentStep=currentStep;
        this.completionPercentage=completionPercentage;

    }

    public String getTitle() {
        return title;
    }

    public String getReference() {
        return reference;
    }

    public String getStatus() {
        return status;
    }

    public String getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }
    public String getCurrentStep() {
        return currentStep;
    }
    public int getCompletionPercentage() {
        return completionPercentage;
    }
}
