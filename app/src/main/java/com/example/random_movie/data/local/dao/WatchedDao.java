package com.example.random_movie.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.random_movie.data.local.entity.WatchedEntity;

import java.util.List;

@Dao
public interface WatchedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WatchedEntity item);

    @Query("DELETE FROM watched WHERE userId = :userId AND movieId = :movieId")
    void delete(String userId, int movieId);

    @Query("SELECT movieId FROM watched WHERE userId = :userId ORDER BY watchedAt DESC")
    List<Integer> getMovieIds(String userId);

    @Query("SELECT EXISTS(SELECT 1 FROM watched WHERE userId = :userId AND movieId = :movieId)")
    boolean exists(String userId, int movieId);

    @Query("SELECT userRating FROM watched WHERE userId = :userId AND movieId = :movieId LIMIT 1")
    int getRating(String userId, int movieId);

    @Query("UPDATE watched SET userRating = :rating WHERE userId = :userId AND movieId = :movieId")
    void updateRating(String userId, int movieId, int rating);
}