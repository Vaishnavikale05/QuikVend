package com.example.quikvend;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class VendorProfileFragment extends Fragment {

    private TextView tvName, tvContact, tvCategory, tvLocation, tvMenuItem;
    private ImageView imgProfile;
    private DatabaseReference vendorRef;  // FIXED: Defined vendorRef

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_vendor_profile_fragment, container, false);

        tvName = view.findViewById(R.id.tvVendorName);
        tvContact = view.findViewById(R.id.tvVendorContact);
        tvCategory = view.findViewById(R.id.tvVendorCategory);
        tvLocation = view.findViewById(R.id.tvVendorLocation);
        tvMenuItem = view.findViewById(R.id.tvVendorMenuItem);
        imgProfile = view.findViewById(R.id.imgVendorProfile);

        // FIXED: Initialize Firebase Database Reference
        String vendorUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        vendorRef = FirebaseDatabase.getInstance().getReference("Vendors").child(vendorUid);

        loadVendorProfile();
        return view;
    }

    private void loadVendorProfile() {
        vendorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String contact = snapshot.child("contact").getValue(String.class);
                    String category = snapshot.child("category").getValue(String.class);
                    String location = snapshot.child("address").getValue(String.class);
                    String menuItem = snapshot.child("menuItem").getValue(String.class);
                    String encodedImage = snapshot.child("profileImageBase64").getValue(String.class);

                    tvName.setText("Name: " + (name != null ? name : "Not Available"));
                    tvContact.setText("Contact: " + (contact != null ? contact : "Not Available"));
                    tvCategory.setText("Category: " + (category != null ? category : "Not Available"));
                    tvLocation.setText("Location: " + (location != null ? location : "Not Available"));
                    tvMenuItem.setText("Menu Item: " + (menuItem != null ? menuItem : "Not Available"));

                    // Decode and display the image
                    if (encodedImage != null && !encodedImage.isEmpty()) {
                        try {
                            byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            imgProfile.setImageBitmap(decodedBitmap);
                        } catch (IllegalArgumentException e) {
                            Toast.makeText(requireContext(), "Error decoding image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
