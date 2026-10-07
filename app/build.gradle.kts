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

    buildTypes {
        debug {
            // ponytail: default points at the dev deployment; override with -Pbase_url
            val defaultBaseUrl = project.findProperty("base_url") as String? ?: "http://192.0.2.10:8080"
            buildConfigField("String", "DEFAULT_BASE_URL", "\"$defaultBaseUrl\"")
        }
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
