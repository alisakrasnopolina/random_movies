package com.example.random_movie;

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
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class FindRandomMovie extends BaseActivity {

    private ImageButton findMovieButton, likeButton;
    private View filterButton;
    private TextView movieName, movieGenre, movieYear, movieLength, movieRating;
    private ShapeableImageView movieImage;
    private CardView movieCard;
    private ProgressBar loadingProgress;

    private MoviesRepository moviesRepository;
    private FavoritesRepository favoritesRepo;
    private SessionManager sessionManager;

    private String userId = "";
    private int id = 0;
    private final MovieFilters currentFilters = new MovieFilters();

    private View emptyStateContainer;

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
        filterButton = findViewById(R.id.filter_button);
        movieCard = findViewById(R.id.movie_card);
        likeButton = findViewById(R.id.like_button);
        loadingProgress = findViewById(R.id.loading_progress);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);

        moviesRepository = new MoviesRepository(this);
        favoritesRepo = new FavoritesRepository(this);
        sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        NavigationBar.setup(this, R.id.random_movie);

        ActivityResultLauncher<Intent> activityResultLaunch = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == 200) {
                            Intent data = result.getData();

                            MovieFilters filters = extractFilters(data);
                            currentFilters.genre = filters.genre;
                            currentFilters.country = filters.country;
                            currentFilters.yearFrom = filters.yearFrom;
                            currentFilters.yearTo = filters.yearTo;
                            currentFilters.ratingFrom = filters.ratingFrom;
                            currentFilters.ratingTo = filters.ratingTo;

                            Log.d("FindRandomMovie", "filters genre=" + encode(String.valueOf(filters.genre)));
                            loadRandomMovie(filters.isEmpty() ? null : filters);
                        } else if (result.getResultCode() == 400) {
                            Log.e("FindRandomMovie", "Filter result code 400");
                        }
                    }
                });

        findMovieButton.setOnClickListener(view -> loadRandomMovie(null));

        filterButton.setOnClickListener(view -> {
            Intent intent = new Intent(FindRandomMovie.this, FilterFindMovieActivity.class);
            intent.putExtra("genre", currentFilters.genre);
            intent.putExtra("country", currentFilters.country);
            intent.putExtra("year_from", currentFilters.yearFrom);
            intent.putExtra("year_to", currentFilters.yearTo);
            intent.putExtra("rating_from", currentFilters.ratingFrom);
            intent.putExtra("rating_to", currentFilters.ratingTo);
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

    private MovieFilters extractFilters(Intent data) {
        MovieFilters filters = new MovieFilters();
        if (data == null) return filters;

        filters.genre = data.getStringExtra("genre");
        filters.country = data.getStringExtra("country");

        if (data.hasExtra("year_from")) {
            filters.yearFrom = data.getIntExtra("year_from", 0);
        }
        if (data.hasExtra("year_to")) {
            filters.yearTo = data.getIntExtra("year_to", 0);
        }
        if (data.hasExtra("rating_from")) {
            filters.ratingFrom = data.getFloatExtra("rating_from", 0f);
        }
        if (data.hasExtra("rating_to")) {
            filters.ratingTo = data.getFloatExtra("rating_to", 0f);
        }

        return filters;
    }

    private void loadRandomMovie(MovieFilters filters) {
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisibility(View.GONE);
        }

        movieCard.setVisibility(View.GONE);
        loadingProgress.setVisibility(View.VISIBLE);

        moviesRepository.getRandomMovie(filters, new MoviesRepository.MovieCallback() {
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
                    movieCard.setVisibility(View.GONE);

                    if (emptyStateContainer != null) {
                        emptyStateContainer.setVisibility(View.VISIBLE);
                    }

                    Toast.makeText(FindRandomMovie.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
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
                                    Toast.makeText(FindRandomMovie.this, message, Toast.LENGTH_LONG).show()
                            );
                        }
                    });
                } else {
                    favoritesRepo.addFavorite(userId, id, new FavoritesRepository.VoidCallback() {
                        @Override
                        public void onDone() {
                            runOnUiThread(() -> {
                                likeButton.setBackgroundResource(R.drawable.liked);
                                Toast.makeText(FindRandomMovie.this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() ->
                                    Toast.makeText(FindRandomMovie.this, message, Toast.LENGTH_LONG).show()
                            );
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        Toast.makeText(FindRandomMovie.this, message, Toast.LENGTH_LONG).show()
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

    private String encode(String value) {
        return value == null ? "null" : value;
    }
}