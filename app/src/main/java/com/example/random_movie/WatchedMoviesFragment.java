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
import com.example.random_movie.data.repository.WatchedRepository;

import java.util.ArrayList;
import java.util.List;

public class WatchedMoviesFragment extends Fragment {

    private WatchedRepository watchedRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_watched_movies, container, false);

        watchedRepository = new WatchedRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        loadWatchedMovies();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWatchedMovies();
    }

    private void loadWatchedMovies() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            if (!isAdded()) return;
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_LONG).show();
            return;
        }

        watchedRepository.getWatchedIds(userId, new WatchedRepository.IdsCallback() {
            @Override
            public void onResult(List<Integer> ids) {
                if (!isAdded()) return;

                ArrayList<String> idStrings = new ArrayList<>();
                for (Integer i : ids) idStrings.add(String.valueOf(i));

                Bundle args = new Bundle();
                args.putStringArrayList("movieIds", idStrings);

                CardOfWatchedMovieFragment fragment = new CardOfWatchedMovieFragment();
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
                        Toast.makeText(requireContext(), "Ошибка загрузки просмотренных: " + message, Toast.LENGTH_LONG).show()
                );
            }
        });
    }
}