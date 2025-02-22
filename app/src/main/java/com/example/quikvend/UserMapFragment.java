package com.example.quikvend;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference vendorsRef;
    private Marker userMarker;
    private final Map<String, Marker> vendorMarkers = new HashMap<>(); // Track vendor markers by UID

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_user_map_fragment, container, false);

        mapView = view.findViewById(R.id.mapView);
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        vendorsRef = FirebaseDatabase.getInstance().getReference("Vendors");

        startUserLocationUpdates();
        trackLiveVendors();

        return view;
    }

    private void startUserLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission not granted.", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000) // Update every 5 seconds
                .setFastestInterval(2000); // Minimum interval of 2 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    GeoPoint userPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    updateUserMarker(userPoint);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
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

    private void trackLiveVendors() {
        vendorsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot vendorSnapshot : snapshot.getChildren()) {
                    VendorProfile vendor = vendorSnapshot.getValue(VendorProfile.class);
                    if (vendor != null) {
                        String vendorUid = vendorSnapshot.getKey();
                        GeoPoint vendorPoint = new GeoPoint(vendor.getLatitude(), vendor.getLongitude());

                        if (vendorMarkers.containsKey(vendorUid)) {
                            vendorMarkers.get(vendorUid).setPosition(vendorPoint); // Update existing marker
                        } else {
                            if (vendor.getProfileImageUrl() != null && !vendor.getProfileImageUrl().isEmpty()) {
                                loadVendorMarkerWithImage(vendor, vendorUid);
                            } else {
                                addVendorMarker(vendor, ContextCompat.getDrawable(requireContext(), R.drawable.ic_default_vendor_marker), vendorUid);
                            }
                        }
                    }
                }
                mapView.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to track vendors.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadVendorMarkerWithImage(VendorProfile vendor, String vendorUid) {
        Glide.with(requireContext())
                .asBitmap()
                .load(vendor.getProfileImageUrl())
                .circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                        Drawable markerDrawable = new BitmapDrawable(getResources(), resource);
                        addVendorMarker(vendor, markerDrawable, vendorUid);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {}
                });
    }

    private void addVendorMarker(VendorProfile vendor, Drawable markerDrawable, String vendorUid) {
        GeoPoint vendorPoint = new GeoPoint(vendor.getLatitude(), vendor.getLongitude());
        Marker vendorMarker = new Marker(mapView);
        vendorMarker.setPosition(vendorPoint);
        vendorMarker.setTitle(vendor.getName() + " - " + vendor.getCategory());
        vendorMarker.setSubDescription("Contact: " + vendor.getContact());
        vendorMarker.setIcon(markerDrawable);
        vendorMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        mapView.getOverlays().add(vendorMarker);
        vendorMarkers.put(vendorUid, vendorMarker); // Track for updates
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback); // Stop updates to save battery
        }
    }
}