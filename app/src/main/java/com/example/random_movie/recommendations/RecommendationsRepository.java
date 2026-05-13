package com.example.random_movie.recommendations;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.random_movie.BuildConfig;
import com.example.random_movie.auth.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RecommendationsRepository {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final Context context;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public RecommendationsRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void recommendMovie(
            int movieId,
            int userRating,
            String movieTitle,
            int movieYear,
            String movieGenre,
            String moviePosterUrl,
            int movieRuntimeMin,
            double movieRatingImdb,
            @NonNull Callback<RecommendationItem> callback
    ) {
        io.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("movie_id", movieId);
                body.put("user_rating", userRating);
                body.put("movie_title", movieTitle != null ? movieTitle : "");
                body.put("movie_year", movieYear > 0 ? movieYear : JSONObject.NULL);
                body.put("movie_genre", movieGenre != null ? movieGenre : JSONObject.NULL);
                body.put("movie_poster_url", moviePosterUrl != null ? moviePosterUrl : JSONObject.NULL);
                body.put("movie_runtime_min", movieRuntimeMin > 0 ? movieRuntimeMin : JSONObject.NULL);
                body.put("movie_rating_imdb", movieRatingImdb > 0 ? movieRatingImdb : JSONObject.NULL);

                Request request = new Request.Builder()
                        .url(BuildConfig.API_BASE_URL + "/recommendations")
                        .post(RequestBody.create(body.toString(), JSON))
                        .addHeader("accept", "application/json")
                        .addHeader("content-type", "application/json")
                        .build();

                try (Response response = ApiClient.get(context).newCall(request).execute()) {
                    String raw = response.body() != null ? response.body().string() : "{}";

                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + raw);
                    }

                    callback.onSuccess(parseItem(new JSONObject(raw)));
                }
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    public void getRecommendations(@NonNull Callback<List<RecommendationItem>> callback) {
        io.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(BuildConfig.API_BASE_URL + "/recommendations")
                        .get()
                        .addHeader("accept", "application/json")
                        .build();

                try (Response response = ApiClient.get(context).newCall(request).execute()) {
                    String raw = response.body() != null ? response.body().string() : "[]";

                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + raw);
                    }

                    JSONArray array = new JSONArray(raw);
                    List<RecommendationItem> result = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        result.add(parseItem(array.getJSONObject(i)));
                    }

                    callback.onSuccess(result);
                }
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    private RecommendationItem parseItem(JSONObject obj) {
        RecommendationItem item = new RecommendationItem();

        item.id = obj.optString("id", "");

        item.userId = obj.optString("user_id", "");
        item.userDisplayName = obj.optString("user_display_name", "");
        item.userAvatarUrl = obj.optString("user_avatar_url", "");

        item.movieId = obj.optInt("movie_id", 0);
        item.movieTitle = obj.optString("movie_title", "");
        item.movieYear = obj.optInt("movie_year", 0);
        item.movieGenre = obj.optString("movie_genre", "—");
        item.moviePosterUrl = obj.optString("movie_poster_url", "");
        item.movieRuntimeMin = obj.optInt("movie_runtime_min", 0);
        item.movieRatingImdb = obj.optDouble("movie_rating_imdb", 0.0);

        item.userRating = obj.optInt("user_rating", 0);
        item.createdAt = obj.optString("created_at", "");

        return item;
    }

    private String errorMessage(Throwable t) {
        return t.getMessage() != null ? t.getMessage() : "Unknown error";
    }
}