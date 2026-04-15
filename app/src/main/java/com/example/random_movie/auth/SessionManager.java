package com.example.random_movie.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @brief Класс для локального хранения сессии пользователя.
 *
 * SessionManager использует SharedPreferences для сохранения:
 * - access token;
 * - refresh token;
 * - идентификатора пользователя;
 * - email;
 * - отображаемого имени;
 * - флага принудительного выхода из аккаунта.
 */
public class SessionManager {
    /** Имя файла SharedPreferences. */
    private static final String PREF = "auth";

    /** Ключ для access token. */
    private static final String KEY_ACCESS = "access_token";

    /** Ключ для refresh token. */
    private static final String KEY_REFRESH = "refresh_token";

    /** Ключ для идентификатора пользователя. */
    private static final String KEY_USER_ID = "user_id";

    /** Ключ для email пользователя. */
    private static final String KEY_EMAIL = "email";

    /** Ключ для отображаемого имени пользователя. */
    private static final String KEY_DISPLAY_NAME = "display_name";

    /** Ключ для флага принудительного logout. */
    private static final String KEY_FORCE_LOGOUT = "force_logout";

    /** Экземпляр SharedPreferences. */
    private final SharedPreferences prefs;

    /**
     * @brief Создает менеджер сессии.
     *
     * @param context контекст приложения или Activity
     */
    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /**
     * @brief Сохраняет access и refresh токены.
     *
     * @param access access token
     * @param refresh refresh token
     */
    public void saveTokens(String access, String refresh) {
        prefs.edit()
                .putString(KEY_ACCESS, access)
                .putString(KEY_REFRESH, refresh)
                .apply();
    }

    /**
     * @brief Сохраняет данные пользователя.
     *
     * @param userId идентификатор пользователя
     * @param email email пользователя
     * @param displayName отображаемое имя пользователя
     */
    public void saveUser(String userId, String email, String displayName) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_DISPLAY_NAME, displayName)
                .apply();
    }

    /**
     * @brief Возвращает access token.
     *
     * @return access token или пустая строка
     */
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS, "");
    }

    /**
     * @brief Возвращает refresh token.
     *
     * @return refresh token или пустая строка
     */
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH, "");
    }

    /**
     * @brief Возвращает идентификатор пользователя.
     *
     * @return user id или пустая строка
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    /**
     * @brief Возвращает email пользователя.
     *
     * @return email или пустая строка
     */
    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    /**
     * @brief Возвращает отображаемое имя пользователя.
     *
     * @return display name или пустая строка
     */
    public String getDisplayName() {
        return prefs.getString(KEY_DISPLAY_NAME, "");
    }

    /**
     * @brief Устанавливает флаг принудительного выхода.
     *
     * @param force true, если требуется повторный вход
     */
    public void setForceLogout(boolean force) {
        prefs.edit().putBoolean(KEY_FORCE_LOGOUT, force).apply();
    }

    /**
     * @brief Проверяет, установлен ли флаг принудительного выхода.
     *
     * @return true, если сессия была сброшена и нужен повторный вход
     */
    public boolean isForceLogout() {
        return prefs.getBoolean(KEY_FORCE_LOGOUT, false);
    }

    /**
     * @brief Полностью очищает локальную сессию пользователя.
     */
    public void clearSession() {
        prefs.edit().clear().apply();
    }
}