package com.example.makanmana.model;

import android.content.Context;

import com.example.makanmana.AppConfig;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight representation of a place, shared by the map, the decision spinner,
 * favourites and the detail screen. Also serialises to/from Firestore.
 */
public class PlaceItem implements Serializable {

    public String placeId;
    public String name;
    public String address;
    public double rating;
    public double lat;
    public double lng;
    public String photoRef;   // Google Places photo_reference (may be null)
    public int priceLevel;    // 0-4, -1 if unknown
    public boolean openNow;

    public PlaceItem() {
    }

    public PlaceItem(String placeId, String name, String address, double rating,
                     double lat, double lng, String photoRef, int priceLevel, boolean openNow) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.lat = lat;
        this.lng = lng;
        this.photoRef = photoRef;
        this.priceLevel = priceLevel;
        this.openNow = openNow;
    }

    /** Builds a usable Places photo URL (needs the API key), or null if no photo. */
    public String photoUrl(Context context, int maxWidth) {
        if (photoRef == null || photoRef.isEmpty()) return null;
        return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=" + maxWidth
                + "&photo_reference=" + photoRef
                + "&key=" + AppConfig.getMapsApiKey(context);
    }

    public String priceLabel() {
        if (priceLevel < 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.max(priceLevel, 1); i++) sb.append("$");
        return sb.toString();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("placeId", placeId);
        m.put("name", name);
        m.put("address", address);
        m.put("rating", rating);
        m.put("lat", lat);
        m.put("lng", lng);
        m.put("photoRef", photoRef);
        m.put("priceLevel", priceLevel);
        m.put("savedAt", System.currentTimeMillis());
        return m;
    }

    public static PlaceItem fromMap(Map<String, Object> m) {
        PlaceItem p = new PlaceItem();
        p.placeId = str(m.get("placeId"));
        p.name = str(m.get("name"));
        p.address = str(m.get("address"));
        p.rating = dbl(m.get("rating"));
        p.lat = dbl(m.get("lat"));
        p.lng = dbl(m.get("lng"));
        p.photoRef = str(m.get("photoRef"));
        Object pl = m.get("priceLevel");
        p.priceLevel = pl instanceof Number ? ((Number) pl).intValue() : -1;
        return p;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static double dbl(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0.0;
    }
}
