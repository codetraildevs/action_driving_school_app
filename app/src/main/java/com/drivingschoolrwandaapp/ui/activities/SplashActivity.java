package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.drivingschoolrwandaapp.App;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.utils.EdgeToEdgeUtils;
import com.drivingschoolrwandaapp.utils.LanguageUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Inject
    TokenManager tokenManager;

    private boolean navigated = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply the saved locale before creating the splash UI, avoiding a
        // second configuration recreation during startup.
        LanguageUtils.loadAppLanguage(this);
        super.onCreate(savedInstanceState);

        // Keep the splash window stable on older / OEM-specific devices.
        // EdgeToEdgeUtils.enable() already logs and swallows UnsupportedOperationException
        // on firmware that rejects the cutout / decor APIs used there.
        try {
            EdgeToEdgeUtils.enable(getWindow());
        } catch (Exception e) {
            Log.w(TAG, "Edge-to-edge setup skipped", e);
        }

        // App requests UMP consent on its lasting host activity before loading ads.
        // Splash has no ads: avoid duplicate requests and a timeout that could
        // finish the activity while its consent form is visible.
        navigateToNextScreen();
    }

    /**
     * Navigate to the appropriate screen based on login state.
     * Safe to call multiple times — only the first call has any effect.
     */
    private void navigateToNextScreen() {
        if (navigated) return;
        navigated = true;

        Intent intent;
        boolean isLoggedIn;
        int roleId;

        try {
            isLoggedIn = tokenManager.isLoggedIn();
            roleId = tokenManager.getRoleId();
        } catch (UnsupportedOperationException e) {
            // Preferences / keystore / crypto path is not usable on this device or in this
            // process state. Treat the user as logged out so they can re-authenticate.
            Log.e(TAG, "Token/preferences setup not supported on this device", e);
            isLoggedIn = false;
            roleId = 0;
        } catch (StackOverflowError e) {
            // Known EncryptedSharedPreferences keystore read-corruption behavior.
            // TokenManager swallows these internally and switches to its fallback store,
            // but we also guard here so splash never crashes even if that fallback misbehaves.
            Log.e(TAG, "Token read failed (keystore issue)", e);
            isLoggedIn = false;
            roleId = 0;
        }

        if (isLoggedIn && com.drivingschoolrwandaapp.utils.RoleUtils.isAdminRole(roleId)) {
            intent = new Intent(SplashActivity.this, AdminActivity.class);
        } else if (isLoggedIn) {
            intent = new Intent(SplashActivity.this, App.class);
        } else {
            intent = new Intent(SplashActivity.this, WelcomeActivity.class);
        }

        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start destination activity", e);
            // As a last resort, clear state and try the welcome screen again.
            intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        finish();
    }
}
