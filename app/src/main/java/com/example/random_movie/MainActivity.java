package com.example.random_movie;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.random_movie.auth.LogoutHelper;
import com.example.random_movie.auth.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * @brief Главный экран профиля пользователя.
 *
 * Activity отображает основные данные пользователя, обеспечивает переход
 * к редактированию профиля, навигацию между разделами приложения
 * и выход из учетной записи.
 */
public class MainActivity extends AppCompatActivity {

    /** Текстовое поле с именем пользователя. */
    private TextView profileName;

    /** Текстовое поле с email пользователя. */
    private TextView profileEmail;

    /** Кнопка перехода к редактированию профиля. */
    private Button editProfile;

    /** Кнопка выхода из аккаунта. */
    private Button logoutButton;

    /** Менеджер локальной сессии пользователя. */
    private SessionManager sessionManager;

    /** Имя пользователя. */
    private String name;

    /** Email пользователя. */
    private String email;

    /** Идентификатор пользователя. */
    private String userId;

    /**
     * @brief Инициализирует главный экран профиля.
     *
     * Проверяет наличие access token, загружает данные пользователя из SessionManager,
     * настраивает нижнюю навигацию, кнопку выхода и кнопку редактирования профиля.
     *
     * @param savedInstanceState сохраненное состояние Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        editProfile = findViewById(R.id.edit_button);
        logoutButton = findViewById(R.id.exit_button);

        sessionManager = new SessionManager(this);

        // если нет токена — сразу на логин
        if (sessionManager.getAccessToken() == null || sessionManager.getAccessToken().isEmpty()) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        userId = sessionManager.getUserId();
        name = sessionManager.getDisplayName();
        email = sessionManager.getEmail();

        showAllUserData();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.random_movie) {
                startActivity(new Intent(getApplicationContext(), FindRandomMovie.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.liked_movies) {
                startActivity(new Intent(getApplicationContext(), LikedMoviesActivity.class));
                finish();
                return true;
            } else return item.getItemId() == R.id.home;
        });

        logoutButton.setOnClickListener(v -> {
            logoutButton.setEnabled(false);
            LogoutHelper.logout(
                    MainActivity.this,
                    () -> Toast.makeText(MainActivity.this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
            );
        });

        editProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditProfileActivity.class);
            intent.putExtra("userID", userId);
            intent.putExtra("name", name);
            intent.putExtra("email", email);
            intent.putExtra("password", "");
            startActivity(intent);
        });
    }

    /**
     * @brief Обновляет отображаемые данные пользователя при возврате на экран.
     */
    @Override
    protected void onStart() {
        super.onStart();
        showAllUserData();
    }

    /**
     * @brief Отображает имя и email пользователя на экране профиля.
     */
    private void showAllUserData() {
        profileName.setText(name != null ? name : "");
        profileEmail.setText(email != null ? email : "");
    }
}