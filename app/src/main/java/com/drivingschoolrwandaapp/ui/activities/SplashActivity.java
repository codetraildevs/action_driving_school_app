package com.drivingschoolrwandaapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

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

        // Initialize language settings
        LanguageUtils.loadAppLanguage(this);

        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Channel creation is now handled by NotificationWorker to ensure it runs every time.

        Intent intent;
        if (tokenManager.getAccessToken() != null && !tokenManager.getAccessToken().isEmpty()) {

            intent = new Intent(SplashActivity.this, App.class);
        } else {

            intent = new Intent(SplashActivity.this, WelcomeActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
