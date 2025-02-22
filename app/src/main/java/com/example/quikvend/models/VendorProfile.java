package com.example.quikvend.models;

public class VendorProfile {
    private String name;
    private String contact;
    private String category;
    private double latitude;
    private double longitude;
    private String profileImageUrl;

    // Required no-argument constructor for Firebase
    public VendorProfile() { }

    public VendorProfile(String name, String contact, String category, double latitude, double longitude, String profileImageUrl) {
        this.name = name;
        this.contact = contact;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.profileImageUrl = profileImageUrl;
    }

    public String getName() { return name; }
    public String getContact() { return contact; }
    public String getCategory() { return category; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getProfileImageUrl() { return profileImageUrl; }
}