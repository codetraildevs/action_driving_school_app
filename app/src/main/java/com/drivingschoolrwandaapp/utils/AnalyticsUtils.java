package com.drivingschoolrwandaapp.utils;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Null-safe wrapper around {@link FirebaseAnalytics} (GA4).
 *
 * <p>This is the single entry point for all app analytics. Every method is
 * defensive: if the SDK is unavailable, the context is null, or anything
 * throws, the call is silently dropped so analytics can never crash the app.
 *
 * <p>Why it exists: Google AdMob requires a linked Google Analytics for
 * Firebase (GA4) property, and these events power AdMob's revenue and
 * audience features once ads are enabled. They are also useful on their own
 * for engagement/retention analysis in Firebase.
 */
public final class AnalyticsUtils {

    private AnalyticsUtils() {
        // utility class
    }

    private static final String EVENT_EXAM_STARTED = "exam_started";
    private static final String EVENT_EXAM_COMPLETED = "exam_completed";
    private static final String EVENT_PAYMENT_METHOD_SELECTED = "payment_method_selected";
    private static final String EVENT_PAYMENT_INSTRUCTIONS_VIEWED = "payment_instructions_viewed";
    private static final String EVENT_SUBSCRIPTION_REQUESTED = "subscription_requested";
    private static final String EVENT_IREMBO_REQUEST_SUBMITTED = "irembo_request_submitted";

    private static final String PARAM_TEST_ID = "test_id";
    private static final String PARAM_TEST_NUMBER = "test_number";
    private static final String PARAM_TEST_NAME = "test_name";
    private static final String PARAM_SCORE = "score";
    private static final String PARAM_TOTAL_MARKS = "total_marks";
    private static final String PARAM_PASSED = "passed";
    private static final String PARAM_CORRECT = "correct_count";
    private static final String PARAM_WRONG = "wrong_count";
    private static final String PARAM_SKIPPED = "skipped_count";
    private static final String PARAM_DURATION = "duration_sec";
    private static final String PARAM_METHOD = "method";
    private static final String PARAM_AMOUNT = "amount";
    private static final String PARAM_DAYS = "days";
    private static final String PARAM_PRICE = "price";
    private static final String PARAM_REQUEST_TYPE = "request_type";

    /**
     * Identify the user in analytics (and set their role) once the profile is
     * known. The phone number is the app's own account identifier, consistent
     * with what is already sent to Crashlytics.
     */
    public static void setUser(@Nullable Context context, @Nullable String userId, @Nullable String role) {
        FirebaseAnalytics fa = get(context);
        if (fa == null) return;
        try {
            if (userId != null && !userId.isEmpty()) {
                fa.setUserId(userId);
            }
            if (role != null && !role.isEmpty()) {
                fa.setUserProperty("user_role", role);
            }
        } catch (Throwable ignored) {
            // never crash the app because of analytics
        }
    }

    /** Manual screen view for fragments (activities are tracked automatically). */
    public static void logScreenView(@Nullable Context context, @Nullable String screenName) {
        Bundle b = new Bundle();
        b.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        b.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName);
        log(context, FirebaseAnalytics.Event.SCREEN_VIEW, b);
    }

    public static void logExamStarted(@Nullable Context context, int testId, int testNumber, @Nullable String testName) {
        Bundle b = new Bundle();
        b.putInt(PARAM_TEST_ID, testId);
        b.putInt(PARAM_TEST_NUMBER, testNumber);
        b.putString(PARAM_TEST_NAME, testName);
        log(context, EVENT_EXAM_STARTED, b);
    }

    public static void logExamCompleted(@Nullable Context context, int testId, int testNumber,
                                        @Nullable String testName, int score, int totalMarks,
                                        boolean passed, int correct, int wrong, int skipped,
                                        int elapsedSeconds) {
        Bundle b = new Bundle();
        b.putInt(PARAM_TEST_ID, testId);
        b.putInt(PARAM_TEST_NUMBER, testNumber);
        b.putString(PARAM_TEST_NAME, testName);
        b.putInt(PARAM_SCORE, score);
        b.putInt(PARAM_TOTAL_MARKS, totalMarks);
        b.putBoolean(PARAM_PASSED, passed);
        b.putInt(PARAM_CORRECT, correct);
        b.putInt(PARAM_WRONG, wrong);
        b.putInt(PARAM_SKIPPED, skipped);
        b.putInt(PARAM_DURATION, elapsedSeconds);
        log(context, EVENT_EXAM_COMPLETED, b);
    }

    /** Logged when the user taps a payment method card (before the USSD dial). */
    public static void logPaymentMethodSelected(@Nullable Context context, @Nullable String method, @Nullable String amount) {
        Bundle b = new Bundle();
        b.putString(PARAM_METHOD, method);
        b.putString(PARAM_AMOUNT, amount);
        log(context, EVENT_PAYMENT_METHOD_SELECTED, b);
    }

    /** Logged when the payment instructions dialog is shown. */
    public static void logPaymentInstructionsViewed(@Nullable Context context) {
        log(context, EVENT_PAYMENT_INSTRUCTIONS_VIEWED, null);
    }

    /** Logged when the user confirms a test-access request (the subscription intent). */
    public static void logSubscriptionRequested(@Nullable Context context, int testNumber, int days, @Nullable String price) {
        Bundle b = new Bundle();
        b.putInt(PARAM_TEST_NUMBER, testNumber);
        b.putInt(PARAM_DAYS, days);
        b.putString(PARAM_PRICE, price);
        log(context, EVENT_SUBSCRIPTION_REQUESTED, b);
    }

    /** Logged when an Irembo request is submitted (driving license / special service). */
    public static void logIremboRequestSubmitted(@Nullable Context context, @Nullable String requestType) {
        Bundle b = new Bundle();
        b.putString(PARAM_REQUEST_TYPE, requestType);
        log(context, EVENT_IREMBO_REQUEST_SUBMITTED, b);
    }

    @Nullable
    private static FirebaseAnalytics get(@Nullable Context context) {
        try {
            if (context == null) return null;
            return FirebaseAnalytics.getInstance(context.getApplicationContext());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void log(@Nullable Context context, String event, @Nullable Bundle params) {
        FirebaseAnalytics fa = get(context);
        if (fa == null) return;
        try {
            if (params == null) {
                fa.logEvent(event, null);
            } else {
                fa.logEvent(event, params);
            }
        } catch (Throwable ignored) {
            // never crash the app because of analytics
        }
    }
}
