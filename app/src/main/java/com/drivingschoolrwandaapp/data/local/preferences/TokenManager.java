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

    private final SharedPreferences encryptedPreferences;

    public TokenManager(Context context) {
        this.encryptedPreferences = createEncryptedPreferences(context);
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
            // Fallback to regular SharedPreferences (less secure)
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    public void saveTokens(String accessToken, String refreshToken) {
        SharedPreferences.Editor editor = encryptedPreferences.edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + (24 * 60 * 60 * 1000)); // 24 hours
        editor.apply();
        Log.d(TAG, "Tokens saved successfully");
    }

    public String getAccessToken() {
        return encryptedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return encryptedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    public boolean isTokenExpired() {
        long expiryTime = encryptedPreferences.getLong(KEY_TOKEN_EXPIRY, 0);
        return System.currentTimeMillis() > expiryTime;
    }

    public void clearTokens() {
        SharedPreferences.Editor editor = encryptedPreferences.edit();
        editor.remove(KEY_ACCESS_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.remove(KEY_TOKEN_EXPIRY);
        editor.apply();
        Log.d(TAG, "Tokens cleared");
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null && !getAccessToken().isEmpty();
    }
}