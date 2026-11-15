package com.drivingschoolrwandaapp.utils;

import android.app.Activity;
import androidx.fragment.app.FragmentManager;
import com.drivingschoolrwandaapp.ui.activities.SplashActivity;
import com.drivingschoolrwandaapp.ui.activities.LoginActivity;
import org.junit.Test;
import org.mockito.MockedStatic;
import java.util.Collections;
import static org.mockito.Mockito.*;

public class JourneyLifecycleTest {
    @Test public void launchIsOncePerProcessAndScreensAreRecordedOnResume() {
        SplashActivity splash = mock(SplashActivity.class);
        LoginActivity login = mock(LoginActivity.class);
        when(splash.getSupportFragmentManager()).thenReturn(mock(FragmentManager.class));
        when(login.getSupportFragmentManager()).thenReturn(mock(FragmentManager.class));
        try (MockedStatic<JourneyAnalytics> analytics = mockStatic(JourneyAnalytics.class)) {
            JourneyLifecycle lifecycle = new JourneyLifecycle();
            lifecycle.onActivityCreated(splash, null);
            lifecycle.onActivityResumed(splash);
            lifecycle.onActivityCreated(login, null);
            lifecycle.onActivityResumed(login);
            analytics.verify(() -> JourneyAnalytics.send(any(), eq("app_launch"),
                    eq(Collections.singletonMap("scope", "process_first_activity"))), times(1));
            analytics.verify(() -> JourneyAnalytics.send(any(), eq("journey_screen"), anyMap()), times(2));
        }
    }

    @Test public void thirdPartyAdActivitiesAreNotRecordedAsAppLaunchesOrScreens() {
        try (MockedStatic<JourneyAnalytics> analytics = mockStatic(JourneyAnalytics.class)) {
            JourneyLifecycle lifecycle = new JourneyLifecycle();
            Activity external = mock(Activity.class);
            lifecycle.onActivityCreated(external, null);
            lifecycle.onActivityResumed(external);
            analytics.verifyNoInteractions();
        }
    }
}
