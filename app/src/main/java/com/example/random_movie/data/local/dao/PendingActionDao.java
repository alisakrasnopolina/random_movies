package com.example.random_movie.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.random_movie.data.local.entity.PendingActionEntity;

import java.util.List;

@Dao
public interface PendingActionDao {
    @Insert
    long insert(PendingActionEntity action);

    @Query("SELECT * FROM pending_actions WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    List<PendingActionEntity> getPending(int limit);

    @Query("UPDATE pending_actions SET status = :status, retryCount = :retryCount WHERE id = :id")
    void updateStatus(long id, String status, int retryCount);

    @Query("DELETE FROM pending_actions WHERE id = :id")
    void delete(long id);
}
