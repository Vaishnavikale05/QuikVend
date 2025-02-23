package com.example.quikvend;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.quikvend.models.VendorProfile;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;
import com.google.firebase.database.*;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.Map;

public class UserMapFragment extends Fragment {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int GPS_REQUEST_CODE = 200;

    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference vendorsRef;
    private Marker userMarker;
    private final Map<String, Marker> vendorMarkers = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_user_map_fragment, container, false);

        mapView = view.findViewById(R.id.userMapView);
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        vendorsRef = FirebaseDatabase.getInstance().getReference("Vendors");

        checkLocationPermissions();
        return view;
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableGPSAndStartLocationUpdates();
        }
    }

    private void enableGPSAndStartLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000);

        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build();
        LocationServices.getSettingsClient(requireContext()).checkLocationSettings(settingsRequest)
                .addOnSuccessListener(response -> startLocationTracking())
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException) e).startResolutionForResult(requireActivity(), GPS_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException ex) {
                            Toast.makeText(requireContext(), "Error enabling GPS.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void startLocationTracking() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    GeoPoint userPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    updateUserMarker(userPoint);
                    fetchNearbyVendors(userPoint);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(LocationRequest.create().setPriority(Priority.PRIORITY_HIGH_ACCURACY).setInterval(5000), locationCallback, null);
        }
    }

    private void updateUserMarker(GeoPoint locationPoint) {
        if (userMarker == null) {
            userMarker = new Marker(mapView);
            userMarker.setTitle("Your Location");
            userMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_user_location_marker));
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(userMarker);
        }
        userMarker.setPosition(locationPoint);
        mapView.getController().setZoom(15.0);
        mapView.getController().animateTo(locationPoint);
        mapView.invalidate();
    }

    private void fetchNearbyVendors(GeoPoint userLocation) {
        vendorsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot vendorSnapshot : snapshot.getChildren()) {
                    VendorProfile vendor = vendorSnapshot.getValue(VendorProfile.class);
                    if (vendor != null) {
                        GeoPoint vendorPoint = new GeoPoint(vendor.getLatitude(), vendor.getLongitude());
                        if (calculateDistance(userLocation, vendorPoint) <= 5.0) {
                            addOrUpdateVendorMarker(vendor, vendorSnapshot.getKey(), vendorPoint);
                        }
                    }
                }
                mapView.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to fetch vendors.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addOrUpdateVendorMarker(VendorProfile vendor, String vendorUid, GeoPoint location) {
        if (vendorMarkers.containsKey(vendorUid)) {
            vendorMarkers.get(vendorUid).setPosition(location);
        } else {
            Marker vendorMarker = new Marker(mapView);
            vendorMarker.setPosition(location);
            vendorMarker.setTitle(vendor.getName() + " - " + vendor.getCategory());
            vendorMarker.setSubDescription("Contact: " + vendor.getContact());
            vendorMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            vendorMarker.setOnMarkerClickListener((marker, mapView) -> {
                openVendorProfileFragment(vendor);
                return true;
            });

            mapView.getOverlays().add(vendorMarker);
            vendorMarkers.put(vendorUid, vendorMarker);
        }
    }

    private void openVendorProfileFragment(VendorProfile vendor) {
        VendorProfileFragment profileFragment = VendorProfileFragment.newInstance(vendor);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit();
    }

    private double calculateDistance(GeoPoint point1, GeoPoint point2) {
        double earthRadius = 6371; // km
        double dLat = Math.toRadians(point2.getLatitude() - point1.getLatitude());
        double dLng = Math.toRadians(point2.getLongitude() - point1.getLongitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(point1.getLatitude())) * Math.cos(Math.toRadians(point2.getLatitude())) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return earthRadius * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableGPSAndStartLocationUpdates();
        } else {
            Toast.makeText(requireContext(), "Location permission required.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback); // Prevent memory leaks
        }
    }
}