# Action Driving School — Complete Codebase Analysis

**Generated:** June 7, 2026  
**Package:** `com.drivingschoolrwandaapp`  
**App Name:** Action Driving School  
**Version:** 1.0.1 (Code 76)  
**Min SDK:** 27 | **Target SDK:** 35 | **Compile SDK:** 36

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack & Dependencies](#2-tech-stack--dependencies)
3. [Project Structure](#3-project-structure)
4. [Architecture & Design Patterns](#4-architecture--design-patterns)
5. [App Flow & Navigation](#5-app-flow--navigation)
6. [User Authentication System](#6-user-authentication-system)
7. [API Layer](#7-api-layer)
8. [Database Layer](#8-database-layer)
9. [DI (Dependency Injection) with Hilt](#9-di-dependency-injection-with-hilt)
10. [UI Screens & Components](#10-ui-screens--components)
11. [Test/Exam System](#11-testexam-system)
12. [Subscription & Payment System](#12-subscription--payment-system)
13. [Learning Materials & PDF Viewer](#13-learning-materials--pdf-viewer)
14. [Irembo Services Integration](#14-irembo-services-integration)
15. [Push Notifications](#15-push-notifications)
16. [Multi-language Support](#16-multi-language-support)
17. [Security Measures](#17-security-measures)
18. [Observations & Areas for Improvement](#18-observations--areas-for-improvement)

---

## 1. Project Overview

**Action Driving School** is an Android application designed to help Rwandan users prepare for their provisional driving license exam (permis de conduire / porovisoiri). The app provides:

- **Practice exams** (21 tests with 20 questions each)
- **Learning materials** (PDFs, images)
- **Irembo services integration** (license applications & special services)
- **WhatsApp groups** for community support
- **Subscription-based access** to premium tests via mobile money payments

The app is localized in **3 languages**: English, French, and Kinyarwanda (default).

**Backend API:** `https://console.amategekoyumuhanda.rw/api/`  
**Website:** `https://console.amategekoyumuhanda.rw`  
**Play Store:** Published on Google Play (`com.drivingschoolrwandaapp`)

---

## 2. Tech Stack & Dependencies

### Build System
- **Gradle Kotlin DSL** (`build.gradle.kts`)
- **Android Gradle Plugin:** 8.13.1
- **Kotlin:** 1.9.22 (with KAPT for annotation processing)

### Core Android Libraries
| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.appcompat:appcompat` | 1.6.1 | UI compatibility |
| `androidx.core:core-ktx` | 1.12.0 | Kotlin extensions |
| `com.google.android.material:material` | 1.12.0 | Material 3 design |
| `androidx.constraintlayout:constraintlayout` | 2.1.4 | Layout engine |
| `androidx.navigation:navigation-fragment-ktx` | 2.7.6 | Navigation component |
| `androidx.activity:activity` | 1.8.0 | Activity API |
| `androidx.swiperefreshlayout:swiperefreshlayout` | 1.1.0 | Pull-to-refresh |

### Architecture & DI
| Library | Purpose |
|---------|---------|
| **Dagger Hilt** 2.50 | Dependency injection (`AppModule`, `DatabaseModule`, `NetworkModule`, `TestModule`) |
| **AndroidX Lifecycle** 2.7.0 | LiveData, ViewModel, lifecycle-aware components |
| **MapStruct** 1.5.5 | Object mapping (between API models and DB entities) |

### Networking
| Library | Version | Purpose |
|---------|---------|---------|
| **Retrofit** 2.9.0 | HTTP client for API calls |
| **OkHttp** 4.12.0 | HTTP engine + interceptors |
| **Gson** 2.10.1 | JSON serialization/deserialization |
| **Logging Interceptor** 4.12.0 | HTTP request/response logging |

### Database
| Library | Version | Purpose |
|---------|---------|---------|
| **Room** 2.6.1 | Local SQLite database with DAOs |
| **Security-Crypto** 1.1.0-alpha06 | Encrypted SharedPreferences for tokens |

### Firebase
| Library | Version | Purpose |
|---------|---------|---------|
| `firebase-bom` | 33.0.0 | Firebase BoM |
| `firebase-messaging` | — | Push notifications |
| `firebase-crashlytics` | — | Crash reporting |

### Other Notable Libraries
| Library | Version | Purpose |
|---------|---------|---------|
| **Glide** 4.16.0 | Image loading with OkHttp integration |
| **PdfBox-Android** 2.0.25.0 | PDF rendering |
| **PhotoView** 2.3.0 | Pinch-to-zoom |
| **AndroidSVG** 1.4 | SVG rendering |
| **Biometric** 1.2.0-alpha05 | Biometric authentication |
| **In-app updates** 2.1.0 | Google Play in-app updates |
| **Hilt-WorkManager** 1.2.0 | DI for WorkManager |
| **DocumentFile** 1.0.1 | Document access |

---

## 3. Project Structure

```
app/src/main/java/com/drivingschoolrwandaapp/
├── api/
│   ├── ApiClient.java                    # Singleton API client (Retrofit + OkHttp)
│   ├── ApiService.java                   # All API endpoint definitions
│   ├── RetrofitClient.java               # (Empty file — unused)
│   ├── endpoints/
│   │   ├── AuthEndpoint.java
│   │   ├── BookmarkEndpoint.java
│   │   ├── PDFEndpoint.java
│   │   ├── SubscriptionEndpoint.java
│   │   ├── TestEndpoint.java
│   │   └── UserEndpoint.java
│   └── interceptors/
│       ├── AuthInterceptor.java          # Adds Bearer token to requests
│       ├── LoggingInterceptor.java       # Detailed HTTP logging (custom)
│       ├── NetworkInterceptor.java       # Checks connectivity, adds cache control
│       └── TokenAuthenticator.java       # Automatic token refresh on 401
│
├── data/
│   ├── local/preferences/
│   │   ├── AppPreferences.java           # Language & layout preferences
│   │   └── TokenManager.java             # Encrypted token storage
│   └── models/
│       ├── LearningMaterial.java
│       ├── LearningMaterialResponse.java
│       └── Pagination.java
│
├── database/
│   ├── AppDatabase.java                  # Room database (version 13)
│   ├── dao/
│   │   ├── BookmarkDao.java
│   │   ├── LearningMaterialDao.java
│   │   ├── PdfDao.java
│   │   ├── QuestionOptionDao.java
│   │   ├── SubscriptionPlanDao.java
│   │   ├── TestDao.java
│   │   ├── TestQuestionDao.java
│   │   ├── UserDao.java
│   │   └── UserSubscriptionDao.java
│   └── entities/
│       ├── Address.java
│       ├── Bookmark.java
│       ├── BookmarkEntity.java
│       ├── Device.java
│       ├── Language.java
│       ├── LearningMaterial.java
│       ├── Notification.java
│       ├── PdfFile.java
│       ├── Question.java                 # (Stub — empty)
│       ├── QuestionOption.java           # (Stub — empty)
│       ├── QuestionOptionEntity.java
│       ├── QuestionWithOptions.java
│       ├── Rating.java
│       ├── SubscriptionPlan.java
│       ├── Test.java                     # (Stub — empty)
│       ├── TestAttempt.java
│       ├── TestEntity.java
│       ├── TestQuestionEntity.java
│       ├── TestWithQuestions.java
│       ├── Transaction.java
│       ├── User.java                     # Core DB user entity
│       ├── UserRole.java
│       ├── UserSubscriptionEntity.java
│       └── UserSubscriptionWithPlan.java
│
├── di/
│   ├── AppModule.java                    # (Stub — empty)
│   ├── DatabaseModule.java               # Provides DB + DAOs
│   ├── NetworkModule.java                # Provides OkHttp, Retrofit, ApiService
│   ├── RepositoryModule.java             # (Stub — empty)
│   └── TestModule.java                   # Provides TestRepository
│
├── models/
│   ├── IremboApplication.java
│   ├── IremboService.java
│   ├── entities/                         # API response models (network layer)
│   │   ├── Address.java
│   │   ├── Bookmark.java
│   │   ├── Device.java
│   │   ├── Language.java
│   │   ├── Notification.java
│   │   ├── PdfFile.java
│   │   ├── Permission.java
│   │   ├── Question.java
│   │   ├── QuestionOption.java
│   │   ├── QuestionOptionTranslation.java
│   │   ├── QuestionTranslation.java
│   │   ├── Rating.java
│   │   ├── ReadingSession.java
│   │   ├── SubscriptionPlan.java
│   │   ├── Test.java
│   │   ├── TestAttempt.java
│   │   ├── TestQuestion.java
│   │   ├── TestResult.java
│   │   ├── TestTranslation.java
│   │   ├── Timezone.java
│   │   ├── Transaction.java
│   │   ├── User.java                     # API user model
│   │   ├── UserActivity.java
│   │   ├── UserRole.java
│   │   ├── UserSubscription.java
│   │   ├── UserTestAccess.java
│   │   └── WhatsAppGroup.java
│   ├── mappers/
│   │   ├── SubscriptionMapper.java
│   │   └── TestMapper.java               # MapStruct mapper: API↔DB entities
│   ├── request/                          # Request DTOs (15+ classes)
│   └── response/                         # Response DTOs (10+ classes)
│
├── repository/
│   ├── AuthRepository.java               # (Stub — empty)
│   ├── LearningMaterialRepository.java
│   ├── NetworkBoundResource.java         # Generic offline-first fetcher
│   ├── PdfRepository.java
│   ├── Resource.java                     # Generic resource wrapper (Success/Error/Loading)
│   ├── TestRepository.java               # Tests with caching logic
│   └── UserRepository.java               # Auth, profile, logout, delete account
│
├── services/
│   ├── MyFirebaseMessagingService.java   # FCM message handling
│   └── NotificationWorker.java           # WorkManager-based notification display
│
├── ui/
│   ├── activities/
│   │   ├── ApplicationDetailsActivity.java
│   │   ├── ChangePasswordActivity.java
│   │   ├── ForgotPasswordActivity.java
│   │   ├── IremboActivity.java           # Irembo services hub
│   │   ├── LoginActivity.java
│   │   ├── MyApplicationsActivity.java
│   │   ├── OtpVerificationActivity.java
│   │   ├── PdfViewerActivity.java        # In-app PDF reader
│   │   ├── RegisterActivity.java
│   │   ├── ResetPasswordActivity.java
│   │   ├── SplashActivity.java           # Entry point
│   │   ├── WebViewActivity.java
│   │   ├── WelcomeActivity.java          # Onboarding/landing
│   │   └── WhatsAppGroupsActivity.java
│   ├── adapters/                         # 12 RecyclerView adapters
│   └── fragments/
│       ├── BookmarksFragment.java
│       ├── ClassesFragment.java
│       ├── DashboardFragment.java        # Main hub
│       ├── ExamsFragment.java            # (Stub)
│       ├── HomeFragment.java
│       ├── IremboFragment.java
│       ├── LearningFragment.java         # (Stub)
│       ├── LessonsFragment.java
│       ├── LibraryFragment.java
│       ├── MaterialsFragment.java        # Learning materials browser
│       ├── MyApplicationsFragment.java
│       ├── ProfileFragment.java          # User profile + subscription status
│       ├── ResultsFragment.java
│       ├── SettingsFragment.java
│       ├── SingleQuestionPageFragment.java
│       ├── TestQuestionsFragment.java    # Exam-taking interface
│       ├── TestResultFragment.java       # Exam results
│       └── TestsFragment.java            # Test list with grid/list toggle
│
├── utils/                                # 17 utility classes
├── viewmodel/
│   ├── AuthViewModel.java                # (Stub — empty)
│   ├── IremboViewModel.java              # Irembo operations
│   ├── LearningMaterialViewModel.java
│   ├── LearningMaterialViewModelFactory.java
│   ├── PdfViewModel.java                 # PDF bookmarks
│   ├── SubscriptionViewModel.java
│   ├── TestViewModel.java                # Test-taking logic + result calculation
│   └── UserViewModel.java                # Auth + profile management
│
├── App.java                              # Main activity (post-login hub)
└── MainApplication.java                  # Application class with Hilt & WorkManager
```

---

## 4. Architecture & Design Patterns

### Architecture: MVVM + Repository + Offline-First

The app follows **Model-View-ViewModel (MVVM)** architecture with an **offline-first** approach:

```
UI (Activities/Fragments) 
    ↓ observes LiveData
ViewModels 
    ↓ calls methods
Repositories 
    ↓ using NetworkBoundResource
API (Retrofit) ← → Database (Room)
```

### Key Patterns

**1. NetworkBoundResource Pattern**
- Abstract class `repository/NetworkBoundResource.java`
- Generic offline-first data fetching: load from DB → check if stale → fetch from API → save to DB → return fresh data
- Used by `UserRepository.getProfile()` and `TestRepository.getTests()`, `TestRepository.getTestWithQuestions()`

**2. Resource Wrapper**
- `repository/Resource.java` — generic container with `Status` enum: `SUCCESS`, `ERROR`, `LOADING`
- Wraps data in a `LiveData<Resource<T>>` throughout the app

**3. Singleton ApiClient**
- `api/ApiClient.java` — thread-safe singleton that provides `ApiService`, `TokenManager`, `OkHttpClient`
- Also set up via **Dagger Hilt** in `NetworkModule.java` (effectively duplicate configuration)

**4. MapStruct Mappers**
- `models/mappers/TestMapper.java` — converts between API models (`models.entities.*`) and Room entities (`database.entities.*`)
- Used for test data, test questions, and question options

### Offline-First Cache Strategy

| Data Type | Cache Duration | Behavior |
|-----------|---------------|----------|
| Tests | Up to 7 days or until user access expires | Uses `lastRefreshed` timestamp per test |
| Test questions | Same as tests | Cascade-deleted and re-fetched per test |
| User profile | Fetched from API on every load, saved to Room | Network-first with DB fallback |

---

## 5. App Flow & Navigation

### User Flow

```
SplashActivity
    ├── Token exists? → App (Main Activity with bottom nav)
    └── No token → WelcomeActivity (carousel + register/login buttons)
                        ├── RegisterActivity → Login internal → App
                        └── LoginActivity → App
        
App (Main Hub)
    ├── Bottom Navigation: Dashboard | Tests | Materials | Profile
    └── Drawer Menu: Share, WhatsApp Groups, About Us, Delete Account
        │
        ├── DashboardFragment
        │   ├── Start Exam → TestsFragment
        │   ├── Learning Materials → MaterialsFragment
        │   ├── Irembo Services → IremboActivity
        │   ├── WhatsApp Groups → WhatsAppGroupsActivity
        │   └── Profile → ProfileFragment
        │
        ├── TestsFragment
        │   ├── Test List (grid/list toggle)
        │   ├── Locked test → Subscription BottomSheet → payment
        │   └── Unlocked test → TestQuestionsFragment
        │       └── TestResultFragment
        │
        ├── MaterialsFragment
        │   └── Learning material → Download → Open (PdfViewerActivity or image viewer)
        │
        └── ProfileFragment
            ├── User info + subscription status
            ├── Change Language
            └── Logout
```

### Navigation Component
- Dual `nav_graph.xml` files (identical) — one at `res/navigation/nav_graph.xml` and `res/navigation/main_nav_graph.xml`
- Uses `NavHostFragment` within `App.java` main activity
- Bottom navigation and drawer navigation both use `NavigationUI` APIs
- `dashboardFragment` is the start destination
- Activities (IremboActivity) are referenced as nav destinations

---

## 6. User Authentication System

### Registration (`RegisterActivity.java`)
- Minimal registration: **full name + phone number only**
- Uses Android's `Settings.Secure.ANDROID_ID` as password (device ID-based)
- Collects device info: manufacturer, model, timezone, language
- Calls `apiService.register(user)` then automatically logs in
- Uses a **direct API call** (Retrofit callback) rather than going through ViewModel/Repository

### Login (`LoginActivity.java`)
- Phone number as identifier + device ID as password
- Uses `UserViewModel` → `UserRepository.login()`
- On success: saves JWT tokens, navigates to `App` main activity
- **Warning:** The login field is named "email" but actually uses the phone number

### Token Management
- **Access Token & Refresh Token** stored in `EncryptedSharedPreferences` (AES256-GCM encrypted)
- `TokenManager.java` handles save, retrieve, expiry check (24h default expiry)
- `AuthInterceptor.java` attaches `Bearer <accessToken>` to all requests except `/auth/refresh`
- `TokenAuthenticator.java` automatically refreshes tokens on 401 responses
- On refresh failure → clear tokens → redirect to `LoginActivity`

### Password Reset Flow
1. `ForgotPasswordActivity` → send OTP to email
2. `OtpVerificationActivity` → verify 6-digit code
3. `ResetPasswordActivity` → set new password

### Account Deletion
- `deleteAccount()` → API call → local logout
- Dialog confirmation before deletion

---

## 7. API Layer

### Base URL
```
https://console.amategekoyumuhanda.rw/api/
```

### ApiService Endpoints (all defined in `ApiService.java`)

**Authentication:**
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `auth/login` | Login with identifier + password + deviceId + clientType (`android_app`) |
| POST | `auth/register` | Register new user |
| POST | `auth/refresh` | Refresh JWT tokens |
| POST | `auth/forgot-password` | Request password reset |
| POST | `auth/verify-otp` | Verify OTP code |
| POST | `auth/reset-password` | Reset password |
| POST | `auth/logout` | Logout |
| DELETE | `users/delete/` | Delete account |

**User Profile:**
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `users/profile` | Get user profile |
| PUT | `users/profile` | Update profile |
| POST | `auth/change-password` | Change password |

**PDFs:**
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `pdfs` | Paginated PDF list (searchable) |
| GET | `pdfs/{id}` | Single PDF details |
| POST | `pdfs/{id}/bookmark` | Add bookmark |

**Tests:**
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `tests` | All tests |
| GET | `tests/{testId}/questions` | Test with questions |
| POST | `tests/{id}/attempt` | Start test attempt |
| POST | `tests/attempts/{attemptId}/submit` | Submit answers |

**Learning Materials:**
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `learning-materials` | Paginated list |
| GET | `learning-materials/{id}/download` | Download file (streaming) |

**Subscriptions:**
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `subscriptions` | All plans |
| GET | `subscriptions/user` | User's subscription |
| POST | `subscriptions/user` | Subscribe/cancel/sleep |
| POST | `subscriptions/user/cancel` | Cancel subscription |

**Irembo:**
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `irembo/driving` | License application |
| POST | `irembo/special` | Special service request |
| GET | `irembo/applications` | User's applications |
| GET | `irembo/applications/{number}` | Track application |

**Other:**
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `firebase/` | Update FCM token |
| GET | `whatsapp-groups` | WhatsApp group list |
| GET | `notifications` | User notifications |

### Interceptors (in order)
1. **NetworkInterceptor** — checks connectivity, adds `Cache-Control: max-age=60` to GET requests
2. **AuthInterceptor** — adds `Authorization: Bearer <token>` header
3. **LoggingInterceptor** — logs full request/response bodies to Logcat
4. **TokenAuthenticator** — (OkHttp Authenticator, runs on 401) attempts token refresh

### Duplicate Network Setup
- `ApiClient.java` — manual singleton setup
- `NetworkModule.java` — Hilt module (identical configuration)
- Both are used in different places (some ViewModels use `ApiClient.getInstance()`, others use injected `ApiService`)

---

## 8. Database Layer

### Room Database
- **Database name:** `driving_school_db`
- **Version:** 13 (with `fallbackToDestructiveMigration()` — all data lost on version upgrades)
- **Main thread queries allowed** (`allowMainThreadQueries()`) — **anti-pattern**

### Entities (8 Room entities)
| Entity | Table | Purpose |
|--------|-------|---------|
| `User` | `users` | Cached user profile |
| `SubscriptionPlan` | `subscription_plans` | Available plans |
| `UserSubscriptionEntity` | `user_subscriptions` | User's subscription |
| `TestEntity` | `tests` | Cached test data |
| `TestQuestionEntity` | `test_questions` | Cached questions |
| `QuestionOptionEntity` | `question_options` | Cached options |
| `BookmarkEntity` | `bookmarks` | PDF bookmarks |
| `PdfFile` | — (via PdfDao) | Cached PDF metadata |

### Type Converters
- `DateConverter.java` — Date ↔ Long (timestamp)
- `DataConverter.java` — JSON string ↔ List of `TestTranslation`, `QuestionTranslation`, `QuestionOptionTranslation`

### DAOs
- `UserDao.java` — insert, getUser (LiveData), getUserSync, deleteAll
- `TestDao.java` — insert with IGNORE, update, delete not-in-ids, getAllTests, getTestWithQuestions
- `TestQuestionDao.java` — insert, delete for test
- `QuestionOptionDao.java` — insert batch, delete for test
- `PdfDao.java` — PDF-related operations
- `BookmarkDao.java` — bookmark CRUD
- Others: `SubscriptionPlanDao`, `UserSubscriptionDao`, `LearningMaterialDao`

---

## 9. DI (Dependency Injection) with Hilt

### Setup
- `@HiltAndroidApp` on `MainApplication.java`
- `@AndroidEntryPoint` on `App.java`, `PdfViewerActivity.java`, `TestsFragment.java`, `TestQuestionsFragment.java`, `TestResultFragment`

### Hilt Modules

| Module | Provides |
|--------|----------|
| `DatabaseModule` | `AppDatabase`, `UserDao`, `TestDao`, `PdfDao` |
| `NetworkModule` | `TokenManager`, `OkHttpClient`, `Retrofit`, `ApiService` |
| `TestModule` | `TestRepository` |
| `AppModule` | (Empty — not used) |
| `RepositoryModule` | (Empty — not used) |

### Hilt ViewModels
- `TestViewModel` via `@HiltViewModel` (constructor injection)
- `PdfViewModel` via `@HiltViewModel`
- Other ViewModels use `ViewModelProvider.Factory` or `AndroidViewModel` with manual DI

### Inconsistency
- Some ViewModels use Hilt injection, others (like `UserViewModel`, `IremboViewModel`, `LearningMaterialViewModel`) use `ApiClient.getInstance()` directly → mixing DI with service locator pattern

---

## 10. UI Screens & Components

### Theme & Styling
- **Base Theme:** `Theme.Material3.DayNight.NoActionBar`
- **Font:** Montserrat (global via theme), Poppins also bundled
- **Primary Color:** `#B7EBF4` (light cyan) with accent `#33A1C9`
- **Bottom Navigation:** Pill-shaped active indicator, selected text hidden (0.1sp)
- **Custom button style** with primary color background

### Key Screens

| Screen | Layout | Purpose |
|--------|--------|---------|
| Splash | `activity_splash.xml` | Branded splash with AndroidX SplashScreen API |
| Welcome | `activity_welcome.xml` | Carousel + Register/Login buttons + support dialog |
| Login | `activity_login.xml` | Phone number input |
| Register | `activity_register.xml` | Name + phone, form validation |
| Dashboard | `fragment_dashboard.xml` | Quick access cards (6 cards) |
| Tests | `fragment_tests.xml` | Grid/list test list with SwipeRefresh |
| Test Questions | `fragment_test_questions.xml` | ViewPager2 with question navigation + timer |
| Test Result | `fragment_test_result.xml` | Score display + pass/fail |
| Materials | `fragment_materials.xml` | Searchable learning materials list |
| Profile | `fragment_profile.xml` | User info + subscription status + language |
| PDF Viewer | `activity_pdf_viewer.xml` | In-app PDF reader with bookmarks |
| Irembo | `activity_irembo.xml` | License services hub |
| Settings | `fragment_settings.xml` | (Basic — to be extended) |

### Dialogs (11 custom dialogs)
- About us, Instructions, Add/View bookmarks, Go to page
- Payment confirmation, Payment instructions
- Irembo license form, Irembo special service form
- Material image viewer, Delete account, Subscription plans
- Support dialog

### Adapters (12 total)
- `TestAdapter` — grid/list for tests, lock status, download progress
- `TestQuestionPagerAdapter` — ViewPager2 for question-by-question navigation
- `PdfAdapter` — PDF page rendering
- `LearningMaterialAdapter` — material cards with download button
- `IremboServiceAdapter` — service grid (2 columns)
- `RecentActivityAdapter` — recent Irembo applications
- `BookmarkAdapter`, `WhatsAppGroupAdapter`, `MyApplicationsAdapter`, etc.

---

## 11. Test/Exam System

### Test Data Model
- **21 tests** (20 questions each)
- Each test has: `id`, `title`, `description`, `testNumber`, `imageUrl`, `totalMarks`, `passMarks`, `duration`, `isFree`, `subscriptionId`
- Questions have: `questionText`, `questionType`, `imageUrl`, `fromPage`, `toPage`, `options`
- Options have: `optionText`, `isCorrect`

### Free vs Paid
- **Test 1 is free** (ubuntu)
- Tests 2–21 require subscription/access

### Taking a Test
1. From `TestsFragment`, user taps a test
2. If locked → bottom sheet with subscription plan selection
3. If unlocked → opens `TestQuestionsFragment` with `ViewPager2`
4. Questions shown one at a time with Previous/Next navigation
5. **Real-time feedback mode** — answer highlights green (correct) or red (incorrect) immediately
6. Timer counts down based on test `duration` value
7. Submit → `TestResultFragment` shows score + pass/fail

### Result Calculation (in `TestViewModel.calculateResult()`)
- Compares user answers against correct options
- Calculates score: `round((correctAnswers / totalQuestions) * totalMarks)`
- Pass/fail based on `passMarks`

### Offline Caching
- Test data cached in Room with `lastRefreshed` timestamp
- Cache valid for up to 7 days or until user's test access expires
- Questions are **cascade-deleted and re-fetched** per test to ensure freshness
- Background download of unlocked tests happens in `TestsFragment.downloadUnlockedTests()`

---

## 12. Subscription & Payment System

### Subscription Plans
Pricing structure (from strings.xml):
```
100 RWF  = 1 test (1 day)
1000 RWF = 10 tests (10 days)
2000 RWF = all tests (25 days)
5000 RWF = all tests (6 months)
```

### Access Control
- `UserTestAccess` object tracks: `maxTest`, `expiresAt`, `status` (ACTIVE/PENDING/INACTIVE)
- Tests are locked/unlocked based on user's `maxTest` value
- Free test (test #1) always accessible

### Payment Methods (Rwanda-specific)
- **MoMo Pay:** `*182*8*1*847318*<amount>#`
- **MTN Mobile Money:** `*182*1*1*0782877442*<amount>#`
- **Airtel Money:** `*182*1*1*0722877442*<amount>#`
- These are dialed via `Intent.ACTION_CALL` (requires CALL_PHONE permission)

### Payment Flow
1. User selects test → bottom sheet shows plan options
2. User picks plan → `subscriptionViewModel.requestTestAccess()`
3. API processes → `showPaymentInstructionsDialog()` with payment method cards
4. User pays via USSD → waits for admin to approve → tests unlock
- `"Sleep subscription"` feature allows pausing the subscription

---

## 13. Learning Materials & PDF Viewer

### Materials
- Learning materials (PDFs, images) fetched from API
- Paginated with search functionality
- Access control based on subscription status
- Downloads to app's internal storage via streaming download

### PDF Viewer (`PdfViewerActivity.java`)
- Uses **Android `PdfRenderer`** API (built-in Android PDF rendering, not PdfBox-Android despite the dependency)
- Renders all pages in a vertical RecyclerView
- Features:
  - Page number indicator ("X / Y")
  - **Bookmarks** system (add, view, navigate to, delete)
  - **Go to page** dialog
  - **Auto-saves reading position** per PDF using SharedPreferences
- Menu: Add bookmark, View bookmarks, Go to page

### Extra Libraries (unused?)
- `com.tom-roush:pdfbox-android:2.0.25.0` is included as a dependency but the PDF viewer uses Android's built-in `PdfRenderer` API instead

---

## 14. Irembo Services Integration

### Services Offered
1. **Provisional/Full License Application** — driver's license via Irembo
2. **Special Irembo Service (BUSANZA)** — special request service

### License Application Flow
1. User fills form: name, phone, national ID (16 digits), province/district, category (A/B/C/D), license type (learner/full)
2. Location data loaded from `assets/location.json` (provinces → districts cascade)
3. API call → payment response → payment confirmation dialog with USSD options
4. User pays → application tracked via application number

### Application Tracking
- Enter application number → fetch details → view in `ApplicationDetailsActivity`
- Recent activity shows last 2 applications on dashboard
- "View All" opens `MyApplicationsActivity` with filter chips (All/Pending/Processing/Action/Approved/Rejected)

---

## 15. Push Notifications

### Firebase Cloud Messaging
- `MyFirebaseMessagingService.java` handles incoming messages
- **Data-only payload** pattern: extracts `title`, `body`, `channelId`, `largeIconUrl` from data payload
- Schedules `NotificationWorker` (WorkManager) for reliable notification display
- Token refresh sends new token to backend via `POST firebase/`

### Notification Channels
| Channel | ID | Purpose |
|---------|----|---------|
| General | `general_channel` | General announcements |
| Exams | `exams_channel` | Exam reminders & results |
| Irembo | `irembo_channel` | Irembo service updates |
| Applications | `application_channel` | Application status |
| Subscription | `subscription_channel` | Subscription updates |

---

## 16. Multi-Language Support

### Languages
- **English** (`values-en/`)
- **French** (`values-fr/`)
- **Kinyarwanda** (`values-rw/`) — **default**

### Implementation
- `LanguageUtils.java` manages locale changes
- Language stored in `AppPreferences` (SharedPreferences)
- Language change triggers restart via `SplashActivity`
- Uses `AppCompatDelegate.setApplicationLocales()` (AndroidX AppCompat approach)
- Resource configurations limited to `en`, `fr`, `rw` via `resourceConfigurations` in build.gradle

### User Language Selection
- From `WelcomeActivity` (change language button), `ProfileFragment`, and `AppPreferences`
- Language IDs mapped: `41=English`, `48=Français`, default = Kinyarwanda

---

## 17. Security Measures

### Implemented
- ✅ **FLAG_SECURE** on main activity window (prevents screenshots/screen recording)
- ✅ **Encrypted token storage** via `EncryptedSharedPreferences` (AES256-GCM)
- ✅ **JWT token authentication** with Bearer token
- ✅ **Automatic token refresh** with fallback to logout
- ✅ **Input validation** on registration forms
- ✅ **Secure network config** (`network_security_config.xml`)

### Missing / Concerns
- ⚠️ **Device ID as password** — uses `Settings.Secure.ANDROID_ID` as user password (not a real password)
- ⚠️ **allowMainThreadQueries()** — Room configured to allow main thread DB operations
- ⚠️ **Biometric dependency** included but not used anywhere
- ⚠️ **No SSL pinning** configured in OkHttp

---

## 18. Observations & Areas for Improvement

### Architecture Issues

1. **Dual network setup** — `ApiClient.java` (singleton) and `NetworkModule.java` (Hilt) have identical configuration; some classes use one, some use the other

2. **Mixed DI approaches** — Some ViewModels use Hilt injection, others use manual `ApiClient.getInstance()` (service locator anti-pattern)

3. **Empty stub files** — Several files exist but are empty:
   - `di/AppModule.java`, `di/RepositoryModule.java`
   - `viewmodel/AuthViewModel.java`
   - `repository/AuthRepository.java`
   - `api/RetrofitClient.java`
   - `database/entities/Test.java`, `database/entities/Question.java`, `database/entities/QuestionOption.java`
   - `ui/fragments/ExamsFragment.java`, `ui/fragments/LearningFragment.java`

4. **Duplicate nav_graph** — Two identical navigation graphs (`nav_graph.xml` and `main_nav_graph.xml`)

5. **MapStruct dependency conflicts** — MapStruct processor is used, but there's also manual mapping in `UserRepository.mapUser()`

### Code Quality Issues

6. **Fragment used as "Activity"** — `App.java` is named `App` (confusing with `Application` class) and is actually an `AppCompatActivity`, not an Application subclass

7. **Hardcoded values** — Phone numbers (0782877442, 0722877442), MoMo Pay code (847318), and pricing logic scattered across fragments

8. **Memory leaks risk** — `observeForever()` used in `UserViewModel` (never stops observing)

9. **Database version 13** with destructive migrations means schema changes lose all user data

10. **Inconsistent error handling** — Some places use Toast, some use Snackbar, some use dialogs

11. **No Kotlin usage** despite Kotlin being configured — all source files are Java

12. **Request/Response models duplication** — `models/entities/` (API models) vs `database/entities/` (Room entities) with similar but different fields

### Feature Gaps

13. **Biometric authentication** library included but not implemented

14. **In-app updates** library included and partially implemented (flexible update flow)

15. **PdfBox-Android** dependency included but not used (uses Android's built-in PdfRenderer)

16. **WorkManager** configured but only used for notifications

17. **No caching for learning materials** beyond what Glide does for images

18. **No real-time score submission** — test results are calculated locally, not submitted to backend for history tracking

### UX Improvements

19. **FLAG_SECURE blocks screenshots** — intentional but prevents users from capturing study material

20. **No dark mode** — night theme colors defined but app doesn't respond to system dark mode consistently

21. **Bottom nav text hidden on active** — active item text set to 0.1sp (intentional design choice)

22. **No onboarding after app update** — language change requires app restart

### Backend Integration

23. **Login field labeled "email" but uses phone number** — confusing UI

24. **Backend URLs hardcoded** in `ApiClient.java` — no build config variant support

25. **No pagination support** for tests or questions (despite `PaginatedResponse` existing for PDFs)
