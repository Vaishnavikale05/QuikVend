package com.example.quikvend;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class UserHomeActivity extends AppCompatActivity {
    private MapView mapView;
    private DatabaseReference vendorLocationsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_home);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView = findViewById(R.id.userMapView);
        mapView.setMultiTouchControls(true);

        vendorLocationsRef = FirebaseDatabase.getInstance().getReference("VendorLocations");
        displayVendorLocations();
    }

    private void displayVendorLocations() {
        vendorLocationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mapView.getOverlays().clear();
                for (DataSnapshot vendorSnapshot : snapshot.getChildren()) {
                    VendorLocation location = vendorSnapshot.getValue(VendorLocation.class);
                    if (location != null) addMarker(location);
                }
                mapView.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void addMarker(VendorLocation location) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(location.getLatitude(), location.getLongitude()));
        marker.setTitle(location.getVendorName() + " - " + location.getCategory());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
    }
}