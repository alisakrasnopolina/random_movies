package com.example.random_movie;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PersonalAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_account);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.random_movie) {
                startActivity(new Intent(getApplicationContext(), FindRandomMovie.class));
                finish();
                return true;
            }
            else if(item.getItemId() == R.id.liked_movies) {
                startActivity(new Intent(getApplicationContext(), LikedMoviesActivity.class));
                finish();
                return true;
            }
            else if(item.getItemId() == R.id.home) {
                return true;
            } else {
                return false;
            }
        });
    }
}