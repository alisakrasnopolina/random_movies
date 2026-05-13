package com.example.random_movie.friends;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.random_movie.BuildConfig;
import com.example.random_movie.auth.ApiClient;
import com.example.random_movie.friends.model.FriendUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Request;
import okhttp3.Response;

public class FriendsRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final Context context;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public FriendsRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void getMyFriends(@NonNull Callback<List<FriendUser>> callback) {
        io.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(BuildConfig.API_BASE_URL + "/friends/me")
                        .get()
                        .addHeader("accept", "application/json")
                        .build();

                try (Response response = ApiClient.get(context).newCall(request).execute()) {
                    String raw = response.body() != null ? response.body().string() : "[]";

                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + raw);
                    }

                    callback.onSuccess(parseFriends(raw));
                }
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    public void getUserFriends(@NonNull String userId, @NonNull Callback<List<FriendUser>> callback) {
        io.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(BuildConfig.API_BASE_URL + "/friends/" + userId + "/friends")
                        .get()
                        .addHeader("accept", "application/json")
                        .build();

                try (Response response = ApiClient.get(context).newCall(request).execute()) {
                    String raw = response.body() != null ? response.body().string() : "[]";

                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + raw);
                    }

                    callback.onSuccess(parseFriends(raw));
                }
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    public void getFriendshipStatus(@NonNull String userId, @NonNull Callback<Boolean> callback) {
        io.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(BuildConfig.API_BASE_URL + "/friends/" + userId + "/status")
                        .get()
                        .addHeader("accept", "application/json")
                        .build();

                try (Response response = ApiClient.get(context).newCall(request).execute()) {
                    String raw = response.body() != null ? response.body().string() : "{}";

                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + raw);
                    }

                    JSONObject obj = new JSONObject(raw);
                    callback.onSuccess(obj.optBoolean("is_friend", false));
                }
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    public void addFriend(@NonNull String userId, @NonNull Callback<Boolean> callback) {
        io.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(BuildConfig.API_BASE_URL + "/friends/" + userId)
                        .post(okhttp3.RequestBody.create("", null))
                        .addHeader("accept", "application/json")
                        .build();

                try (Response response = ApiClient.get(context).newCall(request).execute()) {
                    String raw = response.body() != null ? response.body().string() : "{}";

                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + raw);
                    }

                    JSONObject obj = new JSONObject(raw);
                    callback.onSuccess(obj.optBoolean("is_friend", true));
                }
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    public void removeFriend(@NonNull String userId, @NonNull Callback<Boolean> callback) {
        io.execute(() -> {
            try {
                Request request = new Request.Builder()
                        .url(BuildConfig.API_BASE_URL + "/friends/" + userId)
                        .delete()
                        .addHeader("accept", "application/json")
                        .build();

                try (Response response = ApiClient.get(context).newCall(request).execute()) {
                    String raw = response.body() != null ? response.body().string() : "{}";

                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + raw);
                    }

                    JSONObject obj = new JSONObject(raw);
                    callback.onSuccess(obj.optBoolean("is_friend", false));
                }
            } catch (Exception e) {
                callback.onError(errorMessage(e));
            }
        });
    }

    private List<FriendUser> parseFriends(String raw) throws Exception {
        JSONArray array = new JSONArray(raw);
        List<FriendUser> friends = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            FriendUser friend = new FriendUser(
                    obj.optString("id", ""),
                    obj.optString("display_name", ""),
                    obj.optString("avatar_url", "")
            );

            friends.add(friend);
        }

        return friends;
    }

    private String errorMessage(Throwable t) {
        return t.getMessage() != null ? t.getMessage() : "Unknown error";
    }
}