plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.lagradost.cloudstream3.gradle")
}

android {
    namespace = "com.cloudstream.belgeselx"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}

dependencies {
    cloudstream("com.lagradost:cloudstream3:pre-release")
}
