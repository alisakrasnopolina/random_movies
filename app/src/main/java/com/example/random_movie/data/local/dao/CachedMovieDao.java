package com.example.random_movie.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.random_movie.data.local.entity.CachedMovieEntity;

@Dao
public interface CachedMovieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CachedMovieEntity movie);

    @Query("SELECT * FROM cached_movies WHERE id = :movieId LIMIT 1")
    CachedMovieEntity getById(int movieId);

    @Query("SELECT * FROM cached_movies ORDER BY updatedAt DESC LIMIT 1")
    CachedMovieEntity getLastMovie();

    @Query("SELECT * FROM cached_movies ORDER BY RANDOM() LIMIT 1")
    CachedMovieEntity getRandomAny();
}