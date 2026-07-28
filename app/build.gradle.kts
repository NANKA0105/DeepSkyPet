plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.deepsky.pet"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.deepsky.pet"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        buildConfigField("String", "SUPABASE_URL", "\"https://nnkvsbotrtldtzegtbkp.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5ua3ZzYm90cnRsZHR6ZWd0YmtwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI2MjYzNzQsImV4cCI6MjA5ODIwMjM3NH0.OprrmRa-E9GyqWQ4GAPBB4r0F069XUo-2U6YLehJxZM\"")
    }

    signingConfigs {
        create("release") {
            // In CI: decode from secrets. Local: use your own keystore.
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.json:json:20231013")
}
