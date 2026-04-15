package com.example.random_movie.auth;

import com.example.random_movie.BuildConfig;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthApi {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client;

    public AuthApi(OkHttpClient client) {
        this.client = client;
    }

    public Response refresh(String refreshToken) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("refresh_token", refreshToken);
        } catch (Exception ignored) {}

        Request request = new Request.Builder()
                .url(BuildConfig.API_BASE_URL + "/auth/refresh")
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .build();

        return client.newCall(request).execute();
    }

    public Response logout(String refreshToken) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("refresh_token", refreshToken);
        } catch (Exception ignored) {}

        Request request = new Request.Builder()
                .url(BuildConfig.API_BASE_URL + "/auth/logout")
                .post(RequestBody.create(body.toString(), JSON))
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .build();

        return client.newCall(request).execute();
    }
}