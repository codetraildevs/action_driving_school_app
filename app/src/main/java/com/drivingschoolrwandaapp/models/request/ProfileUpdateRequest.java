package com.drivingschoolrwandaapp.models.request;

import com.google.gson.annotations.SerializedName;

/**
 * Update Profile Request
 */
public class ProfileUpdateRequest {
    private String firstName;
    private String middleName;
    private String lastName;
    private String profilePicture;
    private Integer languageId;
    private Integer timezoneId;

    // Getters and setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public Integer getLanguageId() { return languageId; }
    public void setLanguageId(Integer languageId) { this.languageId = languageId; }

    public Integer getTimezoneId() { return timezoneId; }
    public void setTimezoneId(Integer timezoneId) { this.timezoneId = timezoneId; }
}