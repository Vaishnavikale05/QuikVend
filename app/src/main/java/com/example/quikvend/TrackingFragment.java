package com.example.quikvend;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class TrackingFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int GPS_REQUEST_CODE = 102;

    private MapView mapView;
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private DatabaseReference vendorLocationRef;
    private Marker vendorMarker;
    private boolean isTracking = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_tracking_fragment, container, false);

        mapView = view.findViewById(R.id.mapView);
        Button btnStartTracking = view.findViewById(R.id.btnStartTracking);
        Button btnStopTracking = view.findViewById(R.id.btnStopTracking);

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        locationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        vendorLocationRef = FirebaseDatabase.getInstance().getReference("Vendors")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("location");

        btnStartTracking.setOnClickListener(v -> startLiveLocationTracking());
        btnStopTracking.setOnClickListener(v -> stopLiveLocationTracking());

        checkAndRequestLocationPermission();
        return view;
    }

    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            checkGPSAndFetchLocation();
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void checkGPSAndFetchLocation() {
        LocationRequest locationRequest = LocationRequest.create().setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest).build();

        LocationServices.getSettingsClient(requireContext()).checkLocationSettings(settingsRequest)
                .addOnSuccessListener(response -> fetchCurrentLocationAndFocus())
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException) e).startResolutionForResult(requireActivity(), GPS_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException ex) {
                            Toast.makeText(requireContext(), "Error enabling GPS.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Enable location services to continue.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchCurrentLocationAndFocus() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission not granted.", Toast.LENGTH_SHORT).show();
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) updateMapMarker(location);
            else Toast.makeText(requireContext(), "Fetching location... Ensure GPS is on.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e ->
                Toast.makeText(requireContext(), "Error fetching location: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void startLiveLocationTracking() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isTracking) {
            Toast.makeText(requireContext(), "Tracking already in progress.", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationRequest request = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000); // Update every 5 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateVendorLocation(location);
                    updateMapMarker(location);
                }
            }
        };

        locationClient.requestLocationUpdates(request, locationCallback, null);
        isTracking = true;
        Toast.makeText(requireContext(), "Live tracking started.", Toast.LENGTH_SHORT).show();
    }

    private void stopLiveLocationTracking() {
        if (locationCallback != null && isTracking) {
            locationClient.removeLocationUpdates(locationCallback);
            isTracking = false;
            Toast.makeText(requireContext(), "Live tracking stopped.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateVendorLocation(Location location) {
        vendorLocationRef.child("latitude").setValue(location.getLatitude());
        vendorLocationRef.child("longitude").setValue(location.getLongitude());
    }

    private void updateMapMarker(Location location) {
        GeoPoint vendorPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

        if (vendorMarker == null) {
            vendorMarker = new Marker(mapView);
            vendorMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            vendorMarker.setTitle("Your Location");

            // Resize the icon before setting it
            Drawable originalIcon = ContextCompat.getDrawable(requireContext(), R.drawable.vendor_mapmarker);
            if (originalIcon != null) {
                Bitmap bitmap = ((BitmapDrawable) originalIcon).getBitmap();
                int newWidth = 64; // Adjust the size as needed
                int newHeight = 64;
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
                Drawable resizedIcon = new BitmapDrawable(getResources(), scaledBitmap);
                vendorMarker.setIcon(resizedIcon);
            }

            mapView.getOverlays().add(vendorMarker);
        }

        vendorMarker.setPosition(vendorPoint);
        mapView.getController().setZoom(16.0);
        mapView.getController().animateTo(vendorPoint);
        mapView.invalidate();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkGPSAndFetchLocation();
        } else {
            Toast.makeText(requireContext(), "Location permission required to track location.", Toast.LENGTH_LONG).show();
        }
    }

    // 🚀 Tracking no longer stops when fragment is destroyed
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Removed stopLiveLocationTracking() to allow persistent tracking
    }
}