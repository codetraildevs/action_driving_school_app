# Refactor Exam Module: Replace Online API with Local Multilingual JSON Assets

I need you to completely refactor the exam system of my  Android  application.

## Goal

Remove all dependencies on the online exam/test APIs and make the entire exam module work 100% offline using local JSON files and local images stored in the Flutter assets folder.

The user experience, UI design, navigation, timer, scoring, and exam flow should remain unchanged. Only the data source should change.

---

## Current API Endpoints (To Remove)

The application currently uses these endpoints:

* GET `tests`
* GET `tests/{testId}/questions`
* POST `tests/{id}/attempt`
* POST `tests/attempts/{attemptId}/submit`

These endpoints should no longer be called anywhere in the application.

Remove or replace:

* Test API services
* Repositories that fetch exams from the server
* Online attempt creation
* Online answer submission
* Any network requests related to exams

---

## New Offline Data Source

The application already contains local exam files inside:

```text
assets/json_exams/
```

Files:

```text
assets/json_exams/en_exams.json
assets/json_exams/fr_exams.json
assets/json_exams/rw_exams.json
```

These files contain the complete exam data.

---

## Multilingual Requirement (Very Important)

The app already supports three languages:

* English (en)
* French (fr)
* Kinyarwanda (rw)

The exam content must automatically follow the language selected by the user.

### Required Behavior

If the user selects:

* English → load `en_exams.json`
* French → load `fr_exams.json`
* Kinyarwanda → load `rw_exams.json`

The app must never show English exams while French or Kinyarwanda is selected.

Whenever the language changes:

1. Reload the appropriate JSON file.
2. Refresh exam lists.
3. Refresh questions.
4. Keep all exam content synchronized with the selected application language.

The language selection already exists in the app. Reuse the existing language provider, localization service, or state management solution.

---

## Question Images

Question images are stored locally in:

```text
assets/json_questions_images/
```

Examples:

```text
ex1q4.jpg
ex11q5.jpg
ex12q3.jpg
ex12q4.jpg
ex12q5.jpg
ex12q6.jpg
ex12q16.jpg
exam1.png
exam2.jpg
exam3.jpg
exam4.png
```

Load images using android assets:

```dart
Image.asset(...)
```

Do not fetch images from a server.

If an image is missing:

* not all question have image.
* Do not crash the app.

---

## Create Local Exam Repository

Create a new repository such as:

```dart
LocalExamRepository
```

Responsibilities:

* Load exams from assets.
* Parse JSON.
* Return exams based on selected language.
* Return questions for selected exams.
* Cache loaded data for better performance.

Use:

```dart
rootBundle.loadString(...)
```

for reading JSON files.

---

## Offline Exam Flow

Replace the current server-based flow:

```text
Fetch Tests
→ Fetch Questions
→ Create Attempt
→ Submit Attempt
→ Receive Result
```

with:

```text
Load Local JSON
→ Display Exams
→ Display Questions
→ Save Answers Locally
→ Calculate Score Locally
→ Display Results
```

No backend communication should happen.

---

## Local Scoring

Calculate locally:

* Total questions
* Correct answers
* Incorrect answers
* Skipped questions
* Percentage score
* Pass/Fail result

All calculations must happen on the device.

---

## Preserve Existing Features

Keep all existing:

* UI screens
* Navigation
* Exam timer
* Question navigation
* Progress indicators
* Animations
* Result screens
* Language selector

Only change the source of exam data.

---

## Performance Requirements

Implement caching so JSON files are not reloaded every time the user opens an exam.

Example:

```dart
Map<String, dynamic> cachedExams;
```

Load once and reuse when possible.

---

## Final Expected Result

The entire exam module should function fully offline using only:

```text
assets/json_exams/en_exams.json
assets/json_exams/fr_exams.json
assets/json_exams/rw_exams.json

assets/json_questions_images/*
```

The displayed exams and questions must always match the user's selected language, and there should be zero dependency on the online Tests API.
