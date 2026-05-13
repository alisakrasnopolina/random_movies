package com.example.random_movie;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.random_movie.friends.FriendsRepository;
import com.example.random_movie.friends.model.FriendSessionState;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.random_movie.auth.SessionManager;
import com.example.random_movie.friends.FriendSessionRepository;
import com.example.random_movie.friends.model.FriendUser;

import java.util.ArrayList;
import java.util.List;

import com.bumptech.glide.Glide;

public class FriendsActivity extends BaseActivity {

    private final List<FriendUser> allFriends = new ArrayList<>();
    private final List<FriendUser> filtered = new ArrayList<>();

    private SessionManager sessionManager;
    private FriendSessionRepository repository;
    private FriendsRepository friendsRepository;

    private EditText editSearch;
    private LinearLayout friendsContainer;
    private boolean viewOnlyMode;

    private String source;
    private String targetUserId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        viewOnlyMode = "view_only".equals(getIntent().getStringExtra("mode"));
        source = getIntent().getStringExtra("source");
        targetUserId = getIntent().getStringExtra("user_id");

        NavigationBar.setup(this, R.id.with_friend);

        View buttonBack = findViewById(R.id.buttonBack);
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }

        sessionManager = new SessionManager(this);
        repository = new FriendSessionRepository(this);
        friendsRepository = new FriendsRepository(this);

        editSearch = findViewById(R.id.editSearch);
        friendsContainer = findViewById(R.id.friendsContainer);

        loadFriends();

        if (editSearch != null) {
            editSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilter(s == null ? "" : s.toString());
                }
            });
        }
    }

    private void loadFriends() {
        FriendsRepository.Callback<List<FriendUser>> callback = new FriendsRepository.Callback<List<FriendUser>>() {
            @Override
            public void onSuccess(List<FriendUser> friends) {
                runOnUiThread(() -> {
                    allFriends.clear();
                    if (friends != null) {
                        allFriends.addAll(friends);
                    }
                    applyFilter(editSearch != null ? editSearch.getText().toString() : "");
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(FriendsActivity.this, "Ошибка загрузки друзей", Toast.LENGTH_SHORT).show();
                    allFriends.clear();
                    applyFilter("");
                });
            }
        };

        if ("public_profile".equals(source) && targetUserId != null && !targetUserId.trim().isEmpty()) {
            friendsRepository.getUserFriends(targetUserId, callback);
        } else {
            friendsRepository.getMyFriends(callback);
        }
    }

    private void applyFilter(String query) {
        filtered.clear();
        String q = query == null ? "" : query.trim().toLowerCase();

        for (FriendUser f : allFriends) {
            String name = f.getName() == null ? "" : f.getName().toLowerCase();
            if (q.isEmpty() || name.contains(q)) {
                filtered.add(f);
            }
        }
        renderFriends();
    }

    private void renderFriends() {
        if (friendsContainer == null) return;

        friendsContainer.removeAllViews();

        for (FriendUser friend : filtered) {
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, dp(10));
            card.setLayoutParams(cardParams);
            card.setRadius(dp(8));
            card.setCardElevation(dp(4));
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(12), dp(12), dp(12));
            row.setMinimumHeight(dp(72));

            ImageView avatar = new ImageView(this);
            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(48), dp(48));
            avatar.setLayoutParams(avatarParams);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);

            String avatarUrl = friend.getAvatarUrl();

            if (avatarUrl != null
                    && !avatarUrl.trim().isEmpty()
                    && !"null".equalsIgnoreCase(avatarUrl.trim())) {
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.icon_user)
                        .error(R.drawable.icon_user)
                        .circleCrop()
                        .into(avatar);
            } else {
                avatar.setImageResource(R.drawable.icon_user);
            }

            MaterialTextView name = new MaterialTextView(this);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );
            nameParams.setMargins(dp(12), 0, 0, 0);
            name.setLayoutParams(nameParams);
            name.setText(friend.getName());
            name.setTextSize(16);
            name.setTextColor(getColor(R.color.textColor));
            name.setTypeface(null, android.graphics.Typeface.BOLD);

            ImageView arrow = new ImageView(this);
            arrow.setLayoutParams(new LinearLayout.LayoutParams(dp(16), dp(16)));
            arrow.setImageResource(R.drawable.arrow_right);

            row.addView(avatar);
            row.addView(name);
            row.addView(arrow);
            card.addView(row);

            card.setOnClickListener(v -> {
                if (viewOnlyMode) {
                    Intent intent = new Intent(FriendsActivity.this, FriendPublicProfileActivity.class);
                    intent.putExtra("user_id", friend.getId());
                    intent.putExtra("name", friend.getName());
                    intent.putExtra("avatar_url", friend.getAvatarUrl());
                    startActivity(intent);
                } else {
                    inviteFriend(friend);
                }
            });

            friendsContainer.addView(card);
        }
    }

    private void inviteFriend(FriendUser friend) {
        if (friend == null || friend.getId() == null || friend.getId().trim().isEmpty()) {
            Toast.makeText(this, "Некорректный друг", Toast.LENGTH_SHORT).show();
            return;
        }

        repository.invite(friend.getId(), new FriendSessionRepository.Callback<FriendSessionState>() {
            @Override
            public void onSuccess(FriendSessionState data) {
                runOnUiThread(() -> {
                    Toast.makeText(FriendsActivity.this, "Приглашение отправлено", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(FriendsActivity.this, WaitingFriendActivity.class);
                    intent.putExtra("session_id", data.getSessionId());
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        Toast.makeText(FriendsActivity.this, "Ошибка приглашения: " + message, Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
//    private void inviteFriend(FriendUser friend) {
//        String ownerId = sessionManager.getUserId();
//        if (ownerId == null || ownerId.trim().isEmpty()) {
//            Toast.makeText(this, "Сначала авторизуйтесь", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        repository.invite(ownerId, friend.getId(), new FriendSessionRepository.Callback<String>() {
//            @Override
//            public void onSuccess(String sessionId) {
//                runOnUiThread(() -> {
//                    Intent intent = new Intent(FriendsActivity.this, FilterFindMovieActivity.class);
//                    intent.putExtra("mode", "friend_session");
//                    intent.putExtra("session_id", sessionId);
//                    intent.putExtra("friend_id", friend.getId());
//                    startActivity(intent);
//                });
//            }
//
//            @Override
//            public void onError(String message) {
//                runOnUiThread(() ->
//                        Toast.makeText(FriendsActivity.this, "Ошибка приглашения: " + message, Toast.LENGTH_LONG).show()
//                );
//            }
//        });
//    }
//    private void inviteFriend(FriendUser friend) {
//        String fakeSessionId = "demo_session_" + System.currentTimeMillis();
//
//        Intent intent = new Intent(FriendsActivity.this, FilterFindMovieActivity.class);
//        intent.putExtra("mode", "friend_session");
//        intent.putExtra("session_id", fakeSessionId);
//        intent.putExtra("friend_id", friend.getId());
//        startActivity(intent);
//    }