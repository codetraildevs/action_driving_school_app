package com.drivingschoolrwandaapp.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Shared formatting for the admin console "last updated" footers (Dashboard
 * tab, user detail dialog). Keeps the display consistent across screens.
 */
public final class AdminTimeUtils {

    private AdminTimeUtils() {
    }

    /**
     * Formats a refresh timestamp: time-only when it happened today, date +
     * time otherwise. Locale-aware via {@link FormatStyle}.
     */
    public static String formatLastUpdated(long millis) {
        ZonedDateTime loaded = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
        boolean today = loaded.toLocalDate().equals(LocalDate.now());
        if (today) {
            return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(loaded);
        }
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)
                .format(loaded);
    }
}
