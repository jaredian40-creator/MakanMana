package com.example.makanmana; // Keep your package name!

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class GoogleActivity extends AppCompatActivity {

    private TextView userName, userEmail;
    private Button btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google);

        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        btnLogout = findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userName.setText("Welcome, Food Hunter!");
            userEmail.setText(currentUser.getEmail());
        }

        // Logout
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(GoogleActivity.this, MainActivity.class));
            finish();
        });

        Button btnOpenMap = findViewById(R.id.btnOpenMap);
        btnOpenMap.setOnClickListener(v -> {
            startActivity(new Intent(GoogleActivity.this, MapsActivity.class));
        });

        Button btnCamera = findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(v -> {
            startActivity(new Intent(GoogleActivity.this, CameraActivity.class));
        });
    }
}