package com.example.quikvend;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;

import com.google.firebase.database.*;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserMapFragment extends Fragment {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int GPS_REQUEST_CODE = 200;

    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference vendorsRef;
    private Marker userMarker;
    private Polygon userRangeCircle;
    private final Map<String, Marker> vendorMarkers = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_user_map_fragment, container, false);

        // Initialize MapView
        mapView = view.findViewById(R.id.userMapView);
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        Log.d("MapDebug", "MapView initialized successfully.");

        // Initialize Firebase
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        vendorsRef = FirebaseDatabase.getInstance().getReference("Vendors");

        requestLocationPermission();
        return view;
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            checkGPSAndStartTracking();
        }
    }

    private void checkGPSAndStartTracking() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build();
        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();

        LocationServices.getSettingsClient(requireContext()).checkLocationSettings(settingsRequest)
                .addOnSuccessListener(response -> startLocationUpdates())
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException) e).startResolutionForResult(requireActivity(), GPS_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException ex) {
                            Toast.makeText(requireContext(), "Enable GPS manually.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "GPS not enabled!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000) // More frequent updates
                .setWaitForAccurateLocation(true) // Ensures accurate location
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    GeoPoint userPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    Log.d("UserLocation", "User location updated: " + userPoint);
                    showUserLocation(userPoint);
                    fetchNearbyVendors(userPoint);
                } else {
                    Log.e("LocationError", "Failed to get user location.");
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }



    private void showUserLocation(GeoPoint point) {
        if (userMarker == null) {
            userMarker = new Marker(mapView);
            userMarker.setTitle("You are here");

            // Use user_marker.xml for user location
            userMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.user_marker));
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(userMarker);
        }
        userMarker.setPosition(point);

        // Show 5km radius around the user
        showUserRange(point);

        mapView.getController().setZoom(15.0);
        mapView.getController().animateTo(point);
        mapView.invalidate();
    }

    private void displayVendorMarker(VendorProfile vendor, String vendorUid) {
        GeoPoint vendorPoint = new GeoPoint(vendor.getLatitude(), vendor.getLongitude());

        // Check if another marker is too close (within 20m)
        for (Marker existingMarker : vendorMarkers.values()) {
            double distance = distanceBetween(existingMarker.getPosition(), vendorPoint);
            if (distance < 0.02) { // 20 meters
                // Offset new vendor's position slightly
                vendorPoint = new GeoPoint(vendor.getLatitude() + 0.0001, vendor.getLongitude() + 0.0001);
                break;
            }
        }

        Marker vendorMarker = new Marker(mapView);
        vendorMarker.setPosition(vendorPoint);
        vendorMarker.setTitle(vendor.getName() + " - " + vendor.getCategory());
        vendorMarker.setSubDescription("Contact: " + vendor.getContact());

        vendorMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.vendor_mapmarker));
        vendorMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        Log.d("MapDebug", "Adding vendor marker: " + vendor.getName());
        mapView.getOverlays().add(vendorMarker);
        vendorMarkers.put(vendorUid, vendorMarker);
    }



    private void showUserRange(GeoPoint center) {
        if (userRangeCircle == null) {
            userRangeCircle = new Polygon();
            userRangeCircle.setFillColor(0x550000FF); // Translucent blue
            userRangeCircle.setStrokeColor(0xFF0000FF); // Blue border
            userRangeCircle.setStrokeWidth(2);
            mapView.getOverlays().add(userRangeCircle);
        }

        userRangeCircle.setPoints(generateCirclePoints(center, 5.0, 50));
    }

    private List<GeoPoint> generateCirclePoints(GeoPoint center, double radiusKm, int numPoints) {
        double radiusInDegrees = radiusKm / 111.0; // 1 degree ≈ 111 km
        List<GeoPoint> circlePoints = new ArrayList<>();

        for (int i = 0; i < numPoints; i++) {
            double angle = Math.toRadians(i * (360.0 / numPoints));
            double latOffset = radiusInDegrees * Math.cos(angle);
            double lonOffset = radiusInDegrees * Math.sin(angle) / Math.cos(Math.toRadians(center.getLatitude()));
            circlePoints.add(new GeoPoint(center.getLatitude() + latOffset, center.getLongitude() + lonOffset));
        }
        return circlePoints;
    }

    private void fetchNearbyVendors(GeoPoint userLocation) {
        vendorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e("FirebaseDebug", "No vendors found in database!");
                    return;
                }

                Log.d("FirebaseDebug", "Vendors found: " + snapshot.getChildrenCount());

                // Clear old markers before adding new ones
                for (Marker marker : vendorMarkers.values()) {
                    mapView.getOverlays().remove(marker);
                }
                vendorMarkers.clear();

                for (DataSnapshot vendorSnapshot : snapshot.getChildren()) {
                    String name = vendorSnapshot.child("name").getValue(String.class);
                    String category = vendorSnapshot.child("category").getValue(String.class);
                    String contact = vendorSnapshot.child("contact").getValue(String.class);
                    String profileImageUrl = vendorSnapshot.child("profileImageUrl").getValue(String.class); // Fetch image URL

                    // Fetch nested location data
                    DataSnapshot locationSnapshot = vendorSnapshot.child("location");
                    if (locationSnapshot.exists()) {
                        Double latitude = locationSnapshot.child("latitude").getValue(Double.class);
                        Double longitude = locationSnapshot.child("longitude").getValue(Double.class);

                        if (latitude != null && longitude != null) {
                            GeoPoint vendorPoint = new GeoPoint(latitude, longitude);
                            double distance = distanceBetween(userLocation, vendorPoint);

                            Log.d("VendorDistance", name + " is " + distance + " km away.");

                            // Only display vendors within a 5km radius
                            if (distance <= 5.0) {
                                VendorProfile vendor = new VendorProfile(name, contact, category, latitude, longitude, profileImageUrl);
                                displayVendorMarker(vendor, vendorSnapshot.getKey());
                            }
                        } else {
                            Log.e("FirebaseDebug", "Invalid latitude/longitude for vendor: " + name);
                        }
                    } else {
                        Log.e("FirebaseDebug", "No location data for vendor: " + name);
                    }
                }

                // Refresh the map
                mapView.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseDebug", "Error fetching vendors: " + error.getMessage());
                Toast.makeText(requireContext(), "Error fetching vendors.", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private double distanceBetween(GeoPoint p1, GeoPoint p2) {
        float[] results = new float[1];
        Location.distanceBetween(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude(), results);
        return results[0] / 1000; // Convert meters to km
    }
}
