package com.example.random_movie.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_movies")
public class CachedMovieEntity {
    @PrimaryKey
    public int id;

    @NonNull
    public String title = "";
    public int year;
    @NonNull public String posterUrl = "";
    @NonNull public String genre = "";
    public double ratingImdb;
    public int runtimeMin;
    public long updatedAt; // epoch millis
}