package com.drivingschoolrwandaapp.repository;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.database.dao.UserDao;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.models.request.ForgotPasswordRequest;
import com.drivingschoolrwandaapp.models.request.LoginRequest;
import com.drivingschoolrwandaapp.utils.PhoneUtils;
import com.drivingschoolrwandaapp.models.request.PasswordChangeRequest;
import com.drivingschoolrwandaapp.models.request.ResetPasswordRequest;
import com.drivingschoolrwandaapp.models.request.VerifyOtpRequest;
import com.drivingschoolrwandaapp.models.response.ApiResponse;
import com.drivingschoolrwandaapp.models.response.LoginResponse;
import com.drivingschoolrwandaapp.ui.activities.LoginActivity;
import com.drivingschoolrwandaapp.utils.RoleUtils;
import com.google.gson.Gson;

import java.util.Locale;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    private final ApiService apiService;
    private final UserDao userDao;
    private final Context context;
    private final TokenManager tokenManager;
    private final ExecutorService executorService;
    private final Gson gson = new Gson();

    public UserRepository(Context context, ApiService apiService, UserDao userDao, TokenManager tokenManager) {
        this.context = context.getApplicationContext();
        this.apiService = apiService;
        this.userDao = userDao;
        this.tokenManager = tokenManager;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    private String parseErrorMessage(Response<?> response) {
        String errorMessage = context.getString(R.string.something_went_wrong);
        if (response.errorBody() != null) {
            try {
                String errorBodyStr = response.errorBody().string();
                if (!TextUtils.isEmpty(errorBodyStr)) {
                    ApiResponse<?> errorResponse = gson.fromJson(errorBodyStr, ApiResponse.class);
                    if (errorResponse != null) {
                        if (errorResponse.getMessage() != null) {
                            errorMessage = errorResponse.getMessage();
                        } else if (errorResponse.getError() != null) {
                            errorMessage = errorResponse.getError();
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("UserRepository", "Failed to parse error response", e);
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
        // If the error message is about a device, show the full support message with phone numbers
        if (errorMessage != null && errorMessage.toLowerCase(Locale.ROOT).contains("device")) {
            Log.w("UserRepository", "Device-related error detected: " + errorMessage);
            errorMessage = context.getString(R.string.device_not_allowed);
        }
        return errorMessage;
    }

    private String getNetworkErrorMessage(Throwable t) {
        if (t == null) return context.getString(R.string.something_went_wrong);
        String message = t.getMessage();
        if (message != null) {
            String lowerMsg = message.toLowerCase(Locale.ROOT);
            if (lowerMsg.contains("unable to resolve host") || lowerMsg.contains("failed to connect") || lowerMsg.contains("network is unreachable")) {
                return context.getString(R.string.network_error);
            }
            if (lowerMsg.contains("timeout") || lowerMsg.contains("timed out")) {
                return context.getString(R.string.request_timeout);
            }
        }
        return context.getString(R.string.something_went_wrong);
    }

    public LiveData<Resource<LoginResponse>> login(String email, String password, String deviceId) {
        // Normalise the phone number to ensure consistent format with registration
        String normalizedPhone = PhoneUtils.normalize(email);
        Log.d("UserRepository", "login: normalised phone " + email + " → " + normalizedPhone);
        MutableLiveData<Resource<LoginResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        LoginRequest request = new LoginRequest(normalizedPhone, password, deviceId, LoginRequest.CLIENT_TYPE_ANDROID_APP);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(Resource.success(response.body()));
                } else {
                    result.postValue(Resource.error(parseErrorMessage(response), null));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                result.postValue(Resource.error(getNetworkErrorMessage(t), null));
            }
        });
        return result;
    }

    public LiveData<Resource<ApiResponse<Void>>> changePassword(String currentPassword, String newPassword, String confirmPassword) {
        MutableLiveData<Resource<ApiResponse<Void>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        PasswordChangeRequest request = new PasswordChangeRequest(currentPassword, newPassword, confirmPassword);
        apiService.changePassword(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(Resource.success(response.body()));
                } else {
                    result.postValue(Resource.error(parseErrorMessage(response), null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                result.postValue(Resource.error(getNetworkErrorMessage(t), null));
            }
        });
        return result;
    }

    public LiveData<Resource<ApiResponse<Void>>> forgotPassword(String email) {
        MutableLiveData<Resource<ApiResponse<Void>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        apiService.forgotPassword(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(Resource.success(response.body()));
                } else {
                    result.postValue(Resource.error(parseErrorMessage(response), null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                result.postValue(Resource.error(getNetworkErrorMessage(t), null));
            }
        });
        return result;
    }

    public LiveData<Resource<ApiResponse<String>>> verifyOtp(String email, String otp) {
        MutableLiveData<Resource<ApiResponse<String>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        VerifyOtpRequest request = new VerifyOtpRequest(email, otp);
        apiService.verifyOtp(request).enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(Resource.success(response.body()));
                } else {
                    result.postValue(Resource.error(parseErrorMessage(response), null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                result.postValue(Resource.error(getNetworkErrorMessage(t), null));
            }
        });
        return result;
    }

    public LiveData<Resource<ApiResponse<Void>>> resetPassword(String token, String newPassword, String confirmPassword) {
        MutableLiveData<Resource<ApiResponse<Void>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        ResetPasswordRequest request = new ResetPasswordRequest(token, newPassword, confirmPassword);
        apiService.resetPassword(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(Resource.success(response.body()));
                } else {
                    result.postValue(Resource.error(parseErrorMessage(response), null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                result.postValue(Resource.error(getNetworkErrorMessage(t), null));
            }
        });
        return result;
    }

    public void logout() {
        apiService.logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                performLocalLogout();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                performLocalLogout();
            }
        });
    }


    public LiveData<com.drivingschoolrwandaapp.database.entities.User> loadFromDb() {
        return userDao.getUser();
    }

    public void updateUser(User user) {
        executeSafely(() -> userDao.insert(user));
    }

    public LiveData<Resource<ApiResponse<Void>>> sleepSubscription(int languageId) {
        MutableLiveData<Resource<ApiResponse<Void>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        apiService.sleepSubscription(languageId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(Resource.success(response.body()));
                } else {
                    result.postValue(Resource.error(parseErrorMessage(response), null));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                result.postValue(Resource.error(getNetworkErrorMessage(t), null));
            }
        });
        return result;
    }

    public void deleteAccount() {
        apiService.deleteAccount().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                performLocalLogout();
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                performLocalLogout();
            }
        });
    }

    private void performLocalLogout() {
        executeSafely(() -> {
            userDao.deleteAll();
            tokenManager.clearTokens();
            navigateToLogin();
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public LiveData<Resource<com.drivingschoolrwandaapp.database.entities.User>> getProfile() {
        return new NetworkBoundResource<com.drivingschoolrwandaapp.database.entities.User, ApiResponse<com.drivingschoolrwandaapp.models.entities.User>>() {
            @Override
            protected void saveCallResult(@NonNull ApiResponse<com.drivingschoolrwandaapp.models.entities.User> item) {
                if (item.getData() != null) {
                    com.drivingschoolrwandaapp.database.entities.User dbUser = mapUser(item.getData());
                    userDao.insert(dbUser);
                }
            }

            @NonNull
            @Override
            protected LiveData<com.drivingschoolrwandaapp.database.entities.User> loadFromDb() {
                return userDao.getUser();
            }

            @NonNull
            @Override
            protected Call<ApiResponse<com.drivingschoolrwandaapp.models.entities.User>> createCall() {
                return apiService.getProfile();
            }
        }.getAsLiveData();
    }

    com.drivingschoolrwandaapp.database.entities.User mapUser(com.drivingschoolrwandaapp.models.entities.User networkUser) {
        com.drivingschoolrwandaapp.database.entities.User dbUser = new com.drivingschoolrwandaapp.database.entities.User();
        dbUser.setId(networkUser.getId());
        dbUser.setFirstName(networkUser.getFirstName());
        dbUser.setMiddleName(networkUser.getMiddleName());
        dbUser.setLastName(networkUser.getLastName());
        dbUser.setEmail(networkUser.getEmail());
        dbUser.setPhoneNumber(networkUser.getPhoneNumber());
        dbUser.setDob(networkUser.getDob());
        dbUser.setActive(networkUser.isActive());
        dbUser.setProfilePicture(networkUser.getProfilePicture());
        dbUser.setRoleId(networkUser.getRole());
        // Keep the persisted role in sync with the server: a role change made in the
        // web admin console takes effect on the next profile load, without a re-login.
        // Only known role ids (1-10) are synced so a malformed response that omits
        // the role (Gson leaves it at 0) never wipes a valid stored admin role.
        // NOTE: the Room user still records whatever the server sent, so a malformed
        // role=0 would hide the drawer admin entry while TokenManager keeps granting
        // access — a deliberate fail-safe, since real demotions always carry a valid
        // role id and sync correctly. Runs on the background executor; apply() is safe.
        if (networkUser.getRole() >= RoleUtils.ROLE_SUPER_ADMIN
                && networkUser.getRole() <= RoleUtils.ROLE_GUEST) {
            tokenManager.saveRole(networkUser.getRole());
        }
        dbUser.setLanguageId(networkUser.getLanguageId());
        dbUser.setTimezoneId(1); // Placeholder
        dbUser.setCreatedAt(networkUser.getCreatedAt());
        
        if (networkUser.getUserTestAccess() != null) {
            dbUser.setMaxTestAccess(networkUser.getUserTestAccess().getMaxTest());
            dbUser.setTestAccessExpiresAt(networkUser.getUserTestAccess().getExpiresAt());
            dbUser.setTestAccessStatus(networkUser.getUserTestAccess().getStatus());
        } else {
            dbUser.setMaxTestAccess(0);
            dbUser.setTestAccessStatus("INACTIVE");
        }
        
        return dbUser;
    }

    private void executeSafely(Runnable runnable) {
        try {
            if (!executorService.isShutdown() && !executorService.isTerminated()) {
                executorService.execute(runnable);
            }
        } catch (RejectedExecutionException e) {
            Log.w("UserRepository", "Task rejected, executor is shutting down", e);
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
