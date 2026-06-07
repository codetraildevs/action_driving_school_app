package com.drivingschoolrwandaapp.models.entities;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class UserRole implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("roleName")
    private String roleName;

    @SerializedName("description")
    private String description;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}