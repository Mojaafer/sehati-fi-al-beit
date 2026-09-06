import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  // alias(libs.plugins.roborazzi)
  alias(libs.plugins.google.services)
  // Crash reporting. Uploads stay no-op until the console-side Crashlytics setup is done with
  // the real google-services.json, so builds without that file are unaffected.
  alias(libs.plugins.firebase.crashlytics)
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.aistudio.sehatihomecare.sd"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // The transfer destination shown on the receipt screen. Overridable without a code change:
    //   ./gradlew assembleDebug -PsehatiBankAccount=1234567
    // The placeholder below is NOT a real account; set the real one before launch.
    buildConfigField(
        "String",
        "BANK_ACCOUNT_NUMBER",
        "\"${project.findProperty("sehatiBankAccount") ?: "2401234"}\""
    )
    buildConfigField(
        "String",
        "BANK_ACCOUNT_NAME",
        "\"${project.findProperty("sehatiBankAccountName") ?: "صحتي في البيت للخدمات الصحية"}\""
    )
    val envSupabaseUrl = System.getenv("SUPABASE_URL") ?: (project.findProperty("supabaseUrl") as? String) ?: ""
    val envSupabaseAnonKey = System.getenv("SUPABASE_ANON_KEY") ?: (project.findProperty("supabaseAnonKey") as? String) ?: ""
    buildConfigField(
        "String",
        "SUPABASE_URL",
        "\"$envSupabaseUrl\""
    )
    buildConfigField(
        "String",
        "SUPABASE_ANON_KEY",
        "\"$envSupabaseAnonKey\""
    )
    // Data-plane switch: false keeps the app on Firestore; assemble with
    // -PuseSupabase=true to route repositories through Supabase PostgREST.
    buildConfigField(
        "boolean",
        "USE_SUPABASE",
        "${project.findProperty("useSupabase") ?: "false"}"
    )
  }

  signingConfigs {
    create("release") {
      val envPath = System.getenv("KEYSTORE_PATH")
      storeFile = envPath?.let(::file) ?: file("${rootDir}/missing-release.keystore")
      storePassword = System.getenv("STORE_PASSWORD") ?: ""
      keyAlias = System.getenv("KEY_ALIAS") ?: ""
      keyPassword = System.getenv("KEY_PASSWORD") ?: ""
    }
    // No "debugConfig" here: the debug build type below uses AGP's built-in "debug" config, which
    // already points at the committed debug.keystore. A second block only duplicated its
    // credentials without ever being referenced.
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // R8 shrinks and obfuscates. Everything that resolves a name at runtime — Firestore
      // entities, the Moshi DTOs, the Retrofit interface — is pinned in proguard-rules.pro.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debug") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      // Firestore's mapper logs through android.util.Log, which otherwise throws under JVM tests.
      isReturnDefaultValues = true
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Allows the project to build before google-services.json is added; Firebase
// features will not work at runtime until the real file is in place.
googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.messaging)
  implementation(libs.firebase.crashlytics)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  // Robolectric stays off: it resolves nativeruntime-dist-compat plus a ~100 MB android-all jar at
  // test time, and this machine has ~1.9 GB free on a link slow enough to have corrupted a large
  // fetch before. UI verification happens on the physical device instead.
  // testImplementation(libs.robolectric)
  // testImplementation(libs.roborazzi)
  // testImplementation(libs.roborazzi.compose)
  // testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
