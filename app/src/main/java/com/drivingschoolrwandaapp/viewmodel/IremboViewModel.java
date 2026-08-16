package com.drivingschoolrwandaapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.data.local.preferences.IremboCache;
import com.drivingschoolrwandaapp.models.IremboApplication;
import com.drivingschoolrwandaapp.utils.ErrorUtils;
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
    private final IremboCache iremboCache;
    private final MutableLiveData<Resource<IremboPaymentResponse>> licenseRequestStatus = new MutableLiveData<>();
    private final MutableLiveData<Resource<IremboPaymentResponse>> specialRequestStatus = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<IremboApplication>>> recentApplications = new MutableLiveData<>();
    private final MutableLiveData<Resource<IremboApplication>> applicationDetails = new MutableLiveData<>();

    @Inject
    public IremboViewModel(@NonNull Application application, ApiService apiService) {
        super(application);
        this.apiService = apiService;
        this.iremboCache = new IremboCache(application);
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

    /**
     * Fetches the user's applications. Truncated to the two most recent for the
     * hub's "Recent Activity" section. Use {@link #fetchAllApplications()} for
     * the full My Applications list.
     */
    public void fetchRecentApplications() {
        fetchApplications(true);
    }

    /** Fetches the user's full applications list (used by My Applications). */
    public void fetchAllApplications() {
        fetchApplications(false);
    }

    private void fetchApplications(boolean recentOnly) {
        recentApplications.setValue(Resource.loading(null));
        apiService.getRecentIremboApplications()
                .enqueue(new Callback<ApiResponse<List<IremboApplication>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<IremboApplication>>> call, Response<ApiResponse<List<IremboApplication>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<IremboApplication> allData = response.body().getData();
                            iremboCache.cacheRecentApplications(allData);
                            if (allData != null && recentOnly && allData.size() > 3) {
                                recentApplications.setValue(Resource.success(allData.subList(0, 2)));
                            } else {
                                recentApplications.setValue(Resource.success(allData));
                            }
                        } else {
                            recentApplications.setValue(Resource.error(
                                    getApplication().getString(com.drivingschoolrwandaapp.R.string.irembo_fetch_failed), null));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<IremboApplication>>> call, Throwable t) {
                        // Offline fallback: serve the last successfully fetched list.
                        List<IremboApplication> cached = iremboCache.getCachedRecentApplications();
                        if (cached != null && !cached.isEmpty()) {
                            if (recentOnly && cached.size() > 3) {
                                recentApplications.setValue(Resource.success(new java.util.ArrayList<>(cached.subList(0, 2))));
                            } else {
                                recentApplications.setValue(Resource.success(cached));
                            }
                        } else {
                            recentApplications.setValue(Resource.error(ErrorUtils.getUserFriendlyMessage(t), null));
                        }
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
                            licenseRequestStatus.setValue(Resource.error(
                                    getApplication().getString(com.drivingschoolrwandaapp.R.string.irembo_submit_failed), null));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<IremboPaymentResponse>> call, Throwable t) {
                        licenseRequestStatus.setValue(Resource.error(ErrorUtils.getUserFriendlyMessage(t), null));
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
                            specialRequestStatus.setValue(Resource.error(
                                    getApplication().getString(com.drivingschoolrwandaapp.R.string.irembo_submit_failed), null));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<IremboPaymentResponse>> call, Throwable t) {
                        specialRequestStatus.setValue(Resource.error(ErrorUtils.getUserFriendlyMessage(t), null));
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
                            IremboApplication data = response.body().getData();
                            iremboCache.cacheTrackResult(applicationNumber, data);
                            applicationDetails.setValue(Resource.success(data));
                        } else if (response.code() == 404) {
                            // Server explicitly reports the application does not exist.
                            applicationDetails.setValue(Resource.error(
                                    getApplication().getString(com.drivingschoolrwandaapp.R.string.irembo_application_not_found), null));
                        } else {
                            applicationDetails.setValue(Resource.error(
                                    getApplication().getString(com.drivingschoolrwandaapp.R.string.irembo_fetch_failed), null));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<IremboApplication>> call, Throwable t) {
                        // Offline fallback: serve the last cached result for this number.
                        IremboApplication cached = iremboCache.getCachedTrackResult(applicationNumber);
                        if (cached != null) {
                            applicationDetails.setValue(Resource.success(cached));
                        } else {
                            applicationDetails.setValue(Resource.error(ErrorUtils.getUserFriendlyMessage(t), null));
                        }
                    }
                });
    }
}
