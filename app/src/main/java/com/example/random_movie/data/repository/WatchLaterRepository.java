package com.example.random_movie.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.random_movie.data.local.AppDatabase;
import com.example.random_movie.data.local.dao.PendingActionDao;
import com.example.random_movie.data.local.dao.WatchLaterDao;
import com.example.random_movie.data.local.entity.PendingActionEntity;
import com.example.random_movie.data.local.entity.WatchLaterEntity;
import com.example.random_movie.sync.SyncScheduler;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WatchLaterRepository {

    public static final String ACTION_ADD_WATCH_LATER = "ADD_WATCH_LATER";
    public static final String ACTION_REMOVE_WATCH_LATER = "REMOVE_WATCH_LATER";

    private final WatchLaterDao watchLaterDao;
    private final PendingActionDao pendingActionDao;
    private final Context appContext;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public interface VoidCallback {
        void onDone();
        void onError(String message);
    }

    public interface IdsCallback {
        void onResult(List<Integer> ids);
        void onError(String message);
    }

    public WatchLaterRepository(@NonNull Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.watchLaterDao = db.watchLaterDao();
        this.pendingActionDao = db.pendingActionDao();
        this.appContext = context.getApplicationContext();
    }

    public void getWatchLaterIds(@NonNull String userId, @NonNull IdsCallback callback) {
        io.execute(() -> {
            try {
                List<Integer> ids = watchLaterDao.getMovieIds(userId);
                callback.onResult(ids);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "DB error");
            }
        });
    }

    public void addWatchLater(@NonNull String userId, int movieId, @NonNull VoidCallback callback) {
        io.execute(() -> {
            try {
                WatchLaterEntity entity = new WatchLaterEntity();
                entity.userId = userId;
                entity.movieId = movieId;
                entity.createdAt = System.currentTimeMillis();
                entity.synced = false;
                watchLaterDao.insert(entity);

                JSONObject payload = new JSONObject();
                payload.put("movie_id", movieId);

                PendingActionEntity pending = new PendingActionEntity();
                pending.userId = userId;
                pending.actionType = ACTION_ADD_WATCH_LATER;
                pending.movieId = movieId;
                pending.payloadJson = payload.toString();
                pending.createdAt = System.currentTimeMillis();
                pending.retryCount = 0;
                pending.status = "PENDING";
                pendingActionDao.insert(pending);

                SyncScheduler.enqueueOneTime(appContext);
                callback.onDone();
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Add watch_later failed");
            }
        });
    }

    public void removeWatchLater(@NonNull String userId, int movieId, @NonNull VoidCallback callback) {
        io.execute(() -> {
            try {
                watchLaterDao.delete(userId, movieId);

                PendingActionEntity pending = new PendingActionEntity();
                pending.userId = userId;
                pending.actionType = ACTION_REMOVE_WATCH_LATER;
                pending.movieId = movieId;
                pending.payloadJson = "{}";
                pending.createdAt = System.currentTimeMillis();
                pending.retryCount = 0;
                pending.status = "PENDING";
                pendingActionDao.insert(pending);

                SyncScheduler.enqueueOneTime(appContext);
                callback.onDone();
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Remove watch_later failed");
            }
        });
    }

    public void isInWatchLater(@NonNull String userId, int movieId, @NonNull IsInWatchLaterCallback callback) {
        io.execute(() -> {
            try {
                boolean exists = watchLaterDao.exists(userId, movieId);
                callback.onResult(exists);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Check watch_later failed");
            }
        });
    }

    public interface IsInWatchLaterCallback {
        void onResult(boolean inWatchLater);
        void onError(String message);
    }
}