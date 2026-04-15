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
 * @brief Экран регистрации нового пользователя.
 *
 * Activity отвечает за:
 * - ввод имени, email и пароля;
 * - локальную валидацию регистрационной формы;
 * - отправку запроса на backend для создания учетной записи;
 * - переход на экран входа после успешной регистрации.
 */
public class SignupActivity extends AppCompatActivity {

    /** Поле ввода отображаемого имени. */
    private EditText signupName;

    /** Поле ввода email. */
    private EditText signupEmail;

    /** Поле ввода пароля. */
    private EditText signupPassword;

    /** Кнопка отправки формы регистрации. */
    private Button signupButton;

    /** Кнопка перехода на экран входа. */
    private Button loginRedirectButton;

    /** Индикатор загрузки во время регистрации. */
    private ProgressBar signupProgress;

    /** HTTP-клиент для выполнения запросов к backend API. */
    private OkHttpClient client;

    /** MIME-тип JSON для тела запросов. */
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    /**
     * @brief Инициализирует экран регистрации.
     *
     * @param savedInstanceState сохраненное состояние Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupName = findViewById(R.id.signup_name);
        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        loginRedirectButton = findViewById(R.id.loginRedirectButton);
        signupButton = findViewById(R.id.signup_button);
        signupProgress = findViewById(R.id.signup_progress);

        client = ApiClient.get(this);

        signupButton.setOnClickListener(v -> {
            if (validateName() & validateEmail() & validatePassword()) {
                registerUser();
            }
        });

        loginRedirectButton.setOnClickListener(v ->
                startActivity(new Intent(SignupActivity.this, LoginActivity.class))
        );
    }

    /**
     * @brief Проверяет корректность имени пользователя.
     *
     * @return true, если имя заполнено; иначе false
     */
    private boolean validateName() {
        String val = signupName.getText().toString().trim();
        if (val.isEmpty()) {
            signupName.setError("Name cannot be empty");
            return false;
        }
        signupName.setError(null);
        return true;
    }

    /**
     * @brief Проверяет корректность email.
     *
     * @return true, если email заполнен и соответствует формату; иначе false
     */
    private boolean validateEmail() {
        String val = signupEmail.getText().toString().trim();
        if (val.isEmpty()) {
            signupEmail.setError("Email cannot be empty");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(val).matches()) {
            signupEmail.setError("Invalid email format");
            return false;
        }
        signupEmail.setError(null);
        return true;
    }

    /**
     * @brief Проверяет корректность пароля.
     *
     * @return true, если пароль не пустой и содержит не менее 8 символов; иначе false
     */
    private boolean validatePassword() {
        String val = signupPassword.getText().toString().trim();
        if (val.isEmpty()) {
            signupPassword.setError("Password cannot be empty");
            return false;
        }
        if (val.length() < 8) {
            signupPassword.setError("Password must be at least 8 characters");
            return false;
        }
        signupPassword.setError(null);
        return true;
    }

    /**
     * @brief Переключает экран в режим загрузки или обычный режим.
     *
     * @param loading признак активного сетевого запроса
     */
    private void setLoading(boolean loading) {
        signupButton.setEnabled(!loading);
        loginRedirectButton.setEnabled(!loading);
        signupProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    /**
     * @brief Выполняет регистрацию пользователя через backend API.
     *
     * Отправляет POST-запрос на endpoint /auth/register с email, паролем
     * и отображаемым именем. После успешной регистрации переводит пользователя
     * на экран входа.
     */
    private void registerUser() {
        setLoading(true);

        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("email", signupEmail.getText().toString().trim().toLowerCase());
            bodyJson.put("password", signupPassword.getText().toString().trim());
            bodyJson.put("display_name", signupName.getText().toString().trim());

            RequestBody body = RequestBody.create(bodyJson.toString(), JSON_MEDIA);

            Request request = new Request.Builder()
                    .url(BuildConfig.API_BASE_URL + "/auth/register")
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
                        Toast.makeText(SignupActivity.this, "Проверьте интернет", Toast.LENGTH_LONG).show();
                    });
                }

                /**
                 * @brief Обрабатывает ответ сервера на запрос регистрации.
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
                            Toast.makeText(SignupActivity.this, "Регистрация успешна. Войдите в аккаунт.", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            String serverMessage = extractServerMessage(responseBody);
                            String msg = AuthErrorMapper.mapByStatus(response.code(), serverMessage);
                            Toast.makeText(SignupActivity.this, msg, Toast.LENGTH_LONG).show();
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
     * @brief Извлекает текст ошибки из ответа сервера.
     *
     * @param responseBody тело ответа сервера
     * @return сообщение об ошибке или пустая строка
     */
    private String extractServerMessage(String responseBody) {
        try {
            JSONObject err = new JSONObject(responseBody);
            if (err.has("message")) return err.getString("message");
            if (err.has("detail")) return err.getString("detail");
        } catch (Exception ignored) {}
        return "";
    }
}