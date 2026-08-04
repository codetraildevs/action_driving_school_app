import java.util.Properties
import org.gradle.api.GradleException

@file:Suppress("DEPRECATION")

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
    compileSdk = 37

    defaultConfig {
        applicationId = "com.drivingschoolrwandaapp"
        minSdk = 27
        targetSdk = 37
        versionCode = 86
        versionName = "1.3.0"
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
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    lint {
        baseline = file("lint-baseline.xml")
        disable += setOf(
            "ObsoleteSdkInt",
            "KaptUsageInsteadOfKsp",  // Glide, Hilt, MapStruct don't support KSP yet
            "NewerVersionAvailable"   // Retrofit 3.x has breaking API changes; others manually verified
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    sourceSets {
        getByName("main") {
            java {
                setSrcDirs(listOf("src/main/java", "src/main/java/api"))
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

    // ── Phone Number Handling ──
    implementation(libs.libphonenumber)

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

/**
 * Regression guard for the MapStruct release crash
 * (com.drivingschoolrwandaapp.models.mappers.TestMapper.<clinit> NoSuchMethodException).
 *
 * Mappers.getMapper() reflectively instantiates the generated *Impl classes via their public
 * no-arg constructor. In R8 full mode, a bare `-keep class ...mappers.**` rule keeps only the
 * class name, so members are stripped and the app crashes at class-init. This task actually runs
 * R8 (minifyReleaseWithR8) and fails the build if the impl classes, their members, or their
 * class names do not survive.
 */
tasks.register("verifyReleaseMappersKept") {
    group = "verification"
    description = "Runs R8 on the release build and fails if MapStruct mapper impls (TestMapperImpl, SubscriptionMapperImpl) were stripped or renamed."
    dependsOn("minifyReleaseWithR8")

    // Add any new MapStruct mappers here — their generated *Impl classes must survive R8.
    val implClasses = listOf(
        "com.drivingschoolrwandaapp.models.mappers.TestMapperImpl",
        "com.drivingschoolrwandaapp.models.mappers.SubscriptionMapperImpl"
    )
    val seedsFile = layout.buildDirectory.file("outputs/mapping/release/seeds.txt")
    val mappingFile = layout.buildDirectory.file("outputs/mapping/release/mapping.txt")

    doLast {
        val seeds = seedsFile.get().asFile
        val mapping = mappingFile.get().asFile
        if (!seeds.isFile || !mapping.isFile) {
            throw GradleException(
                "MapStruct mapper retention check: R8 mapping output not found " +
                    "(expected ${seeds.absolutePath}, ${mapping.absolutePath}). " +
                    "Did minifyReleaseWithR8 produce outputs/mapping/release?"
            )
        }
        val seedLines = seeds.readLines()
        val mappingLines = mapping.readLines()
        val problems = mutableListOf<String>()

        for (impl in implClasses) {
            // 1. The class must be an R8 seed (class kept at all).
            val classKept = seedLines.any { it == impl || it.startsWith("$impl:") }
            if (!classKept) {
                problems += "$impl is missing from seeds.txt — R8 removed the class"
                continue
            }

            // 2. Members must survive (the historical crash: class kept, members stripped).
            val memberSeeds = seedLines.count { it.startsWith("$impl:") }
            if (memberSeeds == 0) {
                problems += "$impl has no kept members in seeds.txt — members were stripped"
            }

            // 3. The class name must not be obfuscated (identity mapping in mapping.txt).
            val identity = "$impl -> $impl:"
            val identityIndex = mappingLines.indexOfFirst { it.trim() == identity }
            if (identityIndex < 0) {
                problems += "$impl was renamed in mapping.txt — reflective lookup will fail"
                continue
            }

            // 4. The reflectively-invoked no-arg constructor must survive
            //    (Mappers.getMapper() throws NoSuchMethodException if it is stripped).
            val section = mappingLines.drop(identityIndex + 1)
                .takeWhile { !(it.trim().endsWith(":") && it.contains(" -> ")) }
            if (section.none { it.contains("<init>") }) {
                problems += "$impl has no <init> in its mapping.txt section — the no-arg constructor was stripped"
            }
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "MapStruct mapper retention check FAILED:\n  " +
                    problems.joinToString("\n  ") +
                    "\nKeep all mapper members in proguard-rules.pro, e.g. " +
                    "-keep class com.drivingschoolrwandaapp.models.mappers.** { *; }"
            )
        }
        logger.lifecycle("MapStruct mapper retention check passed: ${implClasses.joinToString()}")
    }
}
