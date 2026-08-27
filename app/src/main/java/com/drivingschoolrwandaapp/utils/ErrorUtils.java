package com.drivingschoolrwandaapp.utils;

import android.content.Context;

import com.drivingschoolrwandaapp.R;

import java.util.Locale;

/**
 * Utility class for converting network/Retrofit exceptions into user-friendly error messages.
 * Uses simple string matching to classify common errors (timeout, DNS, unreachable, etc.)
 * and resolves the message through {@code R.string} resources so every locale sees a
 * translated message.
 */
public final class ErrorUtils {

    private ErrorUtils() {
        // Utility class — no instantiation
    }

    /**
     * Converts a Throwable from a Retrofit onFailure callback into a
     * user-friendly, localized error message string.
     *
     * @param context the Context used to resolve the localized string resources
     * @param t       the Throwable caught in onFailure (may be null)
     * @return a human-readable error message in the device's locale
     */
    public static String getUserFriendlyMessage(Context context, Throwable t) {
        if (t == null) {
            return context.getString(R.string.error_unknown);
        }
        String message = t.getMessage();
        if (message == null) {
            return context.getString(R.string.error_unknown);
        }
        String lower = message.toLowerCase(Locale.ROOT);

        // Network connectivity issues
        if (lower.contains("unable to resolve host")
                || lower.contains("failed to connect")
                || lower.contains("network is unreachable")
                || lower.contains("no route to host")
                || lower.contains("connection refused")
                || lower.contains("no internet")) {
            return context.getString(R.string.network_error);
        }

        // Timeout issues
        if (lower.contains("timeout")
                || lower.contains("timed out")
                || lower.contains("time out")) {
            return context.getString(R.string.request_timeout);
        }

        // SSL / security issues
        if (lower.contains("ssl")
                || lower.contains("certificate")) {
            return context.getString(R.string.error_secure_connection);
        }

        // Socket / general IO errors
        if (lower.contains("socket")
                || lower.contains("eof")
                || lower.contains("end of file")
                || lower.contains("unexpected end of stream")) {
            return context.getString(R.string.error_connection_interrupted);
        }

        // Fallback: return a generic message (the raw exception message is too technical for users)
        return context.getString(R.string.something_went_wrong);
    }
}
