package com.example.random_movie;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.random_movie.friends.FriendSessionRepository;
import com.example.random_movie.friends.model.FriendSessionMovie;
import com.example.random_movie.friends.model.FriendSessionState;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FriendResultActivity extends AppCompatActivity {

    private FriendSessionRepository repository;
    private String sessionId;

    private LinearLayout movieContainer;
    private MaterialTextView textMatchesNumber;

    private MaterialTextView textPlayerOneLikes;
    private MaterialTextView textPlayerOneDislikes;
    private MaterialTextView textPlayerTwoLikes;
    private MaterialTextView textPlayerTwoDislikes;

    private ImageView imagePlayerOneAvatar;
    private ImageView imagePlayerTwoAvatar;

    private MaterialTextView textPlayerOneName;
    private MaterialTextView textPlayerTwoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_result);

        repository = new FriendSessionRepository(this);
        sessionId = getIntent().getStringExtra("session_id");

        NavigationBar.setup(this, R.id.with_friend);

        movieContainer = findViewById(R.id.movie_container);
        textMatchesNumber = findViewById(R.id.textMatchesNumber);

        textPlayerOneLikes = findViewById(R.id.textPlayerOneLikes);
        textPlayerOneDislikes = findViewById(R.id.textPlayerOneDislikes);
        textPlayerTwoLikes = findViewById(R.id.textPlayerTwoLikes);
        textPlayerTwoDislikes = findViewById(R.id.textPlayerTwoDislikes);

        imagePlayerOneAvatar = findViewById(R.id.imagePlayerOneAvatar);
        imagePlayerTwoAvatar = findViewById(R.id.imagePlayerTwoAvatar);

        textPlayerOneName = findViewById(R.id.textPlayerOneName);
        textPlayerTwoName = findViewById(R.id.textPlayerTwoName);

        loadState();
    }

    private void loadState() {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        repository.state(sessionId, new FriendSessionRepository.Callback<FriendSessionState>() {
            @Override
            public void onSuccess(FriendSessionState data) {
                runOnUiThread(() -> bindState(data));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (movieContainer != null) {
                        movieContainer.removeAllViews();
                    }
                });
            }
        });
    }

    private void bindState(FriendSessionState state) {
        if (movieContainer == null || state == null) {
            return;
        }

        movieContainer.removeAllViews();

        List<FriendSessionMovie> movies = state.getMovies() != null
                ? state.getMovies()
                : new ArrayList<>();

        int matchedCount = 0;

        for (FriendSessionMovie movie : movies) {
            if (movie.isMatched()) {
                matchedCount++;
            }
        }

        if (textMatchesNumber != null) {
            textMatchesNumber.setText(String.valueOf(matchedCount));
        }

        updatePlayerStats(state);
        bindPlayers(state);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (FriendSessionMovie movie : movies) {
            View card = inflater.inflate(
                    R.layout.fragment_card_of_liked_movie,
                    movieContainer,
                    false
            );

            bindMovieCard(card, movie);
            movieContainer.addView(card);
        }
    }

    private void updatePlayerStats(FriendSessionState state) {
        if (state == null) return;

        int ownerLikes = countVotes(state.getOwnerVotes(), true);
        int ownerDislikes = countVotes(state.getOwnerVotes(), false);

        int friendLikes = countVotes(state.getFriendVotes(), true);
        int friendDislikes = countVotes(state.getFriendVotes(), false);

        if (textPlayerOneLikes != null) {
            textPlayerOneLikes.setText(String.valueOf(ownerLikes));
        }

        if (textPlayerOneDislikes != null) {
            textPlayerOneDislikes.setText(String.valueOf(ownerDislikes));
        }

        if (textPlayerTwoLikes != null) {
            textPlayerTwoLikes.setText(String.valueOf(friendLikes));
        }

        if (textPlayerTwoDislikes != null) {
            textPlayerTwoDislikes.setText(String.valueOf(friendDislikes));
        }
    }

    private int countVotes(Map<String, Boolean> votes, boolean expectedValue) {
        if (votes == null) return 0;

        int count = 0;

        for (Boolean value : votes.values()) {
            if (value != null && value == expectedValue) {
                count++;
            }
        }

        return count;
    }

    private void bindMovieCard(View card, FriendSessionMovie movie) {
        ImageView poster = card.findViewById(R.id.image_card_liked);
        TextView name = card.findViewById(R.id.movie_name);
        TextView genre = card.findViewById(R.id.movie_genre);
        TextView year = card.findViewById(R.id.movie_year);
        TextView length = card.findViewById(R.id.movie_length);
        TextView rate = card.findViewById(R.id.movie_rate);
        View overlay = card.findViewById(R.id.unmatchedOverlay);

        if (poster != null) {
            Glide.with(this)
                    .load(movie.getPosterUrl())
                    .placeholder(R.drawable.joy)
                    .error(R.drawable.joy)
                    .centerCrop()
                    .into(poster);
        }

        if (name != null) {
            name.setText(nullToDash(movie.getTitle()));
        }

        if (genre != null) {
            genre.setText(nullToDash(movie.getGenre()));
        }

        if (year != null) {
            year.setText(movie.getYear() > 0 ? String.valueOf(movie.getYear()) : "—");
        }

        if (length != null) {
            length.setText(movie.getRuntimeMin() > 0
                    ? movie.getRuntimeMin() + " мин"
                    : "—");
        }

        if (rate != null) {
            rate.setText(movie.getRatingImdb() > 0
                    ? String.valueOf(movie.getRatingImdb())
                    : "—");
        }

        if (overlay != null) {
            overlay.setVisibility(movie.isMatched() ? View.GONE : View.VISIBLE);
        }

        card.setOnClickListener(v -> {
            Intent intent = new Intent(FriendResultActivity.this, MovieCardActivity.class);
            intent.putExtra("id", String.valueOf(movie.getMovieId()));
            startActivity(intent);
        });
    }

    private String nullToDash(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value;
    }

    private void bindPlayers(FriendSessionState state) {
        if (textPlayerOneName != null) {
            textPlayerOneName.setText(nullToDash(state.getOwnerDisplayName()));
        }

        if (textPlayerTwoName != null) {
            textPlayerTwoName.setText(nullToDash(state.getFriendDisplayName()));
        }

        loadAvatar(imagePlayerOneAvatar, state.getOwnerAvatarUrl());
        loadAvatar(imagePlayerTwoAvatar, state.getFriendAvatarUrl());
    }

    private void loadAvatar(ImageView imageView, String avatarUrl) {
        if (imageView == null) return;

        imageView.clearColorFilter();

        if (avatarUrl != null
                && !avatarUrl.trim().isEmpty()
                && !"null".equalsIgnoreCase(avatarUrl.trim())) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.icon_user)
                    .error(R.drawable.icon_user)
                    .circleCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.icon_user);
        }
    }
}