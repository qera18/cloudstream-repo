pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "cloudstream-repo"

// Automatically include subprojects only if the corresponding directory exists on disk
// and contains a Gradle build file (build.gradle or build.gradle.kts) or a manifest.json
// This prevents the build from failing when stale includes point to non-existent folders.
val detectedProjects = rootDir.listFiles()
    ?.filter { it.isDirectory }
    ?.filter {
        java.io.File(it, "build.gradle.kts").exists() ||
        java.io.File(it, "build.gradle").exists() ||
        java.io.File(it, "manifest.json").exists()
    }
    ?.map { it.name }
    ?: emptyList()

// Include each detected project
for (proj in detectedProjects) {
    include(":${'$'}proj")
}
