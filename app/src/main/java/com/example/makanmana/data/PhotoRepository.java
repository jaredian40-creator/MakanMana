package com.example.makanmana.data;

import android.graphics.Bitmap;
import android.util.Base64;

import com.example.makanmana.model.Photo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores photos (with caption, rating, place and date) as Base64 JPEG text in
 * Firestore, so the app works on the free Firebase (Spark) plan without Cloud
 * Storage / billing.
 * <ul>
 *   <li>User meal photos: {@code users/{uid}/photos}</li>
 *   <li>Place photos:     {@code places/{placeId}/photos}</li>
 * </ul>
 */
public final class PhotoRepository {

    public interface UploadCallback {
        void onSuccess(String id);

        void onError(String message);
    }

    public interface PhotosCallback {
        void onResult(List<Photo> photos);

        void onError(String message);
    }

    private PhotoRepository() {
    }

    // ---------- encoding helpers ----------

    private static String encode(Bitmap bitmap) {
        Bitmap scaled = scaleDown(bitmap, 1024);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    private static Bitmap scaleDown(Bitmap b, int maxSide) {
        int w = b.getWidth(), h = b.getHeight();
        int max = Math.max(w, h);
        if (max <= maxSide) return b;
        float ratio = (float) maxSide / max;
        return Bitmap.createScaledBitmap(b, Math.round(w * ratio), Math.round(h * ratio), true);
    }

    /** Turns a stored Base64 photo into a byte[] that Glide can load directly. */
    public static byte[] decodeGlideModel(String base64) {
        return Base64.decode(base64, Base64.NO_WRAP);
    }

    private static Map<String, Object> toDoc(String base64, String caption, double rating,
                                             String placeName, String uid) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("imageBase64", base64);
        doc.put("caption", caption == null ? "" : caption);
        doc.put("rating", rating);
        doc.put("placeName", placeName == null ? "" : placeName);
        doc.put("createdAt", System.currentTimeMillis());
        if (uid != null) doc.put("uploadedBy", uid);
        return doc;
    }

    private static List<Photo> collect(QuerySnapshot snap) {
        List<Photo> out = new ArrayList<>();
        snap.getDocuments().forEach(d -> {
            String img = d.getString("imageBase64");
            if (img == null) return;
            Double rating = d.getDouble("rating");
            Long created = d.getLong("createdAt");
            Photo photo = new Photo(
                    img,
                    d.getString("caption"),
                    rating == null ? 0 : rating,
                    d.getString("placeName"),
                    created == null ? 0 : created);
            photo.id = d.getId();
            photo.uploadedBy = d.getString("uploadedBy");
            out.add(photo);
        });
        return out;
    }

    /** Only the editable metadata (never the image or date). */
    private static Map<String, Object> editFields(String caption, double rating, String placeName) {
        Map<String, Object> m = new HashMap<>();
        m.put("caption", caption == null ? "" : caption);
        m.put("rating", rating);
        m.put("placeName", placeName == null ? "" : placeName);
        return m;
    }

    // ---------- place photos ----------

    private static CollectionReference photosOf(String placeId) {
        return FirebaseFirestore.getInstance()
                .collection("places").document(placeId).collection("photos");
    }

    public static void upload(String placeId, Bitmap bitmap, String caption, double rating,
                              String placeName, UploadCallback cb) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "anonymous";
        photosOf(placeId).add(toDoc(encode(bitmap), caption, rating, placeName, uid))
                .addOnSuccessListener(r -> cb.onSuccess(r.getId()))
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public static void getPhotos(String placeId, PhotosCallback cb) {
        photosOf(placeId).orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> cb.onResult(collect(snap)))
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public static void updatePlacePhoto(String placeId, String photoId, String caption,
                                        double rating, String placeName, Runnable onDone) {
        photosOf(placeId).document(photoId).update(editFields(caption, rating, placeName))
                .addOnCompleteListener(t -> {
                    if (onDone != null) onDone.run();
                });
    }

    public static void deletePlacePhoto(String placeId, String photoId, Runnable onDone) {
        photosOf(placeId).document(photoId).delete()
                .addOnCompleteListener(t -> {
                    if (onDone != null) onDone.run();
                });
    }

    // ---------- the signed-in user's own meal photos ("Snap a Meal") ----------

    private static CollectionReference myPhotos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;
        return FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).collection("photos");
    }

    public static void uploadUserPhoto(Bitmap bitmap, String caption, double rating,
                                       String placeName, UploadCallback cb) {
        CollectionReference col = myPhotos();
        if (col == null) {
            cb.onError("Not signed in");
            return;
        }
        col.add(toDoc(encode(bitmap), caption, rating, placeName, null))
                .addOnSuccessListener(r -> cb.onSuccess(r.getId()))
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public static void getUserPhotos(PhotosCallback cb) {
        CollectionReference col = myPhotos();
        if (col == null) {
            cb.onError("Not signed in");
            return;
        }
        col.orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> cb.onResult(collect(snap)))
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public static void updateUserPhoto(String photoId, String caption, double rating,
                                       String placeName, Runnable onDone) {
        CollectionReference col = myPhotos();
        if (col == null) return;
        col.document(photoId).update(editFields(caption, rating, placeName))
                .addOnCompleteListener(t -> {
                    if (onDone != null) onDone.run();
                });
    }

    public static void deleteUserPhoto(String photoId, Runnable onDone) {
        CollectionReference col = myPhotos();
        if (col == null) return;
        col.document(photoId).delete()
                .addOnCompleteListener(t -> {
                    if (onDone != null) onDone.run();
                });
    }
}
