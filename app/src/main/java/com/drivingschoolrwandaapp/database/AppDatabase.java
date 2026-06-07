package com.drivingschoolrwandaapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.drivingschoolrwandaapp.database.dao.PdfDao;
import com.drivingschoolrwandaapp.database.dao.QuestionOptionDao;
import com.drivingschoolrwandaapp.database.dao.SubscriptionPlanDao;
import com.drivingschoolrwandaapp.database.dao.TestDao;
import com.drivingschoolrwandaapp.database.dao.TestQuestionDao;
import com.drivingschoolrwandaapp.database.dao.UserDao;
import com.drivingschoolrwandaapp.database.dao.UserSubscriptionDao;
import com.drivingschoolrwandaapp.database.entities.BookmarkEntity;
import com.drivingschoolrwandaapp.database.entities.QuestionOptionEntity;
import com.drivingschoolrwandaapp.database.entities.SubscriptionPlan;
import com.drivingschoolrwandaapp.database.entities.TestEntity;
import com.drivingschoolrwandaapp.database.entities.TestQuestionEntity;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.database.entities.UserSubscriptionEntity;
import com.drivingschoolrwandaapp.utils.DataConverter;
import com.drivingschoolrwandaapp.utils.DateConverter;

@Database(entities = {User.class, SubscriptionPlan.class, UserSubscriptionEntity.class, TestEntity.class, TestQuestionEntity.class, QuestionOptionEntity.class, BookmarkEntity.class}, version = 13, exportSchema = false)
@TypeConverters({DateConverter.class, DataConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();

    public abstract SubscriptionPlanDao subscriptionPlanDao();

    public abstract UserSubscriptionDao userSubscriptionDao();

    public abstract TestDao testDao();

    public abstract TestQuestionDao testQuestionDao();

    public abstract QuestionOptionDao questionOptionDao();

    public abstract PdfDao pdfDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "driving_school_db")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries() // Allow main thread queries
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
