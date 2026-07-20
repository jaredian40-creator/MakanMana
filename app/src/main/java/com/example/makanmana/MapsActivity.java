package com.example.makanmana;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.makanmana.data.PlacesService;
import com.example.makanmana.model.PlaceItem;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient client;
    private LatLng currentLatLng;
    private String lastType = "restaurant";

    private Chip chipOpenNow, chipHalal, chipBudget, chipHighRated;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 44;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 55;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        NotificationHelper.createChannel(this);
        requestNotificationPermissionIfNeeded();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        client = LocationServices.getFusedLocationProviderClient(this);

        chipOpenNow = findViewById(R.id.chipOpenNow);
        chipHalal = findViewById(R.id.chipHalal);
        chipBudget = findViewById(R.id.chipBudget);
        chipHighRated = findViewById(R.id.chipHighRated);

        findViewById(R.id.btnRestaurant).setOnClickListener(v -> {
            lastType = "restaurant";
            runSearch();
        });
        findViewById(R.id.btnCafe).setOnClickListener(v -> {
            lastType = "cafe";
            runSearch();
        });

        // Re-run the search whenever a filter chip is toggled.
        for (Chip chip : new Chip[]{chipOpenNow, chipHalal, chipBudget, chipHighRated}) {
            chip.setOnCheckedChangeListener((b, checked) -> runSearch());
        }

        findViewById(R.id.btnFilterInfo).setOnClickListener(v -> showFilterInfo());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void runSearch() {
        if (currentLatLng == null) {
            Toast.makeText(this, "Location not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String keyword = chipHalal.isChecked() ? "halal" : null;
        boolean openNow = chipOpenNow.isChecked();
        int maxPrice = chipBudget.isChecked() ? 1 : -1;
        boolean topRatedOnly = chipHighRated.isChecked();

        PlacesService.searchNearby(this, currentLatLng.latitude, currentLatLng.longitude,
                lastType, keyword, openNow, -1, maxPrice, new PlacesService.Callback() {
                    @Override
                    public void onResult(List<PlaceItem> places) {
                        List<PlaceItem> list = new ArrayList<>(places);
                        if (topRatedOnly) {
                            list.removeIf(p -> p.rating < 4.0);
                            Collections.sort(list, (a, c) -> Double.compare(c.rating, a.rating));
                        }
                        renderMarkers(list);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e("MakanMana", "Search failed: " + message);
                        Toast.makeText(MapsActivity.this, "Search failed: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderMarkers(List<PlaceItem> places) {
        if (mMap == null) return;
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));

        int shown = Math.min(places.size(), 20);
        PlaceItem standout = null;   // best-rated place, if it's genuinely great
        for (int i = 0; i < shown; i++) {
            PlaceItem p = places.get(i);
            if (p.rating >= 4.5 && (standout == null || p.rating > standout.rating)) standout = p;
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(p.lat, p.lng))
                    .title(p.name)
                    .snippet(p.rating > 0 ? "⭐ " + p.rating : null)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            if (marker != null) marker.setTag(p.placeId);
        }

        if (shown == 0) {
            Toast.makeText(this, "No places match those filters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Notifications only for something worth interrupting the user about:
        // a standout, highly-rated (>= 4.5) place nearby. Ordinary searches stay quiet.
        if (standout != null) {
            NotificationHelper.showSuggestion(this,
                    "⭐ Highly rated nearby",
                    standout.name + " is rated " + standout.rating + " near you. Tap its pin to see more.");
        }
    }

    /** Explains exactly how each filter changes the Google Places search. */
    private void showFilterInfo() {
        String msg =
                "Filters are applied to the Google Places search near you:\n\n" +
                "• Open now — asks Google to return only places currently open " +
                "(opennow=true).\n\n" +
                "• Halal — adds the keyword \"halal\", so Google matches places whose " +
                "name/description mention halal. It's a text match, not an official " +
                "certification, so double-check for strict needs.\n\n" +
                "• Budget $ — limits results to Google's cheapest price level " +
                "(maxprice=1, \"inexpensive\"). Places with no price data may be hidden.\n\n" +
                "• Top rated ⭐ — keeps only places rated 4.0+ and sorts by rating " +
                "(applied in the app after results come back).\n\n" +
                "You can combine filters — e.g. Halal + Open now.";
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("How filters work")
                .setMessage(msg)
                .setPositiveButton("Got it", null)
                .show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnInfoWindowClickListener(marker -> {
            String placeId = (String) marker.getTag();
            if (placeId != null) {
                Intent intent = new Intent(MapsActivity.this, PlaceDetailActivity.class);
                intent.putExtra("PLACE_ID", placeId);
                if (currentLatLng != null) {
                    intent.putExtra("USER_LAT", currentLatLng.latitude);
                    intent.putExtra("USER_LNG", currentLatLng.longitude);
                }
                startActivity(intent);
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));
                    } else {
                        Toast.makeText(MapsActivity.this, "Unable to fetch location. Please turn on GPS.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                getCurrentLocation();
            }
        }
    }
}
