plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

configurations.all {
    exclude(group = "org.jetbrains", module = "annotations-java5")
}

android {
    namespace = "com.cometchat.uikit.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // ponytail: closed SDK kept for model/config types only — no CometChat.init
    // anywhere; every datasource/repository impl is rewired to the GoChatHub API.
    // If models drag in cloud protocol later, mirror them under data/model/.

    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation("androidx.security:security-crypto:1.1.0")
    api(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("com.squareup.okhttp3:mockwebserver:${libs.versions.okhttp.get()}")
}

// unit tests run on the JVM with plain java.nio
android {
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}
