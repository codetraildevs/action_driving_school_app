package com.drivingschoolrwandaapp.baselineprofile

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test generates a Baseline Profile for the app.
 *
 * Run with: ./gradlew :app:generateReleaseBaselineProfile
 *
 * The generated profile will be written to:
 *   app/src/main/generated/baselineProfiles/
 *
 * Prerequisites:
 * - A device (physical or emulator) running Android 9 (API 28) or higher
 * - The app must already be installed on the device
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        baselineProfileRule.collect(
            packageName = "com.drivingschoolrwandaapp",
            maxIterations = 3
        ) {
            // 1. Cold start – launch the app from the launcher (SplashActivity)
            pressHome()
            startActivityAndWait(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(
                        "com.drivingschoolrwandaapp",
                        "com.drivingschoolrwandaapp.ui.activities.SplashActivity"
                    )
                }
            )

            // 2. Wait for the main Activity to load and settle
            device.wait(Until.hasObject(By.pkg("com.drivingschoolrwandaapp")), 5_000)

            // 3. Navigate bottom nav tabs: Home → Exams → Materials → Profile
            //    This exercises the core navigation graph and fragment lifecycle

            // Click Exams tab (bottom nav index 1)
            device.waitForIdle()
            findTab(device, "Ibizamini", "Exams", "Examens")?.clickAndWait(Until.newWindow(), 3_000)

            // Click Materials tab (bottom nav index 2)
            device.waitForIdle()
            findTab(device, "Igazeti", "Materials", "Documents")?.clickAndWait(Until.newWindow(), 3_000)

            // Click Profile tab (bottom nav index 3)
            device.waitForIdle()
            findTab(device, "Umwirondoro", "Profile", "Profil")?.clickAndWait(Until.newWindow(), 3_000)

            // Return to Home/Dashboard tab (bottom nav index 0)
            device.waitForIdle()
            findTab(device, "Ahabanza", "Home", "Accueil")?.clickAndWait(Until.newWindow(), 3_000)

            // 4. Wait for the UI to be fully idle
            device.waitForIdle()
        }
    }

    /**
     * Finds the first UI element matching any of the given content descriptions.
     * This avoids using [By.Selector.or] which has Kotlin type inference conflicts
     * with [kotlin.math.BigInteger] in certain library versions.
     */
    private fun findTab(device: UiDevice, vararg descriptions: String) =
        descriptions.firstNotNullOfOrNull { desc ->
            device.findObject(By.descContains(desc))
        }
}
