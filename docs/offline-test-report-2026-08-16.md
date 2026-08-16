# On-Device Offline Test Report — 2026-08-16

Tested on a real phone (Infinix, Android, `adb` connected) using actual airplane
mode (not merely disabling Wi-Fi after launch), per fix_issue_update2.md §21.

## Scenarios verified

| # | Scenario | Result |
|---|---|---|
| A | Fresh install + no internet | PASS — opens to Welcome/login, no crash, controlled UX |
| B | Open app with no internet | PASS — straight to cached dashboard, no login prompt |
| C | Open exam with no internet | PASS — exam list (Ikizamini 1–6) from local JSON |
| D | Complete exam with no internet | PASS — answered, navigated, submitted via confirmation dialog |
| E | View previous tests with no internet | PASS — result persisted to Room (verified via pulled SQLite) |
| F | Change language with no internet | PASS — rw→fr switch; whole UI + exams in French |
| G | Lose connection mid-exam | PASS — everything local, no interruption |
| H | Complete exam while offline | PASS — result screen rendered offline |
| I | Internet returns after offline exam | PASS — app healthy, profile refreshes, language persisted |
| J–K | Pending attempt sync / retry | N/A — no exam-submission endpoint exists (device-local by design) |
| L | App killed while offline | PASS — force-stop + relaunch offline: session, language, Room data survived |
| M | Device restarts while offline | SKIPPED (user chose not to reboot the phone) |
| N | Subscription + no internet | PASS — cached access level/expiry shown, no error toast |
| O | No cached data + offline | PASS — controlled login screen; materials show offline message |

## Issues found

1. **Previous Tests screen unreachable** (FIXED this session)
   - `ResultsFragment` (test-history list) existed in code but was not in the
     navigation graph, so no UI could reach it. The dashboard "Tests effectués"
     subtitle and the exams screen both opened the exam grid instead.
   - **Fix**: added `resultsFragment` to `main_nav_graph.xml`, made the
     "Tests effectués" subtitle in the dashboard Exams card a separate click
     target, and added a history toolbar icon on the exams screen.
   - **Verified on device**: history now opens showing "Total: 2 tests,
     Avg 30%, Pass 50%" with per-exam entries (Review / Retake actions).

2. **PDF page counter off-by-one after go-to-page** — entering page 100 shows
   "98 / 235" until scrolled (uses `findFirstVisibleItemPosition`). Cosmetic;
   left as-is.

3. **"Demander un code d'accès à l'examen" card subtitle** — it is the subtitle
   of the *Irembo services* card (string `all_services`), so tapping it opens
   Irembo services. Misleading label, not a functional bug.

4. **WhatsApp groups empty** — app shows "Aucun groupe WhatsApp actif trouvé."
   with a Retry button. Server-side presence of groups could not be verified
   (token is encrypted; endpoint 401 without auth). Check the admin console.

## Other verification

- 0 fatal exceptions across the whole session (logcat).
- Exam scoring validated with real data: an exam completed mid-test scored
  12/20 (60%, passed) and was saved to Room with correct counts/timing.
- PDF viewer rendering fixes confirmed on-device: white page background
  (255,255,255 sampled), no blank space below content, in-app go-to-page and
  bookmark dialogs working.
- Irembo license request form opens prefilled with account data and validates
  inputs (not submitted — would create a real application).
- Room persistence confirmed by pulling the SQLite DB after offline exam and
  after kill/restart.

## Artifacts

Test artifacts (screenshots, UI dumps, DB pulls) are kept locally under
`.offline_test/` and are gitignored — they contain personal data (account
phone number, token backups) and are not suitable for the repository.
