plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.antigravity.githubnoti"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.antigravity.githubnoti"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "1.1.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

// 릴리즈/디버그 빌드 완료 후 지정된 형식(github-noti_v1.x.y.apk)으로 APK 복사 생성
val currentVersion = android.defaultConfig.versionName ?: "1.1.4"

abstract class CopyApkTask : DefaultTask() {
    @get:InputDirectory
    abstract val apkDir: DirectoryProperty

    @get:Input
    abstract val sourceFileName: Property<String>

    @get:Input
    abstract val targetFileName: Property<String>

    @TaskAction
    fun copyApk() {
        val dir = apkDir.get().asFile
        val src = File(dir, sourceFileName.get())
        val target = File(dir, targetFileName.get())
        if (src.exists()) {
            src.copyTo(target, overwrite = true)
            println(">> Generated: ${target.name} (${target.length()} bytes)")
        }
    }
}

val renameReleaseApk = tasks.register<CopyApkTask>("renameReleaseApk") {
    apkDir.set(layout.buildDirectory.dir("outputs/apk/release"))
    sourceFileName.set("app-release.apk")
    targetFileName.set("github-noti_v${currentVersion}.apk")
}

val renameDebugApk = tasks.register<CopyApkTask>("renameDebugApk") {
    apkDir.set(layout.buildDirectory.dir("outputs/apk/debug"))
    sourceFileName.set("app-debug.apk")
    targetFileName.set("github-noti_v${currentVersion}.apk")
}

afterEvaluate {
    tasks.named("assembleRelease").configure {
        finalizedBy(renameReleaseApk)
    }
    tasks.named("assembleDebug").configure {
        finalizedBy(renameDebugApk)
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

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // OkHttp & Serialization
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging)
  implementation(libs.kotlinx.serialization.json)

  // WorkManager for background release download polling
  implementation(libs.androidx.work.runtime.ktx)

  // Extended Material Icons
  implementation(libs.androidx.compose.material.icons.extended)
}
