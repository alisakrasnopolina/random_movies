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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.random_movie.auth.SessionManager;
import com.example.random_movie.data.repository.FavoritesRepository;
import com.example.random_movie.data.repository.WatchedRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MovieCardActivity extends AppCompatActivity {

    private int id;
    private TextView movieName, movieGenre, movieYear, movieLength, movieRating,
            movieCountries, movieDescription, movieDirector, movieActors;
    private ImageView movieImage;
    private Button likedOrWatchedButton;

    private FavoritesRepository favoritesRepo;
    private WatchedRepository watchedRepo;
    private SessionManager sessionManager;
    private String userId;

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

        sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        favoritesRepo = new FavoritesRepository(this);
        watchedRepo = new WatchedRepository(this);

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
                            });
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
            // Отметить как просмотренный
            watchedRepo.addWatched(userId, id, new WatchedRepository.VoidCallback() {
                @Override
                public void onDone() {
                    favoritesRepo.removeFavorite(userId, id, new FavoritesRepository.VoidCallback() {
                        @Override
                        public void onDone() {
                            runOnUiThread(() -> {
                                currentState = CardState.WATCHED;
                                likedOrWatchedButton.setText("Удалить");
                                likedOrWatchedButton.setEnabled(true);
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

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        likedOrWatchedButton.setEnabled(true);
                        Toast.makeText(MovieCardActivity.this, "Ошибка: " + message, Toast.LENGTH_LONG).show();
                    });
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
}