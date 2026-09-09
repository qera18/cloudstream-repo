plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.lagradost.cloudstream3.gradle")
}

android {
    namespace = "com.qera18.belgeselx"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

cloudstream {
    defaultConfig {
        id = 1
        name = "belgeselx"
        version = 1
        language = "tr"
        authors = listOf("qera18")
        tvTypes = listOf("Movie")
    }
}

dependencies {
    cloudstream("com.lagradost:cloudstream3:pre-release")
}
