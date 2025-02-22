package com.example.quikvend;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class VendorProfileFragment extends Fragment {

    private TextView tvName, tvContact, tvCategory, tvLocation, tvMenuItem;
    private ImageView imgProfile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_vendor_profile_fragment, container, false);

        tvName = view.findViewById(R.id.tvVendorName);
        tvContact = view.findViewById(R.id.tvVendorContact);
        tvCategory = view.findViewById(R.id.tvVendorCategory);
        tvLocation = view.findViewById(R.id.tvVendorLocation);
        tvMenuItem = view.findViewById(R.id.tvVendorMenuItem);
        imgProfile = view.findViewById(R.id.imgVendorProfile);

        loadVendorProfile();
        return view;
    }

    private void loadVendorProfile() {
        String vendorUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference vendorRef = FirebaseDatabase.getInstance().getReference("Vendors").child(vendorUid);

        vendorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String contact = snapshot.child("contact").getValue(String.class);
                    String category = snapshot.child("category").getValue(String.class);
                    String location = snapshot.child("location").child("address").getValue(String.class);
                    String menuItem = snapshot.child("menuItem").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    tvName.setText("Name: " + name);
                    tvContact.setText("Contact: " + contact);
                    tvCategory.setText("Category: " + category);
                    tvLocation.setText("Location: " + (location != null ? location : "Not Available"));
                    tvMenuItem.setText("Menu Item: " + (menuItem != null ? menuItem : "Not Available"));

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(requireContext()).load(profileImageUrl).into(imgProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
}