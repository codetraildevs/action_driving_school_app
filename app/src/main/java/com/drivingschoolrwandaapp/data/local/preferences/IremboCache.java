package com.drivingschoolrwandaapp.data.local.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.drivingschoolrwandaapp.models.IremboApplication;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight offline cache for Irembo data.
 *
 * Stores the last successful track result (keyed by application number) and
 * the last fetched list of applications, so the track flow and the My
 * Applications screen still show useful data when the device is offline.
 */
public class IremboCache {

    private static final String PREFS_NAME = "irembo_cache";
    private static final String KEY_RECENT_APPLICATIONS = "recent_applications";
    private static final String KEY_TRACK_PREFIX = "track_";

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    public IremboCache(Context context) {
        // Guard against null (unit tests pass a mocked Application).
        this.preferences = context != null ? context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) : null;
    }

    private boolean isUsable() {
        return preferences != null;
    }

    public void cacheRecentApplications(List<IremboApplication> applications) {
        if (applications == null || !isUsable()) return;
        try {
            preferences.edit().putString(KEY_RECENT_APPLICATIONS, gson.toJson(applications)).apply();
        } catch (Exception ignored) {
        }
    }

    public List<IremboApplication> getCachedRecentApplications() {
        if (!isUsable()) return null;
        String json = preferences.getString(KEY_RECENT_APPLICATIONS, null);
        if (json == null) return null;
        try {
            Type type = new TypeToken<List<IremboApplication>>() {}.getType();
            List<IremboApplication> cached = gson.fromJson(json, type);
            return cached != null ? cached : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void cacheTrackResult(String applicationNumber, IremboApplication application) {
        if (applicationNumber == null || application == null || !isUsable()) return;
        try {
            preferences.edit().putString(KEY_TRACK_PREFIX + applicationNumber.trim(), gson.toJson(application)).apply();
        } catch (Exception ignored) {
        }
    }

    public IremboApplication getCachedTrackResult(String applicationNumber) {
        if (applicationNumber == null || !isUsable()) return null;
        String json = preferences.getString(KEY_TRACK_PREFIX + applicationNumber.trim(), null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, IremboApplication.class);
        } catch (Exception e) {
            return null;
        }
    }

    /** Recent applications cached for offline display (may be empty). */
    public List<IremboApplication> getCachedRecentApplicationsOrEmpty() {
        List<IremboApplication> cached = getCachedRecentApplications();
        return cached != null ? cached : new ArrayList<>();
    }
}
