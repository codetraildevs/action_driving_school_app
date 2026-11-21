package com.drivingschoolrwandaapp.api.interceptors;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Attaches the current Bearer token to outbound API requests.
 * <p>
 * This interceptor must never crash the OkHttp dispatcher thread. If the token store is
 * unavailable or corrupted, the request is sent without an auth header so the server can
 * respond with {@code 401} and the app can log the user out cleanly.
 */
public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor";
    private static final String REFRESH_PATH_SUFFIX = "/refresh";

    private final TokenManager tokenManager;

    public AuthInterceptor(Context context, TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        // Keep network execution outside token-store recovery: retrying here could
        // submit a write twice after the server processed the first request.
        Request.Builder request = originalRequest.newBuilder().removeHeader("Authorization");
        if (!AuthEndpoints.isPublicAuth(originalRequest.url().encodedPath())) {
            String accessToken = readAccessTokenSafely();
            if (accessToken != null && !accessToken.isEmpty()) {
                request.header("Authorization", "Bearer " + accessToken);
            }
        }
        return chain.proceed(request.build());
    }
    private boolean endsWithRefreshPath(Request request) {
        if (request == null) {
            return false;
        }
        String path = request.url().encodedPath();
        if (path == null) {
            return false;
        }
        int len = path.length();
        int suffixLen = REFRESH_PATH_SUFFIX.length();
        return len >= suffixLen && path.regionMatches(true, len - suffixLen, REFRESH_PATH_SUFFIX, 0, suffixLen);
    }

    @Nullable
    private String readAccessTokenSafely() {
        try {
            return tokenManager.getAccessToken();
        } catch (StackOverflowError e) {
            // Known EncryptedSharedPreferences keystore read-corruption behavior.
            // TokenManager should already have switched to its fallback store.
            android.util.Log.e(TAG, "StackOverflow reading token", e);
            return null;
        } catch (RuntimeException e) {
            android.util.Log.e(TAG, "Error reading token", e);
            return null;
        }
    }
}
