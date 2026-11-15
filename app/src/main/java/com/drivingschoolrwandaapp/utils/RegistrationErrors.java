package com.drivingschoolrwandaapp.utils;

import java.util.Locale;

public final class RegistrationErrors {
    private RegistrationErrors() { }

    public static boolean isExistingAccount(String message) {
        if (message == null) return false;
        String text = message.toLowerCase(Locale.ROOT);
        if (text.contains("device")) return false;
        return text.contains("already registered") || text.contains("already exists")
                || text.contains("already in use") || text.contains("already taken");
    }
}
