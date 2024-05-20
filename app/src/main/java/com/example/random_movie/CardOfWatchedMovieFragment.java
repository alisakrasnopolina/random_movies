package com.example.random_movie;
import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

public class CardOfWatchedMovieFragment extends Fragment {

    FirebaseDatabase database;
    DatabaseReference reference, reference_watched;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_watched_movies, container, false);
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
                    View movieCard = inflater.inflate(R.layout.fragment_card_of_watched_movie, movieContainer, false);
                    TextView movieNameTextView = movieCard.findViewById(R.id.movie_name);
                    TextView movieGenreTextView = movieCard.findViewById(R.id.movie_genre);
                    TextView movieYearTextView = movieCard.findViewById(R.id.movie_year);
                    TextView movieLengthTextView = movieCard.findViewById(R.id.movie_length);
                    TextView movieRateTextView = movieCard.findViewById(R.id.movie_rate);
                    ShapeableImageView movieImage = movieCard.findViewById(R.id.image_card_watched);
                    ImageButton deleteButton = movieCard.findViewById(R.id.delete_button);

                    SharedPreferences preferences_login = getActivity().getSharedPreferences("login", MODE_PRIVATE);
                    String userID = preferences_login.getString("userID", "");

                    database = FirebaseDatabase.getInstance();
                    reference_watched = database.getReference("users/"+userID+"/watched");

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
                                intent.putExtra("watched", true);
                                startActivity(intent);
                            });

                            deleteButton.setOnClickListener(v -> {
                                reference_watched.child("film_number"+movieId).removeValue();
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
