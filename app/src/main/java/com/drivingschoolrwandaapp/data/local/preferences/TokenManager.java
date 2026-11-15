package com.drivingschoolrwandaapp.data.local.preferences;



import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import androidx.annotation.VisibleForTesting;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenManager {
    private static final String TAG = "TokenManager";
    private static final String PREFS_NAME = "encrypted_tokens";
    private static final String PREFS_FALLBACK_SUFFIX = "_fallback";
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
    @VisibleForTesting
    TokenManager(Context context, SharedPreferences preferences) {
        this.appContext = context;
        this.encryptedPreferences = preferences;
    }

    private SharedPreferences createEncryptedPreferences(Context context) {
        try {
            // Some devices / OEM firmware / Work Profile configurations can throw
            // UnsupportedOperationException from MasterKey or EncryptedSharedPreferences.
            // In those cases we fall back to plain SharedPreferences so the app still starts.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                Log.w(TAG, "Device too old for EncryptedSharedPreferences; using plain prefs");
                return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            }

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
        } catch (UnsupportedOperationException e) {
            Log.w(TAG, "EncryptedSharedPreferences not supported on this device", e);
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error creating encrypted preferences: " + e.getMessage());
            try {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            } catch (Exception crashlyticsError) {
                Log.w(TAG, "Could not record exception to Crashlytics", crashlyticsError);
            }
            // Fallback to regular SharedPreferences (less secure)
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    /**
     * When EncryptedSharedPreferences is corrupted (StackOverflow on read), migrate to
     * plain SharedPreferences so the app keeps working instead of crashing.
     */
    private synchronized void switchToFallback() {
        if (usingFallback) return;
        usingFallback = true;
        Log.w(TAG, "EncryptedSharedPreferences corrupted — switching to plain fallback");
        SharedPreferences fallback = appContext.getSharedPreferences(PREFS_NAME + PREFS_FALLBACK_SUFFIX, Context.MODE_PRIVATE);
        synchronized (this) {
            encryptedPreferences = fallback;
        }
        // Token data from the encrypted store is unrecoverable; user will need to log in again.
        // Clear any stale tokens in the fallback to avoid using wrong credentials.
        fallback.edit().clear().apply();
    }

    /**
     * Returns the active preferences instance.
     * If the primary EncryptedSharedPreferences becomes unreadable (for example due to a
     * corrupted keystore file), this switches once to a plain fallback store so the rest of
     * the app keeps working.
     */
    private SharedPreferences prefs() {
        if (encryptedPreferences == null) {
            synchronized (this) {
                if (encryptedPreferences == null) {
                    encryptedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                }
            }
        }

        try {
            // Touch the store once to trigger any keystore / decryption failure early.
            encryptedPreferences.getAll();
            return encryptedPreferences;
        } catch (UnsupportedOperationException e) {
            Log.w(TAG, "Preferences not supported on this device; switching to fallback", e);
            switchToFallback();
            return encryptedPreferences;
        } catch (StackOverflowError e) {
            Log.w(TAG, "Preferences read recursion detected; switching to fallback", e);
            switchToFallback();
            return encryptedPreferences;
        } catch (RuntimeException e) {
            Log.w(TAG, "Preferences read failed; switching to fallback", e);
            switchToFallback();
            return encryptedPreferences;
        }
    }

    /**
     * Save tokens using the previously stored Remember Me preference.
     * Used by TokenAuthenticator during automatic token refresh.
     */
    public void saveTokens(String accessToken, String refreshToken) {
        boolean rememberMe = prefs().getBoolean(KEY_REMEMBER_ME, true);
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
            SharedPreferences.Editor editor = prefs().edit();
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
        } catch (RuntimeException e) {
            Log.e(TAG, "Error saving tokens", e);
        }
    }

    public String getAccessToken() {
        try {
            return prefs().getString(KEY_ACCESS_TOKEN, null);
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading access token — keystore corrupted", e);
            switchToFallback();
            return null;
        } catch (RuntimeException e) {
            Log.e(TAG, "Error reading access token", e);
            return null;
        }
    }

    public String getRefreshToken() {
        try {
            return prefs().getString(KEY_REFRESH_TOKEN, null);
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading refresh token — keystore corrupted", e);
            switchToFallback();
            return null;
        } catch (RuntimeException e) {
            Log.e(TAG, "Error reading refresh token", e);
            return null;
        }
    }

    public boolean isTokenExpired() {
        try {
            long expiryTime = prefs().getLong(KEY_TOKEN_EXPIRY, 0);
            return System.currentTimeMillis() > expiryTime;
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading token expiry — keystore corrupted", e);
            switchToFallback();
            return true; // treat as expired so user re-authenticates
        } catch (RuntimeException e) {
            Log.e(TAG, "Error reading token expiry", e);
            return true;
        }
    }

    public void clearTokens() {
        try {
            SharedPreferences.Editor editor = prefs().edit();
            editor.remove(KEY_ACCESS_TOKEN);
            editor.remove(KEY_REFRESH_TOKEN);
            editor.remove(KEY_TOKEN_EXPIRY);
            editor.remove(KEY_ROLE_ID);
            editor.remove(KEY_USER_ID);
            editor.apply();
            Log.d(TAG, "Tokens cleared");
        } catch (RuntimeException e) {
            Log.e(TAG, "Error clearing tokens", e);
        }
    }

    /**
     * Persists the signed-in user's role id (see {@code RoleUtils}) so the app
     * can route admins to the admin console on later launches.
     */
    public void saveRole(int roleId) {
        try {
            prefs().edit().putInt(KEY_ROLE_ID, roleId).apply();
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow saving role", e);
            switchToFallback();
        } catch (RuntimeException e) {
            Log.e(TAG, "Error saving role", e);
        }
    }

    /** Returns the persisted role id, or 0 if not set (regular user). */
    public int getRoleId() {
        try {
            return prefs().getInt(KEY_ROLE_ID, 0);
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading role id — keystore corrupted", e);
            switchToFallback();
            return 0;
        } catch (RuntimeException e) {
            Log.e(TAG, "Error reading role id", e);
            return 0;
        }
    }

    /** Persists the signed-in user's id so Room queries can filter by it. */
    public void saveUserId(int userId) {
        try {
            prefs().edit().putInt(KEY_USER_ID, userId).apply();
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow saving user id", e);
            switchToFallback();
        } catch (RuntimeException e) {
            Log.e(TAG, "Error saving user id", e);
        }
    }

    /** Returns the persisted user id, or 0 if not set. */
    public int getUserId() {
        try {
            return prefs().getInt(KEY_USER_ID, 0);
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading user id — keystore corrupted", e);
            switchToFallback();
            return 0;
        } catch (RuntimeException e) {
            Log.e(TAG, "Error reading user id", e);
            return 0;
        }
    }

    public boolean isLoggedIn() {
        String accessToken = getAccessToken();
        return accessToken != null && !accessToken.isEmpty() && !isTokenExpired();
    }

    /**
     * Returns the token expiry time in milliseconds since epoch, or 0 if not set.
     */
    public long getTokenExpiryTime() {
        try {
            return prefs().getLong(KEY_TOKEN_EXPIRY, 0);
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading token expiry time — keystore corrupted", e);
            switchToFallback();
            return 0;
        } catch (RuntimeException e) {
            Log.e(TAG, "Error reading token expiry time", e);
            return 0;
        }
    }

    /**
     * Returns whether the user chose "Remember Me" for the current session.
     */
    public boolean isRememberMe() {
        try {
            return prefs().getBoolean(KEY_REMEMBER_ME, true);
        } catch (StackOverflowError e) {
            Log.e(TAG, "StackOverflow reading remember me — keystore corrupted", e);
            switchToFallback();
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Error reading remember me", e);
            return true;
        }
    }
}