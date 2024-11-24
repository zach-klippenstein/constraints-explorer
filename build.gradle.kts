plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.publish)
}

android {
  namespace = "com.zachklipp.constraintsexplorer"
  compileSdk = 35

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
  kotlin {
    explicitApi()
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
  testOptions {
    targetSdk = 35
  }
}

dependencies {
  api(libs.compose.ui)

  implementation(platform(libs.compose.bom))
  implementation(libs.compose.foundation)
  implementation(libs.compose.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)

  debugImplementation(libs.compose.tooling)

  testImplementation(libs.test.junit)

  androidTestImplementation(libs.test.androidx.junit)
  androidTestImplementation(libs.test.androidx.junit.ktx)
  androidTestImplementation(libs.test.androidx.espresso.core)
  androidTestImplementation(libs.test.compose.base)
  androidTestImplementation(libs.test.compose.composeRule)
  androidTestImplementation(libs.test.kotlin.junit)
}
