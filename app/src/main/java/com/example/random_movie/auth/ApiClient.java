package com.example.random_movie.auth;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class ApiClient {
    private static OkHttpClient instance;

    public static OkHttpClient get(Context context) {
        if (instance == null) {
            instance = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(40, TimeUnit.SECONDS)
                    .addInterceptor(new AuthInterceptor(context))
                    .authenticator(new TokenAuthenticator(context))
                    .build();
        }
        return instance;
    }
}