package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserRole(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("roleName") var roleName: String? = null,
    @SerializedName("description") var description: String? = null
) : Serializable
