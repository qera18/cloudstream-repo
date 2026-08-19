import com.lagradost.cloudstream3.gradle.CloudstreamExtension

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.0")
        // Kotlin sürümünü 1.9.22 yerine 2.0.0 yapıyoruz
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) =
    extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
