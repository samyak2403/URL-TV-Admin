plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.samyak.urltvadmin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.samyak.urltvadmin"
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
    
    buildFeatures {
        viewBinding = true
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.volley)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    
    // Firebase dependencies with BoM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database")
    
    // Add these for App Check
    implementation("com.google.firebase:firebase-appcheck-interop")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    
    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // OneSignal
    implementation("com.onesignal:OneSignal:[5.0.0, 5.1.99]")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
}