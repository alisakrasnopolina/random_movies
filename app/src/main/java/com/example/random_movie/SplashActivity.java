package com.example.random_movie;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.random_movie.auth.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        String token = sessionManager.getAccessToken();

        Intent intent;

        if (token != null && !token.trim().isEmpty()) {
            intent = new Intent(this, FindRandomMovie.class);
        } else {
            intent = new Intent(this, SignupActivity.class);
        }

        startActivity(intent);
        finish();
    }
}