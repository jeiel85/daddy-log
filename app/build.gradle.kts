import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

abstract class ExportReleaseToDesktopTask : DefaultTask() {
  @get:Input
  abstract val versionName: Property<String>

  @get:Input
  abstract val versionCode: Property<Int>

  @get:InputFile
  abstract val aabFile: RegularFileProperty

  @get:InputFile
  abstract val releaseNotesFile: RegularFileProperty

  @TaskAction
  fun export() {
    val home = File(System.getProperty("user.home"))
    val desktop = listOf(
      File(home, "OneDrive/바탕 화면"),
      File(home, "OneDrive/Desktop"),
      File(home, "Desktop")
    ).firstOrNull { it.isDirectory }
      ?: throw GradleException("Could not find a Desktop directory.")

    // Human-facing release artifacts live under Desktop/Build (flat layout),
    // never the Desktop root.
    val buildDir = File(desktop, "Build").apply { mkdirs() }

    val aab = aabFile.get().asFile
    val releaseNotes = releaseNotesFile.get().asFile
    if (!aab.isFile) {
      throw GradleException("Release AAB not found at ${aab.absolutePath}.")
    }
    if (!releaseNotes.isFile) {
      throw GradleException("Missing release notes at ${releaseNotes.absolutePath}.")
    }

    val releaseNotesText = releaseNotes.readText().trim()
    if (!releaseNotesText.contains("<ko-KR>") || !releaseNotesText.contains("<en-US>")) {
      throw GradleException("Release notes must contain <ko-KR> and <en-US> blocks.")
    }

    // Play Console hard limit: 500 Unicode chars per locale block (tags excluded).
    // Over-limit text is silently truncated by Play Console, so abort the export
    // instead of letting a bad file reach the Desktop.
    val localePattern = Regex("<(ko-KR|en-US|ja-JP|zh-CN|zh-TW)>([\\s\\S]*?)</\\1>")
    val violations = mutableListOf<String>()
    for (match in localePattern.findAll(releaseNotesText)) {
      val locale = match.groupValues[1]
      val body = match.groupValues[2].trim()
      val status = if (body.length > 500) "OVER" else "OK"
      logger.lifecycle("  %-7s %4d / 500  %s".format(locale, body.length, status))
      if (body.length > 500) {
        violations += "$locale (${body.length} chars, ${body.length - 500} over)"
      }
    }
    if (violations.isNotEmpty()) {
      throw GradleException(
        "Play Console release notes exceed the 500-character limit per locale: " +
          violations.joinToString(", ") + ". Trim before exporting."
      )
    }

    val baseName = "DaddyLog-v${versionName.get()}-vc${versionCode.get()}"
    val exportedAab = File(buildDir, "$baseName.aab")
    val exportedNotes = File(buildDir, "$baseName-release-notes.txt")
    aab.copyTo(exportedAab, overwrite = true)
    releaseNotes.copyTo(exportedNotes, overwrite = true)

    logger.lifecycle("Exported ${exportedAab.absolutePath}")
    logger.lifecycle("Exported ${exportedNotes.absolutePath}")
  }
}

val localSigningProperties = Properties().apply {
  val file = rootProject.file(".keystore/release-signing.properties")
  if (file.isFile) {
    file.inputStream().use(::load)
  }
}

fun signingValue(envName: String, propertyName: String): String? =
  System.getenv(envName) ?: localSigningProperties.getProperty(propertyName)

android {
  namespace = "com.jeiel.daddylog"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.jeiel.daddylog"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = signingValue("KEYSTORE_PATH", "keystorePath")
        ?: "${rootDir}/.keystore/daddy-log-upload.jks"
      storeFile = file(keystorePath)
      storePassword = signingValue("STORE_PASSWORD", "storePassword")
      keyAlias = "upload"
      keyPassword = signingValue("KEY_PASSWORD", "keyPassword")
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
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
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.junit)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}

val exportVersionName = android.defaultConfig.versionName
  ?: throw GradleException("versionName is not set in defaultConfig")
val exportVersionCode = android.defaultConfig.versionCode
  ?: throw GradleException("versionCode is not set in defaultConfig")
val exportReleaseNotes = rootProject.layout.projectDirectory.file(
  "store-graphics/play-console-current/release-notes.txt"
)

tasks.register<ExportReleaseToDesktopTask>("exportReleaseToDesktop") {
  dependsOn("signReleaseBundle")
  versionName.set(exportVersionName)
  versionCode.set(exportVersionCode)
  aabFile.set(layout.buildDirectory.file("outputs/bundle/release/app-release.aab"))
  releaseNotesFile.set(exportReleaseNotes)
}
