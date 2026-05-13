package com.example.random_movie.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.random_movie.BuildConfig;
import com.example.random_movie.MovieFilters;
import com.example.random_movie.data.local.AppDatabase;
import com.example.random_movie.data.local.dao.CachedMovieDao;
import com.example.random_movie.data.local.entity.CachedMovieEntity;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.HttpUrl;
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
        MovieFilters filters = new MovieFilters();
        filters.genre = genre;
        getRandomMovie(filters, callback);
    }

    public void getRandomMovie(MovieFilters filters, @NonNull MovieCallback callback) {
        io.execute(() -> {
            try {
                HttpUrl.Builder urlBuilder = HttpUrl.parse(BuildConfig.API_BASE_URL + "/movies/random")
                        .newBuilder();

                if (filters != null) {
                    if (filters.genre != null && !filters.genre.trim().isEmpty() && !"Все".equalsIgnoreCase(filters.genre.trim())) {
                        urlBuilder.addQueryParameter("genre", filters.genre.trim());
                    }
                    if (filters.country != null && !filters.country.trim().isEmpty()) {
                        urlBuilder.addQueryParameter("country", filters.country.trim());
                    }
                    if (filters.yearFrom != null) {
                        urlBuilder.addQueryParameter("year_from", String.valueOf(filters.yearFrom));
                    }
                    if (filters.yearTo != null) {
                        urlBuilder.addQueryParameter("year_to", String.valueOf(filters.yearTo));
                    }
                    if (filters.ratingFrom != null) {
                        urlBuilder.addQueryParameter("rating_from", String.valueOf(filters.ratingFrom));
                    }
                    if (filters.ratingTo != null) {
                        urlBuilder.addQueryParameter("rating_to", String.valueOf(filters.ratingTo));
                    }
                }

                Request request = new Request.Builder()
                        .url(urlBuilder.build())
                        .get()
                        .addHeader("accept", "application/json")
                        .build();

                Exception lastError = null;
                for (int attempt = 1; attempt <= 3; attempt++) {
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
                    } catch (Exception requestError) {
                        lastError = requestError;
                        if (attempt < 3) {
                            try {
                                Thread.sleep(250L * attempt);
                            } catch (InterruptedException ignored) {
                            }
                        }
                    }
                }

                if (lastError != null) {
                    throw lastError;
                }

            } catch (Exception e) {
                try {
                    CachedMovieEntity cached = cachedMovieDao.getRandomAny();
                    if (cached == null) {
                        cached = cachedMovieDao.getLastMovie();
                    }
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

                    String genre = obj.optString("genre", "—");
                    if (genre == null || genre.trim().isEmpty()) {
                        genre = "—";
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