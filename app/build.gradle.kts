import java.util.Properties

configurations.all {
    // UnifiedPush connector needs tink-android; force the -android flavor everywhere.
    resolutionStrategy {
        force("com.google.crypto.tink:tink-android:1.17.0")
        dependencySubstitution {
            substitute(module("com.google.crypto.tink:tink"))
                .using(module("com.google.crypto.tink:tink-android:1.17.0"))
        }
    }
}

// Signing follows the url-save-to-faved convention: keystore.properties
// (gitignored) points at a jks inside ~/keystores/. Absent it, release
// builds unsigned (F-Droid signs out-of-band; CI builds unsigned).
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) load(file.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.gochathub.gochathubclient"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gochathub.gochathubclient"
        minSdk = 28
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProperties.containsKey("storeFile")) signingConfig = signingConfigs.getByName("release")
        }
    }

    // F-Droid reproducibility: no Google-signed dependency metadata blob in the APK.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":chatuikit-core"))
    implementation(project(":chatuikit-compose"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation("org.unifiedpush.android:connector:3.0.10")
    // QR sign-in scanner (both Apache-2.0)
    implementation("androidx.camera:camera-core:1.6.2")
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("androidx.camera:camera-view:1.6.2")
    implementation("com.google.zxing:core:3.5.4")

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")

    testImplementation(libs.junit)
    testImplementation(libs.ext.junit)
    // Share-target intent parsing needs real Intent/Uri behavior.
    testImplementation(libs.robolectric)
}
