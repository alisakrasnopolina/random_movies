package com.example.random_movie;

import static android.content.Context.MODE_PRIVATE;

import static androidx.databinding.DataBindingUtil.setContentView;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.content.SharedPreferences;
import android.widget.Button;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LikedMoviesFragment extends Fragment {
    FirebaseDatabase database;
    DatabaseReference reference;
    String movieId;
    List<String> idsList = new ArrayList<String>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_liked_movies, container, false);

        super.onCreate(savedInstanceState);

        SharedPreferences preferences_login = this.getContext().getSharedPreferences("login", MODE_PRIVATE);
        String userID = preferences_login.getString("userID", "");
        Log.d("userId", userID);

        database = FirebaseDatabase.getInstance();
        reference = database.getReference("users/"+userID+"/liked");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Clear the list before populating it
                idsList.clear();
                // Iterate through the movie IDs and populate the list
                for (DataSnapshot movieSnapshot : dataSnapshot.getChildren()) {
                    Long movieId = movieSnapshot.getValue(Long.class);
                    Log.d("Movie ID", String.valueOf(movieId));
                    idsList.add(String.valueOf(movieId));
                }

                // Create a Bundle to pass data
                Bundle args = new Bundle();
                args.putStringArrayList("movieIds", new ArrayList<>(idsList)); // Put the movie ID in the Bundle

                CardOfLikedMovieFragment fragment = new CardOfLikedMovieFragment();
                fragment.setArguments(args);

                // Navigate to LikedMoviesFragment
                if (!isAdded()) return;
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.movie_container, fragment)
                        .commitAllowingStateLoss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("error", "The read failed: " + databaseError.getCode());
            }
        });
        return rootView;
    }
}