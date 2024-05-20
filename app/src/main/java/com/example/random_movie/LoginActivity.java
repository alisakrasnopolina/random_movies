package com.example.random_movie;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    EditText loginEmail, loginPassword;
    Button loginButton, signupRedirectButton;
    String userId;
    String nameFromDB;
    String emailFromDB;
    String passwordFromDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectButton = findViewById(R.id.loginRedirectButton);

        SharedPreferences preferences = getSharedPreferences("login", MODE_PRIVATE);
        String login = preferences.getString("remember", "");

        if (login.equals("true")) {
            Intent intent = new Intent(LoginActivity.this, FindRandomMovie.class);
            startActivity(intent);
        } else if (login.equals("false")) {
            Toast.makeText(this, "Пожалуйста, войдите.", Toast.LENGTH_SHORT).show();
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!validateEmail() | !validatePassword()) {
                    SharedPreferences preferences = getSharedPreferences("login", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("remember", "false");
                    editor.apply();
                } else {
                    checkUser();
                }
            }
        });

        signupRedirectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

    }

    public Boolean validateEmail() {
        String val = loginEmail.getText().toString();
        try {
            new InternetAddress(val).validate();
            return true;
        } catch (javax.mail.internet.AddressException e) {
            loginEmail.setError("Email cannot be empty");
            return false;
        }
    }

    public Boolean validatePassword(){
        String val = loginPassword.getText().toString();
        if (val.isEmpty()) {
            loginPassword.setError("Password cannot be empty");
            return false;
        } else {
            loginPassword.setError(null);
            return true;
        }
    }
    static String encodeUserEmail(String userEmail) {
        return userEmail.replace(".", ",");
    }

    static String decodeUserEmail(String userEmail) {
        return userEmail.replace(",", ".");
    }

    public void checkUser(){
        String userEmail = loginEmail.getText().toString().trim();
        String UserEmailEncoded = encodeUserEmail(userEmail);
        String userPassword = loginPassword.getText().toString().trim();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        Query checkUserDatabase = reference.orderByChild("email").equalTo(UserEmailEncoded);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()){

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        userId = snapshot.getKey();
                    }

                    loginEmail.setError(null);
                    Log.d("DEBUG", "UserEmailEncoded: " + UserEmailEncoded); // Log UserEmailEncoded

                    passwordFromDB = dataSnapshot.child(userId).child("password").getValue(String.class);
                    Log.d("DEBUG", "PasswordFromDB: " + passwordFromDB);

                    if (passwordFromDB != null && passwordFromDB.equals(userPassword)) {
                        loginEmail.setError(null);

                        nameFromDB = dataSnapshot.child(userId).child("name").getValue(String.class);
                        emailFromDB = dataSnapshot.child(userId).child("email").getValue(String.class);

                        SharedPreferences preferences = getSharedPreferences("login", MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("remember", "true");
                        editor.putString("name", nameFromDB);
                        editor.putString("email",emailFromDB);
                        editor.putString("password", passwordFromDB);
                        editor.putString("userID", userId);
                        editor.apply();

//                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        Intent intent = new Intent(LoginActivity.this, FindRandomMovie.class);
//                        Intent intent = new Intent(LoginActivity.this, NavigationBar.class);

                        intent.putExtra("name", nameFromDB);
                        intent.putExtra("email", decodeUserEmail(emailFromDB));
                        intent.putExtra("password", passwordFromDB);

                        startActivity(intent);
                    } else {
                        loginPassword.setError("Invalid Credentials");
                        loginPassword.requestFocus();
                    }
                } else {
                    loginEmail.setError("User does not exist");
                    loginEmail.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}