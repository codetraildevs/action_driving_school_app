package com.drivingschoolrwandaapp.utils;

import java.util.Locale;

/**
 * Utility class for converting network/Retrofit exceptions into user-friendly error messages.
 * Uses simple string matching to classify common errors (timeout, DNS, unreachable, etc.)
 * without requiring Android Context for localization.
 */
public final class ErrorUtils {

    private ErrorUtils() {
        // Utility class — no instantiation
    }

    /**
     * Converts a Throwable from a Retrofit onFailure callback into a
     * user-friendly error message string.
     *
     * @param t the Throwable caught in onFailure (may be null)
     * @return a human-readable error message
     */
    public static String getUserFriendlyMessage(Throwable t) {
        if (t == null) {
            return "An unknown error occurred. Please try again.";
        }
        String message = t.getMessage();
        if (message == null) {
            return "An unknown error occurred. Please try again.";
        }
        String lower = message.toLowerCase(Locale.ROOT);

        // Network connectivity issues
        if (lower.contains("unable to resolve host")
                || lower.contains("failed to connect")
                || lower.contains("network is unreachable")
                || lower.contains("no route to host")
                || lower.contains("connection refused")) {
            return "Connection failed. Please check your internet connection and try again.";
        }

        // Timeout issues
        if (lower.contains("timeout")
                || lower.contains("timed out")
                || lower.contains("time out")) {
            return "Request timed out. The server is not responding. Please try again later.";
        }

        // SSL / security issues
        if (lower.contains("ssl")
                || lower.contains("certificate")) {
            return "A secure connection could not be established. Please update your app and try again.";
        }

        // Socket / general IO errors
        if (lower.contains("socket")
                || lower.contains("eof")
                || lower.contains("end of file")
                || lower.contains("unexpected end of stream")) {
            return "Connection was interrupted. Please try again.";
        }

        // Fallback: return a generic message (the raw exception message is too technical for users)
        return "Something went wrong. Please try again.";
    }
}
