package com.drivingschoolrwandaapp.utils;

import android.util.Log;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * Utility for normalizing and validating phone numbers from any country.
 *
 * Normalization converts any valid phone number to E.164 international format
 * (e.g., +250782877442) so that registration and login always compare the same
 * canonical string, regardless of how the user entered the number.
 *
 * Default region is Rwanda (RW) for local numbers entered without a country code.
 * Numbers with an explicit + prefix are parsed without a default region.
 */
public final class PhoneUtils {

    private static final String TAG = "PhoneUtils";
    private static final String DEFAULT_REGION = "RW"; // Rwanda – the app's primary market

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    private PhoneUtils() {
        // utility class
    }

    /**
     * Normalise a phone number to E.164 international format (+XXXXXXXXX).
     * <p>
     * Accepts:
     * <ul>
     *   <li>Local numbers (e.g., 0782877442) — parsed with default region RW</li>
     *   <li>International numbers with + prefix (e.g., +250782877442)</li>
     *   <li>International numbers without + prefix but with country code (e.g., 250782877442)</li>
     *   <li>Numbers with spaces, dashes, or parentheses (e.g., +250 78 287 7442)</li>
     * </ul>
     *
     * @param raw the phone number as entered by the user (may be null or empty)
     * @return the normalized E.164 format, or an empty string if parsing fails.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }

        String cleaned = raw.trim();
        try {
            PhoneNumber number = parse(cleaned);
            if (number == null) {
                Log.w(TAG, "Could not parse phone number: " + raw);
                return cleaned;
            }
            return PHONE_UTIL.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (Exception e) {
            Log.w(TAG, "Could not normalize phone number: " + raw, e);
            return cleaned;
        }
    }

    /**
     * Validate whether a phone number is a valid number for its region.
     *
     * @param raw the phone number as entered by the user
     * @return true if the number is structurally valid for its detected or default region
     */
    public static boolean isValid(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        try {
            PhoneNumber number = parse(raw.trim());
            if (number == null) return false;
            return PHONE_UTIL.isValidNumber(number);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate a phone number and return a user-friendly error message if invalid.
     * Returns {@code null} when the number is valid.
     * <p>
     * This is the preferred method for form validation because it gives
     * clear feedback only for truly invalid numbers, never for formatting
     * differences (e.g., +250782877442 vs 0782877442).
     *
     * @param raw the phone number as entered by the user
     * @return {@code null} if valid, or a descriptive error message string if invalid
     */
    public static String getValidationError(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "phone_required";
        }

        String cleaned = raw.trim();

        // Quick structural check: must contain at least some digits
        String digitCount = cleaned.replaceAll("[^\\d]", "");
        if (digitCount.isEmpty()) {
            return "invalid_phone";
        }

        try {
            PhoneNumber number = parse(cleaned);
            if (number == null) {
                return "invalid_phone";
            }

            if (PHONE_UTIL.isValidNumber(number)) {
                return null; // valid
            }

            // The number parsed but isn't valid for any region.
            // This catches numbers with wrong country codes or impossible digits.
            return "invalid_phone";
        } catch (NumberParseException e) {
            // The number couldn't be parsed at all
            Log.w(TAG, "Phone number parse failed: " + raw, e);
            return "invalid_phone";
        }
    }

    /**
     * Try to parse the raw string into a PhoneNumber object.
     * <p>
     * Strategy:
     * <ol>
     *   <li>If it starts with {@code +}, parse without a default region.</li>
     *   <li>If it starts with {@code 00}, replace with {@code +} and parse without region.</li>
     *   <li>Otherwise, parse with the default region (RW).</li>
     * </ol>
     *
     * @param raw a trimmed phone number string
     * @return a PhoneNumber, or {@code null} if parsing produced an empty/anomalous result
     */
    private static PhoneNumber parse(String raw) throws NumberParseException {
        String input = raw;

        if (input.startsWith("+")) {
            return PHONE_UTIL.parse(input, null);
        }

        if (input.startsWith("00")) {
            return PHONE_UTIL.parse("+" + input.substring(2), null);
        }

        // Local format — use the default region (Rwanda)
        return PHONE_UTIL.parse(input, DEFAULT_REGION);
    }
}
