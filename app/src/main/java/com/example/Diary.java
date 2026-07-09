package com.example;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Diary implements Serializable {
    private int id;
    private String title;
    private String content;
    private long timestamp;
    private String photoPath;
    private double latitude;
    private double longitude;
    private String address;
    private String mood;

    // Full constructor
    public Diary(int id, String title, String content, long timestamp, String photoPath, 
                 double latitude, double longitude, String address, String mood) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.photoPath = photoPath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.mood = mood;
    }

    // Constructor without ID (for creation)
    public Diary(String title, String content, long timestamp, String photoPath, 
                 double latitude, double longitude, String address, String mood) {
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.photoPath = photoPath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.mood = mood;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    // Helper for formatting timestamp
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
