package com.drivingschoolrwandaapp.models.entities;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Language implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("languageCode")
    private String languageCode;

    @SerializedName("languageName")
    private String languageName;

    @SerializedName("nativeName")
    private String nativeName;

    @SerializedName("isActive")
    private boolean isActive;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    public String getLanguageName() { return languageName; }
    public void setLanguageName(String languageName) { this.languageName = languageName; }
    public String getNativeName() { return nativeName; }
    public void setNativeName(String nativeName) { this.nativeName = nativeName; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}