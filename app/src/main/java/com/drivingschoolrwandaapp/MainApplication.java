package com.drivingschoolrwandaapp;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

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
        // Clear old identity before lifecycle events can be recorded.
        com.drivingschoolrwandaapp.utils.AnalyticsUtils.clearPersonalIdentity(this);
        registerActivityLifecycleCallbacks(new com.drivingschoolrwandaapp.utils.JourneyLifecycle());
        LanguageUtils.loadAppLanguage(this);

        // Initialize Firebase Crashlytics (fatal crashes are captured automatically)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        FirebaseCrashlytics.getInstance().setUserId("");
        FirebaseCrashlytics.getInstance().setCustomKey("user_name", "");
        Log.d("MainApplication", "Firebase Crashlytics initialized");

    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }
}
