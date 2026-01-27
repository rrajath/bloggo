# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HugoWriter is an Android application built with Kotlin and Jetpack Compose. It targets Android API 33+ (minSdk 33, targetSdk 36).

## Build System

This project uses Gradle with Kotlin DSL (`.gradle.kts` files) and version catalogs (`gradle/libs.versions.toml`).

### Key Build Commands

**Build the project:**
```bash
./gradlew build
```

**Run unit tests:**
```bash
./gradlew test
```

**Run a specific unit test:**
```bash
./gradlew test --tests com.rrajath.hugowriter.ExampleUnitTest
```

**Run instrumented tests (requires emulator or device):**
```bash
./gradlew connectedAndroidTest
```

**Run a specific instrumented test:**
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rrajath.hugowriter.ExampleInstrumentedTest
```

**Clean build:**
```bash
./gradlew clean
```

**Assemble debug APK:**
```bash
./gradlew assembleDebug
```

**Install debug APK on connected device:**
```bash
./gradlew installDebug
```

## Architecture

**UI Framework:** Jetpack Compose with Material3
**Language:** Kotlin (official code style)
**Build Tool:** Gradle 9.0.0 with Android Gradle Plugin
**Compose Compiler:** Kotlin 2.0.21 with Compose plugin

### Code Structure

- **Package:** `com.rrajath.hugowriter`
- **Main Activity:** `MainActivity.kt` - Entry point with edge-to-edge display
- **Theme:** Custom theme files in `ui/theme/` directory
  - `Theme.kt` - Main theme configuration
  - `Color.kt` - Color definitions
  - `Type.kt` - Typography definitions

### Testing Structure

- **Unit tests:** `app/src/test/java/` - JUnit tests
- **Instrumented tests:** `app/src/androidTest/java/` - Android instrumentation tests with Espresso

## Dependencies Management

Dependencies are managed through Gradle version catalog at `gradle/libs.versions.toml`. To add new dependencies:

1. Add the version in `[versions]` section
2. Add the library in `[libraries]` section
3. Reference it in `app/build.gradle.kts` using `libs.` prefix

## Java Compatibility

The project uses Java 11 source and target compatibility (configured in `app/build.gradle.kts`).
