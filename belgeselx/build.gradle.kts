plugins {
    id("com.android.library")
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
    implementation("com.github.Blatzar:NiceHttp:0.4.11")
    implementation("org.jsoup:jsoup:1.18.3")
}
