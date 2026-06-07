package com.drivingschoolrwandaapp.di;

import android.content.Context;

import com.drivingschoolrwandaapp.database.AppDatabase;
import com.drivingschoolrwandaapp.database.dao.PdfDao;
import com.drivingschoolrwandaapp.database.dao.TestDao;
import com.drivingschoolrwandaapp.database.dao.UserDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@InstallIn(SingletonComponent.class)
@Module
public class DatabaseModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return AppDatabase.getDatabase(context);
    }

    @Provides
    public UserDao provideUserDao(AppDatabase appDatabase) {
        return appDatabase.userDao();
    }

    @Provides
    public TestDao provideTestDao(AppDatabase appDatabase) {
        return appDatabase.testDao();
    }

    @Provides
    public PdfDao providePdfDao(AppDatabase appDatabase) {
        return appDatabase.pdfDao();
    }
}
