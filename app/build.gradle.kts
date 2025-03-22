plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.quikvend"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.quikvend"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.cardview:cardview:1.0.0")  // CardView dependency
    implementation("com.google.android.material:material:1.9.0") // Material Components

    // OSMDroid (OpenStreetMap)
    implementation("org.osmdroid:osmdroid-android:6.1.14")

    // Play Services Location (for GPS and Vendor Tracking)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Google Sign-In (for Authentication)
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Firebase
    implementation("com.google.firebase:firebase-auth:22.1.1")
    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation("com.google.firebase:firebase-storage:20.3.0")
// Firebase
    implementation("com.google.firebase:firebase-auth:22.1.1")
    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation("com.google.firebase:firebase-storage:20.3.0")

// Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

// Google Sign-In (for Authentication)
    implementation("com.google.android.gms:play-services-auth:20.7.0")

// Play Services Location (for GPS and Vendor Tracking)
    implementation("com.google.android.gms:play-services-location:21.0.1")

// OSMDroid (OpenStreetMap)
    implementation("org.osmdroid:osmdroid-android:6.1.14")

}