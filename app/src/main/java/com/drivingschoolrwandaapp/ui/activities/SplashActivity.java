package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import com.drivingschoolrwandaapp.utils.EdgeToEdgeUtils;
import androidx.appcompat.app.AppCompatActivity;

import com.drivingschoolrwandaapp.App;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.utils.LanguageUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    @Inject
    TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeUtils.enable(this);

        // Load the current locale from preferences. This is essential when the user
        // changes language from the Profile or Welcome page — the language preference
        // is saved but MainApplication.onCreate() only runs once per process, so we
        // must reload it here to apply the change before navigating to the next screen.
        LanguageUtils.loadAppLanguage(this);

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
            android.util.Log.e("SplashActivity", "Token read failed (keystore corrupted)", e);
            intent = new Intent(SplashActivity.this, WelcomeActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
