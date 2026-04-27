package com.example.random_movie.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.random_movie.BuildConfig;
import com.example.random_movie.auth.ApiClient;
import com.example.random_movie.data.local.AppDatabase;
import com.example.random_movie.data.local.dao.PendingActionDao;
import com.example.random_movie.data.local.entity.PendingActionEntity;
import com.example.random_movie.data.repository.FavoritesRepository;
import com.example.random_movie.data.repository.WatchLaterRepository;
import com.example.random_movie.data.repository.WatchedRepository;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

import java.util.List;

public class PendingActionsWorker extends Worker {

    private static final String TAG = "PendingActionsWorker";
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final PendingActionDao pendingActionDao;
    private final OkHttpClient client;

    public PendingActionsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        AppDatabase db = AppDatabase.getInstance(context);
        this.pendingActionDao = db.pendingActionDao();
        this.client = ApiClient.get(context); // Bearer подставится через интерсептор
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<PendingActionEntity> pending = pendingActionDao.getPending(BATCH_SIZE);
            if (pending == null || pending.isEmpty()) return Result.success();

            Log.d("PendingActionsWorker", "doWork started");

            for (PendingActionEntity action : pending) {
                boolean ok = processAction(action);

                if (ok) {
                    pendingActionDao.delete(action.id);
                } else {
                    int retries = action.retryCount + 1;
                    if (retries >= MAX_RETRIES) {
                        pendingActionDao.updateStatus(action.id, "FAILED", retries);
                    } else {
                        pendingActionDao.updateStatus(action.id, "PENDING", retries);
                    }
                }
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Worker failed", e);
            return Result.retry();
        }
    }

    private boolean processAction(PendingActionEntity action) {
        try {
            Request request = buildRequest(action);
            if (request == null) return false;

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "Action failed HTTP " + response.code() + " type=" + action.actionType + " id=" + action.id);
                }
                return response.isSuccessful();
            }
        } catch (Exception e) {
            Log.e(TAG, "Action exception type=" + action.actionType + " id=" + action.id, e);
            return false;
        }
    }

    private Request buildRequest(PendingActionEntity action) throws Exception {
        String base = BuildConfig.API_BASE_URL;
        String type = action.actionType;

        switch (type) {
            // ---------- FAVORITES ----------
            case FavoritesRepository.ACTION_ADD_FAVORITE: {
                String url = base + "/users/me/favorites";
                String payload = normalizePayload(action.payloadJson, action.movieId);

                return new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(payload, JSON))
                        .addHeader("accept", "application/json")
                        .addHeader("content-type", "application/json")
                        .build();
            }

            case FavoritesRepository.ACTION_REMOVE_FAVORITE: {
                String url = base + "/users/me/favorites/" + action.movieId;

                return new Request.Builder()
                        .url(url)
                        .delete()
                        .addHeader("accept", "application/json")
                        .build();
            }

            // ---------- WATCH LATER ----------
            case WatchLaterRepository.ACTION_ADD_WATCH_LATER: {
                String url = base + "/users/me/watch-later";
                String payload = normalizePayload(action.payloadJson, action.movieId);

                return new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(payload, JSON))
                        .addHeader("accept", "application/json")
                        .addHeader("content-type", "application/json")
                        .build();
            }

            case WatchLaterRepository.ACTION_REMOVE_WATCH_LATER: {
                String url = base + "/users/me/watch-later/" + action.movieId;

                return new Request.Builder()
                        .url(url)
                        .delete()
                        .addHeader("accept", "application/json")
                        .build();
            }

            // ---------- WATCHED ----------
            case WatchedRepository.ACTION_ADD_WATCHED: {
                String url = base + "/users/me/watched";
                String payload = normalizePayload(action.payloadJson, action.movieId);

                return new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(payload, JSON))
                        .addHeader("accept", "application/json")
                        .addHeader("content-type", "application/json")
                        .build();
            }

            case WatchedRepository.ACTION_REMOVE_WATCHED: {
                String url = base + "/users/me/watched/" + action.movieId;

                return new Request.Builder()
                        .url(url)
                        .delete()
                        .addHeader("accept", "application/json")
                        .build();
            }

            default:
                Log.w(TAG, "Unknown actionType: " + type);
                return null;
        }
    }

    private String normalizePayload(String payloadJson, int movieId) {
        try {
            JSONObject obj = new JSONObject(payloadJson == null ? "{}" : payloadJson);
            if (!obj.has("movie_id")) obj.put("movie_id", movieId);
            return obj.toString();
        } catch (Exception e) {
            return "{\"movie_id\":" + movieId + "}";
        }
    }
}