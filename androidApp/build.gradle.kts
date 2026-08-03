import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Firebase (Analytics + Crashlytics) is enabled only when google-services.json is present, so the
// app still builds for anyone who clones without their own Firebase config. When absent, the
// Telemetry facade falls back to a no-op at runtime.
val firebaseEnabled = file("google-services.json").exists()
if (firebaseEnabled) {
    apply(plugin = libs.plugins.googleServices.get().pluginId)
    apply(plugin = libs.plugins.firebaseCrashlytics.get().pluginId)
}

// Release signing reads from a gitignored keystore.properties in the project root. When it's
// absent (CI, fresh clone), the release build is produced UNSIGNED — enough to verify the build
// compiles/minifies, but Play won't accept it and it can't be installed until it's signed.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) load(keystorePropsFile.inputStream())
}
val hasKeystore = keystorePropsFile.exists()

android {
    namespace = "com.blackink.app.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.blackink.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        create("release") {
            if (hasKeystore) {
                storeFile = file(keystoreProps.getProperty("storeFile")!!)
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Only attach the signing config when a keystore is configured; otherwise unsigned.
            signingConfig = if (hasKeystore) signingConfigs.getByName("release") else null
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    // AndroidX SplashScreen: styles the Android 12+ system splash and holds it until the app
    // resolves its start destination, so there is a single splash (no second in-app one).
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.koin.android)

    // Google Play In-App Review — native rating prompt behind the shared ReviewPrompter facade.
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)

    // Firebase — BOM aligns the module versions; Analytics + Crashlytics power the Telemetry facade.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
