package com.example.random_movie;

import static android.net.Uri.encode;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.random_movie.auth.SessionManager;
import com.example.random_movie.data.local.entity.CachedMovieEntity;
import com.example.random_movie.data.repository.FavoritesRepository;
import com.example.random_movie.data.repository.MoviesRepository;
import com.example.random_movie.data.repository.WatchedRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class FindRandomMovie extends AppCompatActivity {

    private ImageButton findMovieButton, likeButton;
    private TextView movieName, movieGenre, movieYear, movieLength, movieRating;
    private ShapeableImageView movieImage;
    private CardView movieCard;
    private ProgressBar loadingProgress;

    private MoviesRepository moviesRepository;
    private FavoritesRepository favoritesRepo;
    private WatchedRepository watchedRepo;
    private SessionManager sessionManager;

    private String userId = "";
    private int id = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_random_movie);

        movieImage = findViewById(R.id.image_movie);
        movieName = findViewById(R.id.movie_name);
        movieGenre = findViewById(R.id.movie_genre);
        movieYear = findViewById(R.id.movie_year);
        movieLength = findViewById(R.id.movie_length);
        movieRating = findViewById(R.id.movie_rate);
        findMovieButton = findViewById(R.id.find_movie_button);
        movieCard = findViewById(R.id.movie_card);
        likeButton = findViewById(R.id.like_button);
        loadingProgress = findViewById(R.id.loading_progress);

        moviesRepository = new MoviesRepository(this);
        favoritesRepo = new FavoritesRepository(this);
        watchedRepo = new WatchedRepository(this);
        sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.random_movie);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.random_movie) return true;
            if (item.getItemId() == R.id.liked_movies) {
                startActivity(new Intent(getApplicationContext(), LikedMoviesActivity.class));
                finish();
                return true;
            }
            if (item.getItemId() == R.id.home) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
                return true;
            }
            return false;
        });

        ActivityResultLauncher<Intent> activityResultLaunch = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == 200) {
                            Intent data = result.getData();
                            String movieGenreFromFilter = data != null ? data.getStringExtra("genre") : null;

                            Log.d("FindRandomMovie", "genre=" + encode(String.valueOf(movieGenreFromFilter)));

                            movieCard.setVisibility(View.INVISIBLE);
                            loadingProgress.setVisibility(View.VISIBLE);

                            moviesRepository.getRandomMovie(movieGenreFromFilter, new MoviesRepository.MovieCallback() {
                                @Override
                                public void onSuccess(CachedMovieEntity movie, boolean fromCache) {
                                    runOnUiThread(() -> {
                                        loadingProgress.setVisibility(View.GONE);
                                        movieCard.setVisibility(View.VISIBLE);

                                        id = movie.id;

                                        Glide.with(FindRandomMovie.this)
                                                .load(movie.posterUrl)
                                                .centerCrop()
                                                .into(movieImage);

                                        movieName.setText(movie.title);
                                        movieGenre.setText(movie.genre);
                                        movieYear.setText("· " + movie.year);
                                        movieLength.setText(" " + movie.runtimeMin);
                                        movieRating.setText(" " + movie.ratingImdb);

                                        refreshLikeIcon();

                                        if (fromCache) {
                                            Toast.makeText(FindRandomMovie.this, "Оффлайн: показан кэш", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                @Override
                                public void onError(String message) {
                                    runOnUiThread(() -> {
                                        loadingProgress.setVisibility(View.GONE);
                                        movieCard.setVisibility(View.INVISIBLE);
                                        Toast.makeText(FindRandomMovie.this, message, Toast.LENGTH_LONG).show();
                                    });
                                }
                            });
                        } else if (result.getResultCode() == 400) {
                            Log.e("FindRandomMovie", "Filter result code 400");
                        }
                    }
                });

        findMovieButton.setOnClickListener(view -> {
            Intent intent = new Intent(FindRandomMovie.this, FilterFindMovieActivity.class);
            activityResultLaunch.launch(intent);
        });

        movieCard.setOnClickListener(view -> {
            if (id == 0) return;
            Intent intent = new Intent(FindRandomMovie.this, MovieCardActivity.class);
            intent.putExtra("id", String.valueOf(id));
            startActivity(intent);
        });

        likeButton.setOnClickListener(view -> toggleLikeCurrentMovie());
    }

    private void toggleLikeCurrentMovie() {
        if (id == 0) return;
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_LONG).show();
            return;
        }

        favoritesRepo.getFavoriteIds(userId, new FavoritesRepository.IdsCallback() {
            @Override
            public void onResult(List<Integer> ids) {
                boolean alreadyLiked = ids.contains(id);

                if (alreadyLiked) {
                    favoritesRepo.removeFavorite(userId, id, new FavoritesRepository.VoidCallback() {
                        @Override
                        public void onDone() {
                            runOnUiThread(() -> {
                                likeButton.setBackgroundResource(R.drawable.like);
                                Toast.makeText(FindRandomMovie.this, "Удалено из избранного", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() ->
                                    Toast.makeText(FindRandomMovie.this, "Ошибка: " + message, Toast.LENGTH_LONG).show()
                            );
                        }
                    });
                } else {
                    favoritesRepo.addFavorite(userId, id, new FavoritesRepository.VoidCallback() {
                        @Override
                        public void onDone() {
                            // если фильм был в watched, можно убрать
                            watchedRepo.removeWatched(userId, id, new WatchedRepository.VoidCallback() {
                                @Override
                                public void onDone() {
                                    runOnUiThread(() -> {
                                        likeButton.setBackgroundResource(R.drawable.liked);
                                        Toast.makeText(FindRandomMovie.this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onError(String message) {
                                    runOnUiThread(() -> {
                                        likeButton.setBackgroundResource(R.drawable.liked);
                                        Toast.makeText(FindRandomMovie.this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() ->
                                    Toast.makeText(FindRandomMovie.this, "Ошибка: " + message, Toast.LENGTH_LONG).show()
                            );
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        Toast.makeText(FindRandomMovie.this, "Ошибка: " + message, Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void refreshLikeIcon() {
        if (id == 0 || userId == null || userId.isEmpty()) {
            likeButton.setBackgroundResource(R.drawable.like);
            return;
        }

        favoritesRepo.getFavoriteIds(userId, new FavoritesRepository.IdsCallback() {
            @Override
            public void onResult(List<Integer> ids) {
                boolean liked = ids.contains(id);
                runOnUiThread(() ->
                        likeButton.setBackgroundResource(liked ? R.drawable.liked : R.drawable.like)
                );
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        likeButton.setBackgroundResource(R.drawable.like)
                );
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshLikeIcon();
    }
}