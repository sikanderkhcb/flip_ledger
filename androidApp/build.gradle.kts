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

android {
    namespace = "com.blackink.app.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.blackink.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
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
    implementation(libs.koin.android)

    // Google Play In-App Review — native rating prompt behind the shared ReviewPrompter facade.
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)

    // Firebase — BOM aligns the module versions; Analytics + Crashlytics power the Telemetry facade.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
