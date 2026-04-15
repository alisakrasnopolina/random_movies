package com.example.random_movie.auth;

import android.app.Activity;
import android.content.Intent;

import com.example.random_movie.LoginActivity;

/**
 * @brief Вспомогательный класс для выхода пользователя из аккаунта.
 *
 * Класс поддерживает:
 * - выход только с текущего устройства;
 * - выход со всех устройств пользователя.
 *
 * В обоих случаях локальная сессия очищается независимо от результата
 * сетевого запроса, чтобы обеспечить корректный logout на клиенте.
 */
public class LogoutHelper {

    /**
     * @brief Выполняет выход пользователя с текущего устройства.
     *
     * Отправляет запрос на backend endpoint /auth/logout, затем очищает
     * локальную сессию и переводит пользователя на экран входа.
     *
     * @param activity текущая Activity
     * @param onUiBeforeRedirect действие, выполняемое на UI перед редиректом
     */
    public static void logout(Activity activity, Runnable onUiBeforeRedirect) {
        SessionManager sm = new SessionManager(activity);
        String refresh = sm.getRefreshToken();

        new Thread(() -> {
            try {
                AuthApi authApi = new AuthApi(new okhttp3.OkHttpClient());
                if (refresh != null && !refresh.isEmpty()) {
                    authApi.logout(refresh).close();
                }
            } catch (Exception ignored) {
            } finally {
                activity.runOnUiThread(() -> {
                    if (onUiBeforeRedirect != null) onUiBeforeRedirect.run();

                    sm.clearSession();

                    Intent intent = new Intent(activity, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                    activity.finish();
                });
            }
        }).start();
    }

    /**
     * @brief Выполняет выход пользователя со всех устройств.
     *
     * Отправляет запрос на backend endpoint /auth/logout-all, затем очищает
     * локальную сессию и переводит пользователя на экран входа.
     *
     * @param activity текущая Activity
     * @param onUiBeforeRedirect действие, выполняемое на UI перед редиректом
     */
    public static void logoutAllDevices(Activity activity, Runnable onUiBeforeRedirect) {
        SessionManager sm = new SessionManager(activity);
        String refresh = sm.getRefreshToken();

        new Thread(() -> {
            try {
                if (refresh != null && !refresh.isEmpty()) {
                    org.json.JSONObject body = new org.json.JSONObject();
                    body.put("refresh_token", refresh);

                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(com.example.random_movie.BuildConfig.API_BASE_URL + "/auth/logout-all")
                            .post(okhttp3.RequestBody.create(
                                    body.toString(),
                                    okhttp3.MediaType.parse("application/json; charset=utf-8")
                            ))
                            .addHeader("accept", "application/json")
                            .addHeader("content-type", "application/json")
                            .build();

                    new okhttp3.OkHttpClient().newCall(request).execute().close();
                }
            } catch (Exception ignored) {
            } finally {
                activity.runOnUiThread(() -> {
                    if (onUiBeforeRedirect != null) onUiBeforeRedirect.run();

                    sm.clearSession();

                    Intent intent = new Intent(activity, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                    activity.finish();
                });
            }
        }).start();
    }
}