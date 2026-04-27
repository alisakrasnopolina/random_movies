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
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_liked_movies, container, false);
        LinearLayout movieContainer = rootView.findViewById(R.id.movie_container);

        Bundle args = getArguments();
        if (args != null) {
            ArrayList<String> movieIds = args.getStringArrayList("movieIds");
            if (movieIds != null) {
                for (String movieId : movieIds) {
                    fetchMovieDetails(movieId, movieContainer, inflater);
                }
            }
        }

        return rootView;
    }

    private void fetchMovieDetails(String movieId, LinearLayout movieContainer, LayoutInflater inflater) {
        OkHttpClient client = new OkHttpClient();
        String url = BuildConfig.API_BASE_URL + "/movies/" + movieId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("CardOfLikedMovieFragment", "Network error", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Request failed: " + response.code() + " " + response.message());
                    }

                    String responseData = responseBody != null ? responseBody.string() : "";
                    JSONObject jsonObj = new JSONObject(responseData);

                    final String name = jsonObj.optString("title", "Unknown");
                    final int year = jsonObj.optInt("year", 0);
                    final String imageUrl = jsonObj.optString("poster_url", "");
                    final double ratingImdb = jsonObj.optDouble("rating_imdb", 0.0);
                    final int length = jsonObj.optInt("runtime_min", 0);
                    final String movieIdFinal = movieId;

                    String tmpGenre = "—";
                    JSONArray genres = jsonObj.optJSONArray("genres");
                    if (genres != null && genres.length() > 0) {
                        tmpGenre = genres.optString(0, "—");
                    }
                    final String genre = tmpGenre;

                    Handler uiHandler = new Handler(Looper.getMainLooper());
                    uiHandler.post(() -> {
                        if (!isAdded() || getActivity() == null) return;

                        View movieCardView = inflater.inflate(R.layout.fragment_card_of_liked_movie, movieContainer, false);

                        TextView movieNameTextView = movieCardView.findViewById(R.id.movie_name);
                        TextView movieGenreTextView = movieCardView.findViewById(R.id.movie_genre);
                        TextView movieYearTextView = movieCardView.findViewById(R.id.movie_year);
                        TextView movieLengthTextView = movieCardView.findViewById(R.id.movie_length);
                        TextView movieRateTextView = movieCardView.findViewById(R.id.movie_rate);
                        ShapeableImageView movieImage = movieCardView.findViewById(R.id.image_card_liked);

                        Glide.with(requireContext())
                                .load(imageUrl)
                                .centerCrop()
                                .into(movieImage);

                        movieNameTextView.setText(name);
                        movieGenreTextView.setText(genre);
                        movieYearTextView.setText("· " + year);
                        movieLengthTextView.setText(String.valueOf(length));
                        movieRateTextView.setText(String.valueOf(ratingImdb));

                        movieCardView.setOnClickListener(v -> {
                            Intent intent = new Intent(getActivity(), MovieCardActivity.class);
                            intent.putExtra("id", movieIdFinal);
                            intent.putExtra("watched", false);
                            startActivity(intent);
                        });

                        movieContainer.addView(movieCardView);
                    });

                } catch (JSONException e) {
                    Log.e("CardOfLikedMovieFragment", "JSON parse error", e);
                } catch (Exception e) {
                    Log.e("CardOfLikedMovieFragment", "Unexpected error", e);
                }
            }
        });
    }
}