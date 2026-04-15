package com.example.random_movie.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF = "auth";
    private static final String KEY_ACCESS = "access_token";
    private static final String KEY_REFRESH = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_FORCE_LOGOUT = "force_logout";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void saveTokens(String access, String refresh) {
        prefs.edit()
                .putString(KEY_ACCESS, access)
                .putString(KEY_REFRESH, refresh)
                .apply();
    }

    public void saveUser(String userId, String email, String displayName) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_DISPLAY_NAME, displayName)
                .apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS, "");
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH, "");
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getDisplayName() {
        return prefs.getString(KEY_DISPLAY_NAME, "");
    }

    public void setForceLogout(boolean force) {
        prefs.edit().putBoolean(KEY_FORCE_LOGOUT, force).apply();
    }

    public boolean isForceLogout() {
        return prefs.getBoolean(KEY_FORCE_LOGOUT, false);
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}