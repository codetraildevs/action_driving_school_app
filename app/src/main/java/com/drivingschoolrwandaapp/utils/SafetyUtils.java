package com.drivingschoolrwandaapp.utils;

import androidx.fragment.app.Fragment;

/**
 * Utility for logging Crashlytics breadcrumbs before fragment lifecycle-sensitive operations,
 * so the crash trail is visible in Crashlytics if an NPE occurs.
 */
public final class SafetyUtils {

    private SafetyUtils() {
        // Utility class — no instantiation
    }

    /**
     * Logs a breadcrumb to Crashlytics for traceability.
     */
    public static void logBreadcrumb(String tag, String message) {
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                .log("[" + tag + "] " + message);
    }

    /**
     * Safely runs {@code action} on the fragment's activity if it is attached.
     * Logs a breadcrumb if the activity is null so Crashlytics captures the trail.
     *
     * @param fragment    the fragment whose getActivity() is called
     * @param callerMethod a short label identifying the calling method (e.g. "onOptionsItemSelected")
     * @param action      the code to run when the activity is non-null
     */
    public static void runIfActivityAttached(Fragment fragment, String callerMethod, Runnable action) {
        if (fragment.getActivity() != null) {
            action.run();
        } else {
            logBreadcrumb("SafetyUtils",
                    callerMethod + ": getActivity() was null — fragment likely detached");
        }
    }
}
