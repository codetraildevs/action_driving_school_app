package com.drivingschoolrwandaapp.data.local.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LANGUAGE_CODE = "language_code";
    private static final String DEFAULT_LANGUAGE = "rw"; // Changed default to Kinyarwanda
    private static final String KEY_IS_GRID_LAYOUT = "is_grid_layout";
    private static final String KEY_DISCLAIMER_SHOWN = "disclaimer_shown";
    private static final String KEY_DEVICE_REGISTERED = "device_registered";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private final SharedPreferences preferences;

    public AppPreferences(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setLanguage(String languageCode) {
        preferences.edit().putString(KEY_LANGUAGE_CODE, languageCode).apply();
    }

    public String getLanguage() {
        return preferences.getString(KEY_LANGUAGE_CODE, DEFAULT_LANGUAGE);
    }

    public boolean isLanguageSet() {
        return preferences.contains(KEY_LANGUAGE_CODE);
    }

    public void setGridLayout(boolean isGridLayout) {
        preferences.edit().putBoolean(KEY_IS_GRID_LAYOUT, isGridLayout).apply();
    }

    public boolean isGridLayout() {
        return preferences.getBoolean(KEY_IS_GRID_LAYOUT, true);
    }

    public boolean isDisclaimerShown() {
        return preferences.getBoolean(KEY_DISCLAIMER_SHOWN, false);
    }

    public void setDisclaimerShown(boolean shown) {
        preferences.edit().putBoolean(KEY_DISCLAIMER_SHOWN, shown).apply();
    }

    /**
     * Returns whether this device has already registered an account.
     * Once set to true, this flag persists across logouts to enforce one account per device.
     */
    public boolean isDeviceRegistered() {
        return preferences.getBoolean(KEY_DEVICE_REGISTERED, false);
    }

    /**
     * Marks this device as having a registered account.
     * Should be called after a successful registration.
     */
    public void setDeviceRegistered(boolean registered) {
        preferences.edit().putBoolean(KEY_DEVICE_REGISTERED, registered).apply();
    }

    /**
     * Returns whether the user wants to stay logged in for 30 days.
     * Default is true (remember me).
     */
    public boolean isRememberMe() {
        return preferences.getBoolean(KEY_REMEMBER_ME, true);
    }

    /**
     * Sets whether the user wants to stay logged in for 30 days.
     */
    public void setRememberMe(boolean rememberMe) {
        preferences.edit().putBoolean(KEY_REMEMBER_ME, rememberMe).apply();
    }
}
