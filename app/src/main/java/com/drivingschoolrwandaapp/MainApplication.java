package com.drivingschoolrwandaapp;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import com.drivingschoolrwandaapp.utils.AdManager;
import com.drivingschoolrwandaapp.utils.LanguageUtils;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MainApplication extends Application implements Configuration.Provider {

    @Inject
    HiltWorkerFactory workerFactory;

    @Override
    public void onCreate() {
        super.onCreate();
        LanguageUtils.loadAppLanguage(this);

        // Initialize Firebase Crashlytics (fatal crashes are captured automatically)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        Log.d("MainApplication", "Firebase Crashlytics initialized");

        // Initialize AdMob SDK
        AdManager.initialize(this);
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }
}
