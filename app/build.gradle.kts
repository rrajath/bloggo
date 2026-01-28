plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.rrajath.hugowriter"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.rrajath.hugowriter"
        minSdk = 33
        targetSdk = 36
        versionCode = 29492969
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // For CI/CD (GitHub Actions)
            val keystoreFile = rootProject.file("keystore.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
            // For local development (optional)
            else if (project.hasProperty("KEYSTORE_FILE")) {
                storeFile = file(project.property("KEYSTORE_FILE") as String)
                storePassword = project.property("KEYSTORE_PASSWORD") as String
                keyAlias = project.property("KEY_ALIAS") as String
                keyPassword = project.property("KEY_PASSWORD") as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only apply signing config if it's configured
            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile?.exists() == true) {
                signingConfig = releaseSigningConfig
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // Markdown
    implementation(libs.multiplatform.markdown.renderer.m3)
    implementation(libs.coil.compose)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}