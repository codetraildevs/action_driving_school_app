package com.drivingschoolrwandaapp.data.local.preferences;



import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenManager {
    private static final String TAG = "TokenManager";
    private static final String PREFS_NAME = "encrypted_tokens";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_ROLE_ID = "role_id";
    private static final String KEY_USER_ID = "user_id";

    private volatile SharedPreferences encryptedPreferences;
    private final Context appContext;
    private volatile boolean usingFallback = false;

    public TokenManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.encryptedPreferences = createEncryptedPreferences(this.appContext);
    }

    /**
     * Package-private constructor for unit testing. Accepts a pre-built
     * SharedPreferences so tests can inject mocks without triggering
     * EncryptedSharedPreferences or FirebaseCrashlytics.
     */
    TokenManager(Context context, SharedPreferences preferences) {
        this.appContext = context;
        this.encryptedPreferences = preferences;
    }

    private SharedPreferences createEncryptedPreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error creating encrypted preferences: " + e.getMessage());
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            // Fallback to regular SharedPreferences (less secure)
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    /**
     * Safe accessor that catches StackOverflowError from corrupted EncryptedSharedPreferences
     * (known Android Keystore bug) and transparently switches to a plain fallback.
     */
    private SharedPreferences safePrefs() {
        return encryptedPreferences;
    }

    /**
     * When EncryptedSharedPreferences is corrupted (StackOverflow on read), migrate to
     * plain SharedPreferences so the app keeps working instead of crashing.
     */
    private void switchToFallback() {
        if (usingFallback) return;
        usingFallback = true;
        Log.w(TAG, "EncryptedSharedPreferences corrupted — switching to plain fallback");
        encryptedPreferences = appContext.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE);
        // Token data from the encrypted store is unrecoverable; user will need to log in again.
        // Clear any stale tokens in the fallback to avoid using wrong credentials.
        encryptedPreferences.edit().clear().apply();
    }

    /**
     * Save tokens using the previously stored Remember Me preference.
     * Used by TokenAuthenticator during automatic token refresh.
     */
    public void saveTokens(String accessToken, String refreshToken) {
        boolean rememberMe = encryptedPreferences.getBoolean(KEY_REMEMBER_ME, true);
        saveTokens(accessToken, refreshToken, rememberMe);
    }

    /**
     * Save tokens with configurable expiry.
     *
     * @param rememberMe if true, tokens expire after 30 days;
     *                   if false, tokens expire after 1 hour (session-only).
     */
    public void saveTokens(String accessToken, String refreshToken, boolean rememberMe) {
        try {
            SharedPreferences.Editor editor = encryptedPreferences.edit();
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
            long expiry = rememberMe
                ? System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)  // 30 days
                : System.currentTimeMillis() + (60L * 60 * 1000);            // 1 hour
            editor.putLong(KEY_TOKEN_EXPIRY, expiry);
            editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
            editor.apply();
            Log.d(TAG, "Tokens saved successfully (rememberMe=" + rememberMe + ")");
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow saving tokens — switching to fallback", e);
            switchToFallback();
        }
    }

    public String getAccessToken() {
        try {
            return encryptedPreferences.getString(KEY_ACCESS_TOKEN, null);
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading access token — keystore corrupted", e);
            switchToFallback();
            return null;
        }
    }

    public String getRefreshToken() {
        try {
            return encryptedPreferences.getString(KEY_REFRESH_TOKEN, null);
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading refresh token — keystore corrupted", e);
            switchToFallback();
            return null;
        }
    }

    public boolean isTokenExpired() {
        try {
            long expiryTime = encryptedPreferences.getLong(KEY_TOKEN_EXPIRY, 0);
            return System.currentTimeMillis() > expiryTime;
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading token expiry — keystore corrupted", e);
            switchToFallback();
            return true; // treat as expired so user re-authenticates
        }
    }

    public void clearTokens() {
        try {
            SharedPreferences.Editor editor = encryptedPreferences.edit();
            editor.remove(KEY_ACCESS_TOKEN);
            editor.remove(KEY_REFRESH_TOKEN);
            editor.remove(KEY_TOKEN_EXPIRY);
            editor.remove(KEY_ROLE_ID);
            editor.remove(KEY_USER_ID);
            editor.apply();
            Log.d(TAG, "Tokens cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing tokens", e);
        }
    }

    /**
     * Persists the signed-in user's role id (see {@code RoleUtils}) so the app
     * can route admins to the admin console on later launches.
     */
    public void saveRole(int roleId) {
        try {
            encryptedPreferences.edit().putInt(KEY_ROLE_ID, roleId).apply();
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow saving role", e);
            switchToFallback();
        } catch (Exception e) {
            Log.e(TAG, "Error saving role", e);
        }
    }

    /** Returns the persisted role id, or 0 if not set (regular user). */
    public int getRoleId() {
        try {
            return encryptedPreferences.getInt(KEY_ROLE_ID, 0);
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading role id — keystore corrupted", e);
            switchToFallback();
            return 0;
        }
    }

    /** Persists the signed-in user's id so Room queries can filter by it. */
    public void saveUserId(int userId) {
        try {
            encryptedPreferences.edit().putInt(KEY_USER_ID, userId).apply();
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow saving user id", e);
            switchToFallback();
        } catch (Exception e) {
            Log.e(TAG, "Error saving user id", e);
        }
    }

    /** Returns the persisted user id, or 0 if not set. */
    public int getUserId() {
        try {
            return encryptedPreferences.getInt(KEY_USER_ID, 0);
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading user id — keystore corrupted", e);
            switchToFallback();
            return 0;
        }
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null && !getAccessToken().isEmpty() && !isTokenExpired();
    }

    /**
     * Returns the token expiry time in milliseconds since epoch, or 0 if not set.
     */
    public long getTokenExpiryTime() {
        try {
            return encryptedPreferences.getLong(KEY_TOKEN_EXPIRY, 0);
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading token expiry time — keystore corrupted", e);
            switchToFallback();
            return 0;
        }
    }

    /**
     * Returns whether the user chose "Remember Me" for the current session.
     */
    public boolean isRememberMe() {
        try {
            return encryptedPreferences.getBoolean(KEY_REMEMBER_ME, true);
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading remember me — keystore corrupted", e);
            switchToFallback();
            return true;
        }
    }
}