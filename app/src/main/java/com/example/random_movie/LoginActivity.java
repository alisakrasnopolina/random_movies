package com.example.random_movie;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectButton = findViewById(R.id.loginRedirectButton);

        // если уже есть токен — сразу в приложение
        SharedPreferences authPrefs = getSharedPreferences("auth", MODE_PRIVATE);
        String accessToken = authPrefs.getString("access_token", "");
        if (accessToken != null && !accessToken.isEmpty()) {
            startActivity(new Intent(LoginActivity.this, FindRandomMovie.class));
            finish();
            return;
        }

        loginButton.setOnClickListener(v -> {
            if (validateEmail() & validatePassword()) {
                loginUser();
            }
        });

        signupRedirectButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
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

    private void loginUser() {
        loginButton.setEnabled(false);

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
                        loginButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        loginButton.setEnabled(true);

                        if (response.isSuccessful()) {
                            try {
                                JSONObject obj = new JSONObject(responseBody);

                                String accessToken = obj.getString("access_token");
                                String refreshToken = obj.getString("refresh_token");
                                JSONObject user = obj.getJSONObject("user");

                                String userId = user.optString("id", "");
                                String email = user.optString("email", "");
                                String displayName = user.optString("display_name", "");

                                SharedPreferences authPrefs = getSharedPreferences("auth", MODE_PRIVATE);
                                authPrefs.edit()
                                        .putString("access_token", accessToken)
                                        .putString("refresh_token", refreshToken)
                                        .putString("user_id", userId)
                                        .putString("email", email)
                                        .putString("display_name", displayName)
                                        .apply();

                                // для совместимости со старой частью приложения:
                                SharedPreferences oldLoginPrefs = getSharedPreferences("login", MODE_PRIVATE);
                                oldLoginPrefs.edit()
                                        .putString("remember", "true")
                                        .putString("email", email)
                                        .putString("name", displayName)
                                        .apply();

                                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, FindRandomMovie.class));
                                finish();

                            } catch (Exception e) {
                                Toast.makeText(LoginActivity.this, "Parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            String msg = "Login failed";
                            try {
                                JSONObject err = new JSONObject(responseBody);
                                if (err.has("detail")) msg = err.getString("detail");
                            } catch (Exception ignored) {}
                            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            loginButton.setEnabled(true);
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}