package com.example.makanmana;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.makanmana.ui.HomeFragment;
import com.example.makanmana.ui.ProfileFragment;
import com.example.makanmana.ui.SavedFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Main hub after login. Hosts the bottom navigation and swaps between the Home,
 * Saved and Profile fragments. The Map tab launches the full-screen MapsActivity.
 */
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        NotificationHelper.createChannel(this);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                show(new HomeFragment());
                return true;
            } else if (id == R.id.nav_saved) {
                show(new SavedFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                show(new ProfileFragment());
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapsActivity.class));
                return false; // keep the current tab selected
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void show(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_container, fragment)
                .commit();
    }
}
