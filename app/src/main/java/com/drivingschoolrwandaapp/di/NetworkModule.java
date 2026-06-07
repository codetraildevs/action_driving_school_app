package com.drivingschoolrwandaapp.di;

import android.content.Context;

import com.drivingschoolrwandaapp.api.ApiClient;
import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.api.interceptors.AuthInterceptor;
import com.drivingschoolrwandaapp.api.interceptors.NetworkInterceptor;
import com.drivingschoolrwandaapp.api.interceptors.TokenAuthenticator;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    private static final String BASE_URL = ApiClient.BASE_URL;

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(
            @ApplicationContext Context context,
            TokenManager tokenManager
    ) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(new NetworkInterceptor(context))
                .addInterceptor(new AuthInterceptor(context, tokenManager))
                .addInterceptor(loggingInterceptor)
                .authenticator(new TokenAuthenticator(context, tokenManager))
                .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}
