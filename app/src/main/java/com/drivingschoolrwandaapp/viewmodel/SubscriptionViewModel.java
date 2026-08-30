package com.drivingschoolrwandaapp.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.database.entities.UserSubscriptionWithPlan;
import com.drivingschoolrwandaapp.models.mappers.SubscriptionMapper;
import com.drivingschoolrwandaapp.utils.ErrorUtils;
import com.drivingschoolrwandaapp.models.entities.SubscriptionPlan;
import com.drivingschoolrwandaapp.models.entities.UserSubscription;
import com.drivingschoolrwandaapp.models.response.ApiResponse;
import com.drivingschoolrwandaapp.models.response.UserSubscriptionResponse;
import com.drivingschoolrwandaapp.repository.Resource;
import com.drivingschoolrwandaapp.repository.SubscriptionRepository;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class SubscriptionViewModel extends AndroidViewModel {

    private final SubscriptionRepository repository;
    private final MutableLiveData<UserSubscription> userSubscription = new MutableLiveData<>();
    private final MutableLiveData<List<SubscriptionPlan>> subscriptionPlans = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<SubscriptionPlan> newSubscriptionSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> requestAccessSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> requestAccessMessage = new MutableLiveData<>();

    @Inject
    public SubscriptionViewModel(@NonNull Application application, SubscriptionRepository repository) {
        super(application);
        this.repository = repository;
    }

    @Override
    protected void onCleared() {
        repository.shutdown();
    }

    public LiveData<UserSubscription> getUserSubscription() {
        return userSubscription;
    }

    public LiveData<List<SubscriptionPlan>> getSubscriptionPlans() {
        return subscriptionPlans;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<SubscriptionPlan> getNewSubscriptionSuccess() {
        return newSubscriptionSuccess;
    }

    public LiveData<Boolean> getRequestAccessSuccess() {
        return requestAccessSuccess;
    }

    public LiveData<String> getRequestAccessMessage() {
        return requestAccessMessage;
    }

    public void doneShowingPaymentDialog() {
        newSubscriptionSuccess.setValue(null);
    }

    public void doneShowingRequestAccessDialog() {
        requestAccessSuccess.setValue(false);
        requestAccessMessage.setValue(null);
    }

    public void fetchUserSubscription() {
        repository.getUserSubscription().observeForever(userSubscriptionWithPlanResource -> {
            // Always update the UI with data from the database if it exists.
            if (userSubscriptionWithPlanResource.data != null) {
                userSubscription.setValue(SubscriptionMapper.INSTANCE.toModel(userSubscriptionWithPlanResource.data));
            }

            // Handle loading and error states
            isLoading.setValue(userSubscriptionWithPlanResource.status == Resource.Status.LOADING);
            if (userSubscriptionWithPlanResource.status == Resource.Status.ERROR) {
                error.setValue(userSubscriptionWithPlanResource.message);
            }
        });
    }

    public void fetchSubscriptionPlans() {
        repository.getSubscriptionPlans().observeForever(listResource -> {
            // Always update the UI with data from the database if it exists.
            if (listResource.data != null) {
                subscriptionPlans.setValue(SubscriptionMapper.INSTANCE.toModel(listResource.data));
            }

            // Handle loading and error states
            isLoading.setValue(listResource.status == Resource.Status.LOADING);
            if (listResource.status == Resource.Status.ERROR) {
                error.setValue(listResource.message);
            }
        });
    }

    public void subscribeToPlan(SubscriptionPlan plan) {
        isLoading.setValue(true);
        repository.subscribeToPlan(plan.getId(), new Callback<UserSubscriptionResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserSubscriptionResponse> call, @NonNull Response<UserSubscriptionResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    newSubscriptionSuccess.setValue(plan);
                } else {
                    handleApiError(response, getApplication().getString(R.string.subscribe_failed));
                }
                isLoading.setValue(false);
            }

            @Override
            public void onFailure(@NonNull Call<UserSubscriptionResponse> call, @NonNull Throwable t) {
                error.setValue(ErrorUtils.getUserFriendlyMessage(getApplication(), t));
                isLoading.setValue(false);
            }
        });
    }

    public void requestTestAccess(int testNumber, int days, int currentLanguageId) {
        isLoading.setValue(true);
        repository.requestTestAccess(testNumber, days,currentLanguageId, new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    requestAccessSuccess.setValue(true);
                    requestAccessMessage.setValue(response.body().getMessage());
                } else {
                    handleApiError(response, getApplication().getString(R.string.request_access_failed));
                }
                isLoading.setValue(false);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                error.setValue(ErrorUtils.getUserFriendlyMessage(getApplication(), t));
                isLoading.setValue(false);
            }
        });
    }

    public void cancelSubscription() {
        isLoading.setValue(true);
        repository.cancelSubscription(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    fetchUserSubscription(); // Refresh user subscription
                } else {
                    handleApiError(response, getApplication().getString(R.string.cancel_subscription_failed));
                }
                isLoading.setValue(false);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                error.setValue(ErrorUtils.getUserFriendlyMessage(getApplication(), t));
                isLoading.setValue(false);
            }
        });
    }

    private <T> void handleApiError(Response<T> response, String defaultErrorMessage) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                Gson gson = new Gson();
                ApiResponse<?> apiResponse = gson.fromJson(errorBody, ApiResponse.class);
                if (apiResponse != null) {
                    if (apiResponse.getError() != null) {
                         error.setValue(apiResponse.getError());
                    } else if (apiResponse.getMessage() != null) {
                         error.setValue(apiResponse.getMessage());
                    } else {
                        error.setValue(defaultErrorMessage);
                    }
                } else {
                    error.setValue(defaultErrorMessage);
                }
            } else {
                error.setValue(defaultErrorMessage);
            }
        } catch (Exception e) {
            Log.e("SubscriptionVM", "Failed to parse API error body", e);
            error.setValue(defaultErrorMessage);
        }
    }
}
