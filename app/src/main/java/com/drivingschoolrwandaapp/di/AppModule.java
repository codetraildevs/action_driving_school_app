package com.drivingschoolrwandaapp.di;

import android.content.Context;

import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@InstallIn(SingletonComponent.class)
@Module
public class AppModule {

    @Provides
    @Singleton
    public TokenManager provideTokenManager(@ApplicationContext Context context) {
        return new TokenManager(context);
    }
}
