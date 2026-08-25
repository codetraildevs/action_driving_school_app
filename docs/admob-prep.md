# AdMob Prep — status & checklist

Everything needed before Google AdMob can serve ads in the app, with what is
already implemented on branch `feature/admob-prep` and what still needs
account/hosting steps.

---

## ✅ Done in this branch (code)

### 1. Google Analytics for Firebase (GA4) — required by AdMob

New AdMob accounts must be linked to a Firebase project; AdMob reads GA4
events for revenue/audience features.

- Added `firebase-analytics` (via the Firebase BOM) to `gradle/libs.versions.toml`
  and `app/build.gradle.kts`.
- New `app/src/main/java/com/drivingschoolrwandaapp/utils/AnalyticsUtils.java` —
  a null-safe GA4 wrapper (never crashes, no-ops when unavailable).
- Events wired in:
  | Event | Where |
  |---|---|
  | `exam_started` | `TestQuestionsFragment` when the countdown starts |
  | `exam_completed` | `TestResultFragment` when the result is shown (score, passed, correct/wrong/skipped, duration) |
  | `payment_method_selected` | `PaymentUtils` when a MoMo/MTN/Airtel card is tapped (method + amount) |
  | `payment_instructions_viewed` | `TestsFragment` when the payment dialog opens |
  | `subscription_requested` | `TestsFragment` when the user confirms a plan (test number, days, price) |
  | `irembo_request_submitted` | `LicenseRequestActivity` / `SpecialRequestActivity` (type) |
  | `screen_view` | Manual logging for `dashboard`, `tests`, `materials`, `profile`, `results` fragments (activities are auto-tracked) |
- User identification: `App` sets `user_id` (phone) once the profile loads.

**Verify:** build & install the debug APK, do a test, then check
Firebase Console → Analytics → DebugView (enable debug mode:
`adb shell setprop debug.firebase.analytics.app com.drivingschoolrwandaapp`).

### 2. Play Integrity API — AdMob integrity policy + backend anti-fraud

- Added `com.google.android.play:integrity:1.6.0` dependency.
- New `app/src/main/java/com/drivingschoolrwandaapp/utils/IntegrityHelper.java` —
  fetches an integrity token with a fresh random nonce and POSTs it to the
  backend. Never throws; reports via callback.
- New backend route `DRIVING_SHOOL_COMPANY_LEGACY/app/api/integrity/verify/route.ts`
  — verifies the token with Google, checks nonce + verdicts
  (`PLAY_RECOGNIZED`, `MEETS_DEVICE_INTEGRITY`/`STRONG`), returns
  `{ verified, requestId, ... }`. Protected by the existing middleware
  (requires a Bearer token).

**Remaining (account/cloud steps below):** enable the API in Play Console,
deploy the backend route, then call `IntegrityHelper.attest()` at login /
subscription / Irembo submit and act on `verified == false`.

---

## 🔲 Account & hosting steps (no code)

### 3. Play Console — enable Play Integrity API

1. Play Console → your app → **Setup → App integrity** (Play Integrity).
2. Follow the steps to enable the API and **link a Google Cloud project**
   (it gives you a cloud project number; you must own/be admin of the project).
3. The app's upload key / signing key is registered automatically by Play.

### 4. Google Cloud — API key + service account

1. In the linked Google Cloud project, enable the **Google Play Integrity API**.
2. Create an **API key** (Credentials → Create credentials → API key).
3. Put it in the backend `.env`:
   ```
   GOOGLE_CLOUD_API_KEY=AIza...
   ```
4. Deploy the backend (`docs/redeploy-backend-cpanel.md`) and verify:
   ```bash
   curl -s -X POST https://console.amategekoyumuhanda.rw/api/integrity/verify \
     -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" \
     -d '{"integrityToken":"x","nonce":"eA=="}'
   # → {"verified":false,"error":"google_verify_failed"}  (Google rejects the junk token; that's correct)
   ```

### 5. App-ads.txt (when creating the AdMob account)

AdMob gives you an **app-ads.txt** file. Publish it at
`https://amategekoyumuhanda.rw/app-ads.txt` (and
`https://console.amategekoyumuhanda.rw/app-ads.txt` if that domain hosts
content) — upload the file in cPanel File Manager next to the site root.
Without it AdMob warns and your ad revenue is exposed to fraud.

### 6. Play listing disclosures

- Play Console → **App content** → complete the **Data safety form** and
  declare **"Contains ads" = yes** once ads are live.
- Privacy policy URL is already hosted and linked in-app
  (`amategekoyumuhanda.rw/privacy-policy`).

---

## 🔜 With the AdMob SDK (the "few days later" step)

### 7. UMP consent SDK (GDPR/CCPA)

AdMob flags apps with no consent collection. When you add the Google Mobile
Ads SDK, also add `com.google.android.ump` and run the consent form flow on
startup before requesting ads. Only needed because some users are in the
EEA/UK; Rwanda is not affected but the SDK covers everyone.

### 8. Ad placements (recommended)

- **Rewarded ads** fit this app best: "watch a video to unlock a test for
  24h" or "reveal an answer". Better UX and higher revenue than forced ads.
- Banner on the dashboard bottom and the materials list.
- Interstitial after finishing an exam (on the result screen transition).
- **Never show ads to paying subscribers** — check the user's subscription
  before requesting ads (you already have `UserSubscription`).

### 9. AdMob account checklist

1. Create the AdMob account (link it to the same Firebase/Google account).
2. Add the app (package `com.drivingschoolrwandaapp`).
3. Link Firebase Analytics (must be the project in `app/google-services.json`).
4. Create ad units (banner, interstitial, rewarded) — use **test ad unit IDs**
   while developing and your device as a test device.
5. Add your bank/payment details to get paid.
6. Publish `app-ads.txt` (step 5) and verify in AdMob.
