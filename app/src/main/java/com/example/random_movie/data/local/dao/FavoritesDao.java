package com.example.random_movie.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.random_movie.data.local.entity.FavoriteEntity;

import java.util.List;

@Dao
public interface FavoritesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteEntity item);

    @Query("DELETE FROM favorites WHERE userId = :userId AND movieId = :movieId")
    void delete(String userId, int movieId);

    @Query("SELECT movieId FROM favorites WHERE userId = :userId")
    List<Integer> getMovieIds(String userId);

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND movieId = :movieId)")
    boolean exists(String userId, int movieId);
}
