// Animecix module

version = 1

cloudstream {
    language = "en"
    authors = listOf("qera18")
    description = "Access to the site is blocked due to a security service"
    status = 1 // 0: Down, 1: Ok, 2: Slow, 3: Beta
    tvTypes = listOf("Anime")
    iconUrl = "https://www.google.com/s2/favicons?domain=animecix.tv&sz=%size%"
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
