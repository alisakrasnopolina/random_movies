package com.example.random_movie;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.random_movie.auth.ApiClient;
import com.example.random_movie.auth.AuthErrorMapper;
import com.example.random_movie.auth.SessionManager;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfileActivity extends AppCompatActivity {

    EditText editName, editEmail, editPassword;
    Button saveButton;
    String nameUser, emailUser, passwordUser, userID;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new SessionManager(this);

        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        saveButton = findViewById(R.id.edit_button);

        showData();

        saveButton.setOnClickListener(view -> saveProfile());
    }

    private void saveProfile() {
        String newName = editName.getText().toString().trim();
        String newEmail = editEmail.getText().toString().trim();
        String newPassword = editPassword.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newEmail.isEmpty()) {
            Toast.makeText(this, "Введите почту", Toast.LENGTH_SHORT).show();
            return;
        }

        saveButton.setEnabled(false);

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("display_name", newName);
                body.put("email", newEmail);

                if (!newPassword.isEmpty()) {
                    body.put("new_password", newPassword);
                }

                Request request = new Request.Builder()
                        .url(BuildConfig.API_BASE_URL + "/profile/me")
                        .patch(RequestBody.create(body.toString(), JSON))
                        .addHeader("accept", "application/json")
                        .addHeader("content-type", "application/json")
                        .build();

                try (Response response = ApiClient.get(EditProfileActivity.this).newCall(request).execute()) {
                    String raw = response.body() != null ? response.body().string() : "";

                    if (!response.isSuccessful()) {
                        String message = AuthErrorMapper.mapByStatus(response.code(), "Не удалось сохранить профиль");

                        runOnUiThread(() -> {
                            saveButton.setEnabled(true);
                            Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    JSONObject obj = new JSONObject(raw);

                    String userId = obj.optString("id", sessionManager.getUserId());
                    String email = obj.optString("email", newEmail);
                    String displayName = obj.optString("display_name", newName);

                    sessionManager.saveUser(userId, email, displayName);

                    runOnUiThread(() -> {
                        Toast.makeText(EditProfileActivity.this, "Профиль сохранён", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    saveButton.setEnabled(true);
                    Toast.makeText(
                            EditProfileActivity.this,
                            "Ошибка сохранения: " + (e.getMessage() != null ? e.getMessage() : "неизвестная ошибка"),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }

    public void showData() {
        Intent intent = getIntent();

        userID = intent.getStringExtra("userID");
        nameUser = intent.getStringExtra("name");
        emailUser = intent.getStringExtra("email");
        passwordUser = intent.getStringExtra("password");

        if (nameUser == null || nameUser.isEmpty()) {
            nameUser = sessionManager.getDisplayName();
        }

        if (emailUser == null || emailUser.isEmpty()) {
            emailUser = sessionManager.getEmail();
        }

        if (passwordUser == null) {
            passwordUser = "";
        }

        editName.setText(nameUser);
        editEmail.setText(emailUser);
        editPassword.setText("");
    }
}
