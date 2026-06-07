package com.drivingschoolrwandaapp.api.interceptors;

import android.content.Context;
import androidx.annotation.NonNull;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    public AuthInterceptor(Context context, TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder();

        // Do not add the Authorization header to the refresh token request
        if (originalRequest.url().encodedPath().endsWith("/refresh")) {
            return chain.proceed(originalRequest);
        }

        String accessToken = tokenManager.getAccessToken();
        if (accessToken != null && !accessToken.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
        }

        Request request = requestBuilder.build();
        return chain.proceed(request);
    }
}
