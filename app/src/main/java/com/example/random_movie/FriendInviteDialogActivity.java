package com.example.random_movie;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.random_movie.friends.FriendSessionRepository;
import com.example.random_movie.friends.model.FriendSessionState;
import com.google.android.material.button.MaterialButton;

public class FriendInviteDialogActivity extends AppCompatActivity {

    private FriendSessionRepository repository;
    private String sessionId;

    private MaterialButton buttonAccept;
    private MaterialButton buttonDecline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_invite_friend);

        repository = new FriendSessionRepository(this);
        sessionId = getIntent().getStringExtra("session_id");

        if (sessionId == null || sessionId.trim().isEmpty()) {
            Toast.makeText(this, "Некорректное приглашение", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        buttonAccept = findViewById(R.id.buttonAccept);
        buttonDecline = findViewById(R.id.buttonDecline);

        buttonAccept.setOnClickListener(v -> acceptInvite());
        buttonDecline.setOnClickListener(v -> rejectInvite());
    }

    private void acceptInvite() {
        buttonAccept.setEnabled(false);
        buttonDecline.setEnabled(false);

        repository.accept(sessionId, new FriendSessionRepository.Callback<FriendSessionState>() {
            @Override
            public void onSuccess(FriendSessionState data) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(FriendInviteDialogActivity.this, FriendMovieVoteActivity.class);
                    intent.putExtra("session_id", sessionId);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(FriendInviteDialogActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void rejectInvite() {
        buttonAccept.setEnabled(false);
        buttonDecline.setEnabled(false);

        repository.reject(sessionId, new FriendSessionRepository.Callback<FriendSessionState>() {
            @Override
            public void onSuccess(FriendSessionState data) {
                runOnUiThread(() -> {
                    Toast.makeText(FriendInviteDialogActivity.this, "Приглашение отклонено", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(FriendInviteDialogActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }
}