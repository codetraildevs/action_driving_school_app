package com.drivingschoolrwandaapp.data.local.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LANGUAGE_CODE = "language_code";
    private static final String DEFAULT_LANGUAGE = "rw"; // Changed default to Kinyarwanda
    private static final String KEY_IS_GRID_LAYOUT = "is_grid_layout";

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
}
