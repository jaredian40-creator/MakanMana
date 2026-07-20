package com.example.makanmana.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.makanmana.AppConfig;
import com.example.makanmana.model.PlaceItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the Google Places Nearby Search endpoint (via Volley) and returns a list
 * of {@link PlaceItem}. Shared by the map screen and the "Surprise Me" spinner.
 */
public final class PlacesService {

    public interface Callback {
        void onResult(List<PlaceItem> places);

        void onError(String message);
    }

    private PlacesService() {
    }

    public static void searchNearby(Context context, double lat, double lng, String type,
                                    String keyword, boolean openNow,
                                    int minPrice, int maxPrice, Callback cb) {

        Uri.Builder b = Uri.parse("https://maps.googleapis.com/maps/api/place/nearbysearch/json").buildUpon()
                .appendQueryParameter("location", lat + "," + lng)
                .appendQueryParameter("radius", "2500")
                .appendQueryParameter("type", type)
                .appendQueryParameter("key", AppConfig.getMapsApiKey(context));
        if (keyword != null && !keyword.isEmpty()) b.appendQueryParameter("keyword", keyword);
        if (openNow) b.appendQueryParameter("opennow", "true");
        if (minPrice >= 0) b.appendQueryParameter("minprice", String.valueOf(minPrice));
        if (maxPrice >= 0) b.appendQueryParameter("maxprice", String.valueOf(maxPrice));

        String url = b.build().toString();
        Log.d("MakanMana", "Places request: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                String status = response.optString("status", "UNKNOWN");
                if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                    cb.onError(response.optString("error_message", "Places error: " + status));
                    return;
                }
                List<PlaceItem> out = new ArrayList<>();
                JSONArray results = response.optJSONArray("results");
                if (results != null) {
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject place = results.getJSONObject(i);
                        JSONObject loc = place.getJSONObject("geometry").getJSONObject("location");

                        String photoRef = null;
                        JSONArray photos = place.optJSONArray("photos");
                        if (photos != null && photos.length() > 0) {
                            photoRef = photos.getJSONObject(0).optString("photo_reference", null);
                        }

                        boolean isOpen = false;
                        JSONObject hours = place.optJSONObject("opening_hours");
                        if (hours != null) isOpen = hours.optBoolean("open_now", false);

                        out.add(new PlaceItem(
                                place.optString("place_id"),
                                place.optString("name"),
                                place.optString("vicinity", ""),
                                place.optDouble("rating", 0.0),
                                loc.getDouble("lat"),
                                loc.getDouble("lng"),
                                photoRef,
                                place.has("price_level") ? place.optInt("price_level") : -1,
                                isOpen));
                    }
                }
                cb.onResult(out);
            } catch (Exception e) {
                cb.onError("Parse error: " + e.getMessage());
            }
        }, error -> cb.onError("Network error: " + error.toString()));

        Volley.newRequestQueue(context).add(request);
    }
}
