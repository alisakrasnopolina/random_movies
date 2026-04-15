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

public class LoginActivity extends AppCompatActivity {

    private EditText loginEmail, loginPassword;
    private Button loginButton, signupRedirectButton;
    private ProgressBar loginProgress;

    private OkHttpClient client;
    private SessionManager sessionManager;

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

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

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        signupRedirectButton.setEnabled(!loading);
        loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

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
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Проверьте интернет", Toast.LENGTH_LONG).show();
                    });
                }

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

                                // Для совместимости со старой частью приложения (если где-то еще читается "login")
                                getSharedPreferences("login", MODE_PRIVATE).edit()
                                        .putString("remember", "true")
                                        .putString("name", displayName)
                                        .putString("email", email)
                                        .apply();

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

    private String extractServerMessage(String responseBody) {
        try {
            JSONObject err = new JSONObject(responseBody);
            if (err.has("message")) return err.getString("message");
            if (err.has("detail")) return err.getString("detail");
        } catch (Exception ignored) {}
        return "";
    }

    private void goToApp() {
        Intent intent = new Intent(LoginActivity.this, FindRandomMovie.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}