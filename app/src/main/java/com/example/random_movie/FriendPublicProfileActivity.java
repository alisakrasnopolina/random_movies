package com.example.random_movie;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.random_movie.friends.model.FriendUser;
import com.example.random_movie.friends.FriendsRepository;
import com.example.random_movie.auth.SessionManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.random_movie.profile.ProfileStatsRepository;

public class FriendPublicProfileActivity extends BaseActivity {

    private TextView profileName;
    private View backButton;
    private LinearLayout publicFriendsPreviewContainer;
    private View openAllFriendsButton;
    private Button buttonSubscribe;
    private FriendsRepository friendsRepository;
    private SessionManager sessionManager;

    private String friendUserId;
    private boolean isFriend = false;

    private ImageView friendAvatar;

    private TextView publicWatchedCountText;
    private TextView publicFriendSessionsCountText;
    private TextView publicRecommendationsCountText;
    private ProfileStatsRepository profileStatsRepository;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_public_profile);

        friendsRepository = new FriendsRepository(this);
        sessionManager = new SessionManager(this);
        profileStatsRepository = new ProfileStatsRepository(this);

        friendUserId = getIntent().getStringExtra("user_id");
        friendAvatar = findViewById(R.id.friendAvatar);

        buttonSubscribe = findViewById(R.id.buttonSubscribe);

        profileName = findViewById(R.id.friendName);
        backButton = findViewById(R.id.buttonBack);
        publicFriendsPreviewContainer = findViewById(R.id.publicFriendsPreviewContainer);
        openAllFriendsButton = findViewById(R.id.openAllFriendsButton);
        publicWatchedCountText = findViewById(R.id.publicWatchedCountText);
        publicFriendSessionsCountText = findViewById(R.id.publicFriendSessionsCountText);
        publicRecommendationsCountText = findViewById(R.id.publicRecommendationsCountText);

        setupPublicFriendsPreview();

        String name = getIntent().getStringExtra("name");
        String avatarUrl = getIntent().getStringExtra("avatar_url");

        if (friendAvatar != null) {
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.icon_user)
                        .circleCrop()
                        .into(friendAvatar);
            } else {
                friendAvatar.setImageResource(R.drawable.icon_user);
            }
        }

        if (profileName != null) {
            profileName.setText(name != null ? name : "");
        }

        setupSubscribeButton();
        loadFriendshipStatus();
        loadPublicProfileStats();

        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void setupSubscribeButton() {
        if (buttonSubscribe == null) return;

        String myUserId = sessionManager.getUserId();

        if (friendUserId == null || friendUserId.trim().isEmpty()) {
            buttonSubscribe.setVisibility(View.GONE);
            return;
        }

        if (myUserId != null && myUserId.equals(friendUserId)) {
            buttonSubscribe.setVisibility(View.GONE);
            return;
        }

        buttonSubscribe.setOnClickListener(v -> {
            if (isFriend) {
                removeFriend();
            } else {
                addFriend();
            }
        });
    }

    private void loadFriendshipStatus() {
        if (buttonSubscribe == null) return;
        if (friendUserId == null || friendUserId.trim().isEmpty()) return;

        buttonSubscribe.setEnabled(false);

        friendsRepository.getFriendshipStatus(friendUserId, new FriendsRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                runOnUiThread(() -> {
                    isFriend = data != null && data;
                    updateSubscribeButton();
                    buttonSubscribe.setEnabled(true);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    isFriend = false;
                    updateSubscribeButton();
                    buttonSubscribe.setEnabled(true);
                });
            }
        });
    }

    private void addFriend() {
        if (buttonSubscribe == null) return;

        buttonSubscribe.setEnabled(false);

        friendsRepository.addFriend(friendUserId, new FriendsRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                runOnUiThread(() -> {
                    isFriend = true;
                    updateSubscribeButton();
                    buttonSubscribe.setEnabled(true);
                    Toast.makeText(FriendPublicProfileActivity.this, "Добавлено в друзья", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    buttonSubscribe.setEnabled(true);
                    Toast.makeText(FriendPublicProfileActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void removeFriend() {
        if (buttonSubscribe == null) return;

        buttonSubscribe.setEnabled(false);

        friendsRepository.removeFriend(friendUserId, new FriendsRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                runOnUiThread(() -> {
                    isFriend = false;
                    updateSubscribeButton();
                    buttonSubscribe.setEnabled(true);
                    Toast.makeText(FriendPublicProfileActivity.this, "Удалено из друзей", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    buttonSubscribe.setEnabled(true);
                    Toast.makeText(FriendPublicProfileActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateSubscribeButton() {
        if (buttonSubscribe == null) return;

        if (isFriend) {
            buttonSubscribe.setText("В друзьях");
            buttonSubscribe.setTag("friend_true");
        } else {
            buttonSubscribe.setText("Добавить в друзья");
            buttonSubscribe.setTag("friend_false");
        }
    }

    private void setupPublicFriendsPreview() {
        if (openAllFriendsButton != null) {
            openAllFriendsButton.setOnClickListener(v -> {
                Intent intent = new Intent(FriendPublicProfileActivity.this, FriendsActivity.class);
                intent.putExtra("mode", "view_only");
                intent.putExtra("source", "public_profile");
                intent.putExtra("user_id", friendUserId);
                startActivity(intent);
            });
        }

        loadPublicFriendsPreview();
    }

    private void loadPublicFriendsPreview() {
        if (publicFriendsPreviewContainer == null) return;

        publicFriendsPreviewContainer.removeAllViews();

        if (friendUserId == null || friendUserId.trim().isEmpty()) {
            showEmptyFriendsText("Друзья не найдены");
            return;
        }

        friendsRepository.getUserFriends(friendUserId, new FriendsRepository.Callback<List<FriendUser>>() {
            @Override
            public void onSuccess(List<FriendUser> friends) {
                runOnUiThread(() -> renderPublicFriendsPreview(friends));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showEmptyFriendsText("Не удалось загрузить друзей"));
            }
        });
    }

    private void renderPublicFriendsPreview(List<FriendUser> friends) {
        if (publicFriendsPreviewContainer == null) return;

        publicFriendsPreviewContainer.removeAllViews();

        if (friends == null || friends.isEmpty()) {
            showEmptyFriendsText("Пока нет друзей");
            return;
        }

        int limit = Math.min(3, friends.size());

        for (int i = 0; i < limit; i++) {
            publicFriendsPreviewContainer.addView(createFriendPreviewCard(friends.get(i)));
        }
    }

    private void showEmptyFriendsText(String text) {
        if (publicFriendsPreviewContainer == null) return;

        publicFriendsPreviewContainer.removeAllViews();

        TextView emptyText = new TextView(this);
        emptyText.setText(text);
        emptyText.setTextSize(14);
        emptyText.setTextColor(ContextCompat.getColor(this, R.color.textColor_subtitles));

        publicFriendsPreviewContainer.addView(emptyText);
    }
//
//    private List<FriendUser> getDemoFriends() {
//        List<FriendUser> friends = new ArrayList<>();
//        friends.add(new FriendUser("u_100", "Василий Иванов", ""));
//        friends.add(new FriendUser("u_101", "Анна Петрова", ""));
//        friends.add(new FriendUser("u_102", "Михаил Смирнов", ""));
//        friends.add(new FriendUser("u_103", "Мария Соколова", ""));
//        return friends;
//    }

    private View createFriendPreviewCard(FriendUser friend) {
        MaterialCardView card = new MaterialCardView(this);

        int cardWidth = getFriendPreviewCardWidth();

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                cardWidth,
                dp(122)
        );
        cardParams.setMargins(0, 0, dp(8), 0);
        card.setLayoutParams(cardParams);

        card.setRadius(dp(10));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(androidx.core.content.ContextCompat.getColor(this, R.color.textColor_subtitles));
        card.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(this, android.R.color.transparent));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout content = new LinearLayout(this);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));

        ImageView avatar = new ImageView(this);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        avatar.setLayoutParams(avatarParams);
        if (friend.getAvatarUrl() != null && !friend.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(friend.getAvatarUrl())
                    .placeholder(R.drawable.icon_user)
                    .circleCrop()
                    .into(avatar);
        } else {
            avatar.setImageResource(R.drawable.icon_user);
        }

        MaterialTextView name = new MaterialTextView(this);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nameParams.setMargins(0, dp(8), 0, 0);
        name.setLayoutParams(nameParams);
        name.setText(friend.getName());
        name.setTextSize(16);
        name.setMaxLines(1);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setTypeface(null, Typeface.BOLD);
        name.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.textColor));

        content.addView(avatar);
        content.addView(name);
        card.addView(content);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(FriendPublicProfileActivity.this, FriendPublicProfileActivity.class);
            intent.putExtra("user_id", friend.getId());
            intent.putExtra("name", friend.getName());
            intent.putExtra("avatar_url", friend.getAvatarUrl());
            startActivity(intent);
        });

        return card;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private int getFriendPreviewCardWidth() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        // activity_friend_public_profile имеет paddingHorizontal="24dp"
        int pageHorizontalPadding = dp(48);

        int gapsBetweenThreeCards = dp(16);

        return (screenWidth - pageHorizontalPadding - gapsBetweenThreeCards) / 3;
    }

    private int getColorFromAttr(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void loadPublicProfileStats() {
        if (profileStatsRepository == null) return;

        if (friendUserId == null || friendUserId.trim().isEmpty()) {
            if (publicWatchedCountText != null) publicWatchedCountText.setText("0");
            if (publicFriendSessionsCountText != null) publicFriendSessionsCountText.setText("0");
            if (publicRecommendationsCountText != null) publicRecommendationsCountText.setText("0");
            return;
        }

        profileStatsRepository.getUserStats(friendUserId, new ProfileStatsRepository.Callback() {
            @Override
            public void onSuccess(ProfileStatsRepository.ProfileStats stats) {
                runOnUiThread(() -> {
                    if (publicWatchedCountText != null) {
                        publicWatchedCountText.setText(String.valueOf(stats.watchedCount));
                    }

                    if (publicFriendSessionsCountText != null) {
                        publicFriendSessionsCountText.setText(String.valueOf(stats.finishedSessionsCount));
                    }

                    if (publicRecommendationsCountText != null) {
                        publicRecommendationsCountText.setText(String.valueOf(stats.recommendationsCount));
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (publicWatchedCountText != null) publicWatchedCountText.setText("0");
                    if (publicFriendSessionsCountText != null) publicFriendSessionsCountText.setText("0");
                    if (publicRecommendationsCountText != null) publicRecommendationsCountText.setText("0");
                });
            }
        });
    }
}