package com.drivingschoolrwandaapp.models.entities

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class WhatsAppGroup(
    @SerializedName("id") var id: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("whatsappLink") var whatsappLink: String? = null,
    @SerializedName("imageUrl") var imageUrl: String? = null,
    @SerializedName("isActive") var isActive: Boolean = false,
    @SerializedName("createdAt") var createdAt: String? = null,
    @SerializedName("updatedAt") var updatedAt: String? = null
) : Serializable
