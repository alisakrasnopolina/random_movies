package com.example.random_movie.auth;

import com.example.random_movie.BuildConfig;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @brief Вспомогательный класс для auth-запросов к backend API.
 *
 * Класс инкапсулирует сетевые вызовы, связанные с аутентификацией:
 * - обновление токенов;
 * - выход из текущей сессии.
 */
public class AuthApi {
    /** MIME-тип JSON для тела запросов. */
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /** HTTP-клиент для выполнения запросов. */
    private final OkHttpClient client;

    /**
     * @brief Создает экземпляр AuthApi.
     *
     * @param client HTTP-клиент OkHttp
     */
    public AuthApi(OkHttpClient client) {
        this.client = client;
    }

    /**
     * @brief Отправляет запрос на обновление токенов.
     *
     * @param refreshToken refresh token текущей сессии
     * @return HTTP-ответ сервера
     * @throws IOException при ошибке сетевого взаимодействия
     */
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

    /**
     * @brief Отправляет запрос на logout текущей сессии.
     *
     * @param refreshToken refresh token текущей сессии
     * @return HTTP-ответ сервера
     * @throws IOException при ошибке сетевого взаимодействия
     */
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