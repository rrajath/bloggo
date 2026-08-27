plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
    namespace = "com.rrajath.bloggo"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.rrajath.bloggo"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
