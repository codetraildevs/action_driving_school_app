package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

public class Permission {

    @SerializedName("id")
    private int id;

    @SerializedName("permissionName")
    private String permissionName;

    public int getId() {
        return id;
    }

    public String getPermissionName() {
        return permissionName;
    }
}
