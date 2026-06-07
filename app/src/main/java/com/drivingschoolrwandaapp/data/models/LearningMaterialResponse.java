
package com.drivingschoolrwandaapp.data.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LearningMaterialResponse {

    @SerializedName("materials")
    private List<LearningMaterial> materials;

    @SerializedName("pagination")
    private Pagination pagination;

    public List<LearningMaterial> getMaterials() {
        return materials;
    }

    public Pagination getPagination() {
        return pagination;
    }
}
