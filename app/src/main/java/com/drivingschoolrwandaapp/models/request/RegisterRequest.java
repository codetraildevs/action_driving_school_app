package com.drivingschoolrwandaapp.models.request;

public class RegisterRequest {
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String password;
    private String dob;
    private int languageId = 1;
    private int timezoneId = 1;
    private DeviceInfoRequest device;

    public RegisterRequest(String firstName, String email, String phoneNumber,
                           String password, String dob, DeviceInfoRequest device) {
        this.firstName = firstName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.dob = dob;
        this.device = device;
    }

    // Getters and setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public int getLanguageId() { return languageId; }
    public void setLanguageId(int languageId) { this.languageId = languageId; }

    public int getTimezoneId() { return timezoneId; }
    public void setTimezoneId(int timezoneId) { this.timezoneId = timezoneId; }

    public DeviceInfoRequest getDevice() { return device; }
    public void setDevice(DeviceInfoRequest device) { this.device = device; }
}