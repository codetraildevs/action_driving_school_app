package com.drivingschoolrwandaapp.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tracks first visible launch and app-owned screens, without view text or arguments. */
public final class JourneyLifecycle implements Application.ActivityLifecycleCallbacks {
    private boolean launched;

    private boolean owned(Activity activity) {
        return activity.getClass().getName().startsWith("com.drivingschoolrwandaapp.");
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {
        if (!owned(activity)) return;
        if (!launched) {
            launched = true;
            JourneyAnalytics.send(activity, "app_launch", Collections.singletonMap("scope", "process_first_activity"));
        }
        if (activity instanceof FragmentActivity) {
            ((FragmentActivity) activity).getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                    new FragmentManager.FragmentLifecycleCallbacks() {
                        @Override public void onFragmentResumed(@NonNull FragmentManager manager, @NonNull Fragment fragment) {
                            if (fragment.getParentFragment() instanceof NavHostFragment
                                    && fragment.getClass().getName().startsWith("com.drivingschoolrwandaapp.ui.fragments.")) {
                                screen(activity, fragment.getClass().getSimpleName(), "fragment");
                            }
                        }
                    }, true);
        }
    }

    private void screen(Activity activity, String name, String type) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("screen", name);
        values.put("screen_type", type);
        JourneyAnalytics.send(activity, "journey_screen", values);
    }

    @Override public void onActivityResumed(Activity activity) {
        if (owned(activity)) screen(activity, activity.getClass().getSimpleName(), "activity");
    }
    @Override public void onActivityStarted(Activity activity) { }
    @Override public void onActivityPaused(Activity activity) { }
    @Override public void onActivityStopped(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
    @Override public void onActivityDestroyed(Activity activity) { }
}
