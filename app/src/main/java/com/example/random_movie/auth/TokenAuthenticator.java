package com.example.random_movie.auth;

import android.content.Context;
import android.content.Intent;

import com.example.random_movie.LoginActivity;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class TokenAuthenticator implements Authenticator {
    private final Context appContext;
    private final SessionManager sessionManager;
    private final AuthApi authApi;

    public TokenAuthenticator(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);

        // отдельный клиент без этого authenticator, чтобы избежать рекурсии
        OkHttpClient refreshClient = new OkHttpClient.Builder().build();
        this.authApi = new AuthApi(refreshClient);
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        // защита от бесконечных циклов ретраев
        if (responseCount(response) >= 2) {
            forceLogout();
            return null;
        }

        String refresh = sessionManager.getRefreshToken();
        if (refresh == null || refresh.isEmpty()) {
            forceLogout();
            return null;
        }

        Response refreshResp = authApi.refresh(refresh);
        if (!refreshResp.isSuccessful()) {
            forceLogout();
            return null;
        }

        String body = refreshResp.body() != null ? refreshResp.body().string() : "";
        try {
            JSONObject obj = new JSONObject(body);
            String newAccess = obj.getString("access_token");
            String newRefresh = obj.getString("refresh_token");
            sessionManager.saveTokens(newAccess, newRefresh);

            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + newAccess)
                    .build();
        } catch (Exception e) {
            forceLogout();
            return null;
        } finally {
            refreshResp.close();
        }
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) result++;
        return result;
    }

    private void forceLogout() {
        sessionManager.clearSession();
        sessionManager.setForceLogout(true);

        Intent intent = new Intent(appContext, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        appContext.startActivity(intent);
    }
}