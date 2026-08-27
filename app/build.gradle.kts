plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

// versionName is hand-managed in gradle.properties (bloggo.versionName).
// versionCode is derived from it: MAJOR.MINOR.PATCH -> MAJOR*10000 + MINOR*100 + PATCH
// (e.g. 1.4.3 -> 10403), so it always increases with the version and MINOR/PATCH
// are capped at 99.
val appVersionName: String = (project.findProperty("bloggo.versionName") as String?)
    ?: error("bloggo.versionName is not set in gradle.properties")
val appVersionCode: Int = run {
    val parts = appVersionName.split(".")
    require(parts.size == 3 && parts.all { it.toIntOrNull() != null }) {
        "bloggo.versionName must be MAJOR.MINOR.PATCH, was \"$appVersionName\""
    }
    val (major, minor, patch) = parts.map { it.toInt() }
    require(minor in 0..99 && patch in 0..99) {
        "bloggo.versionName MINOR and PATCH must each be 0..99, was \"$appVersionName\""
    }
    major * 10000 + minor * 100 + patch
}

android {
    namespace = "com.rrajath.bloggo"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.rrajath.bloggo"
        minSdk = 34
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // CI decodes the keystore and exports these. Absent locally, so
            // assembleRelease produces an unsigned APK unless KEYSTORE_PATH is set.
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEY_STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.okhttp.mockwebserver)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  implementation(libs.androidx.compose.foundation)

  // Preferences (theme, and other small typed settings)
  implementation(libs.androidx.datastore.preferences)

  // Secrets: the GitHub PAT, Keystore-backed (ANDROID_TDD.md §7.3)
  implementation(libs.androidx.security.crypto)

  // GitHub client (ANDROID_TDD.md §5)
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlinx.serialization.converter)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)
  debugImplementation(libs.okhttp.logging.interceptor)

  // Frontmatter cache for the library listing (ANDROID_TDD.md §5.2)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
  testImplementation(libs.androidx.room.runtime)

  // Image loading for Preview and the Media screen — real repo/staged images,
  // not the placeholder box they used to be. coil-network-okhttp reuses the
  // OkHttp dependency already declared above, rather than a second HTTP stack.
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)

  // Bloggo modules
  implementation(project(":designsystem"))
  implementation(project(":coverart"))
}
