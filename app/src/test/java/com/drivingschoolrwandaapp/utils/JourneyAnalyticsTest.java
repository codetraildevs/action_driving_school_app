package com.drivingschoolrwandaapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class JourneyAnalyticsTest {
    private Context context;
    private FirebaseAnalytics firebase;
    private MockedStatic<FirebaseAnalytics> sdk;
    private MockedConstruction<Bundle> bundles;
    private AtomicBoolean firstExam;
    private final List<Map<String, String>> payloads = new ArrayList<>();

    @Before public void setup() {
        context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);
        firebase = mock(FirebaseAnalytics.class);
        sdk = mockStatic(FirebaseAnalytics.class);
        sdk.when(() -> FirebaseAnalytics.getInstance(context)).thenReturn(firebase);
        bundles = mockConstruction(Bundle.class, (bundle, construction) -> {
            Map<String, String> values = new LinkedHashMap<>();
            payloads.add(values);
            doAnswer(call -> { values.put(call.getArgument(0), call.getArgument(1)); return null; })
                    .when(bundle).putString(anyString(), anyString());
        });
        SharedPreferences preferences = mock(SharedPreferences.class);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        when(context.getSharedPreferences(JourneyAnalytics.PREFS, Context.MODE_PRIVATE)).thenReturn(preferences);
        firstExam = new AtomicBoolean();
        when(preferences.getBoolean(anyString(), eq(false))).thenAnswer(call -> firstExam.get());
        when(preferences.edit()).thenReturn(editor);
        when(editor.putBoolean(anyString(), eq(true))).thenAnswer(call -> { firstExam.set(true); return editor; });
    }

    @After public void cleanup() {
        bundles.close();
        sdk.close();
    }

    @Test public void authPayloadContainsOnlyAllowedCategories() {
        JourneyAnalytics.Failure failure = JourneyAnalytics.failure(403,
                "Device for 0729111458 is forbidden; token=secret; name=Private Person");
        JourneyAnalytics.auth(context, JourneyAnalytics.Flow.LOGIN, JourneyAnalytics.Source.LOGIN_FORM,
                JourneyAnalytics.Outcome.FAILURE, failure);
        assertEquals(2, payloads.get(0).size());
        assertEquals("device_restricted", payloads.get(0).get("failure_reason"));
        assertEquals("login_form", payloads.get(0).get("source"));
        verify(firebase).setUserId(null);
        verify(firebase).logEvent(eq("login_failure"), any(Bundle.class));
    }

    @Test public void successfulRegistrationAndAutoLoginAreSeparateSteps() {
        JourneyAnalytics.auth(context, JourneyAnalytics.Flow.REGISTRATION, JourneyAnalytics.Source.REGISTRATION_FORM,
                JourneyAnalytics.Outcome.SUCCESS, JourneyAnalytics.Failure.NONE);
        JourneyAnalytics.auth(context, JourneyAnalytics.Flow.LOGIN, JourneyAnalytics.Source.REGISTRATION_AUTO_LOGIN,
                JourneyAnalytics.Outcome.SUCCESS, JourneyAnalytics.Failure.NONE);
        verify(firebase).logEvent(eq("registration_success"), any(Bundle.class));
        verify(firebase).logEvent(eq("login_success"), any(Bundle.class));
        assertEquals("registration_auto_login", payloads.get(1).get("source"));
    }

    @Test public void firstExamIsRecordedOnceAcrossRepeatedCalls() {
        JourneyAnalytics.firstExam(context);
        JourneyAnalytics.firstExam(context);
        verify(firebase, times(1)).logEvent(eq("first_exam_started"), any(Bundle.class));
        assertEquals("installation_since_tracking", payloads.get(0).get("scope"));
    }

    @Test public void previouslyRecordedFirstExamIsNotRepeatedAfterRestart() {
        firstExam.set(true);
        JourneyAnalytics.firstExam(context);
        verifyNoInteractions(firebase);
    }

    @Test public void analyticsFailureDoesNotConsumeFirstExamMarkerOrCrash() {
        sdk.when(() -> FirebaseAnalytics.getInstance(context)).thenThrow(new IllegalStateException("offline SDK"));
        JourneyAnalytics.firstExam(context);
        assertFalse(firstExam.get());
    }

    @Test public void classifyTransportAndHttpErrorsWithoutSendingText() {
        assertEquals(JourneyAnalytics.Failure.TIMEOUT, JourneyAnalytics.failure(new SocketTimeoutException("private"), false));
        assertEquals(JourneyAnalytics.Failure.NETWORK, JourneyAnalytics.failure(new UnknownHostException("private"), false));
        assertEquals(JourneyAnalytics.Failure.CANCELED, JourneyAnalytics.failure(new UnknownHostException(), true));
        assertEquals(JourneyAnalytics.Failure.RATE_LIMIT, JourneyAnalytics.failure(429, "private"));
        assertEquals(JourneyAnalytics.Failure.SERVER, JourneyAnalytics.failure(503, "private"));
        assertEquals(JourneyAnalytics.Failure.ACCOUNT_EXISTS, JourneyAnalytics.failure(409, "Phone already registered"));
        assertEquals(JourneyAnalytics.Failure.ACCOUNT_NOT_FOUND, JourneyAnalytics.failure(404, "No account found"));
    }
}
