import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.notestasks.xydiln"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

abstract class KspAwtExceptionGuardService :
  org.gradle.api.services.BuildService<org.gradle.api.services.BuildServiceParameters.None> {
  init {
    installGuard()
  }

  companion object {
    @Synchronized
    fun installGuard() {
      val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
      if (previousHandler?.javaClass?.name?.contains("KspAwtExceptionGuardService") != true) {
        val guardHandler =
          Thread.UncaughtExceptionHandler { t, e ->
            if (isHarmlessKspAwtException(t, e)) {
              return@UncaughtExceptionHandler
            }
            previousHandler?.uncaughtException(t, e)
          }
        Thread.setDefaultUncaughtExceptionHandler(guardHandler)
        Thread.getAllStackTraces().keys.forEach { thread ->
          if (thread.name.startsWith("AWT-EventQueue")) {
            thread.uncaughtExceptionHandler = guardHandler
          }
        }
      }
      if (System.getProperty("ksp.awt.eventqueue.guarded") != "true") {
        System.setProperty("ksp.awt.eventqueue.guarded", "true")
        try {
          val toolkitClass = Class.forName("java.awt.Toolkit", true, ClassLoader.getPlatformClassLoader())
          val toolkit = toolkitClass.getMethod("getDefaultToolkit").invoke(null)
          toolkitClass.getMethod("getSystemEventQueue").invoke(toolkit)
          Thread.getAllStackTraces().keys.forEach { thread ->
            if (thread.name.startsWith("AWT-EventQueue")) {
              thread.uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
            }
          }
        } catch (_: Throwable) {}
      }
    }

    private fun isHarmlessKspAwtException(t: Thread?, e: Throwable): Boolean {
      var curr: Throwable? = e
      while (curr != null) {
        val msg = curr.message ?: ""
        if (
          msg.contains("ksp.com.intellij.openapi.application.Application") ||
            curr.stackTrace.any { it.className.startsWith("ksp.com.intellij.") }
        ) {
          return true
        }
        curr = curr.cause
      }
      return t?.name?.startsWith("AWT-EventQueue") == true && e is NullPointerException
    }
  }
}

val kspAwtGuard =
  gradle.sharedServices.registerIfAbsent(
    "kspAwtExceptionGuard",
    KspAwtExceptionGuardService::class.java,
  ) {}
KspAwtExceptionGuardService.installGuard()

tasks.configureEach { usesService(kspAwtGuard) }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.auth)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
