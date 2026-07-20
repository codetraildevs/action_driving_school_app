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
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Load the current locale from preferences. This is essential when the user
        // changes language from the Profile or Welcome page — the language preference
        // is saved but MainApplication.onCreate() only runs once per process, so we
        // must reload it here to apply the change before navigating to the next screen.
        LanguageUtils.loadAppLanguage(this);

        Intent intent;
        if (tokenManager.isLoggedIn()) {

            intent = new Intent(SplashActivity.this, App.class);
        } else {

            intent = new Intent(SplashActivity.this, WelcomeActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
