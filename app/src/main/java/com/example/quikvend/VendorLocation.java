package com.example.quikvend;

public class VendorLocation {
    private double latitude;
    private double longitude;
    private String vendorName;
    private String category;

    public VendorLocation() { }

    public VendorLocation(double latitude, double longitude, String vendorName, String category) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.vendorName = vendorName;
        this.category = category;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getVendorName() { return vendorName; }
    public String getCategory() { return category; }
}