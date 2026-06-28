<div align="center">
  <img src="app/src/main/res/mipmap-hdpi-v4/ic_launcher.webp" alt="Action Driving School Logo" width="100"/>

  # Action Driving School

  **Rwanda's #1 Driving Theory Test Preparation App**

  [![Platform](https://img.shields.io/badge/Platform-Android-33A1C9?logo=android)]()
  [![Min SDK](https://img.shields.io/badge/minSdk-27-00C853)]()
  [![Target SDK](https://img.shields.io/badge/targetSdk-35-2962FF)]()
  [![Languages](https://img.shields.io/badge/Languages-en%20|%20fr%20|%20rw-FF9800)]()
  [![License](https://img.shields.io/badge/License-Proprietary-red)]()

  <br>

  <p>
    <strong>Kinyarwanda</strong> &middot;
    <strong>English</strong> &middot;
    <strong>Français</strong>
  </p>

  <p>
    Prepare for your Rwandan provisional driving license exam with official practice tests,
    study materials, and integrated Irembo services — all in one app.
  </p>

  <br>

  <a href="#-features">Features</a> &middot;
  <a href="#-architecture">Architecture</a> &middot;
  <a href="#-tech-stack">Tech Stack</a> &middot;
  <a href="#-getting-started">Getting Started</a> &middot;
  <a href="#-project-structure">Project Structure</a>
</div>

---

## ✨ Features

### 📝 Practice Exams
- **21 full-length tests** mirroring the official provisional driving exam
- **20 questions per test** covering all road rules and regulations
- **Real-time feedback** — see correct/incorrect answers immediately during practice
- **Timed mode** with countdown timer to simulate real exam conditions
- **Review mode** — revisit completed tests with correct answers highlighted

### 📚 Learning Materials
- **PDF viewer** with bookmark support for study documents
- **Download offline** — save materials for study without internet
- **Page navigation** and custom bookmarks for quick reference

### 🏛️ Irembo Integration
- **License application** — apply for provisional or full driving license directly through the app
- **Application tracking** — monitor your Irembo application status in real-time
- **Special requests** — submit special Irembo service requests (BUSANZA)
- **Payment processing** — integrated payment instructions via Mobile Money, MoMo Pay, Tigo Cash

### 📊 Results & Analytics
- **Detailed score breakdown** per test
- **Performance history** — track progress across all attempts
- **Pass/fail indicators** with percentage scores
- **Average score** and **pass rate** statistics

### 👤 User Management
- **Phone-based registration** and login
- **OTP verification** for secure authentication
- **Password management** — change/reset password flows
- **Profile management** with personal details
- **Multi-language support** — English, French, Kinyarwanda

### 💬 Community & Support
- **WhatsApp groups** — join community discussion groups
- **In-app instructions** — comprehensive guide on how to use the app
- **Direct contact** — call or WhatsApp the support team
- **Subscription plans** — flexible pricing for test access (daily, weekly, monthly)

---

## 🏗️ Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture with **Repository pattern**, built on **Android Jetpack** components and **Dagger Hilt** for dependency injection.

```
┌─────────────────────────────────────────────────────────┐
│                       UI Layer                          │
│  ┌──────────────┐  ┌────────────┐  ┌────────────────┐  │
│  │  Activities   │  │ Fragments  │  │   Adapters     │  │
│  └──────┬───────┘  └─────┬──────┘  └────────────────┘  │
│         │                │                              │
│  ┌──────┴────────────────┴──────────────────────────┐   │
│  │              ViewModels                          │   │
│  └──────┬───────────────────────────────────────────┘   │
├─────────┼───────────────────────────────────────────────┤
│         │              Data Layer                       │
│  ┌──────┴───────────────────────────────────────────┐   │
│  │              Repositories                        │   │
│  └──────┬──────────────────────────────┬────────────┘   │
│         │                              │                │
│  ┌──────┴──────────┐          ┌────────┴────────┐      │
│  │  Remote (API)   │          │  Local (Room)   │      │
│  │  Retrofit +     │          │  SQLite DB      │      │
│  │  OkHttp         │          │  + DataStore    │      │
│  └─────────────────┘          └─────────────────┘      │
└─────────────────────────────────────────────────────────┘
```

### Key Architectural Decisions

| Pattern | Implementation |
|---|---|
| **DI** | Dagger Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@Module`) |
| **Networking** | Retrofit 2 + OkHttp 4 with interceptors for auth, logging, network monitoring |
| **Auth** | JWT token-based with `TokenAuthenticator` for automatic refresh |
| **Database** | Room (SQLite) for offline caching of questions and results |
| **Navigation** | Jetpack Navigation Component with single-activity architecture |
| **State Management** | `LiveData` in ViewModels exposed to Fragments |
| **Image Loading** | Glide with OkHttp integration for network images |
| **Object Mapping** | MapStruct for DTO-to-model conversion |
| **Background Work** | WorkManager for scheduled tasks |
| **Secure Storage** | AndroidX Security Crypto for encrypted token storage |

---

## 📱 Tech Stack

### Languages
| Language | Usage |
|---|---|
| **Java 17** | Activities, Fragments, Adapters, API, DI modules |
| **Kotlin** | Data models, ViewModels, Repositories, Room entities |

### Android SDK & Jetpack
| Component | Purpose |
|---|---|
| `appcompat:1.6.1` | Backward-compatible UI components |
| `core-ktx:1.12.0` | Kotlin extensions for Android |
| `material:1.12.0` | Material Design 3 components |
| `constraintlayout:2.1.4` | Flexible layout engine |
| `navigation-fragment-ktx:2.7.6` | Type-safe navigation |
| `lifecycle-viewmodel:2.7.0` | MVVM ViewModel |
| `room-runtime:2.6.1` | Local SQLite database |
| `work-runtime:2.9.0` | Background task scheduling |
| `swiperefreshlayout:1.1.0` | Pull-to-refresh |
| `security-crypto:1.1.0-alpha06` | Encrypted SharedPreferences |

### Third-Party Libraries
| Library | Purpose |
|---|---|
| **Dagger Hilt 2.50** | Dependency injection |
| **Retrofit 2.9.0** | HTTP API client |
| **OkHttp 4.12.0** | HTTP client + interceptors |
| **Gson 2.10.1** | JSON serialization |
| **Glide 4.16.0** | Image loading |
| **MapStruct 1.5.5** | Object mapping |
| **PhotoView 2.3.0** | Pinch-to-zoom images |

### Firebase Services
| Service | Purpose |
|---|---|
| **Firebase Crashlytics** | Crash reporting |
| **Firebase Cloud Messaging** | Push notifications |
| **Firebase BOM 33.0.0** | Managed Firebase dependencies |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (Ladybug or newer recommended)
- **JDK 17+**
- **Android SDK** (compileSdk 36, targetSdk 35)
- **Google Services** — `google-services.json` from Firebase Console (use `google-services.json.example` as reference)

### Setup

```bash
# Clone the repository
git clone <repository-url>
cd ACTION-DRIVING-SCHOOL

# Set up Firebase
cp app/google-services.json.example app/google-services.json
# 👆 Then replace with your actual google-services.json from Firebase Console

# Build
./gradlew assembleDebug

# Build release AAB (with signing)
./gradlew bundleRelease
```

### Signing Configuration

The app uses a PKCS12 keystore for release signing:

| Item | Value |
|---|---|
| **Keystore location** | `app/upload-keystore.jks` |
| **Key alias** | `upload` |
| **Certificate CN** | `Nomiso` |
| **SHA1 fingerprint** | `AD:14:AD:F8:8B:2B:11:C5:37:99:D7:29:09:42:D0:25:9D:25:D3:11` |

> ⚠️ The keystore password is stored securely and not committed to version control.

---

## 📁 Project Structure

```
app/
├── src/main/
│   ├── java/com/drivingschoolrwandaapp/
│   │   ├── api/
│   │   │   ├── ApiClient.java           # Retrofit client singleton
│   │   │   ├── ApiService.java          # API endpoint definitions
│   │   │   └── interceptors/            # OkHttp interceptors
│   │   │       ├── AuthInterceptor.java      # JWT auth header injection
│   │   │       ├── NetworkInterceptor.java   # Network connectivity checks
│   │   │       └── TokenAuthenticator.java   # Automatic token refresh
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── AppDatabase.java     # Room database
│   │   │   │   ├── dao/                 # Room DAOs
│   │   │   │   └── preferences/         # SharedPreferences + DataStore
│   │   │   ├── models/                  # Data transfer objects
│   │   │   └── repository/              # Repository implementations
│   │   ├── di/
│   │   │   ├── NetworkModule.java       # Retrofit/OkHttp DI
│   │   │   ├── DatabaseModule.java      # Room DI
│   │   │   └── AppModule.java           # App-wide DI
│   │   ├── models/
│   │   │   ├── entities/               # Room entities
│   │   │   ├── request/                # API request bodies
│   │   │   └── response/               # API response models
│   │   ├── services/
│   │   │   ├── MyFirebaseMessagingService.java  # FCM push notifications
│   │   │   └── NotificationHelper.java          # Notification channels/display
│   │   ├── ui/
│   │   │   ├── activities/             # Activities (Splash, Login, Register, etc.)
│   │   │   ├── fragments/              # Fragments (Dashboard, Tests, Results, etc.)
│   │   │   └── adapters/               # RecyclerView adapters
│   │   ├── utils/                      # Utility classes
│   │   └── viewmodel/                  # ViewModels
│   └── res/
│       ├── drawable/                   # Vector drawables and XML graphics
│       ├── font/                       # Custom fonts (Montserrat family)
│       ├── layout/                     # XML layouts
│       ├── menu/                       # Menu resources
│       ├── navigation/                 # Navigation graph
│       ├── values/                     # Strings (Kinyarwanda), colors, themes
│       ├── values-en/                  # English strings
│       ├── values-fr/                  # French strings
│       └── values-night/               # Dark theme resources
├── build.gradle.kts                    # App-level build configuration
├── upload-keystore.jks                 # Release signing keystore (PKCS12)
└── upload_certificate.pem              # Google Play upload certificate

release/                                # Release build outputs (gitignored)
```

---

## 🌐 Localization

The app supports **3 languages** with full UI translation:

| Language | Locale | Resource Directory |
|---|---|---|
| **Kinyarwanda** (default) | `rw` | `res/values/` |
| **English** | `en` | `res/values-en/` |
| **French** | `fr` | `res/values-fr/` |

The exam content (questions and answers) is also available in all three languages via bundled JSON assets at `base/assets/json_exams/`.

---

## 🔧 Build Variants

| Variant | Description |
|---|---|
| **debug** | Unoptimized, debuggable, unsigned — for development |
| **release** | Minified (R8/ProGuard), shrunk resources, signed — for Play Store |

### Build Commands

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Release App Bundle (for Play Store)
./gradlew bundleRelease

# Clean build
./gradlew clean

# Run lint checks
./gradlew lint
```

---

## 🔄 CI/CD

The project includes a GitHub Actions workflow at `.github/workflows/android-ci.yml` for automated Android CI builds. The workflow runs on pull requests and pushes to the main branch.

---

## 📄 License

**Proprietary** — All rights reserved.

This project is proprietary software owned by Action Driving School. Unauthorized copying, modification, distribution, or use of this software is strictly prohibited.

---

<div align="center">
  <sub>Built with ❤️ for Rwandan drivers</sub>
  <br>
  <sub>© 2026 Action Driving School. All rights reserved.</sub>
</div>
