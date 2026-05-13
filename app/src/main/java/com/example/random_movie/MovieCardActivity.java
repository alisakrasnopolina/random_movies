package com.example.random_movie;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.example.random_movie.auth.SessionManager;
import com.example.random_movie.data.repository.FavoritesRepository;
import com.example.random_movie.data.repository.WatchedRepository;
import com.example.random_movie.recommendations.RecommendationsRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MovieCardActivity extends BaseActivity {

    private int id;
    private TextView movieName, movieGenre, movieYear, movieLength, movieRating,
            movieCountries, movieDescription, movieDirector, movieActors;
    private ImageView movieImage;
    private Button likedOrWatchedButton;
    private Button recommendButton;

    private TextView myRatingValue;
    private LinearLayout myRatingStars;
    private int currentUserRating = 0;

    private FavoritesRepository favoritesRepo;
    private WatchedRepository watchedRepo;
    private RecommendationsRepository recommendationsRepository;
    private SessionManager sessionManager;
    private String userId;

    private View myRatingContainer;

    private String currentMovieTitle = "";
    private String currentMovieGenre = "";
    private String currentMoviePosterUrl = "";
    private int currentMovieYear = 0;
    private int currentMovieRuntimeMin = 0;
    private double currentMovieRatingImdb = 0.0;


    private enum CardState {
        WANT_TO_WATCH, WATCHED, NONE
    }

    private CardState currentState = CardState.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_card);

        movieImage = findViewById(R.id.movie_pic);
        movieName = findViewById(R.id.movie_name);
        movieCountries = findViewById(R.id.movie_countries);
        movieGenre = findViewById(R.id.movie_genre);
        movieYear = findViewById(R.id.movie_year);
        movieLength = findViewById(R.id.movie_length);
        movieDescription = findViewById(R.id.movie_description);
        movieDirector = findViewById(R.id.movie_director);
        movieActors = findViewById(R.id.movie_actors);
        movieRating = findViewById(R.id.movie_rate);
        likedOrWatchedButton = findViewById(R.id.liked_or_watched_button);
        recommendButton = findViewById(R.id.RecommendButton);
        myRatingValue = findViewById(R.id.my_rating_value);
        myRatingStars = findViewById(R.id.my_rating_stars);
        myRatingContainer = findViewById(R.id.my_rating_container);

        sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        favoritesRepo = new FavoritesRepository(this);
        watchedRepo = new WatchedRepository(this);
        recommendationsRepository = new RecommendationsRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setElevation(2);
                getSupportActionBar().setTitle("");
            }
        }

        Intent intent = getIntent();
        String movieId = intent.getStringExtra("id");
        if (movieId == null || movieId.isEmpty()) {
            Toast.makeText(this, "movie id is empty", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        id = Integer.parseInt(movieId);

        loadMovieDetails(movieId);
        refreshActionButtonState();

        likedOrWatchedButton.setOnClickListener(v -> handleActionClick());
        if (recommendButton != null) {
            recommendButton.setOnClickListener(v -> recommendCurrentMovie());
        }
        setupMyRatingStars();
        updateRatingUi(0);
        setRatingEnabled(false);
    }

    private void loadMovieDetails(String movieId) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = BuildConfig.API_BASE_URL + "/movies/" + movieId;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("accept", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code() + " " + response.message());
                }
                ResponseBody rb = response.body();
                String responseData = rb != null ? rb.string() : "{}";

                JSONObject jsonObj = new JSONObject(responseData);

                String title = jsonObj.optString("title", "Unknown");
                String posterUrl = jsonObj.optString("poster_url", "");
                int year = jsonObj.optInt("year", 0);
                double ratingImdb = jsonObj.optDouble("rating_imdb", 0.0);
                int runtime = jsonObj.optInt("runtime_min", 0);
                String description = jsonObj.optString("description", "");
                String director = jsonObj.optString("director", "—");

                JSONArray genresArr = jsonObj.optJSONArray("genres");
                List<String> genresList = new ArrayList<>();
                if (genresArr != null) {
                    for (int i = 0; i < genresArr.length(); i++) {
                        genresList.add(genresArr.optString(i));
                    }
                }
                String allGenres = String.join(", ", genresList);

                JSONArray countriesArr = jsonObj.optJSONArray("countries");
                List<String> countriesList = new ArrayList<>();
                if (countriesArr != null) {
                    for (int i = 0; i < countriesArr.length(); i++) {
                        countriesList.add(countriesArr.optString(i));
                    }
                }
                String allCountries = String.join(", ", countriesList);

                JSONArray actorsArr = jsonObj.optJSONArray("actors");
                List<String> actorsList = new ArrayList<>();
                if (actorsArr != null) {
                    for (int i = 0; i < actorsArr.length(); i++) {
                        actorsList.add(actorsArr.optString(i));
                    }
                }
                String allActors = String.join(", ", actorsList);

                Handler ui = new Handler(Looper.getMainLooper());
                ui.post(() -> {
                    currentMovieTitle = title;
                    currentMoviePosterUrl = posterUrl;
                    currentMovieYear = year;
                    currentMovieGenre = allGenres;
                    currentMovieRuntimeMin = runtime;
                    currentMovieRatingImdb = ratingImdb;

                    Glide.with(movieImage).load(posterUrl).centerCrop().into(movieImage);

                    movieName.setText(title);
                    movieCountries.setText(allCountries);
                    movieGenre.setText(allGenres);
                    movieYear.setText(String.valueOf(year));
                    movieLength.setText(String.valueOf(runtime));
                    movieRating.setText(String.valueOf(ratingImdb));
                    movieDescription.setText(description);
                    movieDirector.setText(director);
                    movieActors.setText(allActors);
                });

            } catch (Exception e) {
                Log.e("MovieCardActivity", "loadMovieDetails error", e);
                runOnUiThread(() ->
                        Toast.makeText(MovieCardActivity.this, "Ошибка загрузки фильма", Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void refreshActionButtonState() {
        likedOrWatchedButton.setEnabled(false);

        watchedRepo.isWatched(userId, id, new WatchedRepository.IsWatchedCallback() {
            @Override
            public void onResult(boolean watched) {
                if (watched) {
                    runOnUiThread(() -> {
                        currentState = CardState.WATCHED;
                        likedOrWatchedButton.setText("Удалить");
                        likedOrWatchedButton.setEnabled(true);
                        loadUserRating();
                    });
                } else {
                    favoritesRepo.getFavoriteIds(userId, new FavoritesRepository.IdsCallback() {
                        @Override
                        public void onResult(List<Integer> ids) {
                            boolean inFav = ids.contains(id);
                            runOnUiThread(() -> {
                                if (inFav) {
                                    currentState = CardState.WANT_TO_WATCH;
                                    likedOrWatchedButton.setText("Просмотрен");
                                } else {
                                    currentState = CardState.NONE;
                                    likedOrWatchedButton.setText("Буду смотреть");
                                }
                                likedOrWatchedButton.setEnabled(true);
                                loadUserRating();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                currentState = CardState.NONE;
                                likedOrWatchedButton.setText("Буду смотреть");
                                likedOrWatchedButton.setEnabled(true);
                                loadUserRating();
                            });
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    currentState = CardState.NONE;
                    likedOrWatchedButton.setText("Буду смотреть");
                    likedOrWatchedButton.setEnabled(true);
                });
            }
        });
    }

    private void handleActionClick() {
        likedOrWatchedButton.setEnabled(false);

        if (currentState == CardState.NONE) {
            // Добавить "буду смотреть"
            favoritesRepo.addFavorite(userId, id, new FavoritesRepository.VoidCallback() {
                @Override
                public void onDone() {
                    runOnUiThread(() -> {
                        currentState = CardState.WANT_TO_WATCH;
                        likedOrWatchedButton.setText("Просмотрен");
                        likedOrWatchedButton.setEnabled(true);
                        updateRatingUi(0);
                        setRatingEnabled(true);
                        Toast.makeText(MovieCardActivity.this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        likedOrWatchedButton.setEnabled(true);
                        Toast.makeText(MovieCardActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                    });
                }
            });

        } else if (currentState == CardState.WANT_TO_WATCH) {
            favoritesRepo.getRating(userId, id, new FavoritesRepository.RatingCallback() {
                @Override
                public void onResult(int rating) {
                    moveFavoriteToWatched(rating);
                }

                @Override
                public void onError(String message) {
                    moveFavoriteToWatched(currentUserRating);
                }
            });

        } else { // WATCHED
            // Удалить из просмотренных и избранного
            watchedRepo.removeWatched(userId, id, new WatchedRepository.VoidCallback() {
                @Override
                public void onDone() {
                    favoritesRepo.removeFavorite(userId, id, new FavoritesRepository.VoidCallback() {
                        @Override
                        public void onDone() {
                            runOnUiThread(() -> {
                                currentState = CardState.NONE;
                                likedOrWatchedButton.setText("Буду смотреть");
                                likedOrWatchedButton.setEnabled(true);
                                updateRatingUi(0);
                                setRatingEnabled(false);
                                Toast.makeText(MovieCardActivity.this, "Удалено", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                likedOrWatchedButton.setEnabled(true);
                                Toast.makeText(MovieCardActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        likedOrWatchedButton.setEnabled(true);
                        Toast.makeText(MovieCardActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupMyRatingStars() {
        if (myRatingStars == null) return;

        for (int i = 0; i < myRatingStars.getChildCount(); i++) {
            final int rating = i + 1;
            View child = myRatingStars.getChildAt(i);

            child.setOnClickListener(v -> {
                if (currentState == CardState.NONE) {
                    Toast.makeText(
                            MovieCardActivity.this,
                            "Оценку можно поставить только для понравившихся или просмотренных фильмов",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                saveUserRating(rating);
            });
        }
    }

    private void setRatingEnabled(boolean enabled) {
        if (myRatingContainer != null) {
            myRatingContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }

        if (myRatingStars == null) return;

        for (int i = 0; i < myRatingStars.getChildCount(); i++) {
            View child = myRatingStars.getChildAt(i);
            child.setEnabled(enabled);
            child.setClickable(enabled);
        }
    }

    private void updateRatingUi(int rating) {
        currentUserRating = Math.max(0, Math.min(10, rating));

        if (myRatingValue != null) {
            myRatingValue.setText(String.valueOf(currentUserRating));
        }

        if (myRatingStars == null) return;

        for (int i = 0; i < myRatingStars.getChildCount(); i++) {
            View child = myRatingStars.getChildAt(i);

            if (child instanceof TextView) {
                TextView star = (TextView) child;
                int drawable = i < currentUserRating
                        ? R.drawable.star_rate_filled
                        : R.drawable.star_rate_empty;

                star.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
            }
        }
    }

    private void loadUserRating() {
        if (userId == null || userId.isEmpty() || id == 0) {
            updateRatingUi(0);
            setRatingEnabled(false);
            return;
        }

        if (currentState == CardState.WATCHED) {
            watchedRepo.getRating(userId, id, new WatchedRepository.RatingCallback() {
                @Override
                public void onResult(int rating) {
                    runOnUiThread(() -> {
                        updateRatingUi(rating);
                        setRatingEnabled(true);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        updateRatingUi(0);
                        setRatingEnabled(true);
                    });
                }
            });

        } else if (currentState == CardState.WANT_TO_WATCH) {
            favoritesRepo.getRating(userId, id, new FavoritesRepository.RatingCallback() {
                @Override
                public void onResult(int rating) {
                    runOnUiThread(() -> {
                        updateRatingUi(rating);
                        setRatingEnabled(true);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        updateRatingUi(0);
                        setRatingEnabled(true);
                    });
                }
            });

        } else {
            updateRatingUi(0);
            setRatingEnabled(false);
        }
    }

    private void saveUserRating(int rating) {
        updateRatingUi(rating);

        if (currentState == CardState.WATCHED) {
            watchedRepo.updateRating(userId, id, rating, new WatchedRepository.VoidCallback() {
                @Override
                public void onDone() {
                    runOnUiThread(() ->
                            Toast.makeText(MovieCardActivity.this, "Оценка сохранена", Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() ->
                            Toast.makeText(MovieCardActivity.this, "Ошибка сохранения оценки: " + message, Toast.LENGTH_LONG).show()
                    );
                }
            });

        } else if (currentState == CardState.WANT_TO_WATCH) {
            favoritesRepo.updateRating(userId, id, rating, new FavoritesRepository.VoidCallback() {
                @Override
                public void onDone() {
                    runOnUiThread(() ->
                            Toast.makeText(MovieCardActivity.this, "Оценка сохранена", Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() ->
                            Toast.makeText(MovieCardActivity.this, "Ошибка сохранения оценки: " + message, Toast.LENGTH_LONG).show()
                    );
                }
            });
        }
    }

    private void moveFavoriteToWatched(int ratingFromFavorite) {
        watchedRepo.addWatched(userId, id, new WatchedRepository.VoidCallback() {
            @Override
            public void onDone() {
                watchedRepo.updateRating(userId, id, ratingFromFavorite, new WatchedRepository.VoidCallback() {
                    @Override
                    public void onDone() {
                        removeFavoriteAfterMove(ratingFromFavorite);
                    }

                    @Override
                    public void onError(String message) {
                        removeFavoriteAfterMove(ratingFromFavorite);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    likedOrWatchedButton.setEnabled(true);
                    Toast.makeText(MovieCardActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void removeFavoriteAfterMove(int rating) {
        favoritesRepo.removeFavorite(userId, id, new FavoritesRepository.VoidCallback() {
            @Override
            public void onDone() {
                runOnUiThread(() -> {
                    currentState = CardState.WATCHED;
                    likedOrWatchedButton.setText("Удалить");
                    likedOrWatchedButton.setEnabled(true);
                    updateRatingUi(rating);
                    setRatingEnabled(true);
                    Toast.makeText(MovieCardActivity.this, "Отмечен как просмотренный", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    likedOrWatchedButton.setEnabled(true);
                    Toast.makeText(MovieCardActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void recommendCurrentMovie() {
        if (id == 0) return;

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_LONG).show();
            return;
        }

        if (currentState == CardState.NONE) {
            Toast.makeText(this, "Сначала добавьте фильм в понравившиеся или просмотренные", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserRating <= 0) {
            Toast.makeText(this, "Сначала поставьте оценку фильму", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentMovieTitle == null || currentMovieTitle.trim().isEmpty()) {
            Toast.makeText(this, "Данные фильма ещё не загрузились", Toast.LENGTH_SHORT).show();
            return;
        }

        if (recommendButton != null) {
            recommendButton.setEnabled(false);
        }

        recommendationsRepository.recommendMovie(
                id,
                currentUserRating,
                currentMovieTitle,
                currentMovieYear,
                currentMovieGenre,
                currentMoviePosterUrl,
                currentMovieRuntimeMin,
                currentMovieRatingImdb,
                new RecommendationsRepository.Callback<com.example.random_movie.recommendations.RecommendationItem>() {
                    @Override
                    public void onSuccess(com.example.random_movie.recommendations.RecommendationItem data) {
                        runOnUiThread(() -> {
                            if (recommendButton != null) {
                                recommendButton.setEnabled(true);
                                recommendButton.setText("Уже советуете");
                            }
                            Toast.makeText(MovieCardActivity.this, "Фильм добавлен в советы", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            if (recommendButton != null) {
                                recommendButton.setEnabled(true);
                            }
                            Toast.makeText(MovieCardActivity.this, "Ошибка совета: " + message, Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }
}