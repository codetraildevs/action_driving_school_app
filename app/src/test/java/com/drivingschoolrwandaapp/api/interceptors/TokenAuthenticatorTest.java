package com.drivingschoolrwandaapp.api.interceptors;

import android.content.Context;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.api.ApiClient;
import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.models.response.LoginResponse;
import org.mockito.MockedStatic;
import retrofit2.Call;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TokenAuthenticatorTest {
    @Test public void temporaryRefreshFailurePreservesSession() throws Exception {
        TokenManager tokens = mock(TokenManager.class);
        Context context = mock(Context.class);
        when(tokens.getAccessToken()).thenReturn("old");
        when(tokens.getRefreshToken()).thenReturn("refresh");
        ApiClient client = mock(ApiClient.class);
        ApiService service = mock(ApiService.class);
        Call<LoginResponse> call = mock(Call.class);
        retrofit2.Response<LoginResponse> failure = mock(retrofit2.Response.class);
        when(failure.isSuccessful()).thenReturn(false);
        when(failure.code()).thenReturn(503);
        when(client.getApiService()).thenReturn(service);
        when(service.refreshToken(any())).thenReturn(call);
        when(call.execute()).thenReturn(failure);
        try (MockedStatic<ApiClient> api = mockStatic(ApiClient.class)) {
            api.when(() -> ApiClient.getInstance(context)).thenReturn(client);
            assertNull(new TokenAuthenticator(context, tokens).authenticate(null, unauthorized("profile")));
        }
        verify(tokens, never()).clearTokens();
        verifyNoInteractions(context);
    }
    private Response unauthorized(String path) {
        return new Response.Builder().request(new Request.Builder()
                .url("https://example.com/api/" + path)
                .header("Authorization", "Bearer old").build())
                .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build();
    }

    @Test public void publicAuthFailuresDoNotRefreshOrLogOut() {
        TokenManager tokens = mock(TokenManager.class);
        Context context = mock(Context.class);
        TokenAuthenticator auth = new TokenAuthenticator(context, tokens);
        for (String path : new String[]{"auth/login", "auth/register", "auth/refresh"}) {
            assertNull(auth.authenticate(null, unauthorized(path)));
        }
        verifyNoInteractions(tokens, context);
    }

    @Test public void repeatedUnauthorizedResponseStopsRetrying() {
        TokenManager tokens = mock(TokenManager.class);
        Context context = mock(Context.class);
        Response first = unauthorized("profile");
        Response retried = first.newBuilder().priorResponse(first).build();
        assertNull(new TokenAuthenticator(context, tokens).authenticate(null, retried));
        verifyNoInteractions(tokens, context);
    }

    @Test public void concurrentRefreshReusesNewToken() {
        TokenManager tokens = mock(TokenManager.class);
        Context context = mock(Context.class);
        when(tokens.getAccessToken()).thenReturn("new");
        Request retry = new TokenAuthenticator(context, tokens)
                .authenticate(null, unauthorized("profile"));
        assertNotNull(retry);
        assertEquals("Bearer new", retry.header("Authorization"));
        verify(tokens, never()).getRefreshToken();
        verifyNoInteractions(context);
    }
}
