// NetworkInterceptor.java
package com.drivingschoolrwandaapp.api.interceptors;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class NetworkInterceptor implements Interceptor {
    private final Context context;

    public NetworkInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (!isNetworkAvailable()) {
            throw new NoNetworkException("No internet connection available");
        }

        Request request = chain.request();

        // Add cache control for GET requests
        if (request.method().equalsIgnoreCase("GET")) {
            request = request.newBuilder()
                    .header("Cache-Control", "public, max-age=60") // 1 minute cache
                    .build();
        }

        return chain.proceed(request);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    public static class NoNetworkException extends IOException {
        public NoNetworkException(String message) {
            super(message);
        }
    }
}