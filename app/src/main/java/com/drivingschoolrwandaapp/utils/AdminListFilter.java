package com.drivingschoolrwandaapp.utils;

import android.text.TextUtils;

import com.drivingschoolrwandaapp.models.entities.AdminRequest;
import com.drivingschoolrwandaapp.models.entities.AdminUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * In-memory filtering for the admin Users and Requests lists. Pure static
 * helpers (no Android framework calls beyond TextUtils) so they are easy to
 * unit-test on the JVM.
 */
public final class AdminListFilter {

    private AdminListFilter() {
    }

    /**
     * Filters users by a case-insensitive substring match against full name,
     * email, phone number and role name. A blank query returns the list as-is.
     */
    public static List<AdminUser> filterUsers(List<AdminUser> users, String query) {
        if (users == null) return new ArrayList<>();
        String q = normalize(query);
        if (q.isEmpty()) return new ArrayList<>(users);

        List<AdminUser> result = new ArrayList<>();
        for (AdminUser user : users) {
            if (matchesUser(user, q)) {
                result.add(user);
            }
        }
        return result;
    }

    private static boolean matchesUser(AdminUser user, String q) {
        if (user == null) return false;
        return containsIgnoreCase(user.getFullName(), q)
                || containsIgnoreCase(user.getEmail(), q)
                || containsIgnoreCase(user.getPhoneNumber(), q)
                || containsIgnoreCase(user.getRole() != null ? user.getRole().getRoleName() : null, q);
    }

    /**
     * Filters requests by a case-insensitive substring match against title,
     * type, status and message. A blank query returns the list as-is.
     */
    public static List<AdminRequest> filterRequests(List<AdminRequest> requests, String query) {
        if (requests == null) return new ArrayList<>();
        String q = normalize(query);
        if (q.isEmpty()) return new ArrayList<>(requests);

        List<AdminRequest> result = new ArrayList<>();
        for (AdminRequest request : requests) {
            if (matchesRequest(request, q)) {
                result.add(request);
            }
        }
        return result;
    }

    private static boolean matchesRequest(AdminRequest request, String q) {
        if (request == null) return false;
        return containsIgnoreCase(request.getTitle(), q)
                || containsIgnoreCase(request.getType(), q)
                || containsIgnoreCase(request.getStatus(), q)
                || containsIgnoreCase(request.getMessage(), q);
    }

    private static String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsIgnoreCase(String value, String q) {
        return !TextUtils.isEmpty(value) && value.toLowerCase(Locale.ROOT).contains(q);
    }
}
