package com.example.quikvend;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.graphics.Color;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private boolean isVendor = false;
    private static final int RC_SIGN_IN = 100;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) navigateToTrackingFragment();
                else Toast.makeText(this, "Location permission required for tracking.", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button btnUserLogin = findViewById(R.id.btnUserLogin);
        Button btnVendorLogin = findViewById(R.id.btnVendorLogin);
        SignInButton btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

//        btnUserLogin.setBackgroundColor(Color.GREEN);  // Remove this if present
//        btnVendorLogin.setBackgroundColor(Color.GREEN);  // Remove this if present

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnUserLogin.setOnClickListener(v -> handleRoleSelection(false, btnGoogleSignIn));
        btnVendorLogin.setOnClickListener(v -> handleRoleSelection(true, btnGoogleSignIn));

        btnGoogleSignIn.setOnClickListener(v -> startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN));
    }

    private void handleRoleSelection(boolean selectedVendor, SignInButton signInButton) {
        isVendor = selectedVendor;
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {
            databaseReference.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String storedRole = snapshot.child("role").getValue(String.class);
                        if ((isVendor && "vendor".equals(storedRole)) || (!isVendor && "user".equals(storedRole))) {
                            signInButton.setVisibility(View.VISIBLE); // Role matches, allow sign-in
                        } else {
                            Toast.makeText(LoginActivity.this, "Registered as '" + storedRole + "'. Use the correct login option.", Toast.LENGTH_LONG).show();
                            signInButton.setVisibility(View.GONE); // Prevent role switching
                        }
                    } else signInButton.setVisibility(View.VISIBLE); // New user, allow sign-in
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else signInButton.setVisibility(View.VISIBLE); // No logged-in user, allow sign-in
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (task.isSuccessful() && task.getResult() != null) firebaseAuthWithGoogle(task.getResult());
            else Toast.makeText(this, "Sign-In Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    databaseReference.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) saveUserRole(user); // Save role for new users

                            if (isVendor) checkLocationPermission();
                            else navigateToUserHome();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else Toast.makeText(this, "Firebase Auth Failed.", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveUserRole(FirebaseUser user) {
        String role = isVendor ? "vendor" : "user";
        databaseReference.child(user.getUid()).child("role").setValue(role);
        databaseReference.child(user.getUid()).child("email").setValue(user.getEmail());
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            navigateToTrackingFragment();
        } else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void navigateToTrackingFragment() {
        Intent intent = new Intent(this, VendorHomeActivity.class);
        intent.putExtra("openFragment", "TrackingFragment");
        startActivity(intent);
        finish();
    }

    private void navigateToUserHome() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}