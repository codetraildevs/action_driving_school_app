package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Date;

public class User implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("firstName")
    private String firstName;

    @SerializedName("middleName")
    private String middleName;

    @SerializedName("lastName")
    private String lastName;

    @SerializedName("email")
    private String email;

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("dob")
    private Date dob;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("profilePicture")
    private String profilePicture;

    @SerializedName("password")
    private String password;

    @SerializedName("timezone")
    private String timezone;

    @SerializedName("device")
    private Device device;

    @SerializedName("language")
    private String language;
    @SerializedName("languageId")
    private int languageId;
    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("role")
    private int role;

    @SerializedName("userTestAccess")
    private UserTestAccess userTestAccess; // Added UserTestAccess

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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

    public Date getDob() { return dob; }
    public void setDob(Date dob) { this.dob = dob; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getLanguage() { return language; }
    public int getLanguageId(){return  languageId;};
    public void setLanguageId(int languageId){this.languageId=languageId;};
    public void setLanguage(String language) { this.language = language; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getRole() { return role; }
    public void setRole(int role) { this.role = role; }

    public UserTestAccess getUserTestAccess() { return userTestAccess; }
    public void setUserTestAccess(UserTestAccess userTestAccess) { this.userTestAccess = userTestAccess; }

    public String getFullName() {
        StringBuilder name = new StringBuilder();
        if (firstName != null) name.append(firstName);
        if (middleName != null && !middleName.isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(middleName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(lastName);
        }
        return name.toString();
    }
}
