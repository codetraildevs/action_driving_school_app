package com.drivingschoolrwandaapp.repository;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.database.AppDatabase;
import com.drivingschoolrwandaapp.database.dao.SubscriptionPlanDao;
import com.drivingschoolrwandaapp.database.dao.UserSubscriptionDao;
import com.drivingschoolrwandaapp.database.entities.SubscriptionPlan;
import com.drivingschoolrwandaapp.database.entities.UserSubscriptionWithPlan;
import com.drivingschoolrwandaapp.models.mappers.SubscriptionMapper;
import com.drivingschoolrwandaapp.models.response.ApiResponse;
import com.drivingschoolrwandaapp.models.response.SubscriptionPlansResponse;
import com.drivingschoolrwandaapp.models.response.UserSubscriptionResponse;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionRepository {

    private final ApiService apiService;
    private final SubscriptionPlanDao subscriptionPlanDao;
    private final UserSubscriptionDao userSubscriptionDao;
    private final ExecutorService executorService;

    @Inject
    public SubscriptionRepository(Application application, ApiService apiService) {
        this.apiService = apiService;
        AppDatabase database = AppDatabase.getDatabase(application);
        this.subscriptionPlanDao = database.subscriptionPlanDao();
        this.userSubscriptionDao = database.userSubscriptionDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<Resource<List<SubscriptionPlan>>> getSubscriptionPlans() {
        return new NetworkBoundResource<List<SubscriptionPlan>, SubscriptionPlansResponse>() {
            @Override
            protected void saveCallResult(SubscriptionPlansResponse item) {
                if (item != null && item.getData() != null) {
                    executorService.execute(() -> subscriptionPlanDao.insertAll(SubscriptionMapper.INSTANCE.toEntity(item.getData())));
                }
            }

            @NonNull
            @Override
            protected LiveData<List<SubscriptionPlan>> loadFromDb() {
                return subscriptionPlanDao.getAll();
            }

            @NonNull
            @Override
            protected Call<SubscriptionPlansResponse> createCall() {
                return apiService.getSubscriptionPlans();
            }
        }.getAsLiveData();
    }

    public LiveData<Resource<UserSubscriptionWithPlan>> getUserSubscription() {
        return new NetworkBoundResource<UserSubscriptionWithPlan, UserSubscriptionResponse>() {
            @Override
            protected void saveCallResult(UserSubscriptionResponse item) {
                if (item != null && item.getData() != null) {
                    executorService.execute(() -> {
                        // Save the subscription plan first
                        if (item.getData().getSubscriptionPlan() != null) {
                            subscriptionPlanDao.insert(SubscriptionMapper.INSTANCE.toEntity(item.getData().getSubscriptionPlan()));
                        }
                        // Then save the user subscription
                        userSubscriptionDao.insert(SubscriptionMapper.INSTANCE.toEntity(item.getData()));
                    });
                }
            }

            @NonNull
            @Override
            protected LiveData<UserSubscriptionWithPlan> loadFromDb() {
                return userSubscriptionDao.getUserSubscription();
            }

            @NonNull
            @Override
            protected Call<UserSubscriptionResponse> createCall() {
                return apiService.getUserSubscription();
            }
        }.getAsLiveData();
    }

    public void subscribeToPlan(int planId, Callback<UserSubscriptionResponse> callback) {
        apiService.subscribeToPlan(planId).enqueue(callback);
    }

    public void requestTestAccess(int testNumber, int days,int currentLanguageId, Callback<ApiResponse<Void>> callback) {
        apiService.requestTestAccess(testNumber, days, currentLanguageId).enqueue(callback);
    }

    public void cancelSubscription(Callback<ApiResponse<Void>> callback) {
        apiService.cancelSubscription().enqueue(callback);
    }
}
