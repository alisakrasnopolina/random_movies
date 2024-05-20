package com.example.random_movie;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    TextView profileName, profileEmail, profileUsername, profilePassword;
    TextView titleName, titleUsername;
    Button editProfile, logoutButton;
    String name, password, email;
    FirebaseDatabase database;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        editProfile = findViewById(R.id.edit_button);
        logoutButton = findViewById(R.id.exit_button);

        SharedPreferences preferences = getSharedPreferences("login", MODE_PRIVATE);
        String login = preferences.getString("remember", "");
        String userID = preferences.getString("userID", "");

        database = FirebaseDatabase.getInstance();
        reference = database.getReference("users");

        if (login.equals("true")) {
            reference.addListenerForSingleValueEvent(new ValueEventListener() {

                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()){
                        name = snapshot.child(userID).child("name").getValue(String.class);
                        email = snapshot.child(userID).child("email").getValue(String.class).replace(",", ".");
                        showAllUserData();
                    }
                }

                public void onCancelled(DatabaseError databaseError) {
                    Log.d("error", "The read failed: " + databaseError.getCode());
                }
            });
        } else if (login.equals("false")) {
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.random_movie) {
                startActivity(new Intent(getApplicationContext(), FindRandomMovie.class));
                finish();
                return true;
            }
            else if(item.getItemId() == R.id.liked_movies) {
                startActivity(new Intent(getApplicationContext(), LikedMoviesActivity.class));
                finish();
                return true;
            }
            else if(item.getItemId() == R.id.home) {
                return true;
            } else {
                return false;
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences preferences = getSharedPreferences("login", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("remember", "false");
                editor.apply();

                finish();
            }
        });

        showAllUserData();

        editProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                reference.addListenerForSingleValueEvent(new ValueEventListener() {

                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()){

                            String nameFromDB = snapshot.child(userID).child("name").getValue(String.class);
                            String emailFromDB = snapshot.child(userID).child("email").getValue(String.class);
                            String passwordFromDB = snapshot.child(userID).child("password").getValue(String.class);

                            Intent intent = new Intent(MainActivity.this, EditProfileActivity.class);

                            intent.putExtra("userID", userID);
                            intent.putExtra("name", nameFromDB);
                            intent.putExtra("email", emailFromDB);
                            intent.putExtra("password", passwordFromDB);

                            startActivity(intent);

                        }
                    }

                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("error", "The read failed: " + databaseError.getCode());
                    }
                });


            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        showAllUserData();
    }

    public void showAllUserData(){
        profileName.setText(name);
        profileEmail.setText(email);
        Log.d("heck", "what");
    }
}