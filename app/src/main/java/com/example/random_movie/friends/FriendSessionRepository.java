package com.example.random_movie.friends;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.random_movie.BuildConfig;
import com.example.random_movie.MovieFilters;
import com.example.random_movie.auth.ApiClient;
import com.example.random_movie.friends.model.FriendSessionMovie;
import com.example.random_movie.friends.model.FriendSessionState;

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FriendSessionRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final Context context;
    private final ExecutorService io;

    public FriendSessionRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.io = Executors.newSingleThreadExecutor();
    }

    public void invite(@NonNull String friendId, @NonNull Callback<FriendSessionState> callback) {
        io.execute(() -> {
            try {
                JSONObject body = new JSONObject()
                        .put("friend_user_id", friendId);

                JSONObject response = postJson("/friend-sessions/invite", body);
                callback.onSuccess(parseState(response));
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    public void incoming(@NonNull Callback<List<FriendSessionState>> callback) {
        io.execute(() -> {
            try {
                JSONArray response = getArray("/friend-sessions/incoming");

                List<FriendSessionState> result = new ArrayList<>();
                for (int i = 0; i < response.length(); i++) {
                    JSONObject obj = response.optJSONObject(i);
                    if (obj != null) {
                        result.add(parseState(obj));
                    }
                }

                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    public void accept(@NonNull String sessionId, @NonNull Callback<FriendSessionState> callback) {
        postState("/friend-sessions/" + sessionId + "/accept", new JSONObject(), callback);
    }

    public void reject(@NonNull String sessionId, @NonNull Callback<FriendSessionState> callback) {
        postState("/friend-sessions/" + sessionId + "/reject", new JSONObject(), callback);
    }

    public void applyFilters(
            @NonNull String sessionId,
            @NonNull MovieFilters filters,
            @NonNull Callback<FriendSessionState> callback
    ) {
        io.execute(() -> {
            try {
                JSONObject body = new JSONObject();

                if (filters.genre != null) body.put("genre", filters.genre);
                if (filters.country != null) body.put("country", filters.country);
                if (filters.yearFrom != null) body.put("year_from", filters.yearFrom);
                if (filters.yearTo != null) body.put("year_to", filters.yearTo);
                if (filters.ratingFrom != null) body.put("rating_from", filters.ratingFrom);
                if (filters.ratingTo != null) body.put("rating_to", filters.ratingTo);

                JSONObject response = postJson("/friend-sessions/" + sessionId + "/filters", body);
                callback.onSuccess(parseState(response));
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    public void vote(
            @NonNull String sessionId,
            int movieId,
            boolean liked,
            @NonNull Callback<FriendSessionState> callback
    ) {
        io.execute(() -> {
            try {
                JSONObject body = new JSONObject()
                        .put("movie_id", movieId)
                        .put("liked", liked);

                JSONObject response = postJson("/friend-sessions/" + sessionId + "/vote", body);
                callback.onSuccess(parseState(response));
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    public void state(@NonNull String sessionId, @NonNull Callback<FriendSessionState> callback) {
        io.execute(() -> {
            try {
                JSONObject response = getJson("/friend-sessions/" + sessionId);
                callback.onSuccess(parseState(response));
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    private void postState(
            @NonNull String path,
            @NonNull JSONObject body,
            @NonNull Callback<FriendSessionState> callback
    ) {
        io.execute(() -> {
            try {
                JSONObject response = postJson(path, body);
                callback.onSuccess(parseState(response));
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    private JSONObject postJson(@NonNull String path, @NonNull JSONObject body) throws Exception {
        Request request = new Request.Builder()
                .url(baseUrl() + path)
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .build();

        try (Response response = ApiClient.get(context).newCall(request).execute()) {
            String raw = response.body() != null ? response.body().string() : "{}";

            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + raw);
            }

            return new JSONObject(raw.isEmpty() ? "{}" : raw);
        }
    }

    private JSONObject getJson(@NonNull String path) throws Exception {
        Request request = new Request.Builder()
                .url(baseUrl() + path)
                .get()
                .addHeader("accept", "application/json")
                .build();

        try (Response response = ApiClient.get(context).newCall(request).execute()) {
            String raw = response.body() != null ? response.body().string() : "{}";

            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + raw);
            }

            return new JSONObject(raw.isEmpty() ? "{}" : raw);
        }
    }

    private JSONArray getArray(@NonNull String path) throws Exception {
        Request request = new Request.Builder()
                .url(baseUrl() + path)
                .get()
                .addHeader("accept", "application/json")
                .build();

        try (Response response = ApiClient.get(context).newCall(request).execute()) {
            String raw = response.body() != null ? response.body().string() : "[]";

            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + raw);
            }

            return new JSONArray(raw.isEmpty() ? "[]" : raw);
        }
    }

    private FriendSessionState parseState(JSONObject obj) {
        FriendSessionState state = new FriendSessionState();

        state.setSessionId(obj.optString("session_id", ""));
        state.setOwnerUserId(obj.optString("owner_user_id", ""));
        state.setFriendUserId(obj.optString("friend_user_id", ""));
        JSONObject ownerUser = obj.optJSONObject("owner_user");
        if (ownerUser != null) {
            state.setOwnerDisplayName(ownerUser.optString("display_name", ""));
            state.setOwnerAvatarUrl(ownerUser.optString("avatar_url", ""));
        }

        JSONObject friendUser = obj.optJSONObject("friend_user");
        if (friendUser != null) {
            state.setFriendDisplayName(friendUser.optString("display_name", ""));
            state.setFriendAvatarUrl(friendUser.optString("avatar_url", ""));
        }
        state.setStatus(obj.optString("status", FriendSessionState.STATUS_INVITED));
        state.setOwnerProgress(obj.optInt("owner_progress", 0));
        state.setFriendProgress(obj.optInt("friend_progress", 0));
        state.setOwnerVotes(parseVotes(obj.optJSONObject("owner_votes")));
        state.setFriendVotes(parseVotes(obj.optJSONObject("friend_votes")));

        JSONArray moviesArray = obj.optJSONArray("movies");
        if (moviesArray != null) {
            for (int i = 0; i < moviesArray.length(); i++) {
                JSONObject m = moviesArray.optJSONObject(i);
                if (m == null) continue;

                FriendSessionMovie movie = new FriendSessionMovie();
                movie.setMovieId(m.optInt("movie_id", m.optInt("id", 0)));
                movie.setTitle(m.optString("title", ""));
                movie.setPosterUrl(m.optString("poster_url", ""));
                movie.setGenre(m.optString("genre", ""));
                movie.setYear(m.optInt("year", 0));
                movie.setRuntimeMin(m.optInt("runtime_min", 0));
                movie.setRatingImdb(m.optDouble("rating_imdb", 0.0));
                movie.setMatched(m.optBoolean("matched", false));

                state.getMovies().add(movie);
            }
        }

        return state;
    }

    private String baseUrl() {
        String url = BuildConfig.API_BASE_URL;
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String errorMessage(Throwable t) {
        return (t.getMessage() == null || t.getMessage().trim().isEmpty())
                ? "Unknown error"
                : t.getMessage();
    }

    private Map<String, Boolean> parseVotes(JSONObject obj) {
        Map<String, Boolean> result = new HashMap<>();

        if (obj == null) {
            return result;
        }

        Iterator<String> keys = obj.keys();

        while (keys.hasNext()) {
            String movieId = keys.next();
            result.put(movieId, obj.optBoolean(movieId, false));
        }

        return result;
    }
}