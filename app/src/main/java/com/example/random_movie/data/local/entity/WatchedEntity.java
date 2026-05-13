package com.example.random_movie.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(
        tableName = "watched",
        primaryKeys = {"userId", "movieId"}
)
public class WatchedEntity {
    @NonNull
    public String userId = "";
    public int movieId;
    public long watchedAt;
    public boolean synced;
    public int userRating = 0;
}