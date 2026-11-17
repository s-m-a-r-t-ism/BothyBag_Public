plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.bothybag.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bothybag.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.3"
    }

    buildTypes {
        release {
            // Strips unused code and resources. Most of the saving comes from the
            // extended icon set, of which the app uses only a handful.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // OSMDroid
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.37.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
}
