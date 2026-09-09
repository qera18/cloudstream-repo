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
        classpath("com.android.tools.build:gradle:9.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
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
