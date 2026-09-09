buildscript {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            metadataSources {
                artifact()
            }
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        classpath("com.github.recloudstream.gradle:gradle:gradle-master-32895aedb6-1")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
