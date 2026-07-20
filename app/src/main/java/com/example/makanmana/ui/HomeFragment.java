package com.example.makanmana.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.makanmana.MapsActivity;
import com.example.makanmana.PlaceDetailActivity;
import com.example.makanmana.R;
import com.example.makanmana.adapter.PlaceCardAdapter;
import com.example.makanmana.data.PlacesService;
import com.example.makanmana.model.PlaceItem;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Home tab: greeting, the "Surprise Me" decision spinner, and a nearby carousel. */
public class HomeFragment extends Fragment {

    private FusedLocationProviderClient locationClient;
    private Double lat, lng;
    private final List<PlaceItem> nearby = new ArrayList<>();
    private PlaceCardAdapter adapter;
    private TextView tvNearbyStatus;

    private final ActivityResultLauncher<String> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    fetchLocationThenLoad();
                } else {
                    tvNearbyStatus.setText("Location permission needed to find places near you.");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        locationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        tvNearbyStatus = view.findViewById(R.id.tvNearbyStatus);

        TextView greeting = view.findViewById(R.id.tvGreeting);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String who = (user != null && user.getEmail() != null) ? user.getEmail().split("@")[0] : "Food Hunter";
        greeting.setText("Lapar, " + who + "? 👋");

        RecyclerView rv = view.findViewById(R.id.rvNearby);
        rv.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new PlaceCardAdapter(nearby, R.layout.item_place_card_h, this::openDetail);
        rv.setAdapter(adapter);

        view.findViewById(R.id.btnOpenMap).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MapsActivity.class)));

        view.findViewById(R.id.btnSurprise).setOnClickListener(v -> surpriseMe());

        ensureLocation();
    }

    private void ensureLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocationThenLoad();
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void fetchLocationThenLoad() {
        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc == null) {
                        tvNearbyStatus.setText("Turn on GPS to see nearby spots.");
                        return;
                    }
                    lat = loc.getLatitude();
                    lng = loc.getLongitude();
                    loadNearby();
                });
    }

    private void loadNearby() {
        if (lat == null || lng == null) return;
        tvNearbyStatus.setText("Finding tasty spots around you…");
        PlacesService.searchNearby(requireContext(), lat, lng, "restaurant", null, false, -1, -1,
                new PlacesService.Callback() {
                    @Override
                    public void onResult(List<PlaceItem> places) {
                        if (!isAdded()) return;
                        nearby.clear();
                        nearby.addAll(places);
                        adapter.notifyDataSetChanged();
                        tvNearbyStatus.setText(places.isEmpty()
                                ? "No places found nearby."
                                : places.size() + " spots near you");
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) tvNearbyStatus.setText("Couldn't load places: " + message);
                    }
                });
    }

    /** Picks a nearby place weighted by rating and opens it. */
    private void surpriseMe() {
        if (lat == null || lng == null) {
            Toast.makeText(requireContext(), "Getting your location first…", Toast.LENGTH_SHORT).show();
            ensureLocation();
            return;
        }
        if (!nearby.isEmpty()) {
            openDetail(weightedPick(nearby));
        } else {
            PlacesService.searchNearby(requireContext(), lat, lng, "restaurant", null, false, -1, -1,
                    new PlacesService.Callback() {
                        @Override
                        public void onResult(List<PlaceItem> places) {
                            if (!isAdded()) return;
                            if (places.isEmpty()) {
                                Toast.makeText(requireContext(), "No spots found nearby", Toast.LENGTH_SHORT).show();
                            } else {
                                openDetail(weightedPick(places));
                            }
                        }

                        @Override
                        public void onError(String message) {
                            if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private PlaceItem weightedPick(List<PlaceItem> list) {
        double total = 0;
        for (PlaceItem p : list) total += Math.max(p.rating, 1.0);
        double r = new Random().nextDouble() * total;
        for (PlaceItem p : list) {
            r -= Math.max(p.rating, 1.0);
            if (r <= 0) return p;
        }
        return list.get(0);
    }

    private void openDetail(PlaceItem p) {
        Intent i = new Intent(requireContext(), PlaceDetailActivity.class);
        i.putExtra("PLACE_ID", p.placeId);
        if (lat != null && lng != null) {
            i.putExtra("USER_LAT", lat);
            i.putExtra("USER_LNG", lng);
        }
        startActivity(i);
    }
}
