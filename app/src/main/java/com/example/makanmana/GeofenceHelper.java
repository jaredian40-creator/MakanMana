package com.example.makanmana;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Registers a geofence around a destination so the app can fire a local
 * "you've arrived" notification once the user enters it.
 */
public final class GeofenceHelper {

    private static final float RADIUS_METERS = 120f;    // arrival zone
    private static final String TAG = "MakanMana";

    private GeofenceHelper() {
    }

    public static void registerArrivalGeofence(Context context, String placeId,
                                               String placeName, double lat, double lng) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot add geofence: location permission not granted.");
            return;
        }

        Geofence geofence = new Geofence.Builder()
                .setRequestId(placeId)
                .setCircularRegion(lat, lng, RADIUS_METERS)
                .setExpirationDuration(2 * 60 * 60 * 1000L) // 2 hours
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setLoiteringDelay(5000)
                .build();

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        intent.putExtra(GeofenceBroadcastReceiver.EXTRA_PLACE_NAME, placeName);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 2001, intent, flags);

        GeofencingClient client = LocationServices.getGeofencingClient(context);
        client.addGeofences(request, pendingIntent)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Arrival geofence added for " + placeName))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add geofence: " + e.getMessage());
                    Toast.makeText(context,
                            "Couldn't set up arrival alert (enable location & try again)",
                            Toast.LENGTH_SHORT).show();
                });
    }
}
