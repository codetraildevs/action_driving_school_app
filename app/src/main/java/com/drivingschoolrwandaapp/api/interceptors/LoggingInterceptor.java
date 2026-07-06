// LoggingInterceptor.java
package com.drivingschoolrwandaapp.api.interceptors;

import android.util.Log;
import okhttp3.*;
import okio.Buffer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class LoggingInterceptor implements Interceptor {
    private static final String TAG = "API_Logger";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        long startTime = System.nanoTime();

        // Log request
        logRequest(request);

        Response response;
        try {
            response = chain.proceed(request);
        } catch (IOException e) {
            Log.e(TAG, "Request failed: " + e.getMessage());
            throw e;
        }

        long endTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Log response
        logResponse(response, duration);

        return response;
    }

    private void logRequest(Request request) {
        try {
            String url = request.url().toString();
            String method = request.method();
            String headers = request.headers().toString();

            Log.d(TAG, "➡️ REQUEST: " + method + " " + url);
            Log.d(TAG, "Headers: " + headers);

            if (request.body() != null && isTextBasedContentType(request.header("Content-Type"))) {
                Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                String body = buffer.readString(StandardCharsets.UTF_8);
                Log.d(TAG, "Body: " + body);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging request: " + e.getMessage());
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void logResponse(Response response, long duration) {
        try {
            String url = response.request().url().toString();
            int statusCode = response.code();
            String statusMessage = response.message();

            Log.d(TAG, "⬅️ RESPONSE: " + statusCode + " " + statusMessage + " - " + url);
            Log.d(TAG, "Duration: " + duration + "ms");
            Log.d(TAG, "Headers: " + response.headers().toString());

            ResponseBody responseBody = response.body();
            if (responseBody != null && isTextBasedContentType(response.header("Content-Type"))) {
                String body = responseBody.string();
                Log.d(TAG, "Body: " + body);

                // Create new response body since original was consumed
                response = response.newBuilder()
                        .body(ResponseBody.create(responseBody.contentType(), body))
                        .build();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging response: " + e.getMessage());
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private boolean isTextBasedContentType(String contentType) {
        return contentType != null && (
                contentType.contains("application/json") ||
                        contentType.contains("application/xml") ||
                        contentType.contains("text/plain") ||
                        contentType.contains("text/html") ||
                        contentType.contains("application/x-www-form-urlencoded")
        );
    }
}