package com.example.random_movie.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.random_movie.data.local.entity.WatchLaterEntity;

import java.util.List;

@Dao
public interface WatchLaterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WatchLaterEntity item);

    @Query("DELETE FROM watch_later WHERE userId = :userId AND movieId = :movieId")
    void delete(String userId, int movieId);

    @Query("SELECT movieId FROM watch_later WHERE userId = :userId ORDER BY createdAt DESC")
    List<Integer> getMovieIds(String userId);

    @Query("SELECT EXISTS(SELECT 1 FROM watch_later WHERE userId = :userId AND movieId = :movieId)")
    boolean exists(String userId, int movieId);
}