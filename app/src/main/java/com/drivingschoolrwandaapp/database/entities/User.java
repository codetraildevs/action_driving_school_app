package com.drivingschoolrwandaapp.database.entities;

import static android.provider.Settings.System.getString;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.drivingschoolrwandaapp.R;

import java.util.Date;


@Entity(tableName = "users")
public class User {

    @PrimaryKey
    private int id;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Date dob;
    private boolean isActive;
    private String profilePicture;
    private int roleId;
    private int languageId;
    private int timezoneId;
    private String createdAt;
    private int maxTestAccess; 
    private String testAccessExpiresAt;
    private String testAccessStatus; // Added testAccessStatus

    // Getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public int getLanguageId() {
        return languageId;
    }
    public String getLanguage() {
        switch (this.getLanguageId()) {
            case 41:
                return  "English";
            case 48:
                return   "Français";

            default:
                return   "Kinyarwanda";
        }



    }


    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public int getTimezoneId() {
        return timezoneId;
    }

    public void setTimezoneId(int timezoneId) {
        this.timezoneId = timezoneId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getMaxTestAccess() {
        return maxTestAccess;
    }

    public void setMaxTestAccess(int maxTestAccess) {
        this.maxTestAccess = maxTestAccess;
    }

    public String getTestAccessExpiresAt() {
        return testAccessExpiresAt;
    }

    public void setTestAccessExpiresAt(String testAccessExpiresAt) {
        this.testAccessExpiresAt = testAccessExpiresAt;
    }

    public String getTestAccessStatus() {
        return testAccessStatus;
    }

    public void setTestAccessStatus(String testAccessStatus) {
        this.testAccessStatus = testAccessStatus;
    }

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
