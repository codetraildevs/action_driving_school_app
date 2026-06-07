package com.drivingschoolrwandaapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.models.IremboApplication;
import com.drivingschoolrwandaapp.models.request.IremboLicenseRequest;
import com.drivingschoolrwandaapp.models.request.IremboSpecialRequest;
import com.drivingschoolrwandaapp.models.response.ApiResponse;
import com.drivingschoolrwandaapp.models.response.IremboPaymentResponse;
import com.drivingschoolrwandaapp.repository.Resource;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class IremboViewModel extends AndroidViewModel {
    private final ApiService apiService;
    private final MutableLiveData<Resource<IremboPaymentResponse>> licenseRequestStatus = new MutableLiveData<>();
    private final MutableLiveData<Resource<IremboPaymentResponse>> specialRequestStatus = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<IremboApplication>>> recentApplications = new MutableLiveData<>();
    private final MutableLiveData<Resource<IremboApplication>> applicationDetails = new MutableLiveData<>();

    @Inject
    public IremboViewModel(@NonNull Application application, ApiService apiService) {
        super(application);
        this.apiService = apiService;
    }

    public LiveData<Resource<IremboPaymentResponse>> getLicenseRequestStatus() {
        return licenseRequestStatus;
    }

    public LiveData<Resource<IremboPaymentResponse>> getSpecialRequestStatus() {
        return specialRequestStatus;
    }

    public LiveData<Resource<List<IremboApplication>>> getRecentApplications() {
        return recentApplications;
    }

    public LiveData<Resource<IremboApplication>> getApplicationDetails() {
        return applicationDetails;
    }

    public void fetchRecentApplications() {
        recentApplications.setValue(Resource.loading(null));
        apiService.getRecentIremboApplications()
                .enqueue(new Callback<ApiResponse<List<IremboApplication>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<IremboApplication>>> call, Response<ApiResponse<List<IremboApplication>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<IremboApplication> allData = response.body().getData();
                            if (allData != null && allData.size() > 3) {
                                recentApplications.setValue(Resource.success(allData.subList(0, 2)));
                            } else {
                                recentApplications.setValue(Resource.success(allData));
                            }
                        } else {
                            recentApplications.setValue(Resource.error("Failed to fetch recent applications", null));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<IremboApplication>>> call, Throwable t) {
                        recentApplications.setValue(Resource.error(t.getMessage(), null));
                    }
                });
    }

    public void submitLicenseRequest(IremboLicenseRequest request) {
        licenseRequestStatus.setValue(Resource.loading(null));
        apiService.requestIremboLicense(request)
                .enqueue(new Callback<ApiResponse<IremboPaymentResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<IremboPaymentResponse>> call, Response<ApiResponse<IremboPaymentResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            licenseRequestStatus.setValue(Resource.success(response.body().getData()));
                        } else {
                            licenseRequestStatus.setValue(Resource.error("Failed to submit request", null));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<IremboPaymentResponse>> call, Throwable t) {
                        licenseRequestStatus.setValue(Resource.error(t.getMessage(), null));
                    }
                });
    }

    public void submitSpecialRequest(IremboSpecialRequest request) {
        specialRequestStatus.setValue(Resource.loading(null));
        apiService.requestSpecialIremboService(request)
                .enqueue(new Callback<ApiResponse<IremboPaymentResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<IremboPaymentResponse>> call, Response<ApiResponse<IremboPaymentResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            specialRequestStatus.setValue(Resource.success(response.body().getData()));
                        } else {
                            specialRequestStatus.setValue(Resource.error("Failed to submit request", null));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<IremboPaymentResponse>> call, Throwable t) {
                        specialRequestStatus.setValue(Resource.error(t.getMessage(), null));
                    }
                });
    }

    public void fetchApplicationDetails(String applicationNumber) {
        applicationDetails.setValue(Resource.loading(null));
        apiService.getIremboApplicationByNumber(applicationNumber)
                .enqueue(new Callback<ApiResponse<IremboApplication>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<IremboApplication>> call, Response<ApiResponse<IremboApplication>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            applicationDetails.setValue(Resource.success(response.body().getData()));
                        } else {
                            applicationDetails.setValue(Resource.error("Application not found or error occurred", null));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<IremboApplication>> call, Throwable t) {
                        applicationDetails.setValue(Resource.error(t.getMessage(), null));
                    }
                });
    }
}
