package com.example.random_movie;

import static android.app.PendingIntent.getActivity;
import static android.net.Uri.encode;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.material.imageview.ShapeableImageView;
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
import java.net.URL;
import java.sql.Array;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener;
import java.io.*;

import android.content.SharedPreferences;

public class FindRandomMovie extends AppCompatActivity {

    ImageButton findMovieButton, likeButton;
    TextView movieName, movieGenre, movieYear, movieLength, movieRating;
    ShapeableImageView movieImage; // ShapeableImageView
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
                            String url_database = "https://api.kinopoisk.dev/v1.4/movie/random";

                            Request request = new Request.Builder()
                                    .url(url_database + "?notNullFields=name&notNullFields=description&notNullFields=rating.imdb&notNullFields=movieLength&notNullFields=poster.url&notNullFields=year&rating.imdb=6-10" + "&genres.name=" + encode(movieGenreFromFilter))
                                    .get()
                                    .addHeader("accept", "application/json")
                                    .addHeader("X-API-KEY", "TG4WNC1-6SMMDW8-NSRC5EE-E6X5A1M")
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
                                        // пример получения всех заголовков ответа
//                            Headers responseHeaders = response.headers();
//                            for (int i = 0, size = responseHeaders.size(); i < size; i++) {
//                                // вывод заголовков
//                                System.out.println(responseHeaders.name(i) + ": "
//                                        + responseHeaders.value(i));
//                            }

//                            String res = responseBody.string();
//                            try {
//                                 JSONObject jsonObj = new JSONObject(res);
//                                 String name = responseBody.getString("name");
//                                 Log.d("k", name);
//                            } catch (JSONException err){
//                                Log.d("Error", err.toString());
//                            }
                                        // вывод тела ответа
                                        Log.d("k", responseData);

                                        JSONObject jsonObj = new JSONObject(responseData);
                                        name = jsonObj.getString("name");
                                        String alterName = jsonObj.getString("alternativeName");
                                        Log.d("name", name);
                                        Log.d("alterName", alterName);

                                        year = jsonObj.getInt("year");
                                        Log.d("year", String.valueOf(year));

                                        JSONObject poster = jsonObj.getJSONObject("poster");
                                        url = poster.getString("url");
                                        Log.d("url", url);

                                        JSONArray genres = jsonObj.getJSONArray("genres");
                                        JSONObject first_genre = genres.getJSONObject(0);
                                        genre = first_genre.getString("name");
                                        Log.d("genres", genre);

                                        JSONObject ratings = jsonObj.getJSONObject("rating");
                                        ratingImdb = ratings.getInt("imdb");
                                        Log.d("imdb", String.valueOf(ratingImdb));

                                        length = jsonObj.getInt("movieLength");
                                        Log.d("movieLength", String.valueOf(length));

                                        id = jsonObj.getInt("id");

                                        isCardLiked();

                                        Handler uiHandler = new Handler(Looper.getMainLooper());
                                        uiHandler.post(new Runnable(){
                                            @Override
                                            public void run() {
//                                                Picasso.Builder builder = new Picasso.Builder(FindRandomMovie.this);
//                                                builder.listener(new Picasso.Listener()
//                                                {
//                                                    @Override
//                                                    public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception)
//                                                    {
//                                                        exception.printStackTrace();
//                                                    }
//                                                });
//                                                builder.build().load(url).into(movieImage);
                                                loadingProgress.setVisibility(View.GONE);
                                                movieCard.setVisibility(View.VISIBLE);
                                                Picasso.get()
                                                        .load(url)
                                                        .into(movieImage);
//                                                new ImageLoader(movieImage).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
//                                                ImageLoader imageLoader = ImageLoader.getInstance();
//                                                imageLoader.displayImage(imageUri, imageView);
//                                                Glide.with(context).load(url).into(movieImage);
//                                                Uri uri = Uri.parse(url);
//                                                movieImage.setImageURI(uri);
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
                                    catch  (JSONException e) {
                                        Log.e("MYAPP", "unexpected JSON exception", e);
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
//                    reference.child("film_number"+id).setValue(id);
//                if () {
//                } else if () {
//                    likeButton.setBackgroundResource(R.drawable.like);
//                    database = FirebaseDatabase.getInstance();
//                    reference = database.getReference("users/"+userID+"/liked");
//                }
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

        name = settings.getString(MOVIE_NAME,"");
        genre = settings.getString(MOVIE_GENRE, "");
        if (settings.getString(MOVIE_URL, "").equals("")){
            url = "https://bloximages.newyork1.vip.townnews.com/stltoday.com/content/tncms/assets/v3/editorial/3/44/344a8f80-d44a-11e2-81ba-0019bb30f31a/51b9fa6a8d39e.preview-1024.png?crop=1024%2C538%2C0%2C13&resize=1024%2C538&order=crop%2Cresize";
        } else {
            url = settings.getString(MOVIE_URL, "");
        }
        year = Integer.parseInt(settings.getString(MOVIE_YEAR, ""));
        length = Integer.parseInt(settings.getString(MOVIE_LENGTH, ""));
        ratingImdb = Integer.parseInt(settings.getString(MOVIE_RATING, ""));
        id = Integer.parseInt(settings.getString(MOVIE_ID, ""));


        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(new Runnable(){
            @Override
            public void run() {
                Picasso.get()
                        .load(url)
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
}

