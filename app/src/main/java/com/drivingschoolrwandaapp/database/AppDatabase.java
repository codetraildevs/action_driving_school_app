package com.drivingschoolrwandaapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.drivingschoolrwandaapp.database.dao.PdfDao;
import com.drivingschoolrwandaapp.database.dao.QuestionOptionDao;
import com.drivingschoolrwandaapp.database.dao.SubscriptionPlanDao;
import com.drivingschoolrwandaapp.database.dao.TestDao;
import com.drivingschoolrwandaapp.database.dao.TestQuestionDao;
import com.drivingschoolrwandaapp.database.dao.TestResultDao;
import com.drivingschoolrwandaapp.database.dao.UserDao;
import com.drivingschoolrwandaapp.database.dao.UserSubscriptionDao;
import com.drivingschoolrwandaapp.database.entities.BookmarkEntity;
import com.drivingschoolrwandaapp.database.entities.QuestionOptionEntity;
import com.drivingschoolrwandaapp.database.entities.SubscriptionPlan;
import com.drivingschoolrwandaapp.database.entities.TestEntity;
import com.drivingschoolrwandaapp.database.entities.TestQuestionEntity;
import com.drivingschoolrwandaapp.database.entities.TestResultEntity;
import com.drivingschoolrwandaapp.database.entities.User;
import com.drivingschoolrwandaapp.database.entities.UserSubscriptionEntity;
import com.drivingschoolrwandaapp.utils.DataConverter;
import com.drivingschoolrwandaapp.utils.DateConverter;

@Database(entities = {User.class, SubscriptionPlan.class, UserSubscriptionEntity.class, TestEntity.class, TestQuestionEntity.class, QuestionOptionEntity.class, BookmarkEntity.class, TestResultEntity.class}, version = 14, exportSchema = false)
@TypeConverters({DateConverter.class, DataConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();

    public abstract SubscriptionPlanDao subscriptionPlanDao();

    public abstract UserSubscriptionDao userSubscriptionDao();

    public abstract TestDao testDao();

    public abstract TestQuestionDao testQuestionDao();

    public abstract QuestionOptionDao questionOptionDao();

    public abstract PdfDao pdfDao();

    public abstract TestResultDao testResultDao();

    /**
     * v13 -> v14 adds the test_results table that persists completed exam
     * attempts so Previous Tests works offline and across restarts.
     * A real migration (not destructive fallback) keeps every user's existing
     * profile, subscription cache, bookmarks and PDF data intact on upgrade.
     */
    private static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `test_results` ("
                    + "`date` INTEGER NOT NULL, "
                    + "`score` INTEGER NOT NULL, "
                    + "`totalMarks` INTEGER NOT NULL, "
                    + "`passed` INTEGER NOT NULL, "
                    + "`testNumber` INTEGER NOT NULL, "
                    + "`testName` TEXT, "
                    + "`testId` INTEGER NOT NULL, "
                    + "`duration` INTEGER NOT NULL, "
                    + "`elapsedSeconds` INTEGER NOT NULL, "
                    + "`correctCount` INTEGER NOT NULL, "
                    + "`wrongCount` INTEGER NOT NULL, "
                    + "`skippedCount` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`date`))");
        }
    };

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "driving_school_db")
                            .addMigrations(MIGRATION_13_14)
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries() // Allow main thread queries
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
