plugins {
  alias(libs.plugins.android.library)
  // alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.publish)
}

android {
  namespace = "com.zachklipp.constraintsexplorer"
  compileSdk = 34

  defaultConfig {
    minSdk = 21

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.14"
  }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)

  debugImplementation("androidx.compose.ui:ui-tooling")

  testImplementation(libs.junit)

  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
}
