package com.drivingschoolrwandaapp.repository;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.api.AdminApiService;
import com.drivingschoolrwandaapp.models.entities.AdminDashboardResponse;
import com.drivingschoolrwandaapp.models.entities.AdminDashboardStats;
import com.drivingschoolrwandaapp.models.entities.AdminRequest;
import com.drivingschoolrwandaapp.models.entities.AdminUser;
import com.drivingschoolrwandaapp.models.entities.AdminUserDetail;
import com.drivingschoolrwandaapp.models.entities.AdminUserDetailResponse;
import com.drivingschoolrwandaapp.models.entities.AdminUsersResponse;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fetches admin console data (dashboard analytics, users, requests) from the
 * shared backend's {@code /api/admin/*} endpoints.
 */
public class AdminRepository {

    private static final String TAG = "AdminRepository";

    private final Context context;
    private final AdminApiService adminApiService;
    private final Gson gson = new Gson();

    @Inject
    public AdminRepository(@ApplicationContext Context context, AdminApiService adminApiService) {
        this.context = context.getApplicationContext();
        this.adminApiService = adminApiService;
    }

    private String parseErrorMessage(Response<?> response) {
        String errorMessage = context.getString(R.string.something_went_wrong);
        if (response != null && response.errorBody() != null) {
            try {
                String body = response.errorBody().string();
                if (!TextUtils.isEmpty(body)) {
                    com.drivingschoolrwandaapp.models.response.ApiResponse<?> parsed =
                            gson.fromJson(body, com.drivingschoolrwandaapp.models.response.ApiResponse.class);
                    if (parsed != null) {
                        if (parsed.getMessage() != null) {
                            errorMessage = parsed.getMessage();
                        } else if (parsed.getError() != null) {
                            errorMessage = parsed.getError();
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse error response", e);
            }
        }
        return errorMessage;
    }

    private String getNetworkErrorMessage(Throwable t) {
        if (t == null) return context.getString(R.string.something_went_wrong);
        String message = t.getMessage();
        if (message != null) {
            String lower = message.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("unable to resolve host") || lower.contains("failed to connect")
                    || lower.contains("network is unreachable") || lower.contains("timeout")) {
                return context.getString(R.string.network_error);
            }
        }
        return context.getString(R.string.something_went_wrong);
    }

    /** Dashboard analytics. */
    public LiveData<Resource<AdminDashboardStats>> fetchDashboardStats() {
        MutableLiveData<Resource<AdminDashboardStats>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        adminApiService.getDashboardStats().enqueue(new Callback<AdminDashboardResponse>() {
            @Override
            public void onResponse(@NonNull Call<AdminDashboardResponse> call, @NonNull Response<AdminDashboardResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    result.setValue(Resource.success(response.body().getData()));
                } else {
                    result.setValue(Resource.error(parseErrorMessage(response), null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AdminDashboardResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Dashboard fetch failed", t);
                result.setValue(Resource.error(getNetworkErrorMessage(t), null));
            }
        });
        return result;
    }

    /** All users. */
    public LiveData<Resource<List<AdminUser>>> fetchUsers() {
        MutableLiveData<Resource<List<AdminUser>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        adminApiService.getUsers().enqueue(new Callback<AdminUsersResponse>() {
            @Override
            public void onResponse(@NonNull Call<AdminUsersResponse> call, @NonNull Response<AdminUsersResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    result.setValue(Resource.success(response.body().getData()));
                } else {
                    result.setValue(Resource.error(parseErrorMessage(response), null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AdminUsersResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Users fetch failed", t);
                result.setValue(Resource.error(getNetworkErrorMessage(t), null));
            }
        });
        return result;
    }

    /** All Irembo requests (bare list). */
    public LiveData<Resource<List<AdminRequest>>> fetchRequests() {
        MutableLiveData<Resource<List<AdminRequest>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        adminApiService.getRequests().enqueue(new Callback<List<AdminRequest>>() {
            @Override
            public void onResponse(@NonNull Call<List<AdminRequest>> call, @NonNull Response<List<AdminRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(Resource.success(new ArrayList<>(response.body())));
                } else {
                    result.setValue(Resource.error(parseErrorMessage(response), null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AdminRequest>> call, @NonNull Throwable t) {
                Log.e(TAG, "Requests fetch failed", t);
                result.setValue(Resource.error(getNetworkErrorMessage(t), null));
            }
        });
        return result;
    }

    /** A single user with their current subscription. */
    public LiveData<Resource<AdminUserDetail>> fetchUserDetail(int userId) {
        MutableLiveData<Resource<AdminUserDetail>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        adminApiService.getUserDetail(userId).enqueue(new Callback<AdminUserDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<AdminUserDetailResponse> call, @NonNull Response<AdminUserDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    result.setValue(Resource.success(response.body().getData()));
                } else {
                    result.setValue(Resource.error(parseErrorMessage(response), null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AdminUserDetailResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "User detail fetch failed", t);
                result.setValue(Resource.error(getNetworkErrorMessage(t), null));
            }
        });
        return result;
    }

    /** A single user's Irembo requests (bare list). */
    public LiveData<Resource<List<AdminRequest>>> fetchUserRequests(int userId) {
        MutableLiveData<Resource<List<AdminRequest>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        adminApiService.getUserRequests(userId).enqueue(new Callback<List<AdminRequest>>() {
            @Override
            public void onResponse(@NonNull Call<List<AdminRequest>> call, @NonNull Response<List<AdminRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(Resource.success(new ArrayList<>(response.body())));
                } else {
                    result.setValue(Resource.error(parseErrorMessage(response), null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AdminRequest>> call, @NonNull Throwable t) {
                Log.e(TAG, "User requests fetch failed", t);
                result.setValue(Resource.error(getNetworkErrorMessage(t), null));
            }
        });
        return result;
    }
}
