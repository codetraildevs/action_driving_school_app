package com.drivingschoolrwandaapp.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.drivingschoolrwandaapp.database.entities.TestResultEntity;

import java.util.List;

@Dao
public interface TestResultDao {

    /**
     * Persist a completed exam result. IGNORE (instead of REPLACE) keeps the
     * first entry for a given completion timestamp so a result that is emitted
     * twice is never duplicated in history.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(TestResultEntity result);

    /**
     * All stored results, newest first. Exposed as LiveData so the
     * Previous Tests page updates automatically when a new result is saved.
     */
    @Query("SELECT * FROM test_results ORDER BY date DESC")
    LiveData<List<TestResultEntity>> getAll();
}
