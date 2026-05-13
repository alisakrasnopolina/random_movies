package com.example.random_movie;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public final class NavigationBar {

    private NavigationBar() {
    }

    public static void setup(Activity activity, int selectedItemId) {
        BottomNavigationView bottomNavigationView = activity.findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView == null) return;

        bottomNavigationView.setSelectedItemId(selectedItemId);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == selectedItemId) return true;

            Intent intent = null;

            if (id == R.id.random_movie) {
                intent = new Intent(activity, FindRandomMovie.class);
            } else if (id == R.id.liked_movies) {
                intent = new Intent(activity, LikedMoviesActivity.class);
            } else if (id == R.id.with_friend) {
                intent = new Intent(activity, FriendsActivity.class);
            } else if (id == R.id.tips) {
                intent = new Intent(activity, TipsActivity.class);
            } else if (id == R.id.home) {
                intent = new Intent(activity, MainActivity.class);
            }

            if (intent != null) {
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0);
                activity.finish();
                activity.overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });
    }
}