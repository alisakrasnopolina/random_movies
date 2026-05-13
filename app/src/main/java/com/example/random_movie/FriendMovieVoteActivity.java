package com.example.random_movie;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.random_movie.auth.SessionManager;
import com.example.random_movie.friends.FriendSessionRepository;
import com.example.random_movie.friends.model.FriendSessionMovie;
import com.example.random_movie.friends.model.FriendSessionState;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.Locale;

public class FriendMovieVoteActivity extends AppCompatActivity {

    private FriendSessionRepository repository;
    private SessionManager sessionManager;

    private String sessionId;
    private FriendSessionState state;
    private int currentIndex = 0;

    private MaterialTextView textStepNumber;
    private ImageView imageOwnerAvatar;
    private ImageView imageFriendAvatar;
    private MaterialTextView textFriendName;

    private ImageView imageMovie;
    private MaterialTextView movieName;
    private MaterialTextView movieGenre;
    private MaterialTextView movieYear;
    private MaterialTextView movieLength;
    private MaterialTextView movieRate;

    private ImageButton likeImageButton;
    private FloatingActionButton buttonLike;
    private FloatingActionButton buttonDislike;
    private View movieCard;
    private LinearLayout waitingContainer;
    private MaterialTextView textWaiting;
    private View actionButtons;

    private GestureDetector gestureDetector;

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    private boolean isVoting = false;
    private boolean resultsOpened = false;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadState();
            handler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_movie_vote);

        repository = new FriendSessionRepository(this);
        sessionManager = new SessionManager(this);

        NavigationBar.setup(this, R.id.with_friend);

        sessionId = getIntent().getStringExtra("session_id");
        if (sessionId == null || sessionId.trim().isEmpty()) {
            Toast.makeText(this, "Не передан session_id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupClicks();
        setupSwipe();
        loadState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(pollRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(pollRunnable);
    }

    private boolean isOwner() {
        String userId = sessionManager.getUserId();
        return state != null
                && userId != null
                && userId.equals(state.getOwnerUserId());
    }

    private int getMyProgress() {
        if (state == null) return 0;
        return isOwner() ? state.getOwnerProgress() : state.getFriendProgress();
    }

    private void bindViews() {
        textStepNumber = findViewById(R.id.textStepNumber);

        imageOwnerAvatar = findViewById(R.id.imageOwnerAvatar);
        imageFriendAvatar = findViewById(R.id.imageFriendAvatar);
        textFriendName = findViewById(R.id.textFriendName);

        imageMovie = findViewById(R.id.image_movie);
        movieName = findViewById(R.id.movie_name);
        movieGenre = findViewById(R.id.movie_genre);
        movieYear = findViewById(R.id.movie_year);
        movieLength = findViewById(R.id.movie_length);
        movieRate = findViewById(R.id.movie_rate);

        likeImageButton = findViewById(R.id.like_button);
        buttonLike = findViewById(R.id.buttonLike);
        buttonDislike = findViewById(R.id.buttonDislike);

        movieCard = findViewById(R.id.movieCard);
        waitingContainer = findViewById(R.id.waitingContainer);
        textWaiting = findViewById(R.id.textWaiting);
        actionButtons = findViewById(R.id.actionButtons);
    }

    private void setupClicks() {
        if (buttonLike != null) {
            buttonLike.setOnClickListener(v -> animateChoice(true));
        }
        if (buttonDislike != null) {
            buttonDislike.setOnClickListener(v -> animateChoice(false));
        }
        if (likeImageButton != null) {
            likeImageButton.setOnClickListener(v -> animateChoice(true));
        }
    }

//    private void setupSwipe() {
//        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
//            @Override
//            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//                if (e1 == null || e2 == null) return false;
//                if (isVoting) return true;
//
//                float dx = e2.getX() - e1.getX();
//
//                if (Math.abs(dx) < 140f) return false;
//
//                boolean liked = dx > 0;
//                animateChoice(liked);
//                return true;
//            }
//        });
//
//        if (movieCard != null) {
//            movieCard.setOnTouchListener((v, event) -> {
//                gestureDetector.onTouchEvent(event);
//                return true;
//            });
//        }
//    }

    private void setupSwipe() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                openCurrentMovie();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                if (isVoting) return true;

                float dx = e2.getX() - e1.getX();

                if (Math.abs(dx) < 140f) return false;

                boolean liked = dx > 0;
                animateChoice(liked);
                return true;
            }
        });

        if (movieCard != null) {
            movieCard.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return true;
            });
        }
    }

    private void animateChoice(boolean liked) {
        if (movieCard == null || isVoting) return;

        isVoting = true;

        float targetX = liked ? 900f : -900f;
        float rotation = liked ? 18f : -18f;

        movieCard.animate()
                .translationX(targetX)
                .rotation(rotation)
                .alpha(0f)
                .setDuration(220)
                .withEndAction(() -> {
                    movieCard.setTranslationX(0f);
                    movieCard.setRotation(0f);
                    movieCard.setAlpha(1f);
                    sendVote(liked);
                })
                .start();
    }

    private void loadState() {
        repository.state(sessionId, new FriendSessionRepository.Callback<FriendSessionState>() {
            @Override
            public void onSuccess(FriendSessionState data) {
                runOnUiThread(() -> {
                    state = data;

                    if (state == null) {
                        showWaiting("Загружаем сессию...");
                        return;
                    }

                    bindPlayers(state);

                    if (FriendSessionState.STATUS_FINISHED.equalsIgnoreCase(state.getStatus())) {
                        openResults();
                        return;
                    }

                    if (state.getMovies() == null || state.getMovies().isEmpty()) {
                        showWaiting("Ждём, пока друг выберет фильтры...");
                        return;
                    }

                    int total = state.getMovies().size();
                    boolean ownerDone = state.getOwnerProgress() >= total;
                    boolean friendDone = state.getFriendProgress() >= total;

                    if (total > 0 && ownerDone && friendDone) {
                        openResults();
                        return;
                    }

                    int newIndex = getMyProgress();

                    if (!isVoting && (newIndex != currentIndex || movieCard == null || movieCard.getVisibility() != View.VISIBLE)) {
                        currentIndex = newIndex;
                        renderCurrentMovie();
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        Toast.makeText(FriendMovieVoteActivity.this, "Ошибка загрузки: " + message, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void renderCurrentMovie() {
        if (state == null || state.getMovies() == null) return;

        int total = state.getMovies().size();

        boolean ownerDone = state.getOwnerProgress() >= total;
        boolean friendDone = state.getFriendProgress() >= total;

        if (total > 0 && ownerDone && friendDone) {
            openResults();
            return;
        }

        if (currentIndex >= total) {
            showWaiting("Вы проголосовали за все фильмы. Ждём второго пользователя...");
            return;
        }

        showMovieUi();

        FriendSessionMovie movie = state.getMovies().get(currentIndex);

        if (textStepNumber != null) {
            textStepNumber.setText(String.valueOf(currentIndex + 1));
        }

        if (movieName != null) movieName.setText(nullToDash(movie.getTitle()));
        if (movieGenre != null) movieGenre.setText(nullToDash(movie.getGenre()));
        if (movieYear != null) movieYear.setText(movie.getYear() > 0 ? String.valueOf(movie.getYear()) : "—");
        if (movieLength != null) movieLength.setText(movie.getRuntimeMin() > 0 ? movie.getRuntimeMin() + " мин" : "—");
        if (movieRate != null) {
            movieRate.setText(movie.getRatingImdb() > 0
                    ? String.format(Locale.US, "%.1f", movie.getRatingImdb())
                    : "—");
        }

        if (imageMovie != null) {
            Glide.with(this)
                    .load(movie.getPosterUrl())
                    .error(R.drawable.joy)
                    .centerCrop()
                    .into(imageMovie);
        }
    }

    private void sendVote(boolean liked) {
        if (state == null || state.getMovies() == null || currentIndex >= state.getMovies().size()) {
            isVoting = false;
            return;
        }

        int movieId = state.getMovies().get(currentIndex).getMovieId();

        repository.vote(sessionId, movieId, liked, new FriendSessionRepository.Callback<FriendSessionState>() {
            @Override
            public void onSuccess(FriendSessionState data) {
                runOnUiThread(() -> {
                    isVoting = false;
                    state = data;

                    if (FriendSessionState.STATUS_FINISHED.equalsIgnoreCase(state.getStatus())) {
                        openResults();
                        return;
                    }

                    currentIndex = getMyProgress();
                    renderCurrentMovie();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    isVoting = false;

                    if (movieCard != null) {
                        movieCard.setTranslationX(0f);
                        movieCard.setRotation(0f);
                        movieCard.setAlpha(1f);
                    }

                    Toast.makeText(
                            FriendMovieVoteActivity.this,
                            "Ошибка голосования: " + message,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        });
    }

    private void openCurrentMovie() {
        if (state == null || state.getMovies() == null || currentIndex >= state.getMovies().size()) return;

        int movieId = state.getMovies().get(currentIndex).getMovieId();

        Intent intent = new Intent(this, MovieCardActivity.class);
        intent.putExtra("id", String.valueOf(movieId));
        startActivity(intent);
    }

    private void showWaiting(String message) {
        if (waitingContainer != null) waitingContainer.setVisibility(View.VISIBLE);
        if (textWaiting != null) textWaiting.setText(message);
        if (movieCard != null) movieCard.setVisibility(View.GONE);
        if (actionButtons != null) actionButtons.setVisibility(View.GONE);
        if (textStepNumber != null) textStepNumber.setText("—");
    }

    private void showMovieUi() {
        if (waitingContainer != null) waitingContainer.setVisibility(View.GONE);
        if (movieCard != null) movieCard.setVisibility(View.VISIBLE);
        if (actionButtons != null) actionButtons.setVisibility(View.VISIBLE);
    }

    private void openResults() {
        if (resultsOpened) return;
        resultsOpened = true;

        Intent intent = new Intent(this, FriendResultActivity.class);
        intent.putExtra("session_id", sessionId);
        startActivity(intent);
        finish();
    }

    private String nullToDash(String value) {
        return (value == null || value.trim().isEmpty()) ? "—" : value;
    }

    private void bindPlayers(FriendSessionState state) {
        if (state == null) return;

        boolean currentUserIsOwner = isOwner();

        String myAvatarUrl = currentUserIsOwner
                ? state.getOwnerAvatarUrl()
                : state.getFriendAvatarUrl();

        String otherAvatarUrl = currentUserIsOwner
                ? state.getFriendAvatarUrl()
                : state.getOwnerAvatarUrl();

        String otherName = currentUserIsOwner
                ? state.getFriendDisplayName()
                : state.getOwnerDisplayName();

        loadAvatar(imageOwnerAvatar, myAvatarUrl);
        loadAvatar(imageFriendAvatar, otherAvatarUrl);

        if (textFriendName != null) {
            textFriendName.setText(nullToDash(otherName));
        }
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