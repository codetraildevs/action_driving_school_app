package com.drivingschoolrwandaapp.models.request;

public class IremboLicenseRequest {
    private String category;
    private String licenseType;
    private String applicationType;
    private String applicantName;
    private String applicantPhoneNumber;
    private String applicantNationalId;
    private String address;

    public IremboLicenseRequest(String category, String licenseType, String applicationType, String applicantName, String applicantPhoneNumber, String applicantNationalId, String address) {
        this.category = category;
        this.licenseType = licenseType;
        this.applicationType = applicationType;
        this.applicantName = applicantName;
        this.applicantPhoneNumber = applicantPhoneNumber;
        this.applicantNationalId = applicantNationalId;
        this.address = address;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantPhoneNumber() {
        return applicantPhoneNumber;
    }

    public void setApplicantPhoneNumber(String applicantPhoneNumber) {
        this.applicantPhoneNumber = applicantPhoneNumber;
    }

    public String getApplicantNationalId() {
        return applicantNationalId;
    }

    public void setApplicantNationalId(String applicantNationalId) {
        this.applicantNationalId = applicantNationalId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
