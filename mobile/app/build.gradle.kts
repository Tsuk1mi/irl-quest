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
  }

  buildTypes {
    debug {
      isMinifyEnabled = false
    }
  }

  buildFeatures {
    compose = true
  }

  composeOptions {
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
  val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation("androidx.core:core-ktx:1.12.0")
  implementation("androidx.activity:activity-compose:1.8.2")

  // Compose и Material3
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material3:material3-window-size-class")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.compose.ui:ui-tooling-preview")

  // Navigation
  implementation("androidx.navigation:navigation-compose:2.7.6")

  // ViewModel и Lifecycle
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

  // Retrofit и сетевые компоненты
  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
  implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

  // Kotlin Serialization
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

  // Material Design
  implementation("com.google.android.material:material:1.11.0")
  implementation("androidx.compose.material:material")

  // Debug
  debugImplementation("androidx.compose.ui:ui-tooling")
}