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
        versionCode = 1
        versionName = "1.0.0"
    }

    // Signing is optional: F-Droid builds unsigned and signs itself. For a signed
    // release set RELEASE_KEYSTORE, RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD.
    val releaseKeystore = System.getenv("RELEASE_KEYSTORE")
    if (releaseKeystore != null) {
        signingConfigs.create("release") {
            storeFile = file(releaseKeystore)
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
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

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
}
