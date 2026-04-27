package com.example.random_movie.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(
        tableName = "favorites",
        primaryKeys = {"userId", "movieId"}
)
public class FavoriteEntity {
    @NonNull
    public String userId = "";
    public int movieId;
    public long createdAt;
    public boolean synced; // локально/на сервер отправлено
}