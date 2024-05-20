package com.example.random_movie;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FilterFindMovieActivity extends AppCompatActivity {

//    EditText signupName, signupEmail, signupPassword;
    ImageView joyImage;
    CardView cardFun, cardSad, cardAnger, cardScary, cardAnxiety, cardLove, cardDreams, cardCartoons;
//    FirebaseDatabase database;
//    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_find_movie);

        cardFun = findViewById(R.id.card_fun);
        cardSad = findViewById(R.id.card_sad);
        cardAnger = findViewById(R.id.card_anger);
        cardScary = findViewById(R.id.card_scary);
        cardAnxiety = findViewById(R.id.card_anxiety);
        cardLove = findViewById(R.id.card_love);
        cardDreams = findViewById(R.id.card_dream);
        cardCartoons = findViewById(R.id.card_cartoon);

        cardFun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FilterFindMovieActivity.this, FindRandomMovie.class);
                intent.putExtra("genre", "комедия");
                setResult(200, intent);
                finish();
            }
        });

        cardSad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FilterFindMovieActivity.this, FindRandomMovie.class);
                intent.putExtra("genre", "драма");
                setResult(200, intent);
                finish();
            }
        });

        cardAnger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FilterFindMovieActivity.this, FindRandomMovie.class);
                intent.putExtra("genre", "боевик");
                setResult(200, intent);
                finish();
            }
        });

        cardScary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FilterFindMovieActivity.this, FindRandomMovie.class);
                intent.putExtra("genre", "ужасы");
                setResult(200, intent);
                finish();
            }
        });

        cardAnxiety.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FilterFindMovieActivity.this, FindRandomMovie.class);
                intent.putExtra("genre", "триллер");
                setResult(200, intent);
                finish();
            }
        });

        cardLove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FilterFindMovieActivity.this, FindRandomMovie.class);
                intent.putExtra("genre", "мелодрама");
                setResult(200, intent);
                finish();
            }
        });

        cardDreams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FilterFindMovieActivity.this, FindRandomMovie.class);
                intent.putExtra("genre", "фантастика"); // "фантастика" "фэнтези"
                setResult(200, intent);
                finish();
            }
        });

        cardCartoons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FilterFindMovieActivity.this, FindRandomMovie.class);
                intent.putExtra("genre", "мультфильм");
                setResult(200, intent);
                finish();
            }
        });
    }
}