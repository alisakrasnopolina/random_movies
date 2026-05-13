package com.example.random_movie;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.random_movie.friends.FriendSessionRepository;
import com.example.random_movie.friends.model.FriendSessionState;

public class WaitingFriendActivity extends AppCompatActivity {

    private FriendSessionRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private String sessionId;
    private boolean running;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            repository.state(sessionId, new FriendSessionRepository.Callback<FriendSessionState>() {
                @Override
                public void onSuccess(FriendSessionState data) {
                    runOnUiThread(() -> {
                        if (data == null) return;

                        String status = data.getStatus();

                        if (FriendSessionState.STATUS_WAITING.equalsIgnoreCase(status)) {
                            running = false;

                            Intent intent = new Intent(WaitingFriendActivity.this, FilterFindMovieActivity.class);
                            intent.putExtra("mode", "friend_session");
                            intent.putExtra("session_id", sessionId);
                            startActivity(intent);
                            finish();

                        } else if (FriendSessionState.STATUS_REJECTED.equalsIgnoreCase(status)) {
                            running = false;
                            Toast.makeText(WaitingFriendActivity.this, "Друг отказался", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    // Не спамим Toast, просто пробуем дальше
                }
            });

            handler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_friend);

        repository = new FriendSessionRepository(this);
        sessionId = getIntent().getStringExtra("session_id");

        if (sessionId == null || sessionId.trim().isEmpty()) {
            Toast.makeText(this, "Не передан session_id", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        running = true;
        handler.post(pollRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        running = false;
        handler.removeCallbacks(pollRunnable);
    }
}