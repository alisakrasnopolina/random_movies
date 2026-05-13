package com.example.random_movie;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.random_movie.auth.SessionManager;
import com.example.random_movie.friends.FriendInvitePoller;

public abstract class BaseActivity extends AppCompatActivity {

    private FriendInvitePoller invitePoller;
    private SessionManager sessionManager;

    protected boolean shouldPollFriendInvites() {
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        String token = sessionManager.getAccessToken();
        if (shouldPollFriendInvites() && token != null && !token.trim().isEmpty()) {
            invitePoller = new FriendInvitePoller(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (invitePoller != null) {
            invitePoller.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (invitePoller != null) {
            invitePoller.stop();
        }
    }
}