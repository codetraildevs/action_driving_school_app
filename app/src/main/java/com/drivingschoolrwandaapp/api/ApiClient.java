package com.drivingschoolrwandaapp.api;

import android.content.Context;

import com.drivingschoolrwandaapp.api.interceptors.AuthInterceptor;
import com.drivingschoolrwandaapp.api.interceptors.NetworkInterceptor;
import com.drivingschoolrwandaapp.api.interceptors.TokenAuthenticator;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * ApiClient provides static constants and the legacy singleton API client setup.
 * <p>
 * For new code, prefer using Hilt-injected {@link ApiService}, {@link TokenManager},
 * and {@link OkHttpClient} via dependency injection.
 *
 * @deprecated Use Hilt dependency injection instead of {@link #getInstance(Context)}.
 * The static constants (BASE_URL, SITE_URL, etc.) remain available for use.
 */
@Deprecated
public class ApiClient {

    public static final String BASE_URL = "https://console.amategekoyumuhanda.rw/api/";
    public static final String SITE_URL = "https://console.amategekoyumuhanda.rw";
    public static final String PHONE_NUMBER = "+250782877442";
    public static final String PLAYSTORE_LINK = "https://play.google.com/store/apps/details?id=com.drivingschoolrwandaapp";
    private static volatile ApiClient instance;
    private final ApiService apiService;
    private final TokenManager tokenManager;
    private final OkHttpClient okHttpClient;

    private ApiClient(Context context) {
        Context appContext = context.getApplicationContext();
        tokenManager = new TokenManager(appContext);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new NetworkInterceptor(appContext))
                .addInterceptor(new AuthInterceptor(appContext, tokenManager))
                .addInterceptor(loggingInterceptor)
                .authenticator(new TokenAuthenticator(appContext, tokenManager))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    /**
     * @deprecated Use Hilt DI instead. See {@link com.drivingschoolrwandaapp.di.NetworkModule}.
     */
    @Deprecated
    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }

    @Deprecated
    public ApiService getApiService() {
        return apiService;
    }

    @Deprecated
    public TokenManager getTokenManager() {
        return tokenManager;
    }

    @Deprecated
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }
}
