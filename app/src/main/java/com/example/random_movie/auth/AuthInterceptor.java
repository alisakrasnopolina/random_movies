package com.example.random_movie.auth;

import android.content.Context;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @brief Interceptor для автоматического добавления access token в запросы.
 *
 * Если access token сохранен в SessionManager, interceptor добавляет
 * заголовок Authorization ко всем исходящим HTTP-запросам.
 */
public class AuthInterceptor implements Interceptor {
    /** Менеджер локальной сессии пользователя. */
    private final SessionManager sessionManager;

    /**
     * @brief Создает interceptor авторизации.
     *
     * @param context контекст приложения
     */
    public AuthInterceptor(Context context) {
        this.sessionManager = new SessionManager(context.getApplicationContext());
    }

    /**
     * @brief Перехватывает HTTP-запрос и добавляет заголовок Authorization.
     *
     * @param chain цепочка interceptor'ов OkHttp
     * @return HTTP-ответ
     * @throws IOException при ошибке сетевого взаимодействия
     */
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String access = sessionManager.getAccessToken();

        if (access == null || access.isEmpty()) {
            return chain.proceed(original);
        }

        Request withAuth = original.newBuilder()
                .header("Authorization", "Bearer " + access)
                .build();

        return chain.proceed(withAuth);
    }
}