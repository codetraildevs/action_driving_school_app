package com.drivingschoolrwandaapp.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.drivingschoolrwandaapp.database.entities.User;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<User> getUserById(int userId);

    @Query("SELECT * FROM users LIMIT 1")
    LiveData<User> getUser();

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    LiveData<User> getUserById(int userId);

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    User getUserByIdSync(int userId);

    @Query("SELECT * FROM users LIMIT 1")
    User getUserSync();

    @Query("DELETE FROM users")
    void deleteAll();
}
