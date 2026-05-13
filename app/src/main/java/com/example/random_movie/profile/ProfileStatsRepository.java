package com.example.random_movie.profile;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.random_movie.BuildConfig;
import com.example.random_movie.auth.ApiClient;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Request;
import okhttp3.Response;

public class ProfileStatsRepository {

    public static class ProfileStats {
        public int watchedCount;
        public int finishedSessionsCount;
        public int recommendationsCount;
    }

    public interface Callback {
        void onSuccess(ProfileStats stats);
        void onError(String message);
    }

    private final Context context;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public ProfileStatsRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void getMyStats(@NonNull Callback callback) {
        loadStats("/profile/me/stats", callback);
    }

    public void getUserStats(@NonNull String userId, @NonNull Callback callback) {
        loadStats("/profile/" + userId + "/stats", callback);
    }

    private void loadStats(@NonNull String path, @NonNull Callback callback) {
        io.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(BuildConfig.API_BASE_URL + path)
                        .get()
                        .addHeader("accept", "application/json")
                        .build();

                try (Response response = ApiClient.get(context).newCall(request).execute()) {
                    String raw = response.body() != null ? response.body().string() : "{}";

                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + raw);
                    }

                    JSONObject obj = new JSONObject(raw);

                    ProfileStats stats = new ProfileStats();
                    stats.watchedCount = obj.optInt("watched_count", 0);
                    stats.finishedSessionsCount = obj.optInt("finished_sessions_count", 0);
                    stats.recommendationsCount = obj.optInt("recommendations_count", 0);

                    callback.onSuccess(stats);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Stats load failed");
            }
        });
    }
}