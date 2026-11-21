package com.drivingschoolrwandaapp.api.interceptors;

import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AuthInterceptorTest {
    @Test public void networkFailureIsNotRetried() throws Exception {
        TokenManager tokens = mock(TokenManager.class);
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(tokens.getAccessToken()).thenReturn("current-token");
        when(chain.request()).thenReturn(new Request.Builder().url("https://example.com/api/profile").build());
        IOException failure = new IOException("Connection lost");
        when(chain.proceed(any())).thenThrow(failure);
        try {
            new AuthInterceptor(null, tokens).intercept(chain);
            fail("Expected network failure");
        } catch (IOException actual) {
            assertSame(failure, actual);
        }
        verify(chain, times(1)).proceed(any());
    }

    @Test public void tokenStoreFailureDoesNotSendStaleAuthorization() throws Exception {
        TokenManager tokens = mock(TokenManager.class);
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(tokens.getAccessToken()).thenThrow(new IllegalStateException("Store unavailable"));
        when(chain.request()).thenReturn(new Request.Builder().url("https://example.com/api/profile")
                .header("Authorization", "Bearer stale-token").build());
        new AuthInterceptor(null, tokens).intercept(chain);
        ArgumentCaptor<Request> request = ArgumentCaptor.forClass(Request.class);
        verify(chain).proceed(request.capture());
        assertNull(request.getValue().header("Authorization"));
    }
}
