# MakanMana — setup notes for the new build

## 1. Gradle sync
Open the project in Android Studio and **Sync Gradle**. New libraries were added:
Firebase Firestore (via the Firebase BoM). If Studio suggests a newer BoM version,
accept it. **Cloud Storage is not used** — photos are stored as Base64 in Firestore,
so the app runs entirely on the free (Spark) plan with no billing.

## 2. Firebase console (required for Favourites & Photos)
In the Firebase project already linked by `app/google-services.json`:

1. **Firestore Database** → *Create database* (start in test mode for development).
2. Make sure **Authentication → Email/Password** is enabled (already used by login).

You do **not** need to enable Cloud Storage.

Development-only Firestore rules (tighten before any real release):
```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    match /{document=**} { allow read, write: if request.auth != null; }
  }
}
```

No composite indexes are needed — the queries order by a single field.

## 3. API key
The Google Maps/Places key lives in `local.properties` as `MAPS_API_KEY`
(git-ignored) and is injected via the secrets-gradle-plugin. **Rotate the old key**
in Google Cloud Console since it was previously committed in source.

## What changed
- **Design:** Material 3 warm palette (spice red / amber / cream), dark mode,
  rounded cards, restyled login & detail screens.
- **Home hub:** `HomeActivity` with a bottom nav (Home / Map / Saved / Profile).
- **Surprise Me:** weighted-random nearby pick on the Home tab.
- **Filters:** Open-now / Halal / Budget / Top-rated chips on the map.
- **Favourites:** heart toggle on a place → Firestore `users/{uid}/favourites`,
  listed in the Saved tab.
- **Photos-attached-to-places:** "Add a photo" on a place stores the image
  (Base64) in Firestore `places/{placeId}/photos`, shown in a gallery on the detail screen.
- **Snap a Meal:** captured photos are stored (Base64) in `users/{uid}/photos`
  and shown in the "My meals" gallery on the Profile tab.

## Data model (Firestore) — photos stored as Base64, no Cloud Storage
- `users/{uid}/favourites/{placeId}` → placeId, name, address, rating, lat, lng, photoRef, savedAt
- `users/{uid}/photos/{autoId}` → imageBase64, caption, rating, placeName, createdAt  (meal photos, shown on Profile)
- `places/{placeId}/photos/{autoId}` → imageBase64, caption, rating, placeName, uploadedBy, createdAt

Photos are downscaled to ≤1024px and JPEG-compressed before encoding, keeping each
document well under Firestore's 1 MB limit.

## Note
`GoogleActivity` is now unused (replaced by the bottom-nav `HomeActivity`); it is
left in place but no longer part of the flow and can be deleted.
