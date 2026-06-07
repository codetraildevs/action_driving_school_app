package com.drivingschoolrwandaapp.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.drivingschoolrwandaapp.database.entities.QuestionOptionEntity;

import java.util.List;

@Dao
public interface QuestionOptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOptions(List<QuestionOptionEntity> options);

    @Query("SELECT * FROM question_options WHERE questionId = :questionId")
    LiveData<List<QuestionOptionEntity>> getOptionsForQuestion(int questionId);

    @Query("DELETE FROM question_options WHERE questionId IN (SELECT id FROM test_questions WHERE testId = :testId)")
    void deleteOptionsForTest(int testId);
}
