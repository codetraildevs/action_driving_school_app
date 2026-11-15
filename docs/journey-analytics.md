# Privacy-safe user journey tracking

## Build configuration

Supply the real `app/google-services.json` from the Firebase project before building.
The file stays ignored by Git; the example file is not a working production configuration.
Verify that its Android package matches `com.drivingschoolrwandaapp` and that the installed
app sends events to the same Firebase project opened in DebugView.

Language changes use AppCompat's locale configuration update instead of launching the splash
screen. Hosts with a language picker handle the configuration change without recreating the
activity window. Visible fragment views are rebound in the same navigation stack, while the
welcome screen restores its carousel page and scroll position. Auth screens use coordinated
light/dark surfaces and foreground colors rather than a branded background on every window.
The welcome carousel pauses while the language picker is open, and the saved language is
applied before splash UI creation on the next launch.

## What is recorded

| Event | Trigger | Parameters |
| --- | --- | --- |
| `app_launch` | First app-owned activity created in a process; background service startup alone is excluded | `scope=process_first_activity` |
| `journey_screen` | App-owned activity or top-level navigation fragment resumes | `screen`, `screen_type` |
| `login_attempt`, `login_success`, `login_failure` | Login API request and response/failure, including automatic login after signup | `source`, `failure_reason` |
| `registration_attempt`, `registration_success`, `registration_failure` | Signup API request and response/failure | `source`, `failure_reason` |
| `login_validation_failure`, `registration_validation_failure` | Form fails local validation before an API request | `source`, `failure_reason=invalid_input` |
| `first_exam_started` | First observed non-review exam with loaded questions, once per installation since tracking was introduced | `scope=installation_since_tracking` |
| `rewarded_ad_selected`, `rewarded_ad_unavailable`, `rewarded_ad_shown`, `rewarded_ad_earned`, `rewarded_ad_dismissed`, `rewarded_ad_show_failed` | Rewarded-video/interstitial access lifecycle | `ad_format`, `reward_earned` |

Existing exam-start/completion, payment instructions, subscription request, and Irembo request events remain available. `journey_screen` covers splash, welcome, authentication, dashboard, exams, history/results, materials, profile, Irembo, WhatsApp, and the app's viewer activities. It records screen visits, not typed text or every button press. Third-party ad activities and individual question-page fragments are excluded.

`source` distinguishes `login_form`, `registration_form`, and `registration_auto_login`. A signup may succeed while its automatic login fails; keep these as separate funnel steps. `failure_reason` uses a fixed allowlist such as `network`, `timeout`, `device_restricted`, `account_exists`, `account_not_found`, `invalid_credentials`, `rate_limit`, `server`, `rejected`, and `canceled`. Generic or localized server errors may fall into `rejected`; these categories are diagnostic clues, not authoritative server error codes.

Validation failures are separate from API-attempt failures. Screen visits can repeat on resume/rotation. Use distinct users in a funnel rather than treating screen-event counts as unique people. Returning authenticated users can skip login altogether. A process launch is not a session count; Firebase's existing automatic session events measure sessions.

## Privacy and limitations

- New funnel payloads contain only fixed event names, categories, and app-owned screen class names. They do not contain phone numbers, names, device IDs, account IDs, passwords, tokens, raw error messages, screen text, or URLs.
- Removed phone-based Analytics/Crashlytics user IDs and the Crashlytics name field. Clear old identifier fields for future events on application startup; this does not delete previously uploaded records or guarantee removal of older queued SDK records.
- Firebase's own pseudonymous app-instance identity and automatic metadata still apply. This is not zero-data analytics. Existing SDK collection settings remain in force; this change does not force-enable Analytics collection or reset consent.
- First exam means first **observed since tracking**, including an existing user who upgrades. It is not their lifetime first exam. A local flag prevents repeats, survives ordinary restarts, and is excluded from backup/transfer. Clearing app data or reinstalling permits a new first-observed event. SDK submission does not guarantee server delivery, and disabled collection can suppress events.
- Do not infer that a user uninstalled solely because no next event appears. They may be offline, have analytics disabled, or return later.

## Configure reports yourself in Firebase / GA4

1. Use the Firebase project's linked GA4 property. Events begin only after this code is installed; there is no historical backfill.
2. In GA4 Admin → Custom definitions, register event-scoped dimensions for `failure_reason`, `source`, `screen`, `screen_type`, `ad_format`, and `reward_earned` if you want these breakdowns in reports.
3. Create separate Funnel explorations, using indirectly-followed steps:
   - New-user signup: `journey_screen` filtered to `RegisterActivity` → `registration_attempt` → `registration_success` → `login_success` with `source=registration_auto_login` → `first_exam_started`.
   - Existing-user login: `journey_screen` filtered to `LoginActivity` → `login_attempt` with `source=login_form` → `login_success` → `exam_started`.
   - General activation: `app_launch` → dashboard or exam-list screen → `exam_started` → `exam_completed`. Do not require login for users who already have a session.
   - Rewarded retake: `rewarded_ad_selected` with `ad_format=interstitial` → `rewarded_ad_shown` → `rewarded_ad_earned` → `exam_started`.
4. Break down failures by `failure_reason`, app version, and country. Compare release cohorts and matching date ranges, not raw event counts across different windows.
5. Validate with DebugView before interpreting production reports. Standard custom-dimension reporting can take 24–48 hours after collection and dimension creation.

On a connected test phone:

```powershell
adb shell setprop debug.firebase.analytics.app com.drivingschoolrwandaapp
# Open Firebase → Analytics → DebugView, then exercise the app normally.
adb shell setprop debug.firebase.analytics.app .none.
```

Check: cold launch and navigation; rejected login; successful login; failed signup; successful signup and auto-login; first exam followed by a second exam; rewarded completion and skip. Inspect the event parameters for absence of personal data. Do not clear a real user's account to test signup; use an eligible test device/account.

References:
- https://firebase.google.com/docs/analytics/userid
- https://firebase.google.com/docs/analytics/debugview
- https://support.google.com/analytics/answer/14239696
- https://support.google.com/analytics/answer/9327974

No GA4 dashboard or custom definitions were created remotely. No historical analytics records were deleted.
