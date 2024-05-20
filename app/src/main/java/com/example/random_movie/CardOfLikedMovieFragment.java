package com.example.random_movie;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.imageview.ShapeableImageView;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CardOfLikedMovieFragment extends Fragment {

    CardView movieCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_liked_movies, container, false);
        LinearLayout movieContainer = rootView.findViewById(R.id.movie_container);

        Bundle args = getArguments();
        if (args != null) {
            ArrayList<String> movieIds = args.getStringArrayList("movieIds");

            // Iterate through each movie ID and fetch its details
            for (String movieId : movieIds) {
                fetchMovieDetails(movieId, movieContainer, inflater);
            }
        }

        return rootView;
    }

    private void fetchMovieDetails(String movieId, LinearLayout movieContainer, LayoutInflater inflater) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.kinopoisk.dev/v1.4/movie/";

        Request request = new Request.Builder()
                .url(url + movieId)
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
                        throw new IOException("Request to the server was not successful: " +
                                response.code() + " " + response.message());
                    }
                    String responseData = response.body().string();

                    JSONObject jsonObj = new JSONObject(responseData);
                    String name = jsonObj.getString("name");
                    int year = jsonObj.getInt("year");
                    JSONObject poster = jsonObj.getJSONObject("poster");
                    String imageUrl = poster.getString("url");
                    JSONArray genres = jsonObj.getJSONArray("genres");
                    JSONObject firstGenre = genres.getJSONObject(0);
                    String genre = firstGenre.getString("name");
                    JSONObject ratings = jsonObj.getJSONObject("rating");
                    int ratingImdb = ratings.getInt("imdb");
                    int length = jsonObj.getInt("movieLength");

                    // Create and populate the movie card layout
                    View movieCard = inflater.inflate(R.layout.fragment_card_of_liked_movie, movieContainer, false);
                    TextView movieNameTextView = movieCard.findViewById(R.id.movie_name);
                    TextView movieGenreTextView = movieCard.findViewById(R.id.movie_genre);
                    TextView movieYearTextView = movieCard.findViewById(R.id.movie_year);
                    TextView movieLengthTextView = movieCard.findViewById(R.id.movie_length);
                    TextView movieRateTextView = movieCard.findViewById(R.id.movie_rate);
                    ShapeableImageView movieImage = movieCard.findViewById(R.id.image_card_liked);

                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Picasso.get().load(imageUrl).into(movieImage);
                            movieNameTextView.setText(name);
                            movieGenreTextView.setText(genre);
                            movieYearTextView.setText("· " + year);
                            movieLengthTextView.setText(String.valueOf(length));
                            movieRateTextView.setText(String.valueOf(ratingImdb));

                            // Set an OnClickListener to navigate to another activity
                            movieCard.setOnClickListener(v -> {
                                Intent intent = new Intent(getActivity(), MovieCardActivity.class);
                                intent.putExtra("id", movieId);
                                intent.putExtra("watched", false);
                                startActivity(intent);
                            });

                            // Add the movie card to the container
                            movieContainer.addView(movieCard);
                        }
                    });

                } catch (JSONException e) {
                    Log.e("CardOfLikedMovieFragment", "Unexpected JSON exception", e);
                }
            }
        });
    }
}

//import static android.net.Uri.encode;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import com.google.android.material.imageview.ShapeableImageView;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.squareup.picasso.Picasso;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.IOException;
//import java.util.ArrayList;
//
//import okhttp3.Call;
//import okhttp3.Callback;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//import okhttp3.ResponseBody;
//
//public class CardOfLikedMovieFragment extends Fragment {
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.fragment_liked_movies, container, false);
//        // Get reference to the LinearLayout in your fragment layout
//        LinearLayout movieContainer = rootView.findViewById(R.id.movie_container);
//        // Inflate movie card layout
//        View movieCard = inflater.inflate(R.layout.fragment_card_of_liked_movie, movieContainer, false);
//        Bundle args = getArguments();
//        if (args != null) {
//            ArrayList<String> movieIds = args.getStringArrayList("movieIds");
//
//            // Iterate through each movie ID and fetch its details
//            for (String movieId : movieIds) {
//                fetchMovieDetails(movieId, movieContainer, inflater);
//            }
//        }
//
//        return rootView;
//    }private void fetchMovieDetails(String movieId, LinearLayout movieContainer, LayoutInflater inflater) {
//        OkHttpClient client = new OkHttpClient();
//        String url = "https://api.kinopoisk.dev/v1.4/movie/";
//
//        Request request = new Request.Builder()
//                .url(url + movieId)
//                .get()
//                .addHeader("accept", "application/json")
//                .addHeader("X-API-KEY", "0QZTAKB-HX6MTJ1-N6ABCHA-MSF9HBF")
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                try (ResponseBody responseBody = response.body()) {
//                    if (!response.isSuccessful()) {
//                        throw new IOException("Request to the server was not successful: " +
//                                response.code() + " " + response.message());
//                    }
//                    String responseData = response.body().string();
//
//                    JSONObject jsonObj = new JSONObject(responseData);
//                    String name = jsonObj.getString("name");
//                    int year = jsonObj.getInt("year");
//                    JSONObject poster = jsonObj.getJSONObject("poster");
//                    String imageUrl = poster.getString("url");
//                    JSONArray genres = jsonObj.getJSONArray("genres");
//                    JSONObject firstGenre = genres.getJSONObject(0);
//                    String genre = firstGenre.getString("name");
//                    JSONObject ratings = jsonObj.getJSONObject("rating");
//                    int ratingImdb = ratings.getInt("imdb");
//                    int length = jsonObj.getInt("movieLength");
//
//                    // Create and populate the movie card layout
//                    View movieCard = inflater.inflate(R.layout.fragment_card_of_liked_movie, movieContainer, false);
//                    TextView movieNameTextView = movieCard.findViewById(R.id.movie_name);
//                    TextView movieGenreTextView = movieCard.findViewById(R.id.movie_genre);
//                    TextView movieYearTextView = movieCard.findViewById(R.id.movie_year);
//                    TextView movieLengthTextView = movieCard.findViewById(R.id.movie_length);
//                    TextView movieRateTextView = movieCard.findViewById(R.id.movie_rate);
//                    ShapeableImageView movieImage = movieCard.findViewById(R.id.image_card_liked);
//
//                    Handler uiHandler = new Handler(Looper.getMainLooper());
//                    uiHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            Picasso.get().load(imageUrl).into(movieImage);
//                            movieNameTextView.setText(name);
//                            movieGenreTextView.setText(genre);
//                            movieYearTextView.setText("· " + year);
//                            movieLengthTextView.setText(String.valueOf(length));
//                            movieRateTextView.setText(String.valueOf(ratingImdb));
//                        }
//                    });
//
//                    // Add the movie card to the container
//                    movieContainer.addView(movieCard);
//                } catch (JSONException e) {
//                    Log.e("CardOfLikedMovieFragment", "Unexpected JSON exception", e);
//                }
//            }
//        });
//    }
//}
//        TextView movieNameTextView = movieCard.findViewById(R.id.movie_name);
//        TextView movieGenreTextView = movieCard.findViewById(R.id.movie_genre);
//        TextView movieYearTextView = movieCard.findViewById(R.id.movie_year);
//        TextView movieLengthTextView = movieCard.findViewById(R.id.movie_length);
//        TextView movieRateTextView = movieCard.findViewById(R.id.movie_rate);
//        ShapeableImageView movieImage = movieCard.findViewById(R.id.image_card_liked);
//
//        Bundle args = getArguments();
//        if (args != null) {
//            String movieId = args.getString("movieId");
//
//            // Now you can use the movieId to fetch movie details or perform any other actions
//            OkHttpClient client = new OkHttpClient();
//            String url = "https://api.kinopoisk.dev/v1.4/movie/";
//
//            Request request = new Request.Builder()
//                    .url(url + movieId)
//                    .get()
//                    .addHeader("accept", "application/json")
//                    .addHeader("X-API-KEY", "0QZTAKB-HX6MTJ1-N6ABCHA-MSF9HBF")
//                    .build();
//
//            client.newCall(request).enqueue(new Callback() {
//                @Override
//                public void onFailure(Call call, IOException e) {
//                    e.printStackTrace();
//                }
//
//                @Override
//                public void onResponse(Call call, Response response) throws IOException {
//                    try (ResponseBody responseBody = response.body()) {
//                        if (!response.isSuccessful()) {
//                            throw new IOException("Запрос к серверу не был успешен: " +
//                                    response.code() + " " + response.message());
//                        }
//                        String responseData = response.body().string();
//
//                        // вывод тела ответа
//                        Log.d("k", responseData);
//
//
//                        JSONObject jsonObj = new JSONObject(responseData);
//                        String name = jsonObj.getString("name");
//                        String alterName = jsonObj.getString("alternativeName");
//                        Log.d("name", name);
//                        Log.d("alterName", alterName);
//
//                        int year = jsonObj.getInt("year");
//                        Log.d("year", String.valueOf(year));
//
//                        JSONObject poster = jsonObj.getJSONObject("poster");
//                        String url = poster.getString("url");
//                        Log.d("url", url);
//
//                        JSONArray genres = jsonObj.getJSONArray("genres");
//                        JSONObject first_genre = genres.getJSONObject(0);
//                        String genre = first_genre.getString("name");
//                        Log.d("genres", genre);
//
//                        JSONObject ratings = jsonObj.getJSONObject("rating");
//                        int ratingImdb = ratings.getInt("imdb");
//                        Log.d("imdb", String.valueOf(ratingImdb));
//
//                        int length = jsonObj.getInt("movieLength");
//                        Log.d("movieLength", String.valueOf(length));
//
//                        int id = jsonObj.getInt("id");
//
//                        Handler uiHandler = new Handler(Looper.getMainLooper());
//                        uiHandler.post(new Runnable(){
//                            @Override
//                            public void run() {
//                                Picasso.get()
//                                        .load(url)
//                                        .into(movieImage);
//                                movieNameTextView.setText("");
//                                movieNameTextView.append(name);
//                                movieGenreTextView.setText("");
//                                movieGenreTextView.append(genre);
//                                movieYearTextView.setText("· ");
//                                movieYearTextView.append(String.valueOf(year));
//                                movieLengthTextView.setText(" ");
//                                movieLengthTextView.append(String.valueOf(length));
//                                movieRateTextView.setText(" ");
//                                movieRateTextView.append(String.valueOf(ratingImdb));
//                            }
//                        });
//                    }
//                    catch  (JSONException e) {
//                        Log.e("MYAPP", "unexpected JSON exception", e);
//                    }
//                }
//            });
//        }
//
//        // Add movie card to the container
//        movieContainer.addView(movieCard);
//
//        return rootView;
//    }
//}