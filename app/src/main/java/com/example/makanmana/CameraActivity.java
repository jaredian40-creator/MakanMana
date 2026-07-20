package com.example.makanmana;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.makanmana.data.PhotoRepository;
import com.example.makanmana.ui.PhotoDetailsDialog;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.Locale;

/**
 * In-app camera capture. After a photo is taken we prefill the "place" from the
 * phone's GPS location (reverse-geocoded), let the user add a description and a
 * star rating, then save it to their profile (Firestore, Base64).
 */
public class CameraActivity extends AppCompatActivity {

    private ImageView imgPreview;
    private TextView tvHint;
    private FusedLocationProviderClient locationClient;

    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else Toast.makeText(this, "Camera permission is required to take a photo",
                        Toast.LENGTH_SHORT).show();
            });

    // Best-effort location permission so we can prefill the place; no-op if denied.
    private final ActivityResultLauncher<String> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    private final ActivityResultLauncher<android.content.Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null
                        && result.getData().getExtras() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        imgPreview.setImageBitmap(photo);
                        tvHint.setText("Add a few details to save it.");
                        collectDetailsAndSave(photo);
                        return;
                    }
                }
                Toast.makeText(this, "No photo captured", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        imgPreview = findViewById(R.id.imgPreview);
        tvHint = findViewById(R.id.tvHint);
        Button btnCapture = findViewById(R.id.btnCapture);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        btnCapture.setOnClickListener(v -> ensurePermissionThenCapture());
    }

    private void ensurePermissionThenCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        android.content.Intent takePicture = new android.content.Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePicture);
        } else {
            Toast.makeText(this, "No camera app found on this device", Toast.LENGTH_SHORT).show();
        }
    }

    /** Prefill the place from GPS (if allowed), then show the details dialog. */
    private void collectDetailsAndSave(Bitmap photo) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            showDialog(photo, "");
            return;
        }
        try {
            locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(loc -> {
                        if (loc != null) reverseGeocode(loc, place -> showDialog(photo, place));
                        else showDialog(photo, "");
                    })
                    .addOnFailureListener(e -> showDialog(photo, ""));
        } catch (SecurityException e) {
            showDialog(photo, "");
        }
    }

    private interface PlaceCallback {
        void onPlace(String place);
    }

    /** Geocoder is a blocking call, so run it off the main thread. */
    private void reverseGeocode(Location loc, PlaceCallback cb) {
        new Thread(() -> {
            String place = "";
            try {
                if (Geocoder.isPresent()) {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> results = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                    if (results != null && !results.isEmpty()) {
                        Address a = results.get(0);
                        String area = a.getSubLocality() != null ? a.getSubLocality()
                                : (a.getFeatureName() != null ? a.getFeatureName() : "");
                        String city = a.getLocality() != null ? a.getLocality() : "";
                        if (!area.isEmpty() && !city.isEmpty() && !area.equals(city)) place = area + ", " + city;
                        else if (!area.isEmpty()) place = area;
                        else place = city;
                    }
                }
            } catch (Exception ignored) {
            }
            final String result = place;
            runOnUiThread(() -> cb.onPlace(result));
        }).start();
    }

    private void showDialog(Bitmap photo, String prefillPlace) {
        PhotoDetailsDialog.show(this, prefillPlace, (place, caption, rating) -> savePhoto(photo, place, caption, rating));
    }

    private void savePhoto(Bitmap photo, String place, String caption, float rating) {
        tvHint.setText("Saving to your profile…");
        PhotoRepository.uploadUserPhoto(photo, caption, rating, place, new PhotoRepository.UploadCallback() {
            @Override
            public void onSuccess(String id) {
                tvHint.setText("Saved to your profile! Tap the button to take another.");
                Toast.makeText(CameraActivity.this, "Photo saved to your profile", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                tvHint.setText("Couldn't save photo. Tap to try again.");
                Toast.makeText(CameraActivity.this, "Save failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
