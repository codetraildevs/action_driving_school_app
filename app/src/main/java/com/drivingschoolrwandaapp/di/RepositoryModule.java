package com.drivingschoolrwandaapp.di;

import android.content.Context;

import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.database.dao.UserDao;
import com.drivingschoolrwandaapp.data.local.preferences.TokenManager;
import com.drivingschoolrwandaapp.repository.UserRepository;
import com.drivingschoolrwandaapp.repository.WhatsAppRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@InstallIn(SingletonComponent.class)
@Module
public class RepositoryModule {

    @Provides
    @Singleton
    public UserRepository provideUserRepository(
            @ApplicationContext Context context,
            ApiService apiService,
            UserDao userDao,
            TokenManager tokenManager
    ) {
        return new UserRepository(context, apiService, userDao, tokenManager);
    }

    @Provides
    @Singleton
    public WhatsAppRepository provideWhatsAppRepository(ApiService apiService) {
        return new WhatsAppRepository(apiService);
    }
}
