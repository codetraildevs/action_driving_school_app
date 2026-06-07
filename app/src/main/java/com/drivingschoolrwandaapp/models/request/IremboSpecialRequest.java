package com.drivingschoolrwandaapp.models.request;

public class IremboSpecialRequest {
    private String serviceName;
    private String category;
    private String applicantName;
    private String applicantPhone;
    private String nationalId;
    private String description;

    public IremboSpecialRequest(String serviceName, String category, String applicantName, String applicantPhone, String nationalId, String description) {
        this.serviceName = serviceName;
        this.category = category;
        this.applicantName = applicantName;
        this.applicantPhone = applicantPhone;
        this.nationalId = nationalId;
        this.description = description;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantPhone() {
        return applicantPhone;
    }

    public void setApplicantPhone(String applicantPhone) {
        this.applicantPhone = applicantPhone;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
