package com.drivingschoolrwandaapp.repository;

import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.data.models.LearningMaterialResponse;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.ResponseBody;
import retrofit2.Call;

@Singleton
public class LearningMaterialRepository {

    private final ApiService apiService;

    @Inject
    public LearningMaterialRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public Call<LearningMaterialResponse> getLearningMaterials(int page, int limit) {
        return apiService.getLearningMaterials(page, limit);
    }

    public Call<ResponseBody> downloadLearningMaterial(int materialId) {
        return apiService.downloadLearningMaterial(materialId);
    }
}
