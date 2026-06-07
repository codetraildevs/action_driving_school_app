# Session Context — Codebuff (June 7, 2026)

## Project
- **Name:** Driving School Rwanda App (DRIVINGSCHOOL2)
- **Path:** `D:\software\DRIVINGSCHOOL2`
- **Stack:** Android (Java + Kotlin), Gradle (Kotlin DSL), Room, Dagger Hilt, Retrofit, Firebase
- **Git:** On branch `master`, working tree has uncommitted changes

---

## Work Completed

### 1. Java → Kotlin Data Class Conversion (~80 files)

Converted all Java model POJOs to Kotlin data classes across:

| Package | Files | Notes |
|---|---|---|
| `models/entities/` | 27 entities | POJOs → `data class` with `@SerializedName` |
| `models/request/` | 29 DTOs | API request classes → `data class` |
| `models/response/` | 11 DTOs | Includes generic `ApiResponse<T>`, `Resource<T>` |
| `database/entities/` | 11/12 Room entities | Room entities → Kotlin |
| `models/` root | 2 files | `IremboApplication`, `IremboService` |

**Key fixes applied:**
- `RegisterResponse` — used `private var _message` with custom getter to avoid JVM signature clash with `getMessage()`
- `Bookmark` (database/entities) — added `@JvmField` for Java direct field access (`bookmark.id`, `bookmark.name`)
- `TestsResponse` / `TestQuestionsResponse` — added `@SerializedName` (no longer extend `ApiResponse`)
- `Resource` — private constructor with companion factory methods
- `BookmarkEntity` — kept in Java due to 3-arg constructor incompatibility with Kotlin data class overloading
- Mappers (`SubscriptionMapper.java`, `TestMapper.java`) — kept in Java (MapStruct needs Java interfaces)

### 2. Local Exam Refactoring (Replace Online Tests with Local JSON)

**Goal:** Make the exam module work 100% offline using local JSON files and local images.

**Files created:**
- `app/src/main/assets/json_exams/en_exams.json` — English exam data
- `app/src/main/assets/json_exams/fr_exams.json` — French exam data
- `app/src/main/assets/json_exams/rw_exams.json` — Kinyarwanda exam data
- `app/src/main/assets/json_questions_images/*.jpg/png` — Exam question images
- `app/src/main/java/.../models/LocalExamModels.kt` — Data classes for JSON parsing (`LocalExamWrapper`, `LocalExam`, `LocalQuestion`)
- `app/src/main/java/.../repository/LocalExamDataSource.kt` — Loads/caches JSON from assets, multilingual support (EN/FR/RW)

**Files modified:**
- `app/src/main/java/.../repository/TestRepository.java` — Refactored to use `LocalExamDataSource` instead of API calls. Builds `TestEntity`, `TestWithQuestions`, etc. from parsed JSON
- `app/src/main/java/.../di/TestModule.java` — Updated DI to provide `LocalExamDataSource` and updated `TestRepository` dependency

**Build status:** ✅ `compileDebugKotlin` passes

**Known remaining issues:**
- `getCurrentLanguage()` in `TestRepository` is hardcoded to `"en"` — needs integration with app's language preferences for true multilingual support
- Image URLs stored as asset paths (`assets/json_questions_images/...`) but UI expects network URLs — need to convert to `file:///android_asset/` URIs
- Old API endpoints (`getTests()`, `getTestQuestions()`, `startTestAttempt()`, `submitTestAttempt()`) still in `ApiService.java` — should be removed
- `NetworkBoundResource.java` is now dead code — should be removed
- `LocalExamDataSource.loadJsonFromAssets()` swallows exceptions silently — should add logging

### 3. Session Persistence Guide

Provided step-by-step instructions for setting up **WSL2 + tmux** for persistent Codebuff sessions across Windows restarts.

---

## Project Architecture

```
com.drivingschoolrwandaapp/
├── api/              — ApiService (Retrofit), ApiClient, interceptors
├── data/             — Preferences, TokenManager
├── database/         — Room DB, DAOs, entities (now Kotlin)
├── di/               — Hilt modules
├── models/           — entities/, mappers/, request/, response/, LocalExamModels, IremboApplication, IremboService
├── repository/       — TestRepository, LocalExamDataSource, Resource, NetworkBoundResource (dead)
├── services/         — FirebaseMessaging, NotificationWorker
├── ui/               — activities/, adapters/, fragments/ (all Java)
├── utils/            — Helpers (Java)
└── viewmodel/        — TestViewModel, IremboViewModel, etc. (Java)
```

---

## Next Steps / Suggested Work

1. Fix multilingual exam loading — integrate `getCurrentLanguage()` with app's existing language preferences
2. Fix image URLs — convert asset paths to `file:///android_asset/` URIs
3. Clean up dead code — remove unused API endpoints and `NetworkBoundResource.java`
4. Convert remaining Java files to Kotlin (activities, fragments, adapters)
