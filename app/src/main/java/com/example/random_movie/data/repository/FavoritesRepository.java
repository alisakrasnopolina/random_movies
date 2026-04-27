package com.example.random_movie.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.random_movie.data.local.AppDatabase;
import com.example.random_movie.data.local.dao.FavoritesDao;
import com.example.random_movie.data.local.dao.PendingActionDao;
import com.example.random_movie.data.local.entity.FavoriteEntity;
import com.example.random_movie.data.local.entity.PendingActionEntity;
import com.example.random_movie.sync.SyncScheduler;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoritesRepository {

    public static final String ACTION_ADD_FAVORITE = "ADD_FAVORITE";
    public static final String ACTION_REMOVE_FAVORITE = "REMOVE_FAVORITE";
    public static final String ACTION_ADD_WATCH_LATER = "ADD_WATCH_LATER";
    public static final String ACTION_REMOVE_WATCH_LATER = "REMOVE_WATCH_LATER";
    public static final String ACTION_ADD_WATCHED = "ADD_WATCHED";
    public static final String ACTION_REMOVE_WATCHED = "REMOVE_WATCHED";

    private final FavoritesDao favoritesDao;
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

    public FavoritesRepository(@NonNull Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.favoritesDao = db.favoritesDao();
        this.pendingActionDao = db.pendingActionDao();
        this.appContext = context.getApplicationContext();
    }

    public void getFavoriteIds(String userId, @NonNull IdsCallback callback) {
        io.execute(() -> {
            try {
                List<Integer> ids = favoritesDao.getMovieIds(userId);
                callback.onResult(ids);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void addFavorite(String userId, int movieId, @NonNull VoidCallback callback) {
        io.execute(() -> {
            try {
                FavoriteEntity entity = new FavoriteEntity();
                entity.userId = userId;
                entity.movieId = movieId;
                entity.createdAt = System.currentTimeMillis();
                entity.synced = false;
                favoritesDao.insert(entity);

                PendingActionEntity pending = new PendingActionEntity();
                pending.userId = userId;
                pending.actionType = ACTION_ADD_FAVORITE;
                pending.movieId = movieId;
                pending.payloadJson = new JSONObject()
                        .put("movie_id", movieId)
                        .toString();
                pending.createdAt = System.currentTimeMillis();
                pending.retryCount = 0;
                pending.status = "PENDING";
                pendingActionDao.insert(pending);

                SyncScheduler.enqueueOneTime(appContext); // запускаем синк
                callback.onDone();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void removeFavorite(String userId, int movieId, @NonNull VoidCallback callback) {
        io.execute(() -> {
            try {
                favoritesDao.delete(userId, movieId);

                PendingActionEntity pending = new PendingActionEntity();
                pending.userId = userId;
                pending.actionType = ACTION_REMOVE_FAVORITE;
                pending.movieId = movieId;
                pending.payloadJson = "{}";
                pending.createdAt = System.currentTimeMillis();
                pending.retryCount = 0;
                pending.status = "PENDING";
                pendingActionDao.insert(pending);

                SyncScheduler.enqueueOneTime(appContext);
                callback.onDone();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
}