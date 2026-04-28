package com.example.random_movie;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class FriendPublicProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_public_profile);
    }

    public void onSubscribeClick(View view) {
        MaterialButton button = (MaterialButton) view;
        Object tagValue = button.getTag();
        boolean subscribed = tagValue != null && "subscribed_true".equals(tagValue.toString());

        if (subscribed) {
            button.setTag("subscribed_false");
            button.setText(R.string.subscribe);
        } else {
            button.setTag("subscribed_true");
            button.setText(R.string.unsubscribe);
        }
    }

    public void onBackClick(View view) {
        finish();
    }
}
