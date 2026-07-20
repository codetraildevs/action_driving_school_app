import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.plugin)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.drivingschoolrwandaapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.drivingschoolrwandaapp"
        minSdk = 27
        targetSdk = 35
        versionCode = 80
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        localeFilters += listOf("en", "fr", "rw")
    }



    bundle {
        language {
            

            enableSplit = false

        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val properties = Properties()
                properties.load(keystorePropertiesFile.inputStream())
                storeFile = file(properties.getProperty("storeFile", "upload-keystore.jks"))
                storePassword = properties.getProperty("storePassword", "Password123.")
                keyAlias = properties.getProperty("keyAlias", "upload")
                keyPassword = properties.getProperty("keyPassword", "Password123.")
            } else {
                // Fallback: use env vars (GitHub Actions) or hardcoded defaults (local dev)
                storeFile = file(System.getenv("KEYSTORE_FILE") ?: "upload-keystore.jks")
                storePassword = System.getenv("KEYSTORE_STORE_PASSWORD") ?: "Password123."
                keyAlias = System.getenv("KEYSTORE_KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("KEYSTORE_KEY_PASSWORD") ?: "Password123."
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        disable += setOf("ObsoleteSdkInt")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    sourceSets {
        getByName("main") {
            java {
                srcDirs("src\\main\\java", "src\\main\\java\\api")
            }
        }
    }
}

dependencies {

    // ── AndroidX Core ──
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.com.google.android.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core.splashscreen)

    // ── Lifecycle ──
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)
    kapt(libs.androidx.lifecycle.compiler)

    // ── Navigation ──
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // ── Room Database ──
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)

    // ── WorkManager ──
    implementation(libs.androidx.work.runtime)

    // ── Security ──
    implementation(libs.androidx.security.crypto)

    // ── Networking ──
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.gson)
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.okhttp.logging)
    implementation(libs.google.gson)

    // ── Image Loading (Glide) ──
    implementation(libs.glide)
    implementation(libs.glide.okhttp)
    kapt(libs.glide.compiler)

    // ── DI (Dagger Hilt) ──
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.work)
    kapt(libs.hilt.work.compiler)

    // ── MapStruct ──
    implementation(libs.mapstruct)
    kapt(libs.mapstruct.processor)

    // ── Photo Viewer ──
    implementation(libs.photoview)

    // ── In-App Updates ──
    implementation(libs.app.update)
    implementation(libs.app.update.ktx)

    // ── Shimmer ──
    implementation(libs.shimmer)

    // ── SVG ──
    implementation(libs.androidsvg)

    // ── Baseline Profile ──
    implementation(libs.profileinstaller)
    baselineProfile(project(":baseline-profile"))

    // ── Firebase ──
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)

    // ── Testing ──
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.core.testing)
    androidTestImplementation(libs.junit.androidx)
    androidTestImplementation(libs.espresso.core)
}


kapt {
    correctErrorTypes = true
}
