package com.drivingschoolrwandaapp.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.drivingschoolrwandaapp.database.entities.UserSubscriptionEntity;
import com.drivingschoolrwandaapp.database.entities.UserSubscriptionWithPlan;

@Dao
public interface UserSubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserSubscriptionEntity userSubscription);

    @Transaction
    @Query("SELECT * FROM user_subscription LIMIT 1")
    LiveData<UserSubscriptionWithPlan> getUserSubscription();

    @Query("DELETE FROM user_subscription")
    void delete();
}
