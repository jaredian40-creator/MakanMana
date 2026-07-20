package com.example.makanmana.data;

import androidx.annotation.NonNull;

import com.example.makanmana.model.PlaceItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores a user's favourite places in Firestore under
 * {@code users/{uid}/favourites/{placeId}}.
 */
public final class FavouritesRepository {

    public interface BoolCallback {
        void onResult(boolean value);
    }

    public interface ListCallback {
        void onResult(List<PlaceItem> places);

        void onError(String message);
    }

    private FavouritesRepository() {
    }

    private static String uid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private static CollectionReference favourites() {
        String uid = uid();
        if (uid == null) return null;
        return FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("favourites");
    }

    public static void add(@NonNull PlaceItem place, Runnable onDone) {
        CollectionReference ref = favourites();
        if (ref == null || place.placeId == null) return;
        ref.document(place.placeId).set(place.toMap())
                .addOnCompleteListener(t -> {
                    if (onDone != null) onDone.run();
                });
    }

    public static void remove(@NonNull String placeId, Runnable onDone) {
        CollectionReference ref = favourites();
        if (ref == null) return;
        ref.document(placeId).delete()
                .addOnCompleteListener(t -> {
                    if (onDone != null) onDone.run();
                });
    }

    public static void isFavourite(@NonNull String placeId, BoolCallback cb) {
        CollectionReference ref = favourites();
        if (ref == null) {
            cb.onResult(false);
            return;
        }
        ref.document(placeId).get()
                .addOnSuccessListener(doc -> cb.onResult(doc.exists()))
                .addOnFailureListener(e -> cb.onResult(false));
    }

    public static void getAll(ListCallback cb) {
        CollectionReference ref = favourites();
        if (ref == null) {
            cb.onError("Not signed in");
            return;
        }
        ref.orderBy("savedAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snap -> {
                    List<PlaceItem> out = new ArrayList<>();
                    snap.getDocuments().forEach(d -> {
                        if (d.getData() != null) out.add(PlaceItem.fromMap(d.getData()));
                    });
                    cb.onResult(out);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }
}
