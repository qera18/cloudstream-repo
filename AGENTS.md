# AGENTS.md

## Project Overview

This is a **CloudStream3 plugin repository** — a Kotlin/Gradle Android library
project that builds `.cs3` plugin files for the CloudStream3 Android streaming app.
There is no web server, database, or API; the "app" is the Gradle build itself.

## Build Environment

- **JDK 17** (eclipse-temurin:17-jdk base image)
- **Android SDK** (platforms;android-35, build-tools;35.0.0) installed in Dockerfile.base44
- **Gradle 8.11.1** via wrapper (wrapper was downgraded from 9.3.1 for AGP 8.7.3 compatibility)
- CloudStream3 Gradle plugin + library pulled from JitPack at build time

## How It Runs

`docker-compose.base44.yml` starts a single `builder` service that:
1. Mounts the source at `/app`
2. Runs `scripts/serve.py` which:
   - Starts an HTTP server on port 3000 immediately (shows "building…" page)
   - Runs `./gradlew :Animecix:make --no-daemon --stacktrace` in a background thread
   - Regenerates the status page when the build finishes (success/failure + .cs3 artifacts)
   - Serves built `.cs3` files and `plugins.json` for download

## Key Files

- `settings.gradle.kts` — only includes `:Animecix` module (other dirs are not Gradle subprojects)
- `build.gradle.kts` — root build script; applies AGP, Kotlin, and CloudStream Gradle plugin to all subprojects
- `Animecix/src/main/kotlin/com/qera18/animecix/` — the actual compiled Kotlin source
- `Animecix/build.gradle.kts` — module-level CloudStream config (language, authors, tvTypes)
- `plugins.json` / `repo.json` — plugin manifests consumed by the CloudStream3 app
- `build.py` — standalone Python script to scan for .cs3 files and emit manifests (does NOT run Gradle)

## Verification

- `docker compose -f docker-compose.base44.yml up -d --build`
- Wait for the build (can take several minutes on first run due to Gradle/AGP/JitPack downloads)
- Open port 3000 — the page should show "Build Successful" with the Animecix.cs3 plugin listed
- Check `docker compose -f docker-compose.base44.yml logs builder` if the build fails

## Notes

- Gradle cache is persisted in a Docker volume (`gradle-cache`) to speed up rebuilds
- The `local.properties` file (pointing to the Android SDK) is generated at runtime by serve.py
- No external secrets are required
