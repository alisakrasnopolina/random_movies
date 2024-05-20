package com.example.random_movie;

import static android.net.Uri.encode;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MovieCardActivity extends AppCompatActivity {

    int id;
    TextView movieName, movieGenre, movieYear, movieLength, movieRating, movieCountries, movieDescription, movieDirector, movieActors;
    ImageView movieImage;
    Button likedOrWatchedButton;

    String allCountries = "";
    String allGenres = "";
    String allActors = "";
    String director;

    SharedPreferences settings;
    SharedPreferences.Editor prefEditor;

    String name;

    Boolean isWatched;

    private static final String MOVIE_NAME = "Name";
    private static final String MOVIE_URL = "url";
    private static final String MOVIE_LENGTH = "length";
    private static final String MOVIE_RATING = "rating";
    private static final String MOVIE_GENRE = "genre";
    private static final String MOVIE_YEAR = "year";
    private static final String PREFS_FILE = "Random movie";
    private static final String MOVIE_ID = "id movie";
    FirebaseDatabase database;
    DatabaseReference reference_liked, reference_watched;

    void islikedOrWatchedButton () {
        SharedPreferences preferences_login = getSharedPreferences("login", MODE_PRIVATE);
        String userID = preferences_login.getString("userID", "");

        database = FirebaseDatabase.getInstance();
        reference_liked = database.getReference("users/"+userID+"/liked");
        reference_watched = database.getReference("users/"+userID+"/watched");

        reference_liked.addListenerForSingleValueEvent(new ValueEventListener() {

            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("film_number"+id)) {
                    likedOrWatchedButton.setText("Просмотрен");
                } else if (isWatched) {
                    likedOrWatchedButton.setText("Удалить");
                } else {
                    likedOrWatchedButton.setText("Буду смотреть");
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                Log.d("error", "The read failed: " + databaseError.getCode());
            }
        });

        reference_watched.addListenerForSingleValueEvent(new ValueEventListener() {

            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("film_number"+id)) {
                    Log.d("huh", "delete");
                    likedOrWatchedButton.setText("Удалить");
                    isWatched = true;
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                Log.d("error", "The read failed: " + databaseError.getCode());
            }
        });
    }

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

        settings = getSharedPreferences(PREFS_FILE, MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setElevation(2); // or other
            getSupportActionBar().setTitle("");
        }

        Intent intent = getIntent();
        String MovieId = intent.getStringExtra("id");
        isWatched = intent.getBooleanExtra("watched", false);
        Log.d("huhuhuh", String.valueOf(isWatched));
        id = Integer.parseInt(MovieId);

        islikedOrWatchedButton();

        OkHttpClient client = new OkHttpClient();
        String url = "https://api.kinopoisk.dev/v1.4/movie/";

        Request request = new Request.Builder()
                .url(url + MovieId)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("X-API-KEY", "0QZTAKB-HX6MTJ1-N6ABCHA-MSF9HBF")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Запрос к серверу не был успешен: " +
                                response.code() + " " + response.message());
                    }
                    String responseData = response.body().string();

                    JSONObject jsonObj = new JSONObject(responseData);

                    Log.d("k", responseData);

                    name = jsonObj.getString("name");

                    JSONObject poster = jsonObj.getJSONObject("poster");
                    String url = poster.getString("url");
                    Log.d("url", url);

                    int year = jsonObj.getInt("year");

                    JSONObject ratings = jsonObj.getJSONObject("rating");
                    int ratingImdb = ratings.getInt("imdb");

                    int length = jsonObj.getInt("movieLength");
                    Log.d("movieLength", String.valueOf(length));

                    String description = jsonObj.getString("description");

                    id = jsonObj.getInt("id");
                    Log.d("id", String.valueOf(id));

                    JSONArray genres = jsonObj.getJSONArray("genres");
                    List<String> genresList = new ArrayList<String>();
                    for (int i = 0; i < genres.length(); i++) {
                        genresList.add(genres.getJSONObject(i).getString("name"));
                    }

                    JSONObject first_genre = genres.getJSONObject(0);
                    String genre = first_genre.getString("name");

                    JSONArray persons = jsonObj.getJSONArray("persons");
                    List<String> actorsList = new ArrayList<String>();
                    for (int i = 0; i < persons.length(); i++) {
                        if (persons.getJSONObject(i).getString("enProfession").equals("actor")) {
                            actorsList.add(persons.getJSONObject(i).getString("name"));
                        } else if (persons.getJSONObject(i).getString("enProfession").equals("director")) {
                            director = persons.getJSONObject(i).getString("name");
                        }
                    }
                    Log.d("director", director);

                    JSONArray countries = jsonObj.getJSONArray("countries");
                    List<String> countriesList = new ArrayList<String>();
                    for (int i = 0; i < countries.length(); i++) {
                        countriesList.add(countries.getJSONObject(i).getString("name"));
                    }

                    allCountries = String.join(", ", countriesList);
                    allActors = String.join(", ", actorsList);
                    allGenres = String.join(", ", genresList);

                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Picasso.get()
                                    .load(url)
                                    .into(movieImage);
                            movieName.setText("");
                            movieName.append(name);
                            movieCountries.setText("");
                            movieCountries.append(allCountries);
                            movieGenre.setText("");
                            movieGenre.append(allGenres);
                            movieYear.setText("");
                            movieYear.append(String.valueOf(year));
                            movieLength.setText(" ");
                            movieLength.append(String.valueOf(length));
                            movieRating.setText(" ");
                            movieRating.append(String.valueOf(ratingImdb));
                            movieDescription.setText("");
                            movieDescription.append(description);
                            movieDirector.setText("");
                            movieDirector.append(director);
                            movieActors.setText("");
                            movieActors.append(allActors);
                        }
                    });

                    prefEditor = settings.edit();
                    prefEditor.putString(MOVIE_NAME, name);
                    prefEditor.putString(MOVIE_URL, url);
                    prefEditor.putString(MOVIE_LENGTH, String.valueOf(length));
                    prefEditor.putString(MOVIE_RATING, String.valueOf(ratingImdb));
                    prefEditor.putString(MOVIE_GENRE, genre);
                    prefEditor.putString(MOVIE_YEAR, String.valueOf(year));
                    prefEditor.putString(MOVIE_ID, String.valueOf(id));
                    prefEditor.apply();
                } catch (JSONException e) {
                    Log.e("MYAPP", "unexpected JSON exception", e);
                }
            }
        });
        likedOrWatchedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences preferences_login = getSharedPreferences("login", MODE_PRIVATE);
                String userID = preferences_login.getString("userID", "");

                database = FirebaseDatabase.getInstance();
                reference_liked = database.getReference("users/"+userID+"/liked");
                reference_watched = database.getReference("users/"+userID+"/watched");

                reference_liked.addListenerForSingleValueEvent(new ValueEventListener() {

                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.hasChild("film_number"+id) && !isWatched) {
                            Log.d("huh", "watched");
                            likedOrWatchedButton.setText("Просмотрен");
                            reference_watched.child("film_number"+id).removeValue();
                            reference_liked.child("film_number"+id).setValue(id);
                        } else if (isWatched) {
                            reference_watched.child("film_number" + id).removeValue();
                            finish();
                        } else if (dataSnapshot.hasChild("film_number"+id)) {
                            likedOrWatchedButton.setText("Удалить");
                            isWatched = true;
                            reference_liked.child("film_number"+id).removeValue();
                            reference_watched.child("film_number"+id).setValue(id);
                        }
//                        else {
//                            likedOrWatchedButton.setText("Удалить");
//                            reference_liked.child("film_number"+id).removeValue();
//                            reference_watched.child("film_number"+id).setValue(id);
//                        }
                    }

                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("error", "The read failed: " + databaseError.getCode());
                    }
                });

                reference_watched.addListenerForSingleValueEvent(new ValueEventListener() {

                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild("film_number"+id)) {
                            Log.d("huh", "delete");
                            likedOrWatchedButton.setText("Удалить");
                            reference_liked.child("film_number"+id).removeValue();
                            reference_watched.child("film_number"+id).removeValue();
                            finish();
                        }
                    }

                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("error", "The read failed: " + databaseError.getCode());
                    }
                });

//                reference_watched.addListenerForSingleValueEvent(new ValueEventListener() {
//
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        if (dataSnapshot.hasChild("film_number"+id)) {
//                            likedOrWatchedButton.setText("Просмотрен");
//                            reference_watched.child("film_number"+id).removeValue();
//                            reference_liked.child("film_number"+id).setValue(id);
//                        } else {
//                            likedOrWatchedButton.setText("Буду смотреть");
//                            reference_liked.child("film_number"+id).removeValue();
//                            reference_watched.child("film_number"+id).setValue(id);
//                        }
//                    }
//
//                    public void onCancelled(DatabaseError databaseError) {
//                        Log.d("error", "The read failed: " + databaseError.getCode());
//                    }
//                });
            }
        });
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