You are working on my Android application locally.

The current Google Play Console Android Vitals report for release 93 (version 1.4.0) shows serious stability and Android 15 compatibility problems.

IMPORTANT:
Do NOT make random changes or hide/suppress errors.
First inspect the existing project, architecture, dependencies, activities, fragments, utilities, image-loading code, AndroidManifest, Gradle configuration, and ProGuard/R8 configuration.
Preserve all existing app functionality, UI, navigation, authentication, subscriptions, exams, offline exam data, languages, Firebase/AdMob, and payment functionality.

GOAL:
Fix the issues locally so the next release has a significantly lower user-perceived crash rate and is fully compatible with Android 15 / SDK 35.

CURRENT GOOGLE PLAY CONSOLE DATA:

Release: 93 (1.4.0)

User-perceived crash rate:
5.26%

Google Play bad-behavior threshold:
1.09%

User-perceived ANR rate:
0.09%

Slow cold start:
8.18%

Slow warm start:
13.68%

Slow hot start:
5.19%

Excessive slow frames:
0.76%

Excessive frozen frames:
8.52%

Google Play specifically reports:

1. EDGE-TO-EDGE PROBLEM

"Edge-to-edge may not display for all users."

From Android 15, apps targeting SDK 35 display edge-to-edge by default.

Google recommends:

* Kotlin: enableEdgeToEdge()
* Java: EdgeToEdge.enable()

The project contains both Java and Kotlin, so inspect the actual activities and use the correct implementation for each language.

Do not simply add enableEdgeToEdge() everywhere.

Inspect the application's current window/insets implementation and migrate it correctly.

Make sure:

* Status bar content is displayed correctly.
* Navigation bar content is displayed correctly.
* Toolbar/app bar does not overlap system bars.
* RecyclerViews, ScrollViews, NestedScrollViews and other content receive correct WindowInsets.
* Bottom buttons/forms are not hidden behind the navigation bar.
* Keyboard/IME insets are handled correctly.
* Display cutouts/notches are handled correctly.
* Android 15 behavior is correct.
* Older Android versions continue working correctly.

2. DEPRECATED EDGE-TO-EDGE APIs

Google Play reports these deprecated APIs:

android.view.Window.setStatusBarColor
android.view.Window.setNavigationBarColor
LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

They are reported from:

com.drivingschoolrwandaapp.ui.activities.LoginActivity.<init>

androidx.profileinstaller.ProfileInstallReceiver.onReceive

com.drivingschoolrwandaapp.ui.fragments.TestsFragment$SubscriptionPlanAdapter.lambda$onBindViewHolder$0

n0.s

Investigate each occurrence.

Search the entire project for:

setStatusBarColor
setNavigationBarColor
LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
statusBarColor
navigationBarColor
window flags
WindowManager.LayoutParams
systemUiVisibility
decorFitsSystemWindows
edge-to-edge
enableEdgeToEdge
EdgeToEdge.enable

Replace application-owned deprecated implementations with the modern AndroidX/WindowInsets approach.

IMPORTANT:
Do not modify third-party/library bytecode or generated classes just to make the warning disappear.

If a warning such as androidx.profileinstaller.ProfileInstallReceiver comes from a dependency:

* identify the dependency and version;
* check whether a newer compatible version is available;
* update it only if safe;
* avoid breaking the project.

For n0.s, determine which dependency/class has been obfuscated to that name and identify the original source before changing anything.

3. INVESTIGATE THE 5.26% CRASH RATE

This is the highest priority.

Do not assume the crash is caused by edge-to-edge.

Inspect all available local crash information, Logcat configurations, Firebase Crashlytics configuration, and code paths that could cause runtime crashes.

Search for common crash sources such as:

* NullPointerException
* IllegalStateException
* IndexOutOfBoundsException
* ClassCastException
* ActivityNotFoundException
* SecurityException
* permission-related crashes
* network failures
* JSON parsing failures
* serialization errors
* bitmap/image decoding errors
* OutOfMemoryError
* Fragment lifecycle errors
* RecyclerView/ViewHolder crashes
* Context leaks
* Activity/Fragment detached errors
* incorrect navigation
* background-thread UI access
* Android 13/14/15 permission differences
* notification permission issues
* file URI issues
* Intent failures
* WebView issues
* IremboActivity crashes
* LoginActivity crashes
* TestsFragment crashes
* SubscriptionPlanAdapter crashes
* PhoneUtils initialization crashes

Pay particular attention to the classes mentioned by Google Play:

PhoneUtils.<clinit>
IremboActivity.onRequestPermissionsResult
Hilt_IremboActivity.<init>
LoginActivity
TestsFragment.SubscriptionPlanAdapter

Do not just add broad try/catch blocks.

Fix the actual root cause.

If Crashlytics is already configured, inspect the implementation and make sure crashes are properly recorded.

4. BITMAP / IMAGE MEMORY PROBLEM

Google Play reports:

"Improve your app's performance with bitmap image optimization."

It reports manually downloaded/decoded images in:

com.drivingschoolrwandaapp.utils.PhoneUtils.<clinit>

com.drivingschoolrwandaapp.ui.activities.IremboActivity.onRequestPermissionsResult

com.drivingschoolrwandaapp.ui.activities.Hilt_IremboActivity.<init>

and references kotlinx.serialization.json.JsonNullSerializer.<clinit>.

Investigate the actual source of these image operations.

Do NOT assume kotlinx.serialization itself is downloading images. Determine whether this is a stack-trace attribution caused by the application's call path or a dependency.

For application-owned network images:

* use an appropriate Android image-loading library such as Coil or Glide if the project does not already have one;
* use lifecycle-aware loading;
* enable caching;
* use appropriate image dimensions/downsampling;
* avoid loading full-resolution images when displaying thumbnails;
* avoid keeping large Bitmap objects in static fields;
* avoid storing Bitmaps in singleton objects;
* release references when appropriate;
* avoid decoding images on the main thread;
* avoid loading unnecessary images.

For local exam/question images:

* inspect how the 158 bundled images are loaded;
* make sure WebP/JPG files are not unnecessarily decoded at full resolution;
* use ImageView sizing and appropriate resource handling;
* avoid loading all exam images into memory at once.

IMPORTANT:
Do not replace working local assets with network downloads.
The app must continue supporting offline exam content.

5. PHONEUTILS

Inspect:

com.drivingschoolrwandaapp.utils.PhoneUtils.<clinit>

The <clinit> indicates static/class initialization.

Look for:

* static Bitmap creation
* static image decoding
* static network requests
* static initialization that can throw exceptions
* static Context usage
* static resource loading
* expensive work during class initialization

Move expensive or failure-prone work out of static initialization.

Make PhoneUtils safe to initialize on every supported Android version.

6. IREMBOACTIVITY

Inspect:

IremboActivity.onRequestPermissionsResult

and:

Hilt_IremboActivity.<init>

Investigate:

* permission handling
* image loading
* URI handling
* Activity lifecycle
* file access
* camera/gallery/document permissions
* Android 13+ permission behavior
* Android 14/15 behavior
* nullable Activity/Context references
* results returned after Activity destruction

If image downloading or decoding happens there, refactor it to a proper lifecycle-aware image loading mechanism.

Do not request unnecessary permissions.

7. LOGINACTIVITY

Inspect LoginActivity carefully because Google Play identifies:

com.drivingschoolrwandaapp.ui.activities.LoginActivity.<init>

Check:

* edge-to-edge implementation
* status/navigation bar code
* initialization code
* static initialization
* view binding
* intent extras
* Firebase initialization
* phone validation
* network initialization
* lifecycle issues
* null references
* configuration changes

Do not break the existing login/register behavior.

8. TESTSFRAGMENT / SUBSCRIPTIONPLANADAPTER

Inspect:

com.drivingschoolrwandaapp.ui.fragments.TestsFragment$SubscriptionPlanAdapter.lambda$onBindViewHolder$0

Check:

* click listeners
* subscription state
* payment flow
* Firebase data
* null values
* RecyclerView lifecycle
* Context references
* Activity casts
* Intent handling
* network failures
* JSON parsing
* UI updates after Fragment destruction

The subscription/payment functionality must remain intact.

9. ANDROID 15 / SDK CONFIGURATION

Inspect:

compileSdk
targetSdk
minSdk

Gradle plugins
AndroidX versions
Kotlin version
Java version
dependencies

If the project is targeting SDK 35, make the required Android 15 changes correctly.

Do not downgrade targetSdk just to remove the warning.

Keep targetSdk at the current required level unless there is a genuine build compatibility reason.

10. DEPENDENCY AUDIT

Inspect all dependencies for:

* outdated AndroidX libraries
* outdated Material libraries
* outdated Activity libraries
* outdated Fragment libraries
* outdated ProfileInstaller
* outdated Hilt
* outdated Firebase
* outdated image-loading libraries

Do not blindly upgrade everything.

For every dependency update:

* verify compatibility;
* avoid unnecessary major-version upgrades;
* make sure the app still builds;
* make sure Firebase/AdMob/Hilt functionality remains intact.

11. PERFORMANCE

Also investigate the reported:

Slow cold start: 8.18%
Slow warm start: 13.68%
Slow hot start: 5.19%
Excessive frozen frames: 8.52%

Look for:

* heavy work in Application.onCreate()
* database initialization on main thread
* JSON parsing on main thread
* loading all exam data synchronously
* decoding large images on main thread
* network requests on main thread
* unnecessary Firebase initialization
* unnecessary AdMob initialization
* expensive static initialization
* large RecyclerView binding operations

Move expensive operations to background threads where appropriate.

Do NOT make startup so asynchronous that the user sees broken/empty UI.

12. DO NOT BREAK THESE EXISTING FEATURES

The following must continue working:

* English / French / Kinyarwanda
* Login
* Registration
* Phone number validation
* International and Rwandan phone numbers
* Exams
* Previous tests
* Exam questions
* Question images
* Offline exam data
* Subscription plans
* Payment instructions
* Subscription activation
* Firebase
* AdMob
* Irembo functionality
* Admin/API functionality where applicable
* Navigation
* Existing UI functionality

13. IMPLEMENTATION REQUIREMENTS

Before modifying code:

A. Inspect the project structure.
B. Identify Java vs Kotlin activities.
C. Identify the current edge-to-edge implementation.
D. Identify all deprecated window APIs.
E. Identify all image download/decode locations.
F. Identify likely crash paths.
G. Inspect Gradle/dependency versions.

Then implement the fixes.

Use:

* WindowCompat / WindowInsetsCompat where appropriate.
* AndroidX Activity edge-to-edge APIs where appropriate.
* lifecycle-aware components.
* Glide/Coil only where appropriate.
* background execution for expensive work.
* proper null/error handling.

Do not use broad exception swallowing.

14. VALIDATION

After making changes:

Run a clean build.

Run:

* ./gradlew clean
* ./gradlew assembleDebug
* ./gradlew assembleRelease

If the project uses Windows PowerShell, use the appropriate Gradle wrapper commands.

Check for:

* compilation errors
* lint errors
* resource errors
* dependency conflicts
* R8/ProGuard errors

Then test on:

* Android 12/13
* Android 14
* Android 15

Especially test:

* cold start
* login
* registration
* tests list
* opening an exam
* loading question images
* previous tests
* subscription screen
* payment flow
* IremboActivity
* permission requests
* screen rotation/configuration changes if supported
* keyboard opening
* scrolling
* bottom navigation
* status/navigation bar
* devices with display cutouts

15. IMPORTANT CRASH INVESTIGATION OUTPUT

Before finishing, provide a concise report containing:

1. Root cause(s) of the crash or most likely crash paths.
2. Exact files changed.
3. Exact deprecated APIs removed/replaced.
4. How Android 15 edge-to-edge was implemented.
5. How bitmap/image loading was optimized.
6. Dependencies upgraded, if any, and why.
7. Performance improvements made.
8. Tests performed.
9. Any remaining warnings that are caused by third-party libraries rather than our code.
10. Any issue that still requires real-device/Play Console testing.

Do not claim that the Google Play crash rate is fixed unless the actual crash has been reproduced or the root cause has strong evidence.

Most important priority order:

PRIORITY 1 — Find and fix the actual crash causing the 5.26% user-perceived crash rate.

PRIORITY 2 — Fix Android 15 edge-to-edge implementation and remove deprecated application-owned window APIs.

PRIORITY 3 — Fix bitmap/image downloading and decoding to reduce memory pressure and possible crashes.

PRIORITY 4 — Improve startup and rendering performance.

PRIORITY 5 — Update only necessary dependencies.

Make minimal, safe, production-quality changes and preserve all existing functionality.
