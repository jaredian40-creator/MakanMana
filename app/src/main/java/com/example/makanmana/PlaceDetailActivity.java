package com.example.makanmana;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.makanmana.adapter.PhotoAdapter;
import com.example.makanmana.data.FavouritesRepository;
import com.example.makanmana.data.PhotoRepository;
import com.example.makanmana.model.Photo;
import com.example.makanmana.model.PlaceItem;
import com.example.makanmana.ui.PhotoDetailsDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlaceDetailActivity extends AppCompatActivity {

    private ImageView placeImage;
    private TextView tvName, tvRating, tvAddress, tvPhone, tvDistance, tvNoPhotos;
    private MaterialButton btnNavigate, btnAddPhoto;
    private FloatingActionButton btnFavourite;
    private RecyclerView rvPhotos;

    private String placeId;
    private PlaceItem currentPlace;
    private boolean isFavourite = false;

    private final List<Photo> photos = new ArrayList<>();
    private PhotoAdapter photoAdapter;

    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<android.content.Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null
                        && result.getData().getExtras() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) uploadPhoto(photo);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

        placeImage = findViewById(R.id.placeImage);
        tvName = findViewById(R.id.tvName);
        tvRating = findViewById(R.id.tvRating);
        tvAddress = findViewById(R.id.tvAddress);
        tvPhone = findViewById(R.id.tvPhone);
        tvDistance = findViewById(R.id.tvDistance);
        tvNoPhotos = findViewById(R.id.tvNoPhotos);
        btnNavigate = findViewById(R.id.btnNavigate);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnFavourite = findViewById(R.id.btnFavourite);
        rvPhotos = findViewById(R.id.rvPhotos);

        rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        photoAdapter = new PhotoAdapter(photos, this::onPhotoTapped);
        rvPhotos.setAdapter(photoAdapter);

        placeId = getIntent().getStringExtra("PLACE_ID");
        if (placeId == null) {
            Toast.makeText(this, "Error: Place ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        btnFavourite.setOnClickListener(v -> toggleFavourite());
        btnAddPhoto.setOnClickListener(v -> ensureCameraThenCapture());

        refreshFavouriteIcon();
        loadPhotos();
        getPlaceDetails(placeId);
    }

    private void getPlaceDetails(String placeId) {
        String url = "https://maps.googleapis.com/maps/api/place/details/json?place_id="
                + placeId + "&key=" + AppConfig.getMapsApiKey(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                if (!response.getString("status").equals("OK")) return;
                JSONObject result = response.getJSONObject("result");

                String name = result.optString("name", "Name not available");
                String address = result.optString("formatted_address", "Address not available");
                tvName.setText(name);
                tvAddress.setText(address);
                tvPhone.setText(result.optString("formatted_phone_number", "Phone not available"));

                double rating = result.optDouble("rating", 0.0);
                tvRating.setText(rating > 0
                        ? String.format(Locale.getDefault(), "⭐ %.1f / 5.0", rating)
                        : "⭐ Rating: N/A");

                String photoRef = null;
                if (result.has("photos")) {
                    JSONArray photos = result.getJSONArray("photos");
                    photoRef = photos.getJSONObject(0).getString("photo_reference");
                    String photoUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photo_reference="
                            + photoRef + "&key=" + AppConfig.getMapsApiKey(this);
                    Glide.with(this).load(photoUrl).placeholder(R.drawable.ic_food_placeholder).into(placeImage);
                }

                JSONObject location = result.getJSONObject("geometry").getJSONObject("location");
                double destLat = location.getDouble("lat");
                double destLng = location.getDouble("lng");

                currentPlace = new PlaceItem(placeId, name, address, rating, destLat, destLng, photoRef, -1, false);
                refreshFavouriteIcon();

                double userLat = getIntent().getDoubleExtra("USER_LAT", 0.0);
                double userLng = getIntent().getDoubleExtra("USER_LNG", 0.0);
                if (userLat != 0.0 && userLng != 0.0) {
                    float[] d = new float[1];
                    android.location.Location.distanceBetween(userLat, userLng, destLat, destLng, d);
                    tvDistance.setText(String.format(Locale.getDefault(), "📍 %.2f km away", d[0] / 1000));
                } else {
                    tvDistance.setText("📍 Distance: Unknown");
                }

                btnNavigate.setOnClickListener(v -> {
                    GeofenceHelper.registerArrivalGeofence(this, placeId, name, destLat, destLng);
                    android.net.Uri uri = android.net.Uri.parse("google.navigation:q=" + destLat + "," + destLng);
                    android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, uri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                });

            } catch (Exception e) {
                Toast.makeText(this, "Error parsing details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, error -> Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }

    // ---------- Favourites ----------

    private void refreshFavouriteIcon() {
        FavouritesRepository.isFavourite(placeId, fav -> {
            isFavourite = fav;
            btnFavourite.setImageResource(fav ? R.drawable.ic_heart : R.drawable.ic_heart_border);
        });
    }

    private void toggleFavourite() {
        if (currentPlace == null) {
            Toast.makeText(this, "Still loading place…", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isFavourite) {
            FavouritesRepository.remove(placeId, () -> {
                isFavourite = false;
                btnFavourite.setImageResource(R.drawable.ic_heart_border);
                Toast.makeText(this, "Removed from saved", Toast.LENGTH_SHORT).show();
            });
        } else {
            FavouritesRepository.add(currentPlace, () -> {
                isFavourite = true;
                btnFavourite.setImageResource(R.drawable.ic_heart);
                Toast.makeText(this, "Saved to favourites ❤", Toast.LENGTH_SHORT).show();
            });
        }
    }

    // ---------- Photos ----------

    private void ensureCameraThenCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        android.content.Intent i = new android.content.Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (i.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(i);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadPhoto(Bitmap bitmap) {
        // Prefill the place with this restaurant's name, then collect a description + rating.
        String prefill = currentPlace != null ? currentPlace.name : "";
        PhotoDetailsDialog.show(this, prefill, (place, caption, rating) -> {
            Toast.makeText(this, "Uploading photo…", Toast.LENGTH_SHORT).show();
            PhotoRepository.upload(placeId, bitmap, caption, rating, place,
                    new PhotoRepository.UploadCallback() {
                        @Override
                        public void onSuccess(String id) {
                            Toast.makeText(PlaceDetailActivity.this, "Photo added!", Toast.LENGTH_SHORT).show();
                            loadPhotos();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(PlaceDetailActivity.this, "Upload failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    /** Tapping a photo you added lets you edit its details or delete it. */
    private void onPhotoTapped(Photo p) {
        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        boolean mine = user != null && p.uploadedBy != null && p.uploadedBy.equals(user.getUid());
        if (!mine) {
            Toast.makeText(this, "You can only edit photos you added", Toast.LENGTH_SHORT).show();
            return;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(p.caption != null && !p.caption.isEmpty() ? p.caption : "Photo")
                .setItems(new String[]{"Edit details", "Delete"}, (d, which) -> {
                    if (which == 0) {
                        PhotoDetailsDialog.showEdit(this, p.placeName, p.caption, (float) p.rating,
                                (place, caption, rating) ->
                                        PhotoRepository.updatePlacePhoto(placeId, p.id, caption, rating, place, () -> {
                                            Toast.makeText(this, "Photo updated", Toast.LENGTH_SHORT).show();
                                            loadPhotos();
                                        }));
                    } else {
                        confirmDelete(p);
                    }
                })
                .show();
    }

    private void confirmDelete(Photo p) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Delete photo?")
                .setMessage("This will remove the photo permanently.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) ->
                        PhotoRepository.deletePlacePhoto(placeId, p.id, () -> {
                            Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
                            loadPhotos();
                        }))
                .show();
    }

    private void loadPhotos() {
        PhotoRepository.getPhotos(placeId, new PhotoRepository.PhotosCallback() {
            @Override
            public void onResult(List<Photo> result) {
                photos.clear();
                photos.addAll(result);
                photoAdapter.notifyDataSetChanged();
                tvNoPhotos.setVisibility(result.isEmpty() ? TextView.VISIBLE : TextView.GONE);
            }

            @Override
            public void onError(String message) {
                tvNoPhotos.setText("Couldn't load photos.");
            }
        });
    }
}
