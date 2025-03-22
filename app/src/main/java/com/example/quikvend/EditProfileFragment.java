package com.example.quikvend;
import android.util.Base64;
import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
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
        put("चाट और स्नैक्स (Chaat & Snacks)", new String[]{
                "पानी पूरी (Pani Puri)", "गोलगप्पे (Golgappa)", "पापड़ी चाट (Papdi Chaat)",
                "भेल पूरी (Bhel Puri)", "सेव पूरी (Sev Puri)", "दही पूरी (Dahi Puri)",
                "आलू टिक्की (Aloo Tikki)", "समोसा चाट (Samosa Chaat)", "कचौरी चाट (Kachori Chaat)",
                "राम लड्डू (Ram Ladoo)"
        });

        put("पराठा और ब्रेड आइटम्स (Paratha & Bread Items)", new String[]{
                "पनीर पराठा (Paneer Paratha)", "आलू पराठा (Aloo Paratha)",
                "गोभी पराठा (Gobi Paratha)", "चटनी सैंडविच (Chutney Sandwich)",
                "ब्रेड पकौड़ा (Bread Pakora)", "चीज़ बर्स्ट सैंडविच (Cheese Burst Sandwich)",
                "मसाला टोस्ट (Masala Toast)", "मक्खन टोस्ट (Butter Toast)",
                "गार्लिक ब्रेड (Garlic Bread)", "तंदूरी पनीर सैंडविच (Tandoori Paneer Sandwich)"
        });

        put("पकौड़े और फ्राइड फूड (Pakora & Fried Food)", new String[]{
                "ब्रेड पकौड़ा (Bread Pakora)", "आलू बोंडा (Aloo Bonda)",
                "मिर्ची भज्जी (Mirchi Bhajji)", "बटाटा वडा (Batata Vada)",
                "सिंधी पकोड़ा (Sindhi Pakora)", "कटलेट (Cutlet)", "बेसन गट्टे (Besan Gatte)",
                "वेज कटलेट (Veg Cutlet)", "साबूदाना वडा (Sabudana Vada)", "कड़ी पकौड़ा (Kadhi Pakora)"
        });

        put("वड़ा और इडली (Vada & Idli)", new String[]{
                "इडली सांभर (Idli Sambar)", "वडा पाव (Vada Pav)",
                "वडा सांभर (Vada Sambar)", "मद्रास दाल वड़ा (Madras Dal Vada)",
                "मोरकू वडा (Morukku Vada)"
        });

        put("डोसा और साउथ इंडियन (Dosa & South Indian)", new String[]{
                "मसाला डोसा (Masala Dosa)", "पेपर डोसा (Paper Dosa)",
                "मैसूर डोसा (Mysore Dosa)", "रसम राइस (Rasam Rice)",
                "कोकोनट चटनी डोसा (Coconut Chutney Dosa)"
        });

        put("रोल और बर्गर (Rolls & Burgers)", new String[]{
                "एग रोल (Egg Roll)", "चिकन रोल (Chicken Roll)", "पनीर रोल (Paneer Roll)",
                "चीज़ फ्रेंकी (Cheese Frankie)", "टिक्की बर्गर (Tikki Burger)",
                "वेज फ्रेंकी (Veg Frankie)", "चिकन टिक्का रोल (Chicken Tikka Roll)",
                "हॉट डॉग (Hot Dog)"
        });

        put("देसी तंदूरी और ग्रिल्ड (Desi Tandoori & Grilled)", new String[]{
                "पनीर टिक्का (Paneer Tikka)", "चिकन टिक्का (Chicken Tikka)",
                "कबाब रोल (Kebab Roll)", "मटन कबाब (Mutton Kebab)", "मटन चॉप (Mutton Chop)",
                "कटहल कबाब (Jackfruit Kebab)", "चिकन 65 (Chicken 65)"
        });

        put("उत्तर भारतीय व्यंजन (North Indian Dishes)", new String[]{
                "छोले भटूरे (Chole Bhature)", "राजमा चावल (Rajma Chawal)",
                "दाल बाफला (Dal Bafla)", "लिट्टी चोखा (Litti Chokha)",
                "दाल खिचड़ी (Dal Khichdi)"
        });

        put("गुजराती स्ट्रीट फूड (Gujarati Street Food)", new String[]{
                "मिसल पाव (Misal Pav)", "ढोकला (Dhokla)", "खांडवी (Khandvi)",
                "फाफड़ा-जलबी (Fafda Jalebi)", "मेथी थेपला (Methi Thepla)", "दाबेली (Dabeli)"
        });

        put("चीनी स्ट्रीट फूड (Chinese Street Food)", new String[]{
                "मोमोज (Momos)", "चीज़ मोमोज (Cheese Momos)", "चाउमीन (Chowmein)",
                "वेज मंचूरियन (Veg Manchurian)", "चिकन मंचूरियन (Chicken Manchurian)",
                "मंचूरियन रोल (Manchurian Roll)", "चाइनीज भेल (Chinese Bhel)",
                "वेज नूडल्स (Veg Noodles)", "चिकन नूडल्स (Chicken Noodles)",
                "नूडल समोसा (Noodle Samosa)"
        });

        put("पास्ता और पिज्जा (Pasta & Pizza)", new String[]{
                "वाइट सॉस पास्ता (White Sauce Pasta)", "रेड सॉस पास्ता (Red Sauce Pasta)",
                "चीज़ बर्गर (Cheese Burger)", "चीज़ बॉल्स (Cheese Balls)",
                "हनी चिली पोटैटो (Honey Chilli Potato)", "स्प्रिंग रोल (Spring Roll)"
        });

        put("मिठाइयाँ और मीठे व्यंजन (Sweets & Desserts)", new String[]{
                "जलेबी (Jalebi)", "इमरती (Imarti)", "बालूशाही (Balushahi)",
                "गुलाब जामुन (Gulab Jamun)", "रसगुल्ला (Rasgulla)", "रबड़ी (Rabri)",
                "मालपुआ (Malpua)", "खीर (Kheer)", "बर्फी (Barfi)", "सोहन हलवा (Sohan Halwa)"
        });

        put("कुल्फी और आइसक्रीम (Kulfi & Ice Cream)", new String[]{
                "मटका कुल्फी (Matka Kulfi)", "गुलकंद कुल्फी (Gulkand Kulfi)",
                "केसर कुल्फी (Kesar Kulfi)", "मलाई बर्फ (Malai Barf)"
        });

        put("देसी ड्रिंक्स और शरबत (Desi Drinks & Sherbet)", new String[]{
                "आम पन्ना (Aam Panna)", "बेल का शरबत (Bel Sherbet)", "गन्ने का रस (Sugarcane Juice)",
                "मसाला कोल्ड ड्रिंक (Masala Cold Drink)", "ठंडाई (Thandai)", "शिकंजी (Shikanji)",
                "लस्सी (Lassi)", "नारियल पानी (Coconut Water)", "हॉट चॉकलेट (Hot Chocolate)",
                "सोडा शरबत (Soda Sherbet)"
        });

        put("चाय और कॉफी (Tea & Coffee)", new String[]{
                "अदरक चाय (Ginger Tea)", "मसाला चाय (Masala Tea)",
                "गुड़ वाली चाय (Jaggery Tea)", "केसर दूध (Kesar Milk)"
        });
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
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        vendorRef = FirebaseDatabase.getInstance().getReference("Vendors").child(userId);
        storageRef = FirebaseStorage.getInstance().getReference("VendorProfiles");

        String[] categories = categoryMenuItems.keySet().toArray(new String[0]);
        spinnerCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories));

        btnSelectMenuItems.setOnClickListener(v -> showMenuItemsDialog());
        btnUploadImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnSaveProfile.setOnClickListener(v -> saveVendorProfile());
        btnFetchLocation.setOnClickListener(v -> fetchCurrentLocation());

        loadVendorProfile(); // Load existing data
        return view;

    }
    private void saveVendorProfile() {
        String name = etName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (name.isEmpty() || contact.isEmpty() || selectedMenuItems.isEmpty()) {
            Toast.makeText(requireContext(), "Fill all fields and select menu items.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Creating a map to store vendor details
        HashMap<String, Object> vendorData = new HashMap<>();
        vendorData.put("name", name);
        vendorData.put("contact", contact);
        vendorData.put("category", category);
        vendorData.put("menuItems", selectedMenuItems);
        vendorData.put("location", new HashMap<String, Object>() {{
            put("latitude", latitude);
            put("longitude", longitude);}});
        vendorData.put("address", getAddressFromLocation(latitude, longitude));


        // Adding placeholder for profile image URL
        vendorData.put("profileImageUrl", "");

        // Saving vendor details in Firebase Realtime Database
        vendorRef.updateChildren(vendorData).addOnSuccessListener(aVoid -> {
            if (imageUri != null) {
                uploadProfileImage(); // Upload image only if selected
            } else {
                Toast.makeText(requireContext(), "Profile saved.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(requireContext(), "Failed to save profile.", Toast.LENGTH_SHORT).show()
        );
    }
    private void uploadProfileImage() {
        if (imageUri == null) {
            Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Convert image to Bitmap
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);

            // Convert Bitmap to Base64 string
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            // Store encoded string in Firebase Database
            vendorRef.child("profileImageBase64").setValue(encodedImage)
                    .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Image uploaded successfully.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to upload image.", Toast.LENGTH_SHORT).show());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error encoding image.", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadVendorProfile() {
        vendorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    etName.setText(snapshot.child("name").getValue(String.class));
                    etContact.setText(snapshot.child("contact").getValue(String.class));
                    tvSelectedMenuItems.setText("Selected: " + snapshot.child("menuItems").getValue());
                    latitude = snapshot.child("location/latitude").getValue(Double.class);
                    longitude = snapshot.child("location/longitude").getValue(Double.class);
                    tvCurrentLocation.setText("Current Location: " + getAddressFromLocation(latitude, longitude));

                    String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(requireContext()).load(imageUrl).into(imgProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMenuItemsDialog() {
        String selectedCategory = spinnerCategory.getSelectedItem().toString();
        String[] menuItems = categoryMenuItems.getOrDefault(selectedCategory, new String[]{});
        boolean[] checkedItems = new boolean[menuItems.length];

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Menu Items")
                .setMultiChoiceItems(menuItems, checkedItems, (dialog, index, isChecked) -> {
                    if (isChecked && !selectedMenuItems.contains(menuItems[index]))
                        selectedMenuItems.add(menuItems[index]);
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
}