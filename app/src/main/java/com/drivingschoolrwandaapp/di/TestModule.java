package com.drivingschoolrwandaapp.di;

import android.content.Context;

import com.drivingschoolrwandaapp.repository.LocalExamDataSource;
import com.drivingschoolrwandaapp.repository.TestRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class TestModule {

    @Provides
    @Singleton
    public static LocalExamDataSource provideLocalExamDataSource(@ApplicationContext Context context) {
        return new LocalExamDataSource(context);
    }

    // TestRepository is auto-provided by Hilt via @Inject constructor with @ApplicationContext
}
