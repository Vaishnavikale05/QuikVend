package com.example.quikvend;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class VendorHomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vendor_home);

        BottomNavigationView bottomNav = findViewById(R.id.vendorBottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;

            if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new VendorProfileFragment();
            } else if (item.getItemId() == R.id.nav_edit_profile) {
                selectedFragment = new EditProfileFragment();
            } else {
                selectedFragment = new TrackingFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.vendorFragmentContainer, selectedFragment)
                    .commit();
            return true;
        });

        // Load TrackingFragment by default
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_tracking);
        }
    }
}