package com.drivingschoolrwandaapp.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

/**
 * Google User Messaging Platform (UMP) helper for GDPR/consent management.
 *
 * <p>EU users must consent to personalized ads before AdMob can serve them.
 * This class wraps the UMP SDK and provides a simple callback-based API.
 *
 * <p>Usage:
 * <ol>
 *   <li>Call {@link #requestConsent(Activity, ConsentCallback)} once at app startup
 *       (e.g. from {@code MainApplication} or the first visible activity).</li>
 *   <li>The callback fires with {@code canShowAds = true} once consent is obtained
 *       or the user is outside the EEA. Only then should ads be loaded.</li>
 * </ol>
 *
 * <p>For debug/testing, set {@link #setDebugMode(boolean, String)} before calling
 * {@link #requestConsent}. This forces the consent form to appear for testing.
 */
public final class ConsentHelper {

    private static final String TAG = "ConsentHelper";

    private static boolean debugMode = false;
    private static String debugGeographyHash = null; // test device hash for debug
    private static ConsentInformation consentInformation;
    private static boolean consentRequested = false;

    private ConsentHelper() { /* static utility */ }

    /**
     * Enable debug mode to force the consent form to appear (for testing).
     * Must be called before {@link #requestConsent}.
     *
     * @param enabled         true to enable debug geography
     * @param debugDeviceHash the hashed device ID from the UMP debug settings
     *                        (get it from Logcat after a test run, or use
     *                        {@code UserMessagingPlatform.getDebugSettings(context).getDebugGeographyHash()})
     */
    public static void setDebugMode(boolean enabled, @Nullable String debugDeviceHash) {
        debugMode = enabled;
        debugGeographyHash = debugDeviceHash;
    }

    /**
     * Request user consent. If the user has already consented or is outside the
     * EEA, the callback fires immediately with {@code canShowAds = true}.
     * Otherwise the UMP consent form is shown.
     *
     * @param activity the current activity (needed to show the consent form)
     * @param callback fires when consent status is determined
     */
    public static void requestConsent(@NonNull Activity activity,
                                      @NonNull ConsentCallback callback) {
        Context context = activity.getApplicationContext();

        // Build request parameters
        ConsentRequestParameters.Builder paramsBuilder =
                new ConsentRequestParameters.Builder()
                        .setTagForUnderAgeOfConsent(false); // app is for users 16+

        // Debug: force consent form for testing
        if (debugMode && debugGeographyHash != null) {
            ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(context)
                    .setDebugGeography(
                            ConsentDebugSettings.DebugGeography
                                    .DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId(debugGeographyHash)
                    .build();
            paramsBuilder.setConsentDebugSettings(debugSettings);
        }

        ConsentRequestParameters params = paramsBuilder.build();

        consentInformation = UserMessagingPlatform.getConsentInformation(context);

        // Check if we need to update consent status
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    // Info updated — show form if required
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            activity,
                            formError -> {
                                if (formError != null) {
                                    Log.w(TAG, "Consent form error: " + formError.getMessage());
                                }
                                // Consent status is now determined
                                boolean canShow = consentInformation.canRequestAds();
                                Log.d(TAG, "Consent result: canShowAds=" + canShow);
                                callback.onConsentResult(canShow);
                            }
                    );
                },
                requestConsentError -> {
                    // Failed to update consent info — assume consent granted
                    // (fail open so the app still works outside EEA)
                    Log.w(TAG, "Consent info update failed: " + requestConsentError.getMessage());
                    callback.onConsentResult(true);
                }
        );

        consentRequested = true;
    }

    /**
     * Returns whether ads can be shown based on the current consent status.
     * Safe to call before {@link #requestConsent} — returns {@code false}
     * until consent is determined.
     */
    public static boolean canShowAds() {
        if (consentInformation == null) return false;
        return consentInformation.canRequestAds();
    }

    /**
     * Returns true if the UMP consent form is available and the user can reset consent.
     */
    public static boolean isConsentFormAvailable() {
        if (consentInformation == null) return false;
        return consentInformation.isConsentFormAvailable();
    }

    /**
     * Callback for consent request results.
     */
    public interface ConsentCallback {
        /**
         * @param canShowAds true if the user consented (or is outside the EEA)
         *                   and personalized ads can be shown.
         */
        void onConsentResult(boolean canShowAds);
    }
}
