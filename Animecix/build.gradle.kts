plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.qera18.animecix"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    cloudstream("com.lagradost:cloudstream3:pre-release")
}
