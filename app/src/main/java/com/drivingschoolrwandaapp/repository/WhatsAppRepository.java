package com.drivingschoolrwandaapp.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.models.entities.WhatsAppGroup;
import com.drivingschoolrwandaapp.utils.ErrorUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WhatsAppRepository {
    private final ApiService apiService;

    public WhatsAppRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<Resource<List<WhatsAppGroup>>> getWhatsAppGroups() {
        MutableLiveData<Resource<List<WhatsAppGroup>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        apiService.getWhatsAppGroups().enqueue(new Callback<List<WhatsAppGroup>>() {
            @Override
            public void onResponse(@NonNull Call<List<WhatsAppGroup>> call, @NonNull Response<List<WhatsAppGroup>> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        result.setValue(Resource.success(response.body()));
                    } else {
                        result.setValue(Resource.error("No groups found", null));
                    }
                } else {
                    result.setValue(Resource.error("Failed to fetch groups: " + response.message(), null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<WhatsAppGroup>> call, @NonNull Throwable t) {
                result.setValue(Resource.error(ErrorUtils.getUserFriendlyMessage(t), null));
            }
        });

        return result;
    }
}
