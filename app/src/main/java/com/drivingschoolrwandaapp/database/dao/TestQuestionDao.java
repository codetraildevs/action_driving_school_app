package com.drivingschoolrwandaapp.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.drivingschoolrwandaapp.database.entities.TestQuestionEntity;

import java.util.List;

@Dao
public interface TestQuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertQuestions(List<TestQuestionEntity> questions);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertQuestion(TestQuestionEntity question);

    @Query("SELECT * FROM test_questions WHERE testId = :testId")
    LiveData<List<TestQuestionEntity>> getQuestionsForTest(int testId);

    @Query("DELETE FROM test_questions WHERE testId = :testId")
    void deleteQuestionsForTest(int testId);

}
