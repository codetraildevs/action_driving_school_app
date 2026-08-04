package com.drivingschoolrwandaapp.utils;

import java.util.Locale;

/**
 * Formatting helpers for test timing.
 */
public final class TimeFormatUtils {

    private TimeFormatUtils() {
    }

    /**
     * Formats an elapsed duration in seconds as {@code mm:ss}, or {@code h:mm:ss}
     * once it reaches an hour.
     */
    public static String formatElapsed(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes >= 60) {
            int hours = minutes / 60;
            minutes = minutes % 60;
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
