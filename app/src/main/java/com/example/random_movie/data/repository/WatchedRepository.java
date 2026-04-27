package com.example.random_movie.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.random_movie.data.local.AppDatabase;
import com.example.random_movie.data.local.dao.PendingActionDao;
import com.example.random_movie.data.local.dao.WatchedDao;
import com.example.random_movie.data.local.entity.PendingActionEntity;
import com.example.random_movie.data.local.entity.WatchedEntity;
import com.example.random_movie.sync.SyncScheduler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WatchedRepository {

    // actionType для pending_actions
    public static final String ACTION_ADD_WATCHED = "ADD_WATCHED";
    public static final String ACTION_REMOVE_WATCHED = "REMOVE_WATCHED";

    private final WatchedDao watchedDao;
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

    public WatchedRepository(@NonNull Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.watchedDao = db.watchedDao();
        this.pendingActionDao = db.pendingActionDao();
        this.appContext = context.getApplicationContext();
    }

    public void getWatchedIds(@NonNull String userId, @NonNull IdsCallback callback) {
        io.execute(() -> {
            try {
                List<Integer> ids = watchedDao.getMovieIds(userId);
                callback.onResult(ids);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown DB error");
            }
        });
    }

    public void addWatched(@NonNull String userId, int movieId, @NonNull VoidCallback callback) {
        io.execute(() -> {
            try {
                // 1) Локально сохраняем просмотренное
                WatchedEntity entity = new WatchedEntity();
                entity.userId = userId;
                entity.movieId = movieId;
                entity.watchedAt = System.currentTimeMillis();
                entity.synced = false;
                watchedDao.insert(entity);

                // 2) Ставим pending action для синка
                PendingActionEntity pending = new PendingActionEntity();
                pending.userId = userId;
                pending.actionType = ACTION_ADD_WATCHED;
                pending.movieId = movieId;
                pending.payloadJson = "{\"movie_id\":" + movieId + "}";
                pending.createdAt = System.currentTimeMillis();
                pending.retryCount = 0;
                pending.status = "PENDING";
                pendingActionDao.insert(pending);

                // 3) Пинаем воркер
                SyncScheduler.enqueueOneTime(appContext);

                callback.onDone();
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Add watched failed");
            }
        });
    }

    public void removeWatched(@NonNull String userId, int movieId, @NonNull VoidCallback callback) {
        io.execute(() -> {
            try {
                // 1) Локально удаляем
                watchedDao.delete(userId, movieId);

                // 2) Ставим pending action на удаление на сервере
                PendingActionEntity pending = new PendingActionEntity();
                pending.userId = userId;
                pending.actionType = ACTION_REMOVE_WATCHED;
                pending.movieId = movieId;
                pending.payloadJson = "{}";
                pending.createdAt = System.currentTimeMillis();
                pending.retryCount = 0;
                pending.status = "PENDING";
                pendingActionDao.insert(pending);

                // 3) Пинаем воркер
                SyncScheduler.enqueueOneTime(appContext);

                callback.onDone();
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Remove watched failed");
            }
        });
    }

    public void isWatched(@NonNull String userId, int movieId, @NonNull IsWatchedCallback callback) {
        io.execute(() -> {
            try {
                boolean exists = watchedDao.exists(userId, movieId);
                callback.onResult(exists);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Check watched failed");
            }
        });
    }

    public interface IsWatchedCallback {
        void onResult(boolean watched);
        void onError(String message);
    }
}