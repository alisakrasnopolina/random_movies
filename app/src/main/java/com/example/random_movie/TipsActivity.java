package com.example.random_movie;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.random_movie.recommendations.RecommendationItem;
import com.example.random_movie.recommendations.RecommendationsRepository;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class TipsActivity extends BaseActivity {

    private LinearLayout recommendationContainer;
    private RecommendationsRepository recommendationsRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips);

        NavigationBar.setup(this, R.id.tips);

        recommendationContainer = findViewById(R.id.recommendation_container);
        recommendationsRepository = new RecommendationsRepository(this);

        loadRecommendations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecommendations();
    }

    private void loadRecommendations() {
        if (recommendationContainer == null) return;

        recommendationsRepository.getRecommendations(new RecommendationsRepository.Callback<List<RecommendationItem>>() {
            @Override
            public void onSuccess(List<RecommendationItem> data) {
                runOnUiThread(() -> renderRecommendations(data));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    recommendationContainer.removeAllViews();
                    Toast.makeText(TipsActivity.this, "Ошибка загрузки советов: " + message, Toast.LENGTH_LONG).show();
                    showEmptyText("Не удалось загрузить советы");
                });
            }
        });
    }

    private void renderRecommendations(List<RecommendationItem> items) {
        if (recommendationContainer == null) return;

        recommendationContainer.removeAllViews();

        if (items == null || items.isEmpty()) {
            showEmptyText("Пока никто не советовал фильмы");
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (RecommendationItem item : items) {
            View card = inflater.inflate(
                    R.layout.fragment_card_of_recommended_movie,
                    recommendationContainer,
                    false
            );

            bindRecommendationCard(card, item);
            recommendationContainer.addView(card);
        }
    }

    private void bindRecommendationCard(View card, RecommendationItem item) {
        ShapeableImageView image = card.findViewById(R.id.image_card_liked);

        TextView movieName = card.findViewById(R.id.movie_name);
        TextView movieGenre = card.findViewById(R.id.movie_genre);
        TextView movieYear = card.findViewById(R.id.movie_year);
        TextView movieLength = card.findViewById(R.id.movie_length);
        TextView movieRate = card.findViewById(R.id.movie_rate);

        TextView userName = card.findViewById(R.id.recommendation_user_name);
        TextView userRating = card.findViewById(R.id.recommendation_user_rating);
        View userRow = card.findViewById(R.id.recommendation_user_row);
        ImageButton openMovieButton = card.findViewById(R.id.like_button);
        ImageView userAvatar = card.findViewById(R.id.recommendation_user_avatar);

        if (image != null) {
            Glide.with(this)
                    .load(item.moviePosterUrl)
                    .centerCrop()
                    .placeholder(R.drawable.joy)
                    .into(image);
        }

        if (userAvatar != null) {
            if (item.userAvatarUrl != null && !item.userAvatarUrl.isEmpty()) {
                Glide.with(this)
                        .load(item.userAvatarUrl)
                        .placeholder(R.drawable.icon_user)
                        .circleCrop()
                        .into(userAvatar);
            } else {
                userAvatar.setImageResource(R.drawable.icon_user);
            }
        }

        if (movieName != null) movieName.setText(item.movieTitle);
        if (movieGenre != null) movieGenre.setText(item.movieGenre != null ? item.movieGenre : "—");
        if (movieYear != null) movieYear.setText(item.movieYear > 0 ? "· " + item.movieYear : "");
        if (movieLength != null) movieLength.setText(item.movieRuntimeMin > 0 ? String.valueOf(item.movieRuntimeMin) : "—");
        if (movieRate != null) movieRate.setText(item.movieRatingImdb > 0 ? String.valueOf(item.movieRatingImdb) : "—");

        if (userName != null) userName.setText(item.userDisplayName);
        if (userRating != null) userRating.setText(String.valueOf(item.userRating));

        View.OnClickListener openMovieListener = v -> {
            Intent intent = new Intent(TipsActivity.this, MovieCardActivity.class);
            intent.putExtra("id", String.valueOf(item.movieId));
            startActivity(intent);
        };

        card.setOnClickListener(openMovieListener);

        if (openMovieButton != null) {
            openMovieButton.setOnClickListener(openMovieListener);
        }

        if (userRow != null) {
            userRow.setOnClickListener(v -> {
                Intent intent = new Intent(TipsActivity.this, FriendPublicProfileActivity.class);
                intent.putExtra("user_id", item.userId);
                intent.putExtra("name", item.userDisplayName);
                intent.putExtra("avatar_url", item.userAvatarUrl);
                startActivity(intent);
            });
        }
    }

    private void showEmptyText(String text) {
        if (recommendationContainer == null) return;

        recommendationContainer.removeAllViews();

        TextView empty = new TextView(this);
        empty.setText(text);
        empty.setTextSize(16);
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setPadding(24, 48, 24, 24);
        recommendationContainer.addView(empty);
    }
}