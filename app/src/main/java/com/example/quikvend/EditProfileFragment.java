package com.example.quikvend;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.*;

public class EditProfileFragment extends Fragment {
    private EditText etName, etContact;
    private Spinner spinnerCategory;
    private TextView tvSelectedMenuItems, tvCurrentLocation;
    private ImageView imgProfile;
    private Uri imageUri;
    private List<String> selectedMenuItems = new ArrayList<>();

    private final Map<String, String[]> categoryMenuItems = new HashMap<String, String[]>() {{
        put("Snacks", new String[]{"Pav Bhaji", "Vada Pav", "Bhel Puri", "Samosa"});
        put("Beverages", new String[]{"Tea", "Coffee", "Lassi", "Juice"});
        put("Fast Food", new String[]{"Burger", "Pizza", "Fries", "Hot Dog"});
    }};

    private FusedLocationProviderClient fusedLocationClient;
    private double latitude = 0.0, longitude = 0.0;
    private DatabaseReference vendorRef;
    private StorageReference storageRef;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    Glide.with(requireContext()).load(uri).into(imgProfile);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_edit_profile_fragment, container, false);

        etName = view.findViewById(R.id.etName);
        etContact = view.findViewById(R.id.etContact);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        tvSelectedMenuItems = view.findViewById(R.id.tvSelectedMenuItems);
        tvCurrentLocation = view.findViewById(R.id.tvCurrentLocation);
        imgProfile = view.findViewById(R.id.imgProfile);
        Button btnSelectMenuItems = view.findViewById(R.id.btnSelectMenuItems);
        Button btnUploadImage = view.findViewById(R.id.btnUploadImage);
        Button btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        Button btnFetchLocation = view.findViewById(R.id.btnFetchLocation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        vendorRef = FirebaseDatabase.getInstance().getReference("Vendors")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        storageRef = FirebaseStorage.getInstance().getReference("VendorProfiles");

        String[] categories = categoryMenuItems.keySet().toArray(new String[0]);
        spinnerCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories));

        btnSelectMenuItems.setOnClickListener(v -> showMenuItemsDialog());
        btnUploadImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnSaveProfile.setOnClickListener(v -> saveVendorProfile());
        btnFetchLocation.setOnClickListener(v -> fetchCurrentLocation());

        return view;
    }

    private void showMenuItemsDialog() {
        String selectedCategory = spinnerCategory.getSelectedItem().toString();
        String[] menuItems = categoryMenuItems.getOrDefault(selectedCategory, new String[]{});
        boolean[] checkedItems = new boolean[menuItems.length];

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Menu Items")
                .setMultiChoiceItems(menuItems, checkedItems, (dialog, index, isChecked) -> {
                    if (isChecked && !selectedMenuItems.contains(menuItems[index])) selectedMenuItems.add(menuItems[index]);
                    else selectedMenuItems.remove(menuItems[index]);
                })
                .setPositiveButton("OK", (dialog, which) -> tvSelectedMenuItems.setText("Selected: " + selectedMenuItems))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission not granted.", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        tvCurrentLocation.setText("Current Location: " + getAddressFromLocation(latitude, longitude));
                    } else {
                        Toast.makeText(requireContext(), "Unable to fetch location.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getAddressFromLocation(double lat, double lon) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            return addresses != null && !addresses.isEmpty() ? addresses.get(0).getAddressLine(0) : "Address not found";
        } catch (IOException e) {
            return "Address fetch error";
        }
    }

    private void saveVendorProfile() {
        String name = etName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (name.isEmpty() || contact.isEmpty() || selectedMenuItems.isEmpty()) {
            Toast.makeText(requireContext(), "Fill all fields and select menu items.", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> vendorData = new HashMap<>();
        vendorData.put("name", name);
        vendorData.put("contact", contact);
        vendorData.put("category", category);
        vendorData.put("menuItems", selectedMenuItems);
        vendorData.put("location", new HashMap<String, Object>() {{
            put("latitude", latitude);
            put("longitude", longitude);
        }});

        vendorRef.updateChildren(vendorData).addOnSuccessListener(aVoid -> {
            if (imageUri != null) uploadProfileImage();
            else Toast.makeText(requireContext(), "Profile saved.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to save profile.", Toast.LENGTH_SHORT).show());
    }

    private void uploadProfileImage() {
        storageRef.child(vendorRef.getKey() + ".jpg").putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.child(vendorRef.getKey() + ".jpg")
                        .getDownloadUrl().addOnSuccessListener(uri -> vendorRef.child("profileImageUrl").setValue(uri.toString())
                                .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Profile image uploaded.", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to save image URL.", Toast.LENGTH_SHORT).show()))
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "Image URL retrieval failed.", Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Image upload failed.", Toast.LENGTH_SHORT).show());
    }
}