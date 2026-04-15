package com.example.random_movie.auth;

public class AuthErrorMapper {
    public static String mapByStatus(int code, String fallback) {
        if (code == 401) return "Сессия истекла, войдите снова";
        if (code == 409) return "Email уже занят";
        if (code == 422) return "Проверьте поля формы";
        if (code == 429) return "Слишком много попыток, попробуйте позже";
        return fallback != null && !fallback.isEmpty() ? fallback : "Ошибка запроса";
    }
}