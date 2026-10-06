package com.drivingschoolrwandaapp.api.interceptors;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.drivingschoolrwandaapp.api.ApiClient;
import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.models.request.RefreshTokenRequest;
import com.drivingschoolrwandaapp.models.response.LoginResponse;
import com.drivingschoolrwandaapp.ui.activities.LoginActivity;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

/**
 * TokenAuthenticator handles automatic token refresh on 401 responses.
 * <p>
 * It creates the ApiService lazily via {@link ApiClient#getInstance(Context)} to avoid
 * a circular dependency: OkHttpClient → TokenAuthenticator → ApiService → Retrofit → OkHttpClient.
 * By the time a 401 occurs, the ApiClient singleton is already fully constructed.
 */
public class TokenAuthenticator implements Authenticator {

    private final Context context;
    private final TokenManager tokenManager;

    public TokenAuthenticator(Context context, TokenManager tokenManager) {
        this.context = context;
        this.tokenManager = tokenManager;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) {
        synchronized (this) {
            String refreshToken;
            try {
                refreshToken = tokenManager.getRefreshToken();
            } catch (StackOverflowError e) {
                Log.e("TokenAuthenticator", "StackOverflow reading refresh token", e);
                logoutUser();
                return null;
            }

            if (refreshToken == null || refreshToken.isEmpty()) {
                logoutUser();
                return null;
            }

            // Lazily create ApiService to avoid circular dependency
            // ApiClient singleton is always fully constructed by the time a 401 occurs
            ApiService apiService = ApiClient.getInstance(context).getApiService();
            Call<LoginResponse> refreshTokenCall = apiService.refreshToken(new RefreshTokenRequest(refreshToken));

            try {
                retrofit2.Response<LoginResponse> refreshResponse = refreshTokenCall.execute();

                if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                    LoginResponse loginResponse = refreshResponse.body();
                    if (loginResponse.isSuccess() && loginResponse.getAccessToken() != null && loginResponse.getRefreshToken() != null) {
                        tokenManager.saveTokens(loginResponse.getAccessToken(), loginResponse.getRefreshToken());
                        return response.request().newBuilder()
                                .header("Authorization", "Bearer " + loginResponse.getAccessToken())
                                .build();
                    } else {
                        logoutUser();
                        return null;
                    }
                } else {
                    logoutUser();
                    return null;
                }
            } catch (IOException e) {
                Log.e("TokenAuthenticator", "Failed to refresh access token", e);
                return null; // Let the original request fail.
            }
        }
    }

    private void logoutUser() {
        tokenManager.clearTokens();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
