package com.drivingschoolrwandaapp.utils;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.drivingschoolrwandaapp.api.ApiClient;
import com.google.android.gms.tasks.Tasks;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.StandardIntegrityManager;

import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Play Integrity API client (anti-fraud; required by AdMob's traffic-integrity
 * policy and useful for protecting the subscription/Irembo backend).
 *
 * <p>Flow: generate a random nonce → request an integrity token from Google →
 * POST the token + nonce to the backend ({@code POST /api/integrity/verify}),
 * which independently verifies the token with Google and answers
 * {@code { verified, requestId }}. All work happens on a background thread and
 * every failure is reported through the callback — this must never throw.
 *
 * <p>Prerequisites (see docs/admob-prep.md):
 * <ul>
 *   <li>Enable the Play Integrity API in Play Console and link the cloud project.</li>
 *   <li>Deploy the backend route {@code app/api/integrity/verify/route.ts} and set
 *       {@code GOOGLE_CLOUD_API_KEY} in the backend's .env.</li>
 *   <li>Call {@link #attest(Context, String, IntegrityCallback)} at sensitive
 *       moments (login, subscription request, Irembo submit) once the backend
 *       is live, and fail/flag the action when {@code verified == false}.</li>
 * </ul>
 */
public final class IntegrityHelper {

    private static final String TAG = "IntegrityHelper";
    private static final String VERIFY_URL = ApiClient.BASE_URL + "integrity/verify";
    private static final int NONCE_BYTES = 32;
    private static final long TOKEN_TIMEOUT_SECONDS = 30;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private IntegrityHelper() {
    }

    public interface IntegrityCallback {
        /**
         * @param verified  true only when the backend confirmed the token with Google
         *                  AND the nonce matches AND the verdicts are acceptable.
         * @param requestId Google's request id from the verdict (null when failed).
         * @param error     human-readable failure reason (null on success).
         */
        void onResult(boolean verified, @Nullable String requestId, @Nullable String error);
    }

    /**
     * Attest device/app integrity and verify the result with the backend.
     *
     * @param context   application or activity context.
     * @param authToken the user's Bearer token (TokenManager#getAccessToken()).
     * @param callback  invoked on a background thread.
     */
    public static void attest(@Nullable Context context, @Nullable String authToken,
                              @Nullable IntegrityCallback callback) {
        if (context == null) {
            notifyResult(callback, false, null, "missing context");
            return;
        }

        final byte[] nonceBytes = new byte[NONCE_BYTES];
        SECURE_RANDOM.nextBytes(nonceBytes);
        final String nonce = Base64.encodeToString(nonceBytes, Base64.NO_WRAP);

        EXECUTOR.execute(() -> {
            String token = null;
            try {
                StandardIntegrityManager manager =
                        IntegrityManagerFactory.createStandard(context);

                // Step 1: prepare a token provider (cached internally by the SDK)
                StandardIntegrityManager.StandardIntegrityTokenProvider provider =
                        Tasks.await(
                                manager.prepareIntegrityToken(
                                        StandardIntegrityManager
                                                .PrepareIntegrityTokenRequest.builder()
                                                .setCloudProjectNumber(651445180506L) // Firebase project number
                                                .build()),
                                TOKEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                // Step 2: request an integrity token with the nonce as request hash
                StandardIntegrityManager.StandardIntegrityTokenRequest tokenRequest =
                        StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                                .setRequestHash(nonce)
                                .build();
                token = Tasks.await(
                        provider.request(tokenRequest),
                        TOKEN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .token();
            } catch (Exception e) {
                Log.w(TAG, "Integrity token request failed", e);
                notifyResult(callback, false, null, "token request failed");
                return;
            }

            sendToBackend(token, nonce, authToken, callback);
        });
    }

    private static void sendToBackend(String integrityToken, String nonce,
                                      @Nullable String authToken,
                                      @Nullable IntegrityCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("integrityToken", integrityToken);
            body.put("nonce", nonce);

            Request.Builder rb = new Request.Builder()
                    .url(VERIFY_URL)
                    .post(RequestBody.create(body.toString(), JSON));
            if (authToken != null && !authToken.isEmpty()) {
                rb.header("Authorization", "Bearer " + authToken);
            }

            try (Response response = HTTP.newCall(rb.build()).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                JSONObject json = new JSONObject(responseBody);
                boolean verified = json.optBoolean("verified", false);
                String requestId = json.optString("requestId", null);
                String error = json.optString("error", null);
                if (!response.isSuccessful()) {
                    Log.w(TAG, "Backend verify HTTP " + response.code() + " " + responseBody);
                    notifyResult(callback, false, requestId, error != null ? error : "backend error " + response.code());
                    return;
                }
                notifyResult(callback, verified, requestId, verified ? null : error);
            }
        } catch (Exception e) {
            Log.w(TAG, "Backend verify error", e);
            notifyResult(callback, false, null, "backend verification failed");
        }
    }

    private static void notifyResult(@Nullable IntegrityCallback callback,
                                     boolean verified, @Nullable String requestId,
                                     @Nullable String error) {
        if (callback != null) {
            try {
                callback.onResult(verified, requestId, error);
            } catch (Throwable ignored) {
            }
        }
    }
}
