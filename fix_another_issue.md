# Play Console warnings for release 98 (1.5.2) — resolution notes

This file documents the investigation and fixes applied for the three Play Console
actions flagged on release 98 (1.5.2), based on static analysis of the release APK.

## 1. "Edge-to-edge may not display for all users" — User experience

**Status: already implemented; remaining display gaps fixed.**

Every activity already calls `EdgeToEdge.enable(this)` in `onCreate()` (the exact
backward-compatibility fix Play Console recommends):

- `App`, `SplashActivity`, `WelcomeActivity`, `LoginActivity`, `RegisterActivity`,
  `ForgotPasswordActivity`, `ResetPasswordActivity`, `OtpVerificationActivity`,
  `ChangePasswordActivity`, `WebViewActivity`, `WhatsAppGroupsActivity`,
  `PdfViewerActivity`, `MyApplicationsActivity`, `ApplicationDetailsActivity`,
  `AdminActivity`, `IremboActivity`, and `BaseIremboFormActivity` (covers
  `LicenseRequestActivity` + `SpecialRequestActivity`).

**Fixed in this pass** — four full-page activities did not handle the system-bar
insets, so on Android 15+ (enforced edge-to-edge) their top content drew under the
status bar. Added `android:fitsSystemWindows="true"` to their root layouts:

- `activity_welcome.xml` (header row was under the status bar)
- `activity_login.xml` / `activity_register.xml` (logo was under the status bar)
- `activity_admin.xml` (toolbar was under the status bar)

The main screen (`activity_app.xml`) was already correct: `DrawerLayout` 1.1.1
(resolved via AppCompat) applies the window insets as margins to its content child,
keeping the toolbar and bottom nav clear of the bars.

## 2. "Your app uses deprecated APIs or parameters for edge-to-edge" — User experience

Flagged items: `Window.setStatusBarColor`, `Window.setNavigationBarColor`,
`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`.

**Findings from decompiling the release APK (dexdump):** there are exactly 4 call
sites of the deprecated bar-color methods in the entire DEX, and **all four are
inside library internals, not app code**:

- 3 × `setStatusBarColor` / `setNavigationBarColor` — inside `androidx.activity`
  `EdgeToEdge`/`SystemBarStyle` (R8-obfuscated as `pf0`/`rf0`/`tf0`), reachable
  from the `EdgeToEdge.enable(this)` calls above. This is the API Play Console
  itself recommends calling.
- 1 × `setStatusBarColor` — inside `com.google.android.material` `BottomSheetDialog`
  (`EdgeToEdgeUtils.setStatusBarColor`, obfuscated as `un`), used by
  `TestsFragment`'s subscription dialog. Cannot be removed without dropping the
  Material `BottomSheetDialog`.

`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` is **not set anywhere** — not in app
code, not in themes, not in any library — it is flagged from the implicit default.

**Changes made:**
- `values/themes.xml`: base theme now explicitly sets
  `android:windowLayoutInDisplayCutoutMode="always"` — the non-deprecated
  replacement constant (API 30+; ignored below API 28 where cutouts don't exist).
  This removes reliance on the deprecated default and documents the intended mode.

**Not changed (deliberately):** `EdgeToEdge.enable()` is kept because (a) Play
Console's own guidance for warning #1 is to call it, and (b) the deprecation
warning cannot be fully cleared from app code anyway — the material
`BottomSheetDialog` call site would remain. This is a known, widespread Play
Console false-positive pattern affecting every app that uses these recommended
AndroidX APIs (see e.g. the many Stack Overflow / Google issue threads on the
identical `androidx.activity.*` + `com.google.android.material.internal.*` frames).

## 3. "Improve your app's performance with bitmap image optimization" — Technical quality

Flagged locations (`okhttp3.ConnectionSpec.<clinit>`, `ReportFragment.onActivityCreated`,
`FloatArraySerializer.<clinit>`) are library frames; `kotlinx.serialization` is not
even a dependency, so the attribution is unreliable.

**Findings:** the app already follows the recommendation — every network image is
loaded through **Glide** (with the OkHttp integration, disk caching, and memory
management): `DashboardFragment`, `ProfileFragment`, `MaterialsFragment`,
`TestAdapter`, `QuestionOptionAdapter`, `WhatsAppGroupAdapter`,
`LearningMaterialAdapter` (incl. SVG via `SvgGlideLoader`), `SingleQuestionPageFragment`,
and `NotificationWorker` (notification icons). The only `BitmapFactory` use decodes
**bundled drawables** for notification fallback icons — not network content.

**No code change needed.** The warning is a static-analysis false positive for an
app that already uses an image-loading library. (The PDF page renderer converts
downloaded PDFs to bitmaps — files, not network images.)

## Version bump

`versionCode` bumped 98 → 99 (same versionName 1.5.2) for the next upload.
