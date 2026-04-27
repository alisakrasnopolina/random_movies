package com.example.random_movie.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(
        tableName = "watch_later",
        primaryKeys = {"userId", "movieId"}
)
public class WatchLaterEntity {
    @NonNull
    public String userId = "";

    public int movieId;

    public long createdAt;

    public boolean synced;
}