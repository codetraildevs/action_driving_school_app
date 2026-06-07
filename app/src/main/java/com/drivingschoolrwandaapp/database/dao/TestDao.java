package com.drivingschoolrwandaapp.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.room.Upsert;

import com.drivingschoolrwandaapp.database.entities.TestEntity;
import com.drivingschoolrwandaapp.database.entities.TestWithQuestions;

import java.util.List;

@Dao
public interface TestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTests(List<TestEntity> tests);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertTestsIgnore(List<TestEntity> tests);

    @Update
    void updateTests(List<TestEntity> tests);

    @Upsert
    void upsertTests(List<TestEntity> tests);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTest(TestEntity test);

    @Upsert
    long upsertTest(TestEntity test);

    @Query("DELETE FROM tests")
    void deleteAllTests();

    @Query("DELETE FROM tests WHERE id NOT IN (:ids)")
    void deleteTestsNotIn(List<Integer> ids);

    @Query("SELECT * FROM tests")
    LiveData<List<TestEntity>> getAllTests();

    @Transaction
    @Query("SELECT * FROM tests WHERE id = :testId")
    LiveData<TestWithQuestions> getTestWithQuestions(int testId);

}
