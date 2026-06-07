package com.drivingschoolrwandaapp.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.drivingschoolrwandaapp.database.entities.SubscriptionPlan;

import java.util.List;

@Dao
public interface SubscriptionPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SubscriptionPlan> plans);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SubscriptionPlan plan);

    @Query("SELECT * FROM subscription_plans")
    LiveData<List<SubscriptionPlan>> getAll();

    @Query("DELETE FROM subscription_plans")
    void deleteAll();
}
