package com.drivingschoolrwandaapp.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.drivingschoolrwandaapp.database.entities.LearningMaterial;

import java.util.List;

@Dao
public interface LearningMaterialDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LearningMaterial> materials);

    @Query("SELECT * FROM learning_materials")
    LiveData<List<LearningMaterial>> getAllMaterials();

    @Query("SELECT * FROM learning_materials WHERE id = :materialId")
    LiveData<LearningMaterial> getMaterialById(int materialId);

    @Query("UPDATE learning_materials SET localPath = :localPath WHERE id = :materialId")
    void updateLocalPath(int materialId, String localPath);
}
