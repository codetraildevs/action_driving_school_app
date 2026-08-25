I have a native Android application written in Java.

The APK builds and installs successfully, but the app has serious runtime issues:
- Sometimes it closes immediately after opening.
- Sometimes Android shows "App keeps stopping."
- Sometimes it crashes after I enter login information.
- Sometimes it unexpectedly returns to the previous screen.

Perform a complete debugging and fix the actual root cause instead of guessing.

Tasks:
1. Analyze the entire project for runtime crashes.
2. Check AndroidManifest.xml for incorrect activity declarations, permissions, and exported attributes.
3. Review all Activities, Fragments, Services, BroadcastReceivers, and Intents.
4. Find all possible NullPointerExceptions, IllegalStateExceptions, ClassCastExceptions, NumberFormatExceptions, and IndexOutOfBoundsExceptions.
5. Verify login logic, API requests, Retrofit/Volley/OkHttp configuration, JSON parsing, and authentication flow.
6. Check SQLite/Room database initialization and migrations.
7. Verify SharedPreferences usage and ensure null values are handled safely.
8. Check Firebase initialization, google-services.json, SHA fingerprints, and dependencies (if Firebase is used).
9. Verify internet and other required permissions.
10. Review Gradle dependencies for conflicts or outdated libraries.
11. Check ProGuard/R8 rules if the Release APK crashes but Debug works.
12. Ensure all background threads safely update the UI.
13. Add proper try-catch blocks where appropriate without hiding the real errors.
14. Add detailed Logcat logging to identify the exact crash location.
15. Fix lifecycle issues in Activities and Fragments.
16. Ensure every Activity starts correctly and no Intent extras are null.
17. Check for memory leaks or OutOfMemoryError.
18. Remove any code that unintentionally calls finish(), System.exit(), or causes the app to close.

Required output:
- Identify the exact file and line causing the crash.
- Explain why it crashes.
- Provide the corrected Java code.
- Ensure the app launches successfully, login works correctly, and no screen crashes.
- Verify both Debug and Release APKs work without runtime crashes.

Do not guess. Use Logcat stack traces and code analysis to determine the real root cause before making changes.