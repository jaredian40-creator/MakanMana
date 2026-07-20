package com.example.makanmana;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Central place to read app configuration.
 * <p>
 * The Google Maps / Places API key is no longer hardcoded in source. It lives in
 * {@code local.properties} (git-ignored) and is injected into the manifest as the
 * {@code com.google.android.geo.API_KEY} meta-data via the secrets-gradle-plugin.
 * We read it back here at runtime so there is a single source of truth.
 */
public final class AppConfig {

    private static String cachedKey;

    private AppConfig() {
    }

    public static String getMapsApiKey(Context context) {
        if (cachedKey != null) {
            return cachedKey;
        }
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) {
                cachedKey = ai.metaData.getString("com.google.android.geo.API_KEY", "");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("MakanMana", "Could not read API key from manifest: " + e.getMessage());
        }
        return cachedKey == null ? "" : cachedKey;
    }
}
