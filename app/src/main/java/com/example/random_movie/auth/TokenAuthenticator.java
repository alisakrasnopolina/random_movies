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

/**
 * @brief Authenticator для автоматического обновления access token.
 *
 * Класс используется OkHttp при получении ответа 401 Unauthorized.
 * Он пытается обновить токены через refresh token и повторить исходный запрос.
 * Если обновление невозможно, выполняется принудительный выход пользователя.
 */
public class TokenAuthenticator implements Authenticator {
    /** Контекст приложения. */
    private final Context appContext;

    /** Менеджер локальной сессии пользователя. */
    private final SessionManager sessionManager;

    /** Вспомогательный API-клиент для auth-запросов. */
    private final AuthApi authApi;

    /**
     * @brief Создает authenticator токенов.
     *
     * Для обновления токенов используется отдельный OkHttpClient
     * без текущего authenticator, чтобы избежать рекурсивных вызовов.
     *
     * @param context контекст приложения
     */
    public TokenAuthenticator(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);

        OkHttpClient refreshClient = new OkHttpClient.Builder().build();
        this.authApi = new AuthApi(refreshClient);
    }

    /**
     * @brief Пытается обновить токены после ответа 401.
     *
     * @param route маршрут запроса
     * @param response ответ, вызвавший аутентификацию
     * @return новый запрос с обновленным access token или null, если повтор невозможен
     * @throws IOException при ошибке сетевого взаимодействия
     */
    @Override
    public Request authenticate(Route route, Response response) throws IOException {
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

    /**
     * @brief Подсчитывает количество предыдущих ответов в цепочке retry.
     *
     * @param response текущий HTTP-ответ
     * @return количество ответов в цепочке
     */
    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) result++;
        return result;
    }

    /**
     * @brief Выполняет принудительный выход пользователя.
     *
     * Очищает локальную сессию, устанавливает флаг force logout
     * и открывает экран входа.
     */
    private void forceLogout() {
        sessionManager.clearSession();
        sessionManager.setForceLogout(true);

        Intent intent = new Intent(appContext, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        appContext.startActivity(intent);
    }
}