package com.example.makanmana.model;

import java.io.Serializable;

/**
 * A user-taken photo with its metadata: the image itself (Base64 JPEG), an
 * optional description/caption, a star rating, the place it was taken at, and
 * the date (epoch millis).
 */
public class Photo implements Serializable {

    public String id;          // Firestore document id (for edit/delete)
    public String uploadedBy;  // uid of the uploader (place photos only)
    public String imageBase64;
    public String caption;
    public double rating;      // 0-5
    public String placeName;
    public long createdAt;     // epoch millis

    public Photo() {
    }

    public Photo(String imageBase64, String caption, double rating, String placeName, long createdAt) {
        this.imageBase64 = imageBase64;
        this.caption = caption;
        this.rating = rating;
        this.placeName = placeName;
        this.createdAt = createdAt;
    }
}
