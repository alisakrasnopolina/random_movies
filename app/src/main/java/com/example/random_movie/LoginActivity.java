package com.example.random_movie;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.random_movie.auth.ApiClient;
import com.example.random_movie.auth.AuthErrorMapper;
import com.example.random_movie.auth.SessionManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @brief Экран входа пользователя в приложение.
 *
 * Activity отвечает за:
 * - ввод email и пароля;
 * - локальную валидацию полей формы;
 * - отправку запроса на backend для аутентификации;
 * - сохранение access/refresh токенов и данных пользователя;
 * - переход в основную часть приложения после успешного входа.
 */
public class LoginActivity extends AppCompatActivity {

    /** Поле ввода email пользователя. */
    private EditText loginEmail;

    /** Поле ввода пароля пользователя. */
    private EditText loginPassword;

    /** Кнопка отправки формы входа. */
    private Button loginButton;

    /** Кнопка перехода на экран регистрации. */
    private Button signupRedirectButton;

    /** Индикатор загрузки во время сетевого запроса. */
    private ProgressBar loginProgress;

    /** HTTP-клиент для работы с backend API. */
    private OkHttpClient client;

    /** Менеджер локального хранения токенов и данных пользователя. */
    private SessionManager sessionManager;

    /** MIME-тип JSON для тела HTTP-запросов. */
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    /**
     * @brief Инициализирует экран входа.
     *
     * Настраивает элементы интерфейса, создает API-клиент и SessionManager,
     * проверяет признак принудительного выхода, а также выполняет автоматический
     * переход в приложение, если access token уже сохранен.
     *
     * @param savedInstanceState сохраненное состояние Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectButton = findViewById(R.id.loginRedirectButton);
        loginProgress = findViewById(R.id.login_progress);

        client = ApiClient.get(this);
        sessionManager = new SessionManager(this);

        // Если forced logout был из TokenAuthenticator — просто очищаем флаг и остаемся на логине
        if (sessionManager.isForceLogout()) {
            sessionManager.setForceLogout(false);
            Toast.makeText(this, "Сессия истекла, войдите снова", Toast.LENGTH_LONG).show();
        }

        // Если access уже есть — сразу в приложение
        String accessToken = sessionManager.getAccessToken();
        if (accessToken != null && !accessToken.isEmpty()) {
            goToApp();
            return;
        }

        loginButton.setOnClickListener(v -> {
            if (validateEmail() & validatePassword()) {
                loginUser();
            }
        });

        signupRedirectButton.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );
    }

    /**
     * @brief Проверяет корректность email.
     *
     * Метод убеждается, что поле не пустое и соответствует стандартному формату email.
     *
     * @return true, если email заполнен и корректен; иначе false
     */
    private boolean validateEmail() {
        String val = loginEmail.getText().toString().trim();
        if (val.isEmpty()) {
            loginEmail.setError("Email cannot be empty");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(val).matches()) {
            loginEmail.setError("Invalid email format");
            return false;
        }
        loginEmail.setError(null);
        return true;
    }

    /**
     * @brief Проверяет корректность пароля.
     *
     * Метод проверяет, что пароль не пустой и содержит не менее 8 символов.
     *
     * @return true, если пароль соответствует требованиям; иначе false
     */
    private boolean validatePassword() {
        String val = loginPassword.getText().toString().trim();
        if (val.isEmpty()) {
            loginPassword.setError("Password cannot be empty");
            return false;
        }
        if (val.length() < 8) {
            loginPassword.setError("Password must be at least 8 characters");
            return false;
        }
        loginPassword.setError(null);
        return true;
    }

    /**
     * @brief Переключает экран в режим загрузки или обычный режим.
     *
     * При загрузке блокирует кнопки и показывает ProgressBar.
     *
     * @param loading признак активного запроса
     */
    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        signupRedirectButton.setEnabled(!loading);
        loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    /**
     * @brief Выполняет вход пользователя через backend API.
     *
     * Формирует JSON-запрос с email и паролем, отправляет POST-запрос на
     * endpoint /auth/login, а при успешном ответе сохраняет токены и данные
     * пользователя в SessionManager.
     *
     * В случае ошибки сети показывает уведомление о проблеме с подключением.
     * В случае ошибки сервера выводит сообщение, сопоставленное по HTTP-коду.
     */
    private void loginUser() {
        setLoading(true);

        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("email", loginEmail.getText().toString().trim().toLowerCase());
            bodyJson.put("password", loginPassword.getText().toString().trim());

            RequestBody body = RequestBody.create(bodyJson.toString(), JSON_MEDIA);

            Request request = new Request.Builder()
                    .url(BuildConfig.API_BASE_URL + "/auth/login")
                    .post(body)
                    .addHeader("accept", "application/json")
                    .addHeader("content-type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                /**
                 * @brief Обрабатывает ошибку сетевого запроса.
                 *
                 * @param call объект HTTP-вызова
                 * @param e исключение ввода-вывода
                 */
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Проверьте интернет", Toast.LENGTH_LONG).show();
                    });
                }

                /**
                 * @brief Обрабатывает ответ backend после попытки входа.
                 *
                 * При успешном ответе извлекает access token, refresh token и данные пользователя,
                 * сохраняет их локально и выполняет переход в основную часть приложения.
                 *
                 * @param call объект HTTP-вызова
                 * @param response HTTP-ответ сервера
                 * @throws IOException при ошибке чтения тела ответа
                 */
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        setLoading(false);

                        if (response.isSuccessful()) {
                            try {
                                JSONObject obj = new JSONObject(responseBody);

                                String accessToken = obj.getString("access_token");
                                String refreshToken = obj.getString("refresh_token");

                                JSONObject user = obj.getJSONObject("user");
                                String userId = user.optString("id", "");
                                String email = user.optString("email", "");
                                String displayName = user.optString("display_name", "");

                                sessionManager.saveTokens(accessToken, refreshToken);
                                sessionManager.saveUser(userId, email, displayName);

                                com.example.random_movie.sync.SyncScheduler.enqueuePeriodic(getApplicationContext());
                                com.example.random_movie.sync.SyncScheduler.enqueueOneTime(getApplicationContext());

                                // Для совместимости со старой частью приложения (если где-то еще читается "login")
//                                getSharedPreferences("login", MODE_PRIVATE).edit()
//                                        .putString("remember", "true")
//                                        .putString("name", displayName)
//                                        .putString("email", email)
//                                        .apply();

                                Toast.makeText(LoginActivity.this, "Вход выполнен", Toast.LENGTH_SHORT).show();
                                goToApp();

                            } catch (Exception e) {
                                Toast.makeText(LoginActivity.this, "Ошибка обработки ответа", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            String serverMessage = extractServerMessage(responseBody);
                            String msg = AuthErrorMapper.mapByStatus(response.code(), serverMessage);
                            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            setLoading(false);
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * @brief Извлекает сообщение об ошибке из JSON-ответа сервера.
     *
     * Пытается прочитать поля {@code message} или {@code detail}.
     *
     * @param responseBody тело ответа сервера
     * @return текст ошибки или пустая строка, если извлечь сообщение не удалось
     */
    private String extractServerMessage(String responseBody) {
        try {
            JSONObject err = new JSONObject(responseBody);
            if (err.has("message")) return err.getString("message");
            if (err.has("detail")) return err.getString("detail");
        } catch (Exception ignored) {}
        return "";
    }

    /**
     * @brief Выполняет переход в основную часть приложения.
     *
     * Очищает стек Activity и открывает экран случайного выбора фильма.
     */
    private void goToApp() {
        Intent intent = new Intent(LoginActivity.this, FindRandomMovie.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}