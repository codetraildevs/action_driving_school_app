
package com.drivingschoolrwandaapp.services;

import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.drivingschoolrwandaapp.api.ApiClient;
import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.models.request.FirebaseTokenUpdateRequest;
import com.drivingschoolrwandaapp.models.response.ApiResponse;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom() + ", MessageId: " + remoteMessage.getMessageId());

        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, "Message Data payload: " + data);

        // Always prioritize the data payload for full control over the notification.
        String title = data.get("title");
        String body = data.get("body");
        String channelId = data.get("channel_id");
        String largeIconUrl = data.get("large_icon_url");

        // Fallback for older message formats, if necessary.
        if (title == null && remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
        }
        if (body == null && remoteMessage.getNotification() != null) {
            body = remoteMessage.getNotification().getBody();
        }

        // Schedule a background worker to handle the notification reliably.
        scheduleNotificationWorker(title, body, channelId, largeIconUrl);
    }

    private void scheduleNotificationWorker(String title, String body, String channelId, String iconUrl) {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(NotificationWorker.KEY_TITLE, title);
        dataBuilder.putString(NotificationWorker.KEY_BODY, body);
        if (channelId != null) {
            dataBuilder.putString(NotificationWorker.KEY_CHANNEL_ID, channelId);
        }
        if (iconUrl != null) {
            dataBuilder.putString(NotificationWorker.KEY_ICON_URL, iconUrl);
        }

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInputData(dataBuilder.build())
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        ApiService apiService = ApiClient.getInstance(this).getApiService();
        FirebaseTokenUpdateRequest request = new FirebaseTokenUpdateRequest(token, deviceId);

        Log.d(TAG, "Sending token and deviceId to backend...");
        apiService.updateFirebaseToken(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "SUCCESS: Firebase token updated on server.");
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "ERROR: Failed to update Firebase token on server. Code: " + response.code() + " Message: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "ERROR: Failed to parse error response from token update. Code: " + response.code(), e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Log.e(TAG, "FAILURE: Could not send token API request.", t);
            }
        });
    }
}
