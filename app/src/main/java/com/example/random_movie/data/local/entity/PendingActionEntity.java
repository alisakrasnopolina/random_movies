package com.example.random_movie.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_actions")
public class PendingActionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String userId = "";
    @NonNull public String actionType = ""; // ADD_FAVORITE, REMOVE_FAVORITE, ADD_WATCH_LATER, ...
    public int movieId;
    @NonNull public String payloadJson = "{}";
    public long createdAt;
    public int retryCount;
    @NonNull
    public String status = "PENDING"; // PENDING, IN_PROGRESS, DONE, FAILED
}