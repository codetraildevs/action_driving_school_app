package com.drivingschoolrwandaapp.repository;

import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.models.entities.WhatsAppGroup;

import java.util.List;

import retrofit2.Call;

/**
 * Repository for WhatsApp groups. Exposes the raw {@link Call} so the
 * ViewModel can own a single {@link androidx.lifecycle.LiveData}, cancel
 * in-flight requests on refetch, and bound how long loading may last.
 */
public class WhatsAppRepository {

    private final ApiService apiService;

    public WhatsAppRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public Call<List<WhatsAppGroup>> getWhatsAppGroups() {
        return apiService.getWhatsAppGroups();
    }
}
