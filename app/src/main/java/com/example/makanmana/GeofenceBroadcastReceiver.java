package com.example.makanmana;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Receives geofence transition events and shows an arrival notification when the
 * user enters the destination's geofence.
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    public static final String EXTRA_PLACE_NAME = "place_name";
    private static final String TAG = "MakanMana";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null || event.hasError()) {
            Log.e(TAG, "Geofence event error.");
            return;
        }

        int transition = event.getGeofenceTransition();
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER) {
            return;
        }

        String placeName = intent.getStringExtra(EXTRA_PLACE_NAME);
        if (placeName == null) {
            List<Geofence> triggering = event.getTriggeringGeofences();
            placeName = (triggering != null && !triggering.isEmpty()) ? "your destination" : "your destination";
        }

        NotificationHelper.showArrival(context,
                "🎉 You've arrived!",
                "Welcome to " + placeName + ". Selamat makan — enjoy your meal!");
    }
}
