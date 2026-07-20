package com.example.makanmana.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.makanmana.CameraActivity;
import com.example.makanmana.MainActivity;
import com.example.makanmana.R;
import com.example.makanmana.adapter.PhotoAdapter;
import com.example.makanmana.data.FavouritesRepository;
import com.example.makanmana.data.PhotoRepository;
import com.example.makanmana.model.Photo;
import com.example.makanmana.model.PlaceItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/** Profile tab: user info, saved count, snapped meal photos, and logout. */
public class ProfileFragment extends Fragment {

    private TextView tvFavCount, tvNoMeals;
    private final List<Photo> myPhotos = new ArrayList<>();
    private PhotoAdapter photoAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView name = view.findViewById(R.id.tvProfileName);
        TextView email = view.findViewById(R.id.tvProfileEmail);
        tvFavCount = view.findViewById(R.id.tvFavCount);
        tvNoMeals = view.findViewById(R.id.tvNoMeals);

        RecyclerView rv = view.findViewById(R.id.rvMyPhotos);
        rv.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        photoAdapter = new PhotoAdapter(myPhotos, this::onPhotoTapped);
        rv.setAdapter(photoAdapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            email.setText(user.getEmail());
            name.setText(user.getEmail().split("@")[0]);
        }

        view.findViewById(R.id.btnSnap).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CameraActivity.class)));

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(requireContext(), MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            requireActivity().finish();
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        FavouritesRepository.getAll(new FavouritesRepository.ListCallback() {
            @Override
            public void onResult(List<PlaceItem> places) {
                if (isAdded()) tvFavCount.setText("❤ " + places.size() + " saved spots");
            }

            @Override
            public void onError(String message) {
                /* leave default */
            }
        });

        loadMyPhotos();
    }

    private void loadMyPhotos() {
        PhotoRepository.getUserPhotos(new PhotoRepository.PhotosCallback() {
            @Override
            public void onResult(List<Photo> result) {
                if (!isAdded()) return;
                myPhotos.clear();
                myPhotos.addAll(result);
                photoAdapter.notifyDataSetChanged();
                tvNoMeals.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                /* leave default hint */
            }
        });
    }

    /** Tap a meal photo to edit its details or delete it. */
    private void onPhotoTapped(Photo p) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(p.caption != null && !p.caption.isEmpty() ? p.caption : "Meal photo")
                .setItems(new String[]{"Edit details", "Delete"}, (d, which) -> {
                    if (which == 0) {
                        PhotoDetailsDialog.showEdit(requireContext(), p.placeName, p.caption, (float) p.rating,
                                (place, caption, rating) ->
                                        PhotoRepository.updateUserPhoto(p.id, caption, rating, place, () -> {
                                            if (isAdded()) {
                                                Toast.makeText(requireContext(), "Photo updated", Toast.LENGTH_SHORT).show();
                                                loadMyPhotos();
                                            }
                                        }));
                    } else {
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Delete photo?")
                                .setMessage("This will remove the photo permanently.")
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Delete", (dd, w) ->
                                        PhotoRepository.deleteUserPhoto(p.id, () -> {
                                            if (isAdded()) {
                                                Toast.makeText(requireContext(), "Photo deleted", Toast.LENGTH_SHORT).show();
                                                loadMyPhotos();
                                            }
                                        }))
                                .show();
                    }
                })
                .show();
    }
}
