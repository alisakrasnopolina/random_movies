package com.example.random_movie;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.random_movie.auth.SessionManager;
import com.example.random_movie.data.repository.WatchedRepository;
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

public class CardOfWatchedMovieFragment extends Fragment {

    private WatchedRepository watchedRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_watched_movies, container, false);
        LinearLayout movieContainer = rootView.findViewById(R.id.movie_container);

        watchedRepository = new WatchedRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

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
                Log.e("CardOfWatchedMovieFragment", "Network error", e);
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

                        View movieCardView = inflater.inflate(R.layout.fragment_card_of_watched_movie, movieContainer, false);

                        TextView movieNameTextView = movieCardView.findViewById(R.id.movie_name);
                        TextView movieGenreTextView = movieCardView.findViewById(R.id.movie_genre);
                        TextView movieYearTextView = movieCardView.findViewById(R.id.movie_year);
                        TextView movieLengthTextView = movieCardView.findViewById(R.id.movie_length);
                        TextView movieRateTextView = movieCardView.findViewById(R.id.movie_rate);
                        ShapeableImageView movieImage = movieCardView.findViewById(R.id.image_card_watched);
                        ImageButton deleteButton = movieCardView.findViewById(R.id.delete_button);

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
                            intent.putExtra("watched", true);
                            startActivity(intent);
                        });

                        deleteButton.setOnClickListener(v -> {
                            String userId = sessionManager.getUserId();
                            if (userId == null || userId.isEmpty()) {
                                Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_LONG).show();
                                return;
                            }

                            int movieIdInt;
                            try {
                                movieIdInt = Integer.parseInt(movieIdFinal);
                            } catch (Exception e) {
                                Toast.makeText(requireContext(), "Некорректный movie id", Toast.LENGTH_LONG).show();
                                return;
                            }

                            watchedRepository.removeWatched(userId, movieIdInt, new WatchedRepository.VoidCallback() {
                                @Override
                                public void onDone() {
                                    if (!isAdded()) return;
                                    requireActivity().runOnUiThread(() -> {
                                        movieContainer.removeView(movieCardView);
                                        Toast.makeText(requireContext(), "Удалено из просмотренных", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onError(String message) {
                                    if (!isAdded()) return;
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), "Ошибка: " + message, Toast.LENGTH_LONG).show()
                                    );
                                }
                            });
                        });

                        movieContainer.addView(movieCardView);
                    });

                } catch (JSONException e) {
                    Log.e("CardOfWatchedMovieFragment", "JSON parse error", e);
                } catch (Exception e) {
                    Log.e("CardOfWatchedMovieFragment", "Unexpected error", e);
                }
            }
        });
    }
}