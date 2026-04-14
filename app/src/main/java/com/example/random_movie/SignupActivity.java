package com.example.random_movie;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
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

public class SignupActivity extends AppCompatActivity {

    private EditText signupName, signupEmail, signupPassword;
    private Button signupButton, loginRedirectButton;
    private final OkHttpClient client = new OkHttpClient();

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupName = findViewById(R.id.signup_name);
        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        loginRedirectButton = findViewById(R.id.loginRedirectButton);
        signupButton = findViewById(R.id.signup_button);

        signupButton.setOnClickListener(v -> {
            if (validateName() & validateEmail() & validatePassword()) {
                registerUser();
            }
        });

        loginRedirectButton.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private boolean validateName() {
        String val = signupName.getText().toString().trim();
        if (val.isEmpty()) {
            signupName.setError("Name cannot be empty");
            return false;
        }
        signupName.setError(null);
        return true;
    }

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

    private void registerUser() {
        signupButton.setEnabled(false);

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
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        signupButton.setEnabled(true);
                        Toast.makeText(SignupActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        signupButton.setEnabled(true);

                        if (response.isSuccessful()) {
                            Toast.makeText(SignupActivity.this, "Registration successful. Please log in.", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            String msg = "Registration failed";
                            try {
                                JSONObject err = new JSONObject(responseBody);
                                if (err.has("detail")) msg = err.getString("detail");
                            } catch (Exception ignored) {}
                            Toast.makeText(SignupActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            signupButton.setEnabled(true);
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}