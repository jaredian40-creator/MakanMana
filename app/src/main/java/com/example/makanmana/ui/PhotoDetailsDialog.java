package com.example.makanmana.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;

import com.example.makanmana.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Small dialog that collects a photo's place, description and star rating before
 * it is saved. Reused by the place-detail screen and the "Snap a Meal" flow.
 */
public final class PhotoDetailsDialog {

    public interface OnSubmit {
        void submit(String place, String caption, float rating);
    }

    private PhotoDetailsDialog() {
    }

    /** New-photo dialog (rating starts at 4). */
    public static void show(Context ctx, String prefillPlace, OnSubmit cb) {
        show(ctx, "Photo details", prefillPlace, null, 4f, cb);
    }

    /** Edit dialog, prefilled with an existing photo's details. */
    public static void showEdit(Context ctx, String place, String caption, float rating, OnSubmit cb) {
        show(ctx, "Edit photo", place, caption, rating, cb);
    }

    private static void show(Context ctx, String title, String prefillPlace, String prefillCaption,
                             float prefillRating, OnSubmit cb) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_photo_details, null);
        TextInputEditText etPlace = view.findViewById(R.id.etPlace);
        TextInputEditText etCaption = view.findViewById(R.id.etCaption);
        RatingBar ratingBar = view.findViewById(R.id.ratingBar);

        if (prefillPlace != null) etPlace.setText(prefillPlace);
        if (prefillCaption != null) etCaption.setText(prefillCaption);
        ratingBar.setRating(prefillRating);

        new MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String place = etPlace.getText() != null ? etPlace.getText().toString().trim() : "";
                    String caption = etCaption.getText() != null ? etCaption.getText().toString().trim() : "";
                    cb.submit(place, caption, ratingBar.getRating());
                })
                .show();
    }
}
