package com.example.random_movie.friends;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.example.random_movie.FriendInviteDialogActivity;
import com.example.random_movie.friends.model.FriendSessionState;

import java.util.List;

public class FriendInvitePoller {

    private static final long INTERVAL_MS = 5000;

    private final Activity activity;
    private final FriendSessionRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean running;
    private boolean dialogOpened;

    public FriendInvitePoller(Activity activity) {
        this.activity = activity;
        this.repository = new FriendSessionRepository(activity);
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            repository.incoming(new FriendSessionRepository.Callback<List<FriendSessionState>>() {
                @Override
                public void onSuccess(List<FriendSessionState> data) {
                    activity.runOnUiThread(() -> {
                        if (!running || dialogOpened) return;

                        if (data != null && !data.isEmpty()) {
                            FriendSessionState invite = data.get(0);
                            dialogOpened = true;

                            Intent intent = new Intent(activity, FriendInviteDialogActivity.class);
                            intent.putExtra("session_id", invite.getSessionId());
                            activity.startActivity(intent);
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    // Молча игнорируем, чтобы не спамить Toast
                }
            });

            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    public void start() {
        if (running) return;
        running = true;
        handler.post(pollRunnable);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(pollRunnable);
    }
}