package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.drivingschoolrwandaapp.utils.EdgeToEdgeUtils;
import androidx.appcompat.app.AppCompatActivity;

import com.drivingschoolrwandaapp.App;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.utils.ConsentHelper;
import com.drivingschoolrwandaapp.utils.LanguageUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final long CONSENT_TIMEOUT_MS = 5_000; // 5 seconds max for consent

    @Inject
    TokenManager tokenManager;

    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeUtils.enable(this);

        // Load the current locale from preferences. This is essential when the user
        // changes language from the Profile or Welcome page — the language preference
        // is saved but MainApplication.onCreate() only runs once per process, so we
        // must reload it here to apply the change before navigating to the next screen.
        LanguageUtils.loadAppLanguage(this);

        // Request GDPR consent (UMP) before showing any ads.
        // Ads are gated by ConsentHelper.canShowAds() in AdManager — they won't
        // load until consent is obtained (or the user is outside the EEA).
        ConsentHelper.requestConsent(this, canShowAds -> {
            Log.d(TAG, "Consent result: canShowAds=" + canShowAds);
            navigateToNextScreen();
        });

        // Safety timeout: if the consent form takes too long (or the device is
        // offline), navigate anyway so the user isn't stuck on a blank screen.
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen,
                CONSENT_TIMEOUT_MS);
    }

    /**
     * Navigate to the appropriate screen based on login state.
     * Safe to call multiple times — only the first call has any effect.
     */
    private void navigateToNextScreen() {
        if (navigated) return;
        navigated = true;

        Intent intent;
        try {
            if (tokenManager.isLoggedIn()) {
                // Route admins to the admin console, everyone else to the user app.
                if (com.drivingschoolrwandaapp.utils.RoleUtils.isAdminRole(tokenManager.getRoleId())) {
                    intent = new Intent(SplashActivity.this, AdminActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, App.class);
                }
            } else {
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            }
        } catch (StackOverflowError e) {
            // EncryptedSharedPreferences keystore corruption — tokenManager failed to
            // read. Treat as logged out so the user can re-authenticate.
            Log.e(TAG, "Token read failed (keystore corrupted)", e);
            intent = new Intent(SplashActivity.this, WelcomeActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
