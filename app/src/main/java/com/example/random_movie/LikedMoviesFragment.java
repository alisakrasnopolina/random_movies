package com.example.random_movie;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.random_movie.auth.SessionManager;
import com.example.random_movie.data.repository.FavoritesRepository;

import java.util.ArrayList;
import java.util.List;

public class LikedMoviesFragment extends Fragment {

    private FavoritesRepository favoritesRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_liked_movies, container, false);

        favoritesRepository = new FavoritesRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        loadLikedMovies();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLikedMovies();
    }

    private void loadLikedMovies() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            if (!isAdded()) return;
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_LONG).show();
            return;
        }

        favoritesRepository.syncFavoritesFromServer(userId, new FavoritesRepository.VoidCallback() {
            @Override
            public void onDone() {
                loadLikedMoviesFromLocal(userId);
            }

            @Override
            public void onError(String message) {
                loadLikedMoviesFromLocal(userId);
            }
        });
    }

    private void loadLikedMoviesFromLocal(String userId) {
        favoritesRepository.getFavoriteIds(userId, new FavoritesRepository.IdsCallback() {
            @Override
            public void onResult(List<Integer> ids) {
                if (!isAdded()) return;

                ArrayList<String> idStrings = new ArrayList<>();
                for (Integer i : ids) idStrings.add(String.valueOf(i));

                Bundle args = new Bundle();
                args.putStringArrayList("movieIds", idStrings);

                CardOfLikedMovieFragment fragment = new CardOfLikedMovieFragment();
                fragment.setArguments(args);

                requireActivity().runOnUiThread(() ->
                        getChildFragmentManager().beginTransaction()
                                .replace(R.id.movie_container, fragment)
                                .commitAllowingStateLoss()
                );
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Ошибка загрузки избранного: " + message, Toast.LENGTH_LONG).show()
                );
            }
        });
    }
}