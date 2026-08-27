package com.drivingschoolrwandaapp.repository;

import android.app.Application;
import android.util.Log;

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
import java.util.concurrent.RejectedExecutionException;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionRepository {

    private final ApiService apiService;
    private final SubscriptionPlanDao subscriptionPlanDao;
    private final UserSubscriptionDao userSubscriptionDao;
    private final ExecutorService executorService;
    private final Application application;

    @Inject
    public SubscriptionRepository(Application application, ApiService apiService) {
        this.apiService = apiService;
        this.application = application;
        AppDatabase database = AppDatabase.getDatabase(application);
        this.subscriptionPlanDao = database.subscriptionPlanDao();
        this.userSubscriptionDao = database.userSubscriptionDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<Resource<List<SubscriptionPlan>>> getSubscriptionPlans() {
        return new NetworkBoundResource<List<SubscriptionPlan>, SubscriptionPlansResponse>(application) {
            @Override
            protected void saveCallResult(SubscriptionPlansResponse item) {
                if (item != null && item.getData() != null) {
                    executeSafely(() -> subscriptionPlanDao.insertAll(SubscriptionMapper.INSTANCE.toEntity(item.getData())));
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
        return new NetworkBoundResource<UserSubscriptionWithPlan, UserSubscriptionResponse>(application) {
            @Override
            protected void saveCallResult(UserSubscriptionResponse item) {
                if (item != null && item.getData() != null) {
                    executeSafely(() -> {
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

    private void executeSafely(Runnable runnable) {
        try {
            if (!executorService.isShutdown() && !executorService.isTerminated()) {
                executorService.execute(runnable);
            }
        } catch (RejectedExecutionException e) {
            Log.w("SubscriptionRepo", "Task rejected, executor is shutting down", e);
        }
    }

    /**
     * Cleanly shut down the internal executor to release the background thread.
     * Call from the ViewModel's onCleared() when this repository is no longer needed.
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
