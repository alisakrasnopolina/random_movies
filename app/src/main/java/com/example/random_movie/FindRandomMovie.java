package com.example.random_movie;

import static android.net.Uri.encode;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.*;

public class FindRandomMovie extends AppCompatActivity {

    ImageButton findMovieButton, likeButton;
    TextView movieName, movieGenre, movieYear, movieLength, movieRating;
    ShapeableImageView movieImage;
    CardView movieCard;
    FirebaseDatabase database;
    DatabaseReference reference, reference_watched;
    String userId;
    public static int responseCode = 0;
    public static String responseString = "";
    SharedPreferences settings;
    SharedPreferences.Editor prefEditor;
    String name;
    int id;
    int year;
    String url;
    String genre;
    int ratingImdb;
    int length;

    private static final String MOVIE_NAME = "Name";
    private static final String MOVIE_URL = "url";
    private static final String MOVIE_LENGTH = "length";
    private static final String MOVIE_RATING = "rating";
    private static final String MOVIE_GENRE = "genre";
    private static final String MOVIE_YEAR = "year";
    private static final String PREFS_FILE = "Random movie";
    private static final String MOVIE_ID = "id movie";


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

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.random_movie);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.random_movie) {
                return true;
            }
            else if(item.getItemId() == R.id.liked_movies) {
                startActivity(new Intent(getApplicationContext(), LikedMoviesActivity.class));
                finish();
                return true;
            }
            else if(item.getItemId() == R.id.home) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
                return true;
            } else {
                return false;
            }
        });

//        Context context = this;
//        Fresco.initialize(context);

        settings = getSharedPreferences(PREFS_FILE, MODE_PRIVATE);

        ActivityResultLauncher<Intent> activityResultLaunch = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == 200) {
                            Log.d("dudud", "idk it goes here");
                            Intent data = result.getData();
                            String movieGenreFromFilter = data.getStringExtra("genre");

                            Log.d("url_genre", encode(movieGenreFromFilter));

                            CardView movieCard = findViewById(R.id.movie_card);
                            movieCard.setVisibility(View.INVISIBLE);

                            ProgressBar loadingProgress = findViewById(R.id.loading_progress);
                            loadingProgress.setVisibility(View.VISIBLE);

                            OkHttpClient client = new OkHttpClient();

                            String urlDatabase = BuildConfig.API_BASE_URL + "/movies/random";
                            Request request = new Request.Builder()
                                    .url(urlDatabase + (movieGenreFromFilter != null ? "?genre=" + encode(movieGenreFromFilter) : ""))
                                    .get()
                                    .addHeader("accept", "application/json")
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

                                        // вывод тела ответа
                                        Log.d("k", responseData);

                                        JSONObject jsonObj = new JSONObject(responseData);

                                        name = jsonObj.optString("title", "Unknown");
                                        year = jsonObj.optInt("year", 0);
                                        url = jsonObj.optString("poster_url", "");
                                        genre = jsonObj.optString("genre", "—");
                                        ratingImdb = (int) Math.round(jsonObj.optDouble("rating_imdb", 0.0));
                                        length = jsonObj.optInt("runtime_min", 0);
                                        id = jsonObj.optInt("id", 0);

                                        isCardLiked();

                                        Handler uiHandler = new Handler(Looper.getMainLooper());
                                        uiHandler.post(new Runnable(){
                                            @Override
                                            public void run() {
                                                loadingProgress.setVisibility(View.GONE);
                                                movieCard.setVisibility(View.VISIBLE);
                                                    Glide.with(FindRandomMovie.this)
                                                            .load(url)
                                                            .centerCrop()
                                                            .into(movieImage);
                                                movieName.setText("");
                                                movieName.append(name);
                                                movieGenre.setText("");
                                                movieGenre.append(genre);
                                                movieYear.setText("· ");
                                                movieYear.append(String.valueOf(year));
                                                movieLength.setText(" ");
                                                movieLength.append(String.valueOf(length));
                                                movieRating.setText(" ");
                                                movieRating.append(String.valueOf(ratingImdb));
                                            }
                                        });
                                    }
                                    catch (JSONException e) {
                                        Log.e("MYAPP", "unexpected JSON exception", e);

                                        Handler uiHandler = new Handler(Looper.getMainLooper());
                                        uiHandler.post(() -> {
                                            loadingProgress.setVisibility(View.GONE);
                                            movieCard.setVisibility(View.INVISIBLE);
                                            Toast.makeText(FindRandomMovie.this, "Ошибка обработки данных фильма", Toast.LENGTH_LONG).show();
                                        });
                                    }
                                }
                            });
                        } else if(result.getResultCode() == 400) {
                            Log.e("WHAT", "what happened?");
                        }
                    }
                });

        findMovieButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(FindRandomMovie.this, FilterFindMovieActivity.class);
                activityResultLaunch.launch(intent);
            }
        });

        movieCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FindRandomMovie.this, MovieCardActivity.class);
                intent.putExtra("id", String.valueOf(id));
                intent.putExtra("watched", false);
                startActivity(intent);
            }
        });

        isCardLiked();

        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reference.addListenerForSingleValueEvent(new ValueEventListener() {

                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild("film_number"+id)) {
                            likeButton.setBackgroundResource(R.drawable.like);
                            reference.child("film_number"+id).removeValue();
                        } else {
                            likeButton.setBackgroundResource(R.drawable.liked);
                            reference.child("film_number"+id).setValue(id);
                            reference_watched.child("film_number"+id).removeValue();
                        }
                    }

                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("error", "The read failed: " + databaseError.getCode());
                    }
                });
            }
        });
    }

    void isCardLiked () {
        SharedPreferences preferences_login = getSharedPreferences("login", MODE_PRIVATE);
        String userID = preferences_login.getString("userID", "");

        database = FirebaseDatabase.getInstance();
        reference = database.getReference("users/"+userID+"/liked");
        reference_watched = database.getReference("users/"+userID+"/watched");

        reference.addListenerForSingleValueEvent(new ValueEventListener() {

            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("film_number"+id)) {
                    likeButton.setBackgroundResource(R.drawable.liked);
                } else {
                    likeButton.setBackgroundResource(R.drawable.like);
                }
            }

            public void onCancelled(DatabaseError databaseError) {
                Log.d("error", "The read failed: " + databaseError.getCode());
            }
        });
    }

    @Override
    protected void onPause(){
        super.onPause();
        // сохраняем в настройках
        prefEditor = settings.edit();
        prefEditor.putString(MOVIE_NAME, name);
        prefEditor.putString(MOVIE_URL, url);
        prefEditor.putString(MOVIE_LENGTH, String.valueOf(length));
        prefEditor.putString(MOVIE_RATING, String.valueOf(ratingImdb));
        prefEditor.putString(MOVIE_GENRE, genre);
        prefEditor.putString(MOVIE_YEAR, String.valueOf(year));
        prefEditor.putString(MOVIE_ID, String.valueOf(id));
        prefEditor.apply();
    }

    @Override
    protected void onStart(){
        super.onStart();

        isCardLiked();

        name = settings.getString(MOVIE_NAME,"Name");
        genre = settings.getString(MOVIE_GENRE, "Genre");
        if (settings.getString(MOVIE_URL, "").equals("")){
            url = "https://bloximages.newyork1.vip.townnews.com/stltoday.com/content/tncms/assets/v3/editorial/3/44/344a8f80-d44a-11e2-81ba-0019bb30f31a/51b9fa6a8d39e.preview-1024.png?crop=1024%2C538%2C0%2C13&resize=1024%2C538&order=crop%2Cresize";
        } else {
            url = settings.getString(MOVIE_URL, "https://bloximages.newyork1.vip.townnews.com/stltoday.com/content/tncms/assets/v3/editorial/3/44/344a8f80-d44a-11e2-81ba-0019bb30f31a/51b9fa6a8d39e.preview-1024.png?crop=1024%2C538%2C0%2C13&resize=1024%2C538&order=crop%2Cresize");
        }
        year = Integer.parseInt(settings.getString(MOVIE_YEAR, "2000"));
        length = Integer.parseInt(settings.getString(MOVIE_LENGTH, "111"));
        ratingImdb = Integer.parseInt(settings.getString(MOVIE_RATING, "9"));
        id = Integer.parseInt(settings.getString(MOVIE_ID, "0"));


//        Handler uiHandler = new Handler(Looper.getMainLooper());
//        uiHandler.post(new Runnable(){
//            @Override
//            public void run() {
//                Picasso.get()
//                        .load(url)
//                        .into(movieImage);
//                movieName.setText("");
//                movieName.append(name);
//                movieGenre.setText("");
//                movieGenre.append(genre);
//                movieYear.setText("· ");
//                movieYear.append(String.valueOf(year));
//                movieLength.setText(" ");
//                movieLength.append(String.valueOf(length));
//                movieRating.setText(" ");
//                movieRating.append(String.valueOf(ratingImdb));
//            }
//        });
    }
}

