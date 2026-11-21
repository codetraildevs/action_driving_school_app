# Optimization audit and delivery record

Date: 2026-11-19. Baseline: eb38966, version 1.5.15 (116).
Update: user authorized a new release; implementation target is now 1.5.16 (117).
This is an initial, incomplete audit and staged delivery record, not a release certification.
Later implementation updates below supersede the initial audit table; unresolved items remain explicit.
The current version follows the user's explicit release instructions; the older version rule in writing-block.md is superseded.

## A. Evidence and priorities

| Priority | Evidence | Impact and next step |
| --- | --- | --- |
| High | PdfAdapter.bind calls System.gc in the UI callback and assigns results without checking holder reuse | Remove forced collection and reject stale rendered pages. Implemented in this stage. |
| High | PdfViewerActivity.setupPdfRenderer copies the complete file during onCreate | Main-thread disk/provider I/O. Move ownership of loading, renderer, cancellation and cleanup to a background worker together; avoid introducing close/render races. Pending. |
| High | PdfAdapter opens page metadata on main thread under the same lock used by rendering | Scrolling can wait for background rendering. Move metadata acquisition off main thread with rendering. Pending. |
| High | PdfViewerActivity closes the renderer without coordinating the adapter worker | Potential renderer-close race. Requires lifecycle regression coverage. Pending. |
| High | AppDatabase enables allowMainThreadQueries and fallbackToDestructiveMigration | Audit every synchronous DAO caller and historical migration before removing either option; preserve user records. Pending. |
| High | SplashActivity schedules navigation after five seconds independently of consent UI | Potential consent form/activity lifecycle conflict. Test slow-network and visible-form scenarios before changing flow. Pending. |
| Medium | AdManager deduplicates in-flight loads but has no failed-load cooldown | Repeated user actions can request ads again immediately after no fill. Add and test per-format backoff without bypassing rewards. Pending. |
| Medium | English, French and Kinyarwanda resource directories exist; no Swahili directory | Do not advertise Swahili support until translation and content QA are complete. |
| High | Earlier Play crashes contain obfuscated wd.y and PhoneWindow frames | Exact affected-build mappings and full exception chains required; defensive null checks do not prove those crashes fixed. |

Existing strengths: background local exam loading in TestRepository, singleton injected OkHttp client, production HTTP logging disabled, system certificate trust with debug-only user certificates, consent eligibility gates, two-minute normal-interstitial interval, SDK earned-reward listener, baseline profile module, and Firebase journey analytics.

## B. Implemented in this stage

Follow-up completion pass: removed allowMainThreadQueries after tracing active writes to repository executors and reads to LiveData; synchronous UserDao getters have no production callers. Subscription persistence now completes in the NetworkBoundResource worker instead of scheduling another asynchronous save and reporting success early. Empty successful HTTP responses and persistence exceptions now emit an error with cached data rather than leaving loading state or throwing on the executor.

Administrator authentication: backend login now always verifies the stored password, eliminating the clientType-based bypass. Android login adds an optional administrator-password field (students leave it blank); device ID remains the student's credential. The field is not persisted via view-state saving or autofill. Six backend helper tests pass. These source changes do not secure the deployed server until deployed; older Android admin clients will need the updated app. Existing issued tokens are not automatically revoked by this fix. Review active privileged sessions when deploying.

Store listing delivery: docs/play-store-listing-update.md provides English/French and a Kinyarwanda draft requiring native review. Programmatic checks: all titles 21/30 characters; short descriptions 74, 76 and 68/80; full descriptions 1014, 1123 and 772/4000.

Release 117 follow-up: PDF file copying and renderer creation execute on a dedicated I/O executor; per-viewer temporary files prevent cross-viewer overwrite. Destruction detaches the adapter, cancels pending rendering and queues renderer cleanup after loading, with the renderer lock protecting in-progress renders. Page metadata is read with rendering on a single worker rather than opening a page on the UI thread. Render dimensions preserve aspect ratio while respecting both dimension limits. Menus ignore document actions until the adapter is ready. Failed full-screen ad requests now have a separate 30-second retry cooldown per format; reward eligibility and the two-minute transition frequency are unchanged.

ADB successfully queried on 2026-11-19: no connected devices. No device benchmark is available yet.

Startup: removed the duplicate UMP request and five-second navigation timer from SplashActivity. App already requests consent on its lasting activity and all AdManager requests remain consent-gated. Welcome/login/register/admin contain no AdManager calls. This removes consent-network waiting from the splash path; no measured startup speedup is claimed.

Backend inspection used an isolated checkout of backend-deploy f2b9838. Login phone-variant reads are batched into a single findMany query, preserving variant and ascending-ID priority. Three candidate-order regression tests pass. Repository-wide TypeScript checking fails in multiple modules, so this backend patch is not deployment-certified. A serious pre-existing admin login issue was found: clientType=android_app bypasses password verification for admin roles. This requires a coordinated authentication change and must not be described as safe or fixed by query optimization. No production credentials or database were accessed.

PdfAdapter.java: removed explicit GC from rendered-page delivery. Added per-holder binding generations, invalidated on reuse/recycle, and adapter detach rejection. Only undelivered stale bitmaps are explicitly recycled; displayed images remain managed by Android. No change to ads, access entitlements, scores, stored progress, credentials or version.

## C. Measurements

No device startup, frame timing, memory or retention improvement has been measured. No percentage speedup is claimed. Baseline unit-test reports from the preceding verification contained 435 tests, zero failures/errors. New verification is recorded separately when finished. Signed release artifact 116 predates this optimization and must not be represented as containing it.

## D. Store listing draft

Google limits: title 30 characters, short description 80, full description 4,000.
Source: https://support.google.com/googleplay/android-developer/answer/9859152
Metadata requirements: https://support.google.com/googleplay/android-developer/answer/9898842

Title: Action Driving School

English short description: Practice Rwanda driving theory with exams, road signs and study materials.

English full description:

Prepare for driving theory study in Rwanda with Action Driving School. Practice exam questions, study road signs and traffic rules, and review your results as you learn.

Choose English, French or Kinyarwanda. Explore practice exams and learning materials, revisit your exam history and practise again with retakes. Some content requires access through the options shown in the app. Eligible content may offer rewarded ads; ad availability can vary.

The learning content focuses on Rwanda. Driving rules and licensing requirements differ by country, so this app should not be used as a substitute for another country's official guidance. Practice results do not guarantee a licensing exam result.

French short description: Révisez le code de la route au Rwanda avec des examens et supports de cours.

French full description:

Préparez vos révisions du code de la route au Rwanda avec Action Driving School. Entraînez-vous avec des questions, étudiez les panneaux et les règles de circulation, puis consultez vos résultats.

Choisissez le français, l'anglais ou le kinyarwanda. Retrouvez les examens pratiques, les supports de cours et votre historique. Certains contenus nécessitent un accès selon les options affichées dans l'application. Des annonces avec récompense peuvent être proposées pour les contenus éligibles, selon leur disponibilité.

Le contenu concerne le Rwanda. Les règles et les conditions du permis diffèrent selon les pays. Les résultats des exercices ne garantissent pas la réussite à un examen du permis.

Candidate keywords (relevance hypotheses, not measured search volume): Rwanda driving theory, Rwanda driving test, road signs Rwanda; code de la route Rwanda, examen théorique Rwanda; amategeko y'umuhanda. Validate Kinyarwanda phrasing with a qualified reviewer. Do not target other countries' licensing exams without verified local content.

Screenshot sequence: Practice Rwanda driving theory / Study road signs / Review your exam results / Learn in three languages. Use real current screens; include truthful access/ad messaging. Feature graphic concept: existing brand, one readable road sign motif, and Rwanda theory-study positioning. Icon visual review remains pending.

Proposed category: Education; select only available Console tags that accurately describe the app. Run one store-listing experiment at a time: first screenshot, then short description, then icon. Compare the same country/language/traffic source and date windows. Monitor listing conversion alongside login success, registration success, first exam starts and seven-day return cohorts. Avoid claiming a keyword caused growth from an uncontrolled before/after comparison.

## E. International expansion design

Keep current exams assigned to Rwanda. Future packages should have countryCode, locale, contentVersion, jurisdiction, source references, reviewedAt and reviewer fields. Separate universal safety topics from legal rules. Key future progress records by country/package plus exam ID to avoid collisions. Introduce this through additive migrations with regression tests, not replacement of current exam IDs.

Priority language work: verify existing English/French/Kinyarwanda coverage and fallback; commission reviewed Swahili UI and educational translations before enabling it. No verified non-Rwanda licensing package exists in this audit. International users may study Rwanda content, but must see its jurisdiction explicitly.

Console/external tasks: verify enabled countries, current store text, Data Safety responses, privacy-policy consistency, content rights and regional ad requirements. Repository inspection cannot certify account settings or legal suitability for every country. No countries or live listings have been changed.

## F. Ads and privacy

Preserve existing placements, two-minute transition interval and rewarded access rules. AD_ID is present in the source manifest. ConsentHelper uses canRequestAds; that result is ad-request eligibility, not proof of permission for personalized ads. App initialization clears explicit analytics identity and Crashlytics user name/id fields. This does not mean SDKs collect no identifiers. Full event-parameter and policy audit remains pending.

## G. QA and remaining work

Latest follow-up verification: Android testDebugUnitTest, lintVitalRelease and bundleRelease succeeded after the database/network/admin-login changes. Use ACTION-DRIVING-SCHOOL-1.5.16-CODE-117-UPDATED.aab rather than the earlier PERFORMANCE candidate. Backend authentication fix is local commit 8a33e7c on the review branch; it is not on backend-deploy or deployed. Full device/upgrade QA and backend integration validation remain pending.

Release 117 verification: `:app:testDebugUnitTest :app:lintVitalRelease :app:bundleRelease` completed successfully after all Android changes. 435 tests, zero failures/errors. Version 1.5.16 (117) and AD_ID confirmed in the merged release manifest. Jarsigner reports jar verified, with self-signed trust-chain, timestamp and ZIP stream consistency warnings. Artifact: ACTION-DRIVING-SCHOOL-1.5.16-CODE-117-PERFORMANCE.aab; SHA-256 B32473693D44CBB418271DEA30096667FCAC7C9453DA3B3FF0CD302697CAB437. This is a signed candidate pending device QA, not a claim that all audit findings are resolved.

2026-11-19 verification: `:app:testDebugUnitTest :app:lintVitalRelease` completed successfully after the PDF change. The unit-test task executed (not UP-TO-DATE). `git diff --check` passed. These checks do not substitute for PDF scrolling/lifecycle tests on a device. ADB did not return a usable device inventory during this stage, so device measurements were not collected.

Required device scenarios: rapidly scroll/recycle PDF pages; exit while rendering; rotate; reopen materials; change locale; test offline/slow network; complete reward then open exam/material; skip reward without unlocking; upgrade without losing history. Verify low-memory devices, accessibility text scaling, authentication and all integrations. These manual scenarios have not been executed for this stage.

## H. Release gate

Backend delivery update: merged codex/backend-login-query-optimization into backend-deploy and pushed merge 5ab9043 to origin on 2026-11-19. Six focused tests pass. Production compilation was attempted but failed with EPERM while scanning C:\Users\HP\Application Data; full TypeScript validation also remains unresolved. The server must build successfully before PM2 reload. No server deployment was performed. Android workspace remains on feature/admob-prep.

Complete the remaining audit, collect device measurements, verify migration safety and localization, and rerun device tests. The user has authorized 1.5.16 (117) for the new artifact. No upload, publication or deployment is authorized by this report.

Backend candidate-order tests: 3 passed. Local review commit: b11b3e7 on codex/backend-login-query-optimization. Full backend type checking fails across actions, components and shared API code; the filtered check reported no errors in the changed login/helper files. No database integration test or production deployment was performed.
