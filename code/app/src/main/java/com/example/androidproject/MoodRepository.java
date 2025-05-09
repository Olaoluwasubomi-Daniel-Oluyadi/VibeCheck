package com.example.androidproject;

import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository class to handle all Firestore update operations related to moods
 */
public class MoodRepository {
    private FirebaseFirestore db;

    /**
     * Creates the mood repository
     */
    public MoodRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Updates a mood document with optional image upload
     * @param moodId Mood ID to update
     * @param mood Mood type (e.g., "Happy", "Sad")
     * @param color Color associated with the mood
     * @param situation Social situation
     * @param reason Reason for the mood
     * @param location Location description
     * @param newImageUri New image URI if any
     * @param currentImageUrl Current image URL
     * @param dateTime Date and time of the mood
     * @param isPublic Visibility flag (true = public, false = private)
     * @param listener Callback for update results
     */
    public void updateMood(String moodId, String mood, String color, String situation, String reason,
                           Location location, Uri newImageUri, String currentImageUrl, Calendar dateTime,
                           boolean isPublic, OnMoodUpdateListener listener) {
        // Validate id is not null
        if (moodId == null) {
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("Mood ID cannot be null"));
            }
            return;
        }

        // If we have a new image to upload, do that first
        if (newImageUri != null && newImageUri.toString().startsWith("content://")) {
            // Create a storage reference
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference imageRef = storageRef.child("images/" + moodId);

            // Upload the file
            imageRef.putFile(newImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // After successful upload, update the mood with all data
                        updateMoodData(moodId, mood, color, situation, reason, location, moodId, dateTime, isPublic, listener);
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) {
                            listener.onFailure(e);
                        }
                    });
        } else {
            // No new image to upload, just update the mood data
            // Use current image URL if available, otherwise null
            String imageUrlToUse = (currentImageUrl == null) ? null : moodId;
            updateMoodData(moodId, mood, color, situation, reason, location, imageUrlToUse, dateTime, isPublic, listener);
        }
    }

    /**
     * Helper method to update mood data in Firestore
     */
    private void updateMoodData(String moodId, String mood, String color, String situation, String reason,
                                Location location, String imageUrl, Calendar dateTime,
                                boolean isPublic, OnMoodUpdateListener listener) {
        // Create the main mood document data
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("mood", mood);
        updatedData.put("color", color);
        updatedData.put("timestamp", dateTime.getTimeInMillis());

        // Add visibility field
        updatedData.put("visibility", isPublic);

        // Only add fields if they are not empty
        if (situation != null && !situation.isEmpty()) {
            updatedData.put("situation", situation);
        } else {
            updatedData.put("situation", null);
        }

        if (reason != null && !reason.isEmpty()) {
            updatedData.put("reason", reason);
        }

        if (location != null) {
            // map based on chosen cords
            Map<String, Double> locationMap = new HashMap<>();
            locationMap.put("latitude", location.getLatitude());
            locationMap.put("longitude", location.getLongitude());
            updatedData.put("location", locationMap);
        } else {
            // only adds it if its empty/null
            updatedData.put("location", null);
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            updatedData.put("id", imageUrl);
        }

        // Update Firestore
        db.collection("Moods").document(moodId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    // Now update the dayTime subcollection
                    updateDayTimeSubcollection(moodId, dateTime, listener);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    /**
     * Deletes a mood from Firestore
     */
    public void deleteMood(String id, OnMoodDeleteListener listener) {
        // Delete the mood document
        db.collection("Moods").document(id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    /**
     * Updates all fields in the dayTime field
     */
    private void updateDayTimeSubcollection(String id, Calendar cal, OnMoodUpdateListener listener) {
        Map<String, Object> dayTimeData = new HashMap<>();

        // Month names array
        String[] monthNames = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
                "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"};

        // Day of week names array
        String[] daysOfWeek = {"SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};

        // Year and month details
        dayTimeData.put("year", cal.get(Calendar.YEAR));
        dayTimeData.put("monthValue", cal.get(Calendar.MONTH) + 1);
        dayTimeData.put("month", monthNames[cal.get(Calendar.MONTH)]);

        // Day details
        dayTimeData.put("dayOfMonth", cal.get(Calendar.DAY_OF_MONTH));
        dayTimeData.put("dayOfWeek", daysOfWeek[cal.get(Calendar.DAY_OF_WEEK) - 1]);
        dayTimeData.put("dayOfYear", cal.get(Calendar.DAY_OF_YEAR));

        // Time details
        dayTimeData.put("hour", cal.get(Calendar.HOUR_OF_DAY));
        dayTimeData.put("minute", cal.get(Calendar.MINUTE));
        dayTimeData.put("second", cal.get(Calendar.SECOND));
        long nanos = (cal.get(Calendar.MILLISECOND) * 1000000L);
        dayTimeData.put("nano", nanos);

        Map<String, Object> chronology = new HashMap<>();
        chronology.put("calendarType", "iso8601");
        dayTimeData.put("chronology", chronology);

        db.collection("Moods").document(id)
                .update("dayTime", dayTimeData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("MoodRepository", "DayTime field updated successfully");
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MoodRepository", "Failed to update dayTime field", e);
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    // Interface for callbacks
    public interface OnMoodUpdateListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnMoodDeleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}