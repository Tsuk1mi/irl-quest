plugins {
  id("com.android.application")
  kotlin("android")
  id("org.jetbrains.kotlin.plugin.serialization")
}

android {
  compileSdk = 34
  namespace = "com.irlquest.app"

  defaultConfig {
    applicationId = "com.irlquest.app"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "0.1"
    // Configurable base URL for API. For local development on emulator use 10.0.2.2 -> host's localhost
    // If you run on a physical device, replace with your machine IP (e.g. http://192.168.43.52:8003/)
    buildConfigField("String", "API_BASE_URL", "\"http://192.168.43.52:8003/api/v1/\"")
  }

  buildTypes {
    debug {
      isMinifyEnabled = false
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  composeOptions {
    // Use a Compose compiler extension version available in public repos (reverted to 1.5.3)
    kotlinCompilerExtensionVersion = "1.5.3"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  kotlinOptions {
    jvmTarget = "11"
  }
}

dependencies {
  // Shared KMP module
  implementation(project(":shared"))
  
  // Заменяем BOM на явные версии, чтобы избежать проблем с разрешением артефактов
  val composeVersion = "1.5.3"
  val material3Version = "1.1.1"

  implementation("androidx.core:core-ktx:1.12.0")
  implementation("androidx.activity:activity-compose:1.8.2")

  // Compose и Material3 (указываем версии явно)
  implementation("androidx.compose.ui:ui:$composeVersion")
  implementation("androidx.compose.ui:ui-graphics:$composeVersion")
  implementation("androidx.compose.animation:animation:$composeVersion")
  implementation("androidx.compose.material3:material3:$material3Version")
  implementation("androidx.compose.material3:material3-window-size-class:$material3Version")
  implementation("androidx.compose.material:material-icons-extended:$composeVersion")
  implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")

  // Navigation
  implementation("androidx.navigation:navigation-compose:2.7.6")

  // ViewModel и Lifecycle
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

  val ktorVersion = "2.3.5"
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
  implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

  // Kotlin Serialization
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

  // Material Design
  implementation("com.google.android.material:material:1.11.0")
  implementation("androidx.compose.material:material:$composeVersion")

  // Debug
  debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
  androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")

  // Timber logging
  implementation("com.jakewharton.timber:timber:5.0.1")
}