package com.example.random_movie.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.random_movie.BuildConfig;
import com.example.random_movie.auth.ApiClient;
import com.example.random_movie.data.local.AppDatabase;
import com.example.random_movie.data.local.dao.PendingActionDao;
import com.example.random_movie.data.local.dao.WatchedDao;
import com.example.random_movie.data.local.entity.PendingActionEntity;
import com.example.random_movie.data.local.entity.WatchedEntity;
import com.example.random_movie.sync.SyncScheduler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Request;
import okhttp3.Response;

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

    public interface RatingCallback {
        void onResult(int rating);
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

    public void getRating(@NonNull String userId, int movieId, @NonNull RatingCallback callback) {
        io.execute(() -> {
            try {
                int rating = watchedDao.getRating(userId, movieId);
                callback.onResult(rating);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Get watched rating failed");
            }
        });
    }

    public void updateRating(@NonNull String userId, int movieId, int rating, @NonNull VoidCallback callback) {
        io.execute(() -> {
            try {
                watchedDao.updateRating(userId, movieId, rating);
                callback.onDone();
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Update watched rating failed");
            }
        });
    }

    public interface IsWatchedCallback {
        void onResult(boolean watched);
        void onError(String message);
    }

    public void syncWatchedFromServer(@NonNull String userId, @NonNull VoidCallback callback) {
        io.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(BuildConfig.API_BASE_URL + "/users/me/watched")
                        .get()
                        .addHeader("accept", "application/json")
                        .build();

                try (Response response = ApiClient.get(appContext).newCall(request).execute()) {
                    String raw = response.body() != null ? response.body().string() : "{}";

                    if (!response.isSuccessful()) {
                        throw new RuntimeException("HTTP " + response.code() + ": " + raw);
                    }

                    JSONObject obj = new JSONObject(raw);
                    JSONArray items = obj.optJSONArray("items");

                    if (items != null) {
                        for (int i = 0; i < items.length(); i++) {
                            int movieId = items.optInt(i, 0);
                            if (movieId <= 0) continue;

                            int oldRating = 0;

                            try {
                                oldRating = watchedDao.getRating(userId, movieId);
                            } catch (Exception ignored) {
                                oldRating = 0;
                            }

                            WatchedEntity entity = new WatchedEntity();
                            entity.userId = userId;
                            entity.movieId = movieId;
                            entity.watchedAt = System.currentTimeMillis();
                            entity.synced = true;
                            entity.userRating = oldRating;

                            watchedDao.insert(entity);
                        }
                    }

                    callback.onDone();
                }
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Sync watched failed");
            }
        });
    }
}