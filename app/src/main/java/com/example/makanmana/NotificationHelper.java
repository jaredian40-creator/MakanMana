package com.example.makanmana;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Small helper around local notifications used by MakanMana.
 * <p>
 * Two kinds of notifications are produced:
 *   - "suggestion" : fired when nearby restaurants/cafes are found for the user.
 *   - "arrival"    : fired by {@link GeofenceBroadcastReceiver} when the user
 *                    reaches a place they navigated to.
 */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "makanmana_channel";
    private static final String CHANNEL_NAME = "MakanMana Alerts";

    private NotificationHelper() {
    }

    /** Create the notification channel. Safe to call multiple times. */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Nearby food suggestions and arrival alerts");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /** Notification suggesting nearby places; tapping it opens the map. */
    public static void showSuggestion(Context context, String title, String text) {
        Intent intent = new Intent(context, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        show(context, 1001, android.R.drawable.ic_dialog_map, title, text, intent);
    }

    /** Notification confirming the user has arrived at a destination. */
    public static void showArrival(Context context, String title, String text) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        show(context, 1002, android.R.drawable.star_on, title, text, intent);
    }

    private static void show(Context context, int id, int icon, String title, String text, Intent tapIntent) {
        createChannel(context);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, tapIntent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // notify() silently no-ops if POST_NOTIFICATIONS is not granted (Android 13+),
        // so this call is always safe.
        NotificationManagerCompat.from(context).notify(id, builder.build());
    }
}
