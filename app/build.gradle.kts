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
    id("jacoco")
}

android {
    namespace = "com.drivingschoolrwandaapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.drivingschoolrwandaapp"
        minSdk = 27
        targetSdk = 37
        versionCode = 100
        versionName = "1.5.3"
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
        disable += setOf(
            "ObsoleteSdkInt",
            "KaptUsageInsteadOfKsp",  // Glide, Hilt, MapStruct don't support KSP yet
            "NewerVersionAvailable",  // Retrofit 3.x has breaking API changes; others manually verified
            // The app intentionally launches via SplashActivity (redirects by login state).
            // This check's recommended fix — the AndroidX core-splashscreen library — directly
            // conflicts with the Play Console "deprecated APIs for edge-to-edge" warning, because
            // core-splashscreen 1.2.0 internally calls Window.setStatusBarColor / setNavigationBarColor
            // and sets LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES. We use the native platform splash
            // (API 31+) and a branded starting window on API 27-30 instead.
            "CustomSplashScreen"
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            // libdatastore_shared_counter.so is a stripped DataStore helper that
            // Firebase pulls in transitively. The app has no first-party native
            // code and never uses DataStore multi-process mode, so this .so is
            // dead weight that triggers the Play Console "native code without
            // debug symbols" warning. Excluding it removes the warning entirely.
            excludes += "**/libdatastore_shared_counter.so"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
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
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)

    // ── Play Integrity (anti-fraud; AdMob policy requirement) ──
    implementation(libs.play.integrity)

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

// ── Code coverage (JaCoCo) ──
// AGP 9 removed testCoverageEnabled, so JaCoCo is wired manually: the Gradle jacoco
// plugin instruments the unit-test JVM and this task aggregates the .exec data into
// an HTML/XML report. Run with: ./gradlew :app:jacocoUnitTestReport
jacoco {
    toolVersion = "0.8.12"
}

// Instrument the debug unit tests so coverage data is collected.
tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension>("jacoco") {
        isEnabled = true
    }
}

tasks.register<JacocoReport>("jacocoUnitTestReport") {
    group = "verification"
    description = "Generates a JaCoCo coverage report from the debug unit tests."
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    // Exclude generated / framework classes from the report.
    val excludePatterns = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/Hilt_*.class", "**/Dagger*Component*.class", "**/*_Factory.class",
        "**/*Module*.class", "**/*_HiltModules*.class", "**/*_Impl.class",
        "**/*_MembersInjector.class", "**/*_GeneratedInjector.class",
        "**/BR.class"
    )

    // AGP 9 merges Kotlin + Java class output here.
    val mergedClassesDir = layout.buildDirectory
        .dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes").get().asFile
    classDirectories.setFrom(
        fileTree(mergedClassesDir) { exclude(excludePatterns) }
    )
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(fileTree(layout.buildDirectory) { include("jacoco/*.exec") })
}


/**
 * Regression guard for reflection-based release crashes under R8 full mode.
 *
 * 1. MapStruct: Mappers.getMapper() reflectively instantiates generated *Impl classes via their
 *    public no-arg constructor. A bare `-keep class ...mappers.**` rule keeps only class names,
 *    so members are stripped and the app crashes at <clinit> (NoSuchMethodException).
 * 2. Retrofit: ApiService is turned into a dynamic proxy at runtime — the interface and its
 *    methods must survive.
 * 3. Gson: model fields are read via reflection; if R8 strips or renames them, deserialization
 *    silently returns nulls (no crash, but corrupted data).
 *
 * This task actually runs R8 (minifyReleaseWithR8) and fails the build if any of the guarded
 * classes, members, or names do not survive in seeds.txt / mapping.txt.
 */
tasks.register("verifyReleaseMappersKept") {
    group = "verification"
    description = "Runs R8 on the release build and fails if MapStruct mapper impls, the Retrofit ApiService interface, or Gson model fields were stripped or renamed."
    dependsOn("minifyReleaseWithR8")

    // Add any new MapStruct mappers here — their generated *Impl classes must survive R8.
    val implClasses = listOf(
        "com.drivingschoolrwandaapp.models.mappers.TestMapperImpl",
        "com.drivingschoolrwandaapp.models.mappers.SubscriptionMapperImpl"
    )
    // Retrofit reflects on these interfaces to build the request proxies.
    val apiServiceInterfaces = listOf(
        "com.drivingschoolrwandaapp.api.ApiService",
        "com.drivingschoolrwandaapp.api.AdminApiService"
    )
    // Gson serializes/deserializes these LIVE request/response DTOs via field reflection.
    // Add new request/response models here. NOTE: only add classes that are actually used at
    // runtime — R8 legitimately removes dead classes, and listing one here would fail the build.
    val gsonModelClasses = listOf(
        "com.drivingschoolrwandaapp.models.response.ApiResponse",
        "com.drivingschoolrwandaapp.models.entities.AdminDashboardResponse",
        "com.drivingschoolrwandaapp.models.entities.AdminDashboardStats",
        "com.drivingschoolrwandaapp.models.entities.AdminUsersResponse",
        "com.drivingschoolrwandaapp.models.entities.AdminUser",
        "com.drivingschoolrwandaapp.models.entities.AdminRequest",
        "com.drivingschoolrwandaapp.models.entities.AdminUserDetailResponse",
        "com.drivingschoolrwandaapp.models.entities.AdminUserDetail",
        "com.drivingschoolrwandaapp.models.entities.AdminUserSubscription",
        "com.drivingschoolrwandaapp.models.entities.AdminSubscriptionPlan",
        "com.drivingschoolrwandaapp.models.response.IremboPaymentResponse",
        "com.drivingschoolrwandaapp.models.response.LoginResponse",
        "com.drivingschoolrwandaapp.models.response.RegisterResponse",
        "com.drivingschoolrwandaapp.models.request.BookmarkRequest",
        "com.drivingschoolrwandaapp.models.request.FirebaseTokenUpdateRequest",
        "com.drivingschoolrwandaapp.models.request.IremboLicenseRequest",
        "com.drivingschoolrwandaapp.models.request.LoginRequest",
        "com.drivingschoolrwandaapp.models.request.RefreshTokenRequest",
        "com.drivingschoolrwandaapp.models.request.VerifyOtpRequest"
    )
    val seedsFile = layout.buildDirectory.file("outputs/mapping/release/seeds.txt")
    val mappingFile = layout.buildDirectory.file("outputs/mapping/release/mapping.txt")

    doLast {
        val seeds = seedsFile.get().asFile
        val mapping = mappingFile.get().asFile
        if (!seeds.isFile || !mapping.isFile) {
            throw GradleException(
                "Release reflection guard: R8 mapping output not found " +
                    "(expected ${seeds.absolutePath}, ${mapping.absolutePath}). " +
                    "Did minifyReleaseWithR8 produce outputs/mapping/release?"
            )
        }
        val seedLines = seeds.readLines()
        val mappingLines = mapping.readLines()
        val problems = mutableListOf<String>()

        fun isClassKept(className: String): Boolean =
            seedLines.any { it == className || it.startsWith("$className:") }

        fun isIdentityMapped(className: String): Boolean =
            mappingLines.indexOfFirst { it.trim() == "$className -> $className:" } >= 0

        // ── 1. MapStruct mapper impls ──
        for (impl in implClasses) {
            // 1a. The class must be an R8 seed (class kept at all).
            if (!isClassKept(impl)) {
                problems += "$impl is missing from seeds.txt — R8 removed the class"
                continue
            }

            // 1b. Members must survive (the historical crash: class kept, members stripped).
            val memberSeeds = seedLines.count { it.startsWith("$impl:") }
            if (memberSeeds == 0) {
                problems += "$impl has no kept members in seeds.txt — members were stripped"
            }

            // 1c. The class name must not be obfuscated (identity mapping in mapping.txt).
            val identity = "$impl -> $impl:"
            val identityIndex = mappingLines.indexOfFirst { it.trim() == identity }
            if (identityIndex < 0) {
                problems += "$impl was renamed in mapping.txt — reflective lookup will fail"
                continue
            }

            // 1d. The reflectively-invoked no-arg constructor must survive
            //     (Mappers.getMapper() throws NoSuchMethodException if it is stripped).
            val section = mappingLines.drop(identityIndex + 1)
                .takeWhile { !(it.trim().endsWith(":") && it.contains(" -> ")) }
            if (section.none { it.contains("<init>") }) {
                problems += "$impl has no <init> in its mapping.txt section — the no-arg constructor was stripped"
            }
        }

        // ── 2. Retrofit service interfaces ──
        for (apiServiceInterface in apiServiceInterfaces) {
            if (!isClassKept(apiServiceInterface)) {
                problems += "$apiServiceInterface is missing from seeds.txt — R8 removed the interface Retrofit reflects on"
            } else {
                val memberSeeds = seedLines.count { it.startsWith("$apiServiceInterface:") }
                if (memberSeeds == 0) {
                    problems += "$apiServiceInterface has no kept members in seeds.txt — endpoint methods were stripped"
                }
                if (!isIdentityMapped(apiServiceInterface)) {
                    problems += "$apiServiceInterface was renamed in mapping.txt — Retrofit proxy generation will fail"
                }
            }
        }

        // ── 3. Gson model field retention ──
        // Gson reads @SerializedName fields reflectively. R8 renames (or strips) any field NOT
        // matched by the -keepclassmembers ... @SerializedName <fields> rule, silently changing
        // the JSON keys the server sees. This class of bug is caught by requiring the field seed
        // entries to exist: fields covered by the rule appear as seeds, unannotated ones do not.
        for (model in gsonModelClasses) {
            if (!isClassKept(model)) {
                problems += "$model is missing from seeds.txt — R8 removed the class Gson reflects on"
                continue
            }
            // Field seeds are member lines without parens (methods/constructors always carry "(").
            val fieldSeeds = seedLines.count { it.startsWith("$model:") && !it.contains("(") }
            if (fieldSeeds == 0) {
                problems += "$model has no kept fields in seeds.txt — its @SerializedName fields were stripped/renamed and Gson will send wrong JSON keys"
            }
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "Release reflection guard FAILED:\n  " +
                    problems.joinToString("\n  ") +
                    "\nKeep the guarded classes in proguard-rules.pro, e.g. " +
                    "-keep class com.drivingschoolrwandaapp.models.mappers.** { *; } and " +
                    "-keepclassmembers class com.drivingschoolrwandaapp.models.** { @com.google.gson.annotations.SerializedName <fields>; }"
            )
        }
        logger.lifecycle(
            "Release reflection guard passed: mappers=${implClasses.joinToString()}, " +
                "api=${apiServiceInterfaces.joinToString()}, models=${gsonModelClasses.size} classes"
        )
    }
}

/**
 * Regression guard: formatted strings must never be referenced directly from UI XML.
 *
 * Strings with format specifiers (%1$d, %2$s, %d, %s, %.2f, ...) need arguments supplied at
 * runtime via getString(R.string.x, args...). If such a string is wired into a layout with
 * android:text="@string/x", Android renders the raw placeholders (e.g. "Amanota %1$d / %2$d")
 * because XML has no way to pass format arguments. This was the "AMANOTA %1$d / %2$d" bug.
 *
 * The task collects the names of every formatted string across all values locales' strings.xml,
 * then scans every UI XML resource (layout, menu, and navigation folders) for @string references
 * to them and fails the build if any are found. tools:-namespaced attributes are design-time
 * previews and are skipped; XML comments are stripped to avoid false positives. Runs automatically
 * on every build via preBuild (and via check).
 * Run directly with: ./gradlew :app:verifyNoFormattedStringsInLayouts
 */
tasks.register("verifyNoFormattedStringsInLayouts") {
    group = "verification"
    description = "Fails the build if a formatted string resource (%1\$d, %2\$s, ...) is referenced directly from a layout/menu/navigation XML."
    doLast {
        val resDir = project.layout.projectDirectory.dir("src/main/res").asFile

        // ── 1. Collect names of strings containing format specifiers ──
        // Matches printf-style specifiers Android supports: %d %s %f %02d %.2f %1$d %1$02d ...
        // ("%%" is an escaped percent and is deliberately NOT matched.)
        // The (?<![0-9]) lookbehind avoids false positives on prose like "100% done" — real
        // format specifiers are never preceded by a digit.
        // NOTE: only <string> elements are scanned; format specifiers inside <string-array> or
        // <plurals> items are not currently covered (none exist in this codebase).
        val formatSpecifier = Regex("(?<![0-9])%([0-9]+\\\$)?[-+0,#( ]*[0-9]*([.][0-9]+)?[dsf]")
        val stringElement = Regex("<string\\s+name=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</string>")

        val formatStringNames = mutableSetOf<String>()
        fileTree(resDir) { include("values*/strings.xml") }.files.forEach { file ->
            for (match in stringElement.findAll(file.readText())) {
                val name = match.groupValues[1]
                val content = match.groupValues[2]
                if (formatSpecifier.containsMatchIn(content)) {
                    formatStringNames += name
                }
            }
        }

        // ── 2. Scan UI XML for runtime @string references to those names ──
        val attrRef = Regex("([\\w.]+:)?(\\w+)=\"@string/([A-Za-z0-9_]+)\"")
        val problems = mutableListOf<String>()
        fileTree(resDir) { include("layout*/**/*.xml", "menu/**/*.xml", "navigation/**/*.xml") }.files
            .sorted()
            .forEach { file ->
                // Strip comments first so commented-out references don't trip the guard.
                val content = file.readText().replace(Regex("<!--[\\s\\S]*?-->"), "")
                for (match in attrRef.findAll(content)) {
                    val namespace = match.groupValues[1]
                    if (namespace == "tools:") continue // design-time only, never rendered
                    val stringName = match.groupValues[3]
                    if (stringName in formatStringNames) {
                        problems += "${file.relativeTo(project.projectDir)}: " +
                            "${match.groupValues[2]}=\"@string/$stringName\" renders raw format placeholders at runtime"
                    }
                }
            }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "Formatted string layout guard FAILED:\n  " +
                    problems.joinToString("\n  ") +
                    "\n\nFormatted strings require arguments and display raw placeholders " +
                    "(e.g. \"Amanota %1\$d / %2\$d\") when referenced from XML. " +
                    "Set the text from code instead:\n" +
                    "    textView.setText(getString(R.string.<name>, arg1, arg2));\n" +
                    "or use a plain (non-formatted) string for static labels."
            )
        }
        logger.lifecycle(
            "Formatted string layout guard passed: ${formatStringNames.size} formatted strings, none referenced from UI XML."
        )
    }
}

// Run the guard automatically on every build and on `check` (CI), so regressions fail the build.
tasks.named("preBuild") {
    dependsOn("verifyNoFormattedStringsInLayouts")
}
tasks.named("check") {
    dependsOn("verifyNoFormattedStringsInLayouts")
}
