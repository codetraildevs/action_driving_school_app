package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName

data class Permission(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("permissionName") var permissionName: String? = null
)
