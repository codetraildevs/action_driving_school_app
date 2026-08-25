Now perform a SECOND full audit of the Android application after the previous crash/Android 15 fixes.

The goal is to find and fix remaining issues, improve performance and stability, and make the USER role work offline wherever technically possible.

IMPORTANT:
Do not rewrite the whole application.
Do not remove existing functionality.
Do not make changes just to silence warnings.
Inspect the existing architecture first and make safe, production-quality improvements.

==================================================

1. FULL APPLICATION AUDIT
   ==================================================

Inspect the entire project, including:

* Activities
* Fragments
* ViewModels
* Repositories
* Adapters
* Services
* Utilities
* API/network layer
* Database/local storage
* Firebase
* Hilt/DI
* Navigation
* Authentication
* Subscription system
* Exam system
* Image loading
* Offline assets
* Background tasks
* Permissions
* Notifications
* WebView/Irembo functionality
* AdMob
* JSON parsing
* Serialization
* Error handling
* Gradle/dependencies
* ProGuard/R8
* AndroidManifest

Search for remaining:

* crashes
* ANRs
* memory leaks
* lifecycle problems
* null pointer risks
* race conditions
* network failures
* main-thread blocking
* unnecessary API calls
* unnecessary database operations
* unnecessary image loading
* duplicated data
* inefficient RecyclerView operations
* excessive object creation
* unnecessary Firebase calls
* unnecessary startup work
* deprecated APIs
* permission problems
* configuration-change issues
* Activity/Fragment context leaks

Fix genuine problems rather than adding unnecessary defensive code everywhere.

==================================================
2. USER ROLE SHOULD BE OFFLINE-FIRST
====================================

The USER side of the application should work offline wherever possible.

The app is primarily an exam/learning application, so users should be able to study and practice without an internet connection whenever the required data is already available locally.

Identify all features that can work offline.

Prioritize making these features offline:

* Opening the app
* Home/dashboard
* Traffic theory/rules content
* Exam categories
* Exam list
* Practice tests
* Previous tests
* Questions
* Question images
* Exam navigation
* Answer selection
* Exam submission/storage
* Results
* Score history
* Progress/statistics
* Language selection
* User preferences
* Previously downloaded content
* Subscription status/cache where safe
* Basic profile information

The application must NOT require an internet connection simply to display content that is already bundled or cached locally.

==================================================
3. OFFLINE-FIRST ARCHITECTURE
=============================

Inspect the current data architecture.

If data currently comes from APIs, implement a safe local-first strategy where appropriate:

UI
↓
ViewModel
↓
Repository
↓
Local data source + Remote data source

Preferred behavior:

1. Read available data from local storage first.
2. Display local data immediately.
3. If internet is available, synchronize/update data in the background.
4. If internet is unavailable, continue using local data.
5. If synchronization fails, do not break the existing local experience.
6. Store successful remote responses locally where appropriate.

Do not blindly cache sensitive or temporary server data.

==================================================
4. EXAM DATA SHOULD WORK OFFLINE
================================

The application already contains bundled exam/question data.

Inspect the existing assets, including:

* English exam data
* French exam data
* Kinyarwanda exam data
* Question images

Make sure these resources can be accessed without internet.

Do not replace local exam data with API calls.

When a user starts an exam:

* Questions should load locally whenever possible.
* Question images should load locally.
* Navigation between questions should not require internet.
* Selecting answers should not require internet.
* Timer should work offline.
* Exam completion should work offline.
* Result calculation should work offline.
* Results should be saved locally.

If server synchronization is required, queue the result and synchronize later when internet becomes available.

==================================================
5. OFFLINE EXAM ATTEMPTS
========================

Implement an offline queue for exam attempts if the current backend requires submission.

Example flow:

User completes exam offline
→ calculate result locally
→ save attempt locally
→ show result immediately
→ mark attempt as "pending synchronization"
→ when internet returns
→ synchronize with server
→ mark as synchronized

Do not prevent users from seeing their results because the server is temporarily unavailable.

Prevent duplicate synchronization.

Use a unique attempt ID/local ID to make synchronization idempotent where possible.

==================================================
6. PREVIOUS TESTS / HISTORY
===========================

Previous tests should be available offline for tests already completed on the device.

Store:

* exam/test ID
* date/time
* score
* total questions
* percentage
* language
* pass/fail
* answers if required
* synchronization status

Display local history immediately.

If server history is available, synchronize it with the local database.

Avoid replacing local history with an empty server response when the device is offline.

==================================================
7. USER PROFILE / LOGIN
=======================

Review authentication carefully.

The first login/registration may require internet.

After successful authentication, determine what information can safely be cached locally.

The app should not unnecessarily force a network request for every screen.

Handle:

* offline startup
* expired token
* temporary server failure
* no internet
* slow internet
* timeout
* server unavailable

Do not store passwords in plain text.

Do not weaken authentication or subscription security simply to make the application offline.

==================================================
8. SUBSCRIPTION / PREMIUM ACCESS
================================

Review the subscription system carefully.

The app should cache the user's last known valid subscription state where safe.

For example:

Online:
server confirms active subscription
→ store a locally signed/validated entitlement state with appropriate expiration rules

Offline:
use the cached entitlement according to the application's security/business rules.

Do NOT create a permanent offline premium bypass.

Do NOT remove server-side subscription validation.

The goal is to tolerate temporary offline conditions, not bypass payment or subscription security.

Make sure free content remains available offline where intended.

==================================================
9. NETWORK HANDLING
===================

Audit all network calls.

For every API request, check:

* timeout
* retry behavior
* error handling
* cancellation
* lifecycle
* loading state
* empty state
* offline state

Avoid:

* infinite retries
* retry storms
* repeated API calls
* network calls inside RecyclerView bind methods
* network calls on the main thread
* blocking network requests
* unnecessary polling

Use connectivity awareness where appropriate.

When offline, show useful UI such as:

"You're offline. Showing saved content."

Do not show a crash or blank screen.

==================================================
10. PERFORMANCE AUDIT
=====================

Investigate the current Google Play performance metrics:

* Slow cold start
* Slow warm start
* Slow hot start
* Excessive frozen frames
* Slow rendering
* Bitmap memory usage

Look for expensive operations in:

Application.onCreate()
Activity.onCreate()
Fragment.onCreate()
Fragment.onViewCreated()
ViewModel initialization
Repository initialization
Dependency injection initialization

Move expensive work away from the main thread where appropriate.

Do not initialize everything at application startup.

Use lazy initialization where appropriate.

==================================================
11. STARTUP OPTIMIZATION
========================

Optimize startup so the app can display useful UI quickly.

Check:

* Firebase initialization
* AdMob initialization
* Hilt initialization
* database initialization
* JSON parsing
* asset loading
* image loading
* SharedPreferences/DataStore
* network requests
* analytics
* remote configuration

Do not block the first screen waiting for:

* API responses
* large JSON files
* images
* subscription verification
* analytics
* advertisements

Load non-critical services after the initial UI is ready.

==================================================
12. IMAGE PERFORMANCE
=====================

Audit all images.

Especially inspect the bundled exam/question images.

Make sure:

* images are appropriately sized
* WebP assets are used efficiently
* large images are not decoded at unnecessary resolution
* images are not all loaded simultaneously
* RecyclerViews load images efficiently
* image caching is used correctly
* no memory leaks exist
* no unnecessary network image downloads occur
* offline images remain available

Do not convert everything to low quality.

Preserve sufficient quality for traffic signs and exam questions.

==================================================
13. RECYCLERVIEW PERFORMANCE
============================

Inspect all RecyclerViews and adapters.

Look for:

* expensive onBindViewHolder()
* repeated database queries
* repeated network requests
* repeated image decoding
* notifyDataSetChanged() unnecessarily
* nested RecyclerViews
* large object creation
* unnecessary layout inflation

Use DiffUtil/ListAdapter where appropriate.

Do not introduce unnecessary complexity if the existing list is already efficient.

==================================================
14. DATABASE / LOCAL STORAGE
============================

If a local database already exists, inspect it.

If a database is needed for offline functionality, determine whether Room is appropriate.

Store structured offline data efficiently.

Avoid storing large JSON blobs repeatedly if normalized data would be significantly better.

Add indexes for frequently queried fields.

Perform database operations off the main thread.

Do not migrate the entire database unnecessarily.

==================================================
15. LANGUAGE SUPPORT
====================

The application supports:

* English
* French
* Kinyarwanda

Make sure offline functionality works for ALL THREE languages.

Changing language must not require internet when the required translations/content already exist locally.

Check for:

* missing translations
* hardcoded English text
* missing resources
* incorrect fallback language
* UI text that disappears offline

==================================================
16. ERROR AND EMPTY STATES
==========================

Every important user-facing operation should have:

* Loading state
* Success state
* Empty state
* Offline state
* Error state

Avoid:

* blank screens
* infinite loading
* application crashes
* generic technical error messages
* buttons that appear clickable but do nothing

Give users useful recovery options such as:

* Retry
* Continue offline
* Refresh when online

==================================================
17. MEMORY AND RESOURCE LEAK AUDIT
==================================

Look for:

* Activity stored in static variables
* Fragment stored in static variables
* Context leaks
* Bitmap leaks
* listeners not removed
* observers with incorrect lifecycle
* coroutine/job leaks
* background tasks continuing after Activity destruction
* WebView leaks
* large collections retained unnecessarily

Use lifecycle-aware components.

==================================================
18. SECURITY
============

While making the application offline, DO NOT weaken security.

Check:

* API keys
* Firebase configuration
* tokens
* SharedPreferences
* local database
* logs
* sensitive user data
* subscription state
* authentication

Do not log:

* passwords
* authentication tokens
* payment information
* sensitive personal information

Do not store sensitive credentials in plain text.

==================================================
19. ANDROID COMPATIBILITY
=========================

Test the application across supported Android versions.

Pay special attention to:

Android 12
Android 13
Android 14
Android 15

Check:

* edge-to-edge
* permissions
* notifications
* storage
* file access
* photo access
* intents
* WebView
* keyboard/insets
* display cutouts
* background execution

Do not break older supported Android versions while fixing Android 15.

==================================================
20. BUILD AND STATIC ANALYSIS
=============================

Run:

./gradlew clean
./gradlew assembleDebug
./gradlew assembleRelease

Also run lint/static analysis where configured.

Fix genuine:

* compilation errors
* lint errors
* resource issues
* dependency conflicts
* R8/ProGuard problems

Do not suppress warnings unless there is a documented reason.

==================================================
21. TEST OFFLINE MODE PROPERLY
==============================

Do NOT test offline mode only by turning Wi-Fi off after launching the app.

Test these scenarios:

A. Fresh installation + no internet

B. User opens app with no internet

C. User opens exam with no internet

D. User completes exam with no internet

E. User views previous tests with no internet

F. User changes language with no internet

G. User starts online, loses connection during exam

H. User completes exam while offline

I. Internet returns after offline exam

J. Pending exam attempt synchronizes

K. Synchronization fails and retries later

L. App is killed while an offline attempt is pending

M. Device restarts while offline

N. User has an active subscription and temporarily loses internet

O. User has no cached data and is offline

Each scenario must result in a controlled and useful user experience.

==================================================
22. DO NOT MAKE EVERYTHING OFFLINE
==================================

Use good judgment.

The following may still require internet when appropriate:

* registration
* first authentication
* server-side account changes
* payment processing
* payment verification
* subscription purchase
* server synchronization
* downloading new content
* remote administration
* server-authoritative operations

The objective is:

OFFLINE-FIRST USER EXPERIENCE

not:

"Disable all network security."

==================================================
23. FINAL REPORT
================

When finished, provide a clear report with:

1. Remaining issues found.
2. Critical issues fixed.
3. Crash risks fixed.
4. Performance issues fixed.
5. Startup improvements.
6. Memory improvements.
7. Network improvements.
8. Offline features implemented.
9. Features that still require internet and why.
10. Database/local-storage changes.
11. Files changed.
12. Dependencies changed.
13. Tests performed.
14. Android versions tested.
15. Build result.
16. Any remaining risks.

Also clearly separate:

FIXED
IMPROVED
NOT CHANGED
REQUIRES SERVER/INTERNET
REQUIRES REAL DEVICE TESTING

IMPORTANT FINAL REQUIREMENT:

Do not simply tell me that the application "supports offline mode."

Demonstrate it through the architecture and code.

The final application should follow this principle:

```
                USER
                  ↓
          LOCAL DATA FIRST
                  ↓
         SHOW CONTENT FAST
                  ↓
         INTERNET AVAILABLE?
              /       \
            YES       NO
            ↓          ↓
    SYNC/UPDATE     CONTINUE
      IN BACKGROUND   OFFLINE
            ↓          ↓
         LOCAL DATA REMAINS AVAILABLE
```

The user should be able to study, practice exams, view results, and use already-downloaded/bundled learning content without an internet connection whenever technically and securely possible.
