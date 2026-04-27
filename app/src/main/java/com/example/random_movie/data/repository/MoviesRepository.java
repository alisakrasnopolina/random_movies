package com.example.random_movie.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.random_movie.BuildConfig;
import com.example.random_movie.data.local.AppDatabase;
import com.example.random_movie.data.local.dao.CachedMovieDao;
import com.example.random_movie.data.local.entity.CachedMovieEntity;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MoviesRepository {

    public interface MovieCallback {
        void onSuccess(CachedMovieEntity movie, boolean fromCache);
        void onError(String message);
    }

    private final CachedMovieDao cachedMovieDao;
    private final OkHttpClient client;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public MoviesRepository(@NonNull Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.cachedMovieDao = db.cachedMovieDao();
        this.client = new OkHttpClient();
    }

    public void getRandomMovie(String genre, @NonNull MovieCallback callback) {
        io.execute(() -> {
            try {
                String url = BuildConfig.API_BASE_URL + "/movies/random";
                if (genre != null && !genre.trim().isEmpty()) {
                    url += "?genre=" + URLEncoder.encode(genre.trim(), StandardCharsets.UTF_8);
                }

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("accept", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code());
                    }
                    String body = response.body() != null ? response.body().string() : "{}";
                    JSONObject obj = new JSONObject(body);

                    CachedMovieEntity entity = new CachedMovieEntity();
                    entity.id = obj.optInt("id", 0);
                    entity.title = obj.optString("title", "Unknown");
                    entity.year = obj.optInt("year", 0);
                    entity.posterUrl = obj.optString("poster_url", "");
                    entity.genre = obj.optString("genre", "—");
                    entity.ratingImdb = obj.optDouble("rating_imdb", 0.0);
                    entity.runtimeMin = obj.optInt("runtime_min", 0);
                    entity.updatedAt = System.currentTimeMillis();

                    cachedMovieDao.upsert(entity);
                    callback.onSuccess(entity, false);
                    return;
                }

            } catch (Exception e) {
                // offline fallback
                try {
                    CachedMovieEntity cached = cachedMovieDao.getLastMovie();
                    if (cached != null) {
                        callback.onSuccess(cached, true);
                    } else {
                        callback.onError("Не удалось загрузить фильм (и кэш пуст)");
                    }
                } catch (Exception dbErr) {
                    callback.onError("Ошибка кэша: " + dbErr.getMessage());
                }
            }
        });
    }

    public void getMovieById(int movieId, @NonNull MovieCallback callback) {
        io.execute(() -> {
            try {
                String url = BuildConfig.API_BASE_URL + "/movies/" + movieId;

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("accept", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code());
                    }
                    String body = response.body() != null ? response.body().string() : "{}";
                    JSONObject obj = new JSONObject(body);

                    CachedMovieEntity entity = new CachedMovieEntity();
                    entity.id = obj.optInt("id", movieId);
                    entity.title = obj.optString("title", "Unknown");
                    entity.year = obj.optInt("year", 0);
                    entity.posterUrl = obj.optString("poster_url", "");
                    String genre = "—";
                    if (obj.optJSONArray("genres") != null && obj.optJSONArray("genres").length() > 0) {
                        genre = obj.optJSONArray("genres").optString(0, "—");
                    }
                    entity.genre = genre;
                    entity.ratingImdb = obj.optDouble("rating_imdb", 0.0);
                    entity.runtimeMin = obj.optInt("runtime_min", 0);
                    entity.updatedAt = System.currentTimeMillis();

                    cachedMovieDao.upsert(entity);
                    callback.onSuccess(entity, false);
                    return;
                }

            } catch (Exception e) {
                try {
                    CachedMovieEntity cached = cachedMovieDao.getById(movieId);
                    if (cached != null) {
                        callback.onSuccess(cached, true);
                    } else {
                        callback.onError("Не удалось загрузить фильм (кэш по id пуст)");
                    }
                } catch (Exception dbErr) {
                    callback.onError("Ошибка кэша: " + dbErr.getMessage());
                }
            }
        });
    }
}