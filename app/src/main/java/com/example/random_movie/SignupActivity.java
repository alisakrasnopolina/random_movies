package com.example.random_movie;

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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import javax.mail.internet.InternetAddress;

public class SignupActivity extends AppCompatActivity {

    EditText signupName, signupEmail, signupPassword;
    Button signupButton, loginRedirectButton;
    FirebaseDatabase database;
    DatabaseReference reference;

    static String encodeUserEmail(String userEmail) {
        return userEmail.replace(".", ",");
    }

    static String decodeUserEmail(String userEmail) {
        return userEmail.replace(",", ".");
    }

    public Boolean validateEmail() {
        String val = signupEmail.getText().toString();
        try {
            new InternetAddress(val).validate();
            return true;
        } catch (javax.mail.internet.AddressException e) {
            signupEmail.setError("Email cannot be empty");
            return false;
        }
    }

    public Boolean validatePassword(){
        String val = signupPassword.getText().toString();
        if (val.isEmpty()) {
            signupPassword.setError("Password cannot be empty");
            return false;
        } else {
            signupPassword.setError(null);
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupName = findViewById(R.id.signup_name);
        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        loginRedirectButton = findViewById(R.id.loginRedirectButton);
        signupButton = findViewById(R.id.signup_button);

        SharedPreferences preferences = getSharedPreferences("login", MODE_PRIVATE);
        String login = preferences.getString("remember", "");

        if (login.equals("true")) {
            Intent intent = new Intent(SignupActivity.this, FindRandomMovie.class);
            startActivity(intent);
        } else if (login.equals("false")) {
            Toast.makeText(this, "Пожалуйста, войдите.", Toast.LENGTH_SHORT).show();
        }

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!validateEmail() | !validatePassword()) {

                } else {
                    database = FirebaseDatabase.getInstance();
                    reference = database.getReference("users");

                    String name = signupName.getText().toString();
                    String email = signupEmail.getText().toString();
                    String password = signupPassword.getText().toString();

                    HelperClass helperClass = new HelperClass(name, encodeUserEmail(email), password);
                    DatabaseReference pushedPostRef = reference.push();
                    String postId = pushedPostRef.getKey();
                    reference.child(postId).setValue(helperClass);

                    Toast.makeText(SignupActivity.this, "You have signup successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            }
        });

        loginRedirectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}