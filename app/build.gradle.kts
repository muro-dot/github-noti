plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// 릴리즈 서명 정보는 저장소에 넣지 않고 ~/.gradle/gradle.properties 에서 읽는다. (docs/RELEASE.md 참고)
val hasReleaseSigning = project.hasProperty("GITHUBNOTI_KEYSTORE")

android {
    namespace = "com.antigravity.githubnoti"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.antigravity.githubnoti"
        minSdk = 24
        targetSdk = 36
        versionCode = 10
        versionName = "1.1.8"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(project.property("GITHUBNOTI_KEYSTORE") as String)
                storePassword = project.property("GITHUBNOTI_STORE_PASSWORD") as String
                keyAlias = project.property("GITHUBNOTI_KEY_ALIAS") as String
                keyPassword = project.property("GITHUBNOTI_KEY_PASSWORD") as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 서명 키가 없는 환경에서는 서명 없이 두어, 디버그 키로 잘못 서명되는 일을 막는다
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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

// 릴리즈 빌드를 직접 요청했는데 서명 정보가 없으면 조용히 통과시키지 않고 중단한다. (docs/RELEASE.md 참고)
// 'build' 같은 일반 작업은 영향받지 않도록 명시적으로 요청된 릴리즈 작업만 검사한다.
val releaseTaskPattern = Regex("(^|:)(assemble|bundle|package|install)[A-Za-z]*Release$")
if (gradle.startParameter.taskNames.any { releaseTaskPattern.containsMatchIn(it) }) {
    if (!hasReleaseSigning) {
        throw GradleException("릴리즈 서명 키(GITHUBNOTI_KEYSTORE 등)가 ~/.gradle/gradle.properties 에 없습니다. 릴리즈는 키 보관 PC에서만 빌드하세요. docs/RELEASE.md 참고")
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
