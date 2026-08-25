Continue working on the Android application from the current project state.

You have already worked on:

* Android Vitals crash fixes
* Android 15 edge-to-edge compatibility
* bitmap/image optimization
* performance improvements
* offline-first functionality

Do NOT start the project again and do NOT rewrite working features.

Your job now is to CONTINUE inspecting the project, find remaining problems, fix them directly in the code, test the changes, and keep improving the application until it is production-ready.

==================================================
IMPORTANT WORKING RULE
======================

Do not only analyze and tell me what is wrong.

For every issue that can safely be fixed:

1. Find the source.
2. Fix it directly.
3. Check for side effects.
4. Build the project.
5. Test the affected functionality.
6. Continue looking for the next issue.

Do not stop after fixing one problem.

If something should NOT be changed, leave it unchanged and explain why.

==================================================

1. FULL PROJECT REVIEW
   ==================================================

Inspect the entire project:

* Java
* Kotlin
* Activities
* Fragments
* ViewModels
* Repositories
* Adapters
* Services
* Utilities
* Hilt/DI
* Navigation
* API/network layer
* local storage/database
* JSON/assets
* images
* authentication
* subscription
* payment flow
* Firebase
* AdMob
* IremboActivity
* AndroidManifest
* Gradle files
* ProGuard/R8
* resources
* layouts
* strings
* themes
* colors
* drawables

Look for remaining:

* crashes
* ANRs
* memory leaks
* lifecycle bugs
* null pointer risks
* network errors
* UI overflow
* broken navigation
* slow screens
* unnecessary loading
* duplicated API calls
* unnecessary database queries
* unnecessary image loading
* deprecated Android APIs
* permission problems
* security issues
* poor error handling
* broken offline behavior
* inconsistent UI

==================================================
2. FIX CRITICAL PROBLEMS FIRST
==============================

Use this priority:

P0:

* crashes
* data loss
* security issues

P1:

* broken login
* broken registration
* broken exams
* broken results
* broken subscription/payment
* broken navigation

P2:

* offline problems
* network reliability

P3:

* performance
* memory
* startup

P4:

* Android compatibility

P5:

* UI/UX

P6:

* code quality

Do not spend time on small UI changes while serious issues remain.

==================================================
3. MAKE USER EXPERIENCE OFFLINE-FIRST
=====================================

Continue making the USER role work offline wherever possible.

The following should work without internet whenever the required data already exists locally:

* opening the app
* home screen
* study content
* traffic rules/content
* exam list
* practice tests
* questions
* question images
* taking exams
* timer
* selecting answers
* submitting an exam locally
* calculating results
* viewing results
* previous tests
* progress
* language selection
* user preferences

If internet is unavailable:

DO NOT show a blank screen.

DO NOT crash.

DO NOT keep loading forever.

DO NOT force the user to reconnect when local data is sufficient.

Instead:

LOCAL DATA
→ DISPLAY IMMEDIATELY
→ CONTINUE WORKING OFFLINE
→ SYNCHRONIZE WHEN INTERNET RETURNS

==================================================
4. OFFLINE EXAM ATTEMPTS
========================

Make exam attempts resilient to network failure.

If a user completes an exam offline:

1. Calculate the result locally.
2. Save the attempt locally.
3. Show the result immediately.
4. Mark the attempt as pending synchronization.
5. Synchronize automatically when internet returns.
6. Prevent duplicate submissions.

If the application is killed or the phone restarts before synchronization, the pending attempt must not be lost.

Do NOT create a system that bypasses subscription/payment security.

==================================================
5. LOCAL EXAM CONTENT
=====================

The application contains bundled exam data and question images.

Inspect:

app/src/main/assets

including the English, French and Kinyarwanda exam data.

Make sure these resources remain completely usable offline.

Do not unnecessarily replace local content with API requests.

Do not download a question image from the internet when the same image already exists locally.

Optimize loading so large numbers of images are not decoded into memory at once.

==================================================
6. LANGUAGE QUALITY
===================

The app supports:

English
French
Kinyarwanda

Audit the entire application for:

* hardcoded English text
* missing translations
* incorrect translations
* untranslated dialogs
* untranslated errors
* untranslated subscription messages
* untranslated offline messages
* inconsistent terminology

Fix them directly.

Language switching should work offline for content already stored locally.

==================================================
7. LOGIN / REGISTRATION
=======================

Review login and registration carefully.

Make sure:

* the same phone format is accepted consistently;
* international numbers are supported;
* Rwandan numbers work;
* no country picker is unnecessarily introduced;
* validation is consistent;
* errors are understandable;
* network failures are handled;
* the app does not crash on invalid input;
* authentication state survives app restart appropriately.

Do not weaken authentication.

==================================================
8. EXAM UI
==========

Review the complete exam experience.

Make sure:

* question number is visible;
* question text is readable;
* question image fits correctly;
* all answer choices are accessible;
* scrolling works;
* next/previous works;
* timer works;
* progress is clear;
* buttons are not hidden by navigation bars;
* Android 15 edge-to-edge does not cover content;
* small-screen devices do not experience overflow.

Do not remove existing exam functionality.

==================================================
9. PREVIOUS TESTS
=================

Improve the Previous Tests screen.

It should clearly show:

* exam/test number
* date
* score
* percentage
* pass/fail
* language where useful

Use local history when offline.

Handle empty history properly.

Handle pending synchronization clearly without exposing technical details.

==================================================
10. SUBSCRIPTION
================

Review subscription functionality without changing the business rules.

Ensure:

* free content remains accessible;
* premium content is correctly protected;
* payment instructions are clear;
* activation state is handled correctly;
* temporary loss of internet does not unnecessarily break an already-valid cached state;
* payment verification remains server-controlled.

DO NOT create an offline premium bypass.

==================================================
11. PERFORMANCE
===============

Perform another performance audit.

Find expensive operations in:

* Application
* Activities
* Fragments
* ViewModels
* adapters
* repositories
* startup
* image loading
* JSON parsing
* database operations
* Firebase
* AdMob

Move expensive work away from the main thread where appropriate.

Avoid:

* unnecessary API calls
* unnecessary JSON parsing
* loading all exam data at startup
* decoding all images at startup
* repeated database queries
* repeated RecyclerView refreshes
* unnecessary notifyDataSetChanged()
* infinite retries

Make the application feel fast.

==================================================
12. STARTUP
===========

The first screen should appear quickly.

Do not block startup waiting for:

* network
* subscription verification
* advertisements
* large JSON parsing
* large image loading
* non-critical Firebase operations

Load non-critical work after the initial UI where appropriate.

==================================================
13. MEMORY
==========

Search for:

* static Activity references
* static Context references
* static Bitmap objects
* large collections kept in memory
* image leaks
* Fragment leaks
* listeners not removed
* coroutines/jobs not cancelled
* WebView leaks

Fix real memory problems.

==================================================
14. UI QUALITY
==============

Review the entire app visually and functionally.

Fix obvious issues such as:

* content cut off
* bad spacing
* inconsistent button sizes
* text overflow
* poor loading states
* blank screens
* unclear errors
* buttons too small
* poor empty states
* inconsistent typography
* unnecessary dialogs

Keep the existing design direction.

Do not completely redesign the application unless necessary.

==================================================
15. ERROR HANDLING
==================

Every important operation should handle:

SUCCESS
LOADING
EMPTY
OFFLINE
ERROR

Users should never see raw exceptions such as:

NullPointerException
SocketTimeoutException
HTTP 500
JSON exception

Show useful user-friendly messages instead.

Keep technical details in logs where appropriate.

==================================================
16. SECURITY
============

Inspect for:

* passwords in logs
* tokens in logs
* API secrets
* insecure storage
* exported components
* insecure WebView configuration
* insecure HTTP
* sensitive data leakage

Fix genuine security problems.

Do not weaken authentication or payment protection.

==================================================
17. ANDROID COMPATIBILITY
=========================

Check Android:

12
13
14
15

Pay special attention to:

* edge-to-edge
* permissions
* storage
* notifications
* keyboard/insets
* display cutouts
* WebView
* intents
* background execution

Do not downgrade targetSdk just to hide warnings.

==================================================
18. RELEASE BUILD
=================

Build the application after making changes.

Use the project's existing Gradle setup.

Run:

./gradlew clean
./gradlew assembleDebug
./gradlew assembleRelease

Fix compilation, resource, R8 and dependency issues.

Do not leave the project in a broken build state.

==================================================
19. TEST REAL USER FLOWS
========================

Test as a real user:

Fresh install
→ open app
→ select language
→ register/login
→ open home
→ study
→ start exam
→ answer questions
→ submit
→ view result
→ view previous tests
→ open subscription
→ go offline
→ continue studying
→ take another exam offline
→ view result
→ reconnect internet
→ synchronize

Also test:

* invalid phone number
* no internet
* slow internet
* server unavailable
* app restart
* app killed during exam
* app killed before synchronization
* Android 15
* small-screen device
* keyboard opening
* back navigation

==================================================
20. DO NOT STOP AT ANALYSIS
===========================

This is an implementation task.

If you find a safe issue:

FIX IT.

If you find performance inefficiency:

OPTIMIZE IT.

If you find a broken offline flow:

FIX IT.

If you find a UI problem:

IMPROVE IT.

If you find a security issue:

FIX IT.

If something requires backend changes that cannot safely be done locally:

DO NOT fake the solution.
Document what backend change is required.

==================================================
21. FINAL REPORT
================

At the end, provide:

### FIXED

List important issues fixed.

### PERFORMANCE

List performance improvements.

### OFFLINE

List features that now work offline.

### USER EXPERIENCE

List important UX improvements.

### SECURITY

List security issues fixed.

### ANDROID

List compatibility improvements.

### FILES CHANGED

List important files.

### DEPENDENCIES

List changed dependencies and why.

### TESTING

List builds/tests performed.

### REMAINING ISSUES

List anything still requiring attention.

### BACKEND REQUIRED

Clearly identify anything that cannot be fixed locally.

IMPORTANT:

Keep working through the project until the major user-facing issues have been investigated.

Do not just produce a report.

MAKE THE CHANGES DIRECTLY IN THE CODE.
