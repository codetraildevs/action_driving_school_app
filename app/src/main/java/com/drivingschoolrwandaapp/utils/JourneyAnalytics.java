package com.drivingschoolrwandaapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Funnel payloads use fixed categories only, never account data or server text. */
public final class JourneyAnalytics {
    public enum Flow { LOGIN, REGISTRATION }
    public enum Source { LOGIN_FORM, REGISTRATION_FORM, REGISTRATION_AUTO_LOGIN }
    public enum Outcome { ATTEMPT, SUCCESS, FAILURE }
    public enum RewardFormat { VIDEO, INTERSTITIAL }
    public enum RewardStage { SELECTED, UNAVAILABLE, SHOWN, EARNED, DISMISSED, SHOW_FAILED }
    public enum Failure { NONE, NETWORK, TIMEOUT, CANCELED, DEVICE_RESTRICTED,
        ACCOUNT_EXISTS, ACCOUNT_NOT_FOUND, INVALID_CREDENTIALS, RATE_LIMIT,
        SERVER, REJECTED, INVALID_INPUT, UNKNOWN }
    public static final String PREFS = "journey_analytics";
    private static final String FIRST_EXAM = "first_exam_observed";
    private JourneyAnalytics() { }

    public static void reward(Context context, RewardFormat format, RewardStage stage, boolean earned) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("ad_format", format.name().toLowerCase(Locale.ROOT));
        values.put("reward_earned", Boolean.toString(earned));
        send(context, "rewarded_ad_" + stage.name().toLowerCase(Locale.ROOT), values);
    }

    static Map<String, String> authParameters(Source source, Outcome outcome, Failure reason) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("source", source.name().toLowerCase(Locale.ROOT));
        values.put("failure_reason", (outcome == Outcome.FAILURE ? reason : Failure.NONE)
                .name().toLowerCase(Locale.ROOT));
        return values;
    }

    public static void auth(Context context, Flow flow, Source source, Outcome outcome, Failure reason) {
        send(context, flow.name().toLowerCase(Locale.ROOT) + "_"
                + outcome.name().toLowerCase(Locale.ROOT), authParameters(source, outcome, reason));
    }

    public static void validationFailure(Context context, Flow flow) {
        Source source = flow == Flow.LOGIN ? Source.LOGIN_FORM : Source.REGISTRATION_FORM;
        send(context, flow.name().toLowerCase(Locale.ROOT) + "_validation_failure",
                authParameters(source, Outcome.FAILURE, Failure.INVALID_INPUT));
    }

    /** Text is inspected locally and discarded; only the enum is sent. */
    public static Failure failure(int status, String message) {
        if (status == 429) return Failure.RATE_LIMIT;
        if (status >= 500) return Failure.SERVER;
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (text.contains("device")) return Failure.DEVICE_RESTRICTED;
        if (RegistrationErrors.isExistingAccount(text)) return Failure.ACCOUNT_EXISTS;
        if (text.contains("no account") || text.contains("not found") || text.contains("not registered"))
            return Failure.ACCOUNT_NOT_FOUND;
        if (text.contains("password") || text.contains("credential")) return Failure.INVALID_CREDENTIALS;
        if (status == 400 || status == 422) return Failure.INVALID_INPUT;
        return Failure.REJECTED;
    }

    public static Failure failure(Throwable error, boolean canceled) {
        if (canceled) return Failure.CANCELED;
        if (error instanceof InterruptedIOException) return Failure.TIMEOUT;
        if (error instanceof IOException) return Failure.NETWORK;
        return Failure.UNKNOWN;
    }

    public static synchronized void firstExam(Context context) {
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (prefs.getBoolean(FIRST_EXAM, false)) return;
            Map<String, String> values = new LinkedHashMap<>();
            values.put("scope", "installation_since_tracking");
            if (send(context, "first_exam_started", values)) {
                prefs.edit().putBoolean(FIRST_EXAM, true).apply();
            }
        } catch (RuntimeException ignored) { /* Analytics must never block the exam. */ }
    }

    static boolean send(Context context, String event, Map<String, String> values) {
        if (context == null) return false;
        try {
            Bundle params = new Bundle();
            for (Map.Entry<String, String> entry : values.entrySet()) params.putString(entry.getKey(), entry.getValue());
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context.getApplicationContext());
            analytics.setUserId(null); // Also clear a phone ID retained by older app versions.
            analytics.logEvent(event, params);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
