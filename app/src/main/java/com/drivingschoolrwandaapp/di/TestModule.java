package com.drivingschoolrwandaapp.di;

import android.app.Application;

import com.drivingschoolrwandaapp.api.ApiService;
import com.drivingschoolrwandaapp.repository.TestRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class TestModule {

    @Provides
    @Singleton
    public TestRepository provideTestRepository(Application application, ApiService apiService) {
        return new TestRepository(application, apiService);
    }
}
