# Live Schema Diff Report — hosted DB vs current role-based schema

**Source:** `docs/sxlvhdzo_driving_school.sql` — the phpMyAdmin export of the hosted DB
(`sxlvhdzo_driving_school`, MariaDB 10.11.18 on cPanel) provided by the user.
**Method:** imported into a local scratch MySQL and compared against the current schema
(`DRIVING_SHOOL_COMPANY_LEGACY/prisma/schema.prisma`, mirrored by `docs/production-schema-sync.sql`).

---

## ✅ Headline result: the hosted DB is ALREADY fully role-based — nothing is missing.

| Check | Hosted DB | Verdict |
|---|---|---|
| Table set | 51/51 tables, **identical** to schema (case-insensitive) | ✅ match |
| Column set | 405/405 columns across all tables, **identical** | ✅ match |
| 4 case-sensitive tables | `DataDeletionRequests`, `loginAttemps`, `privacyConsent`, `WhatsAppGroup` — **already correctly cased** on the host | ✅ no rename needed |
| `users.role` column | present (`int NOT NULL`), FK `users_role_fkey` → `user_roles` | ✅ present |
| `users.pendingLanguage` / `userTimezone` | present, FKs to `languages` / `timezones` | ✅ present |
| `user_roles` seed data | **10 roles** (1 Super Admin … 10 Guest) | ✅ seeded |
| Admin users | 1 admin (role 2: Alexis, `0732657995`), 3,538 students (role 5) | ✅ real admins exist |
| Devices | 3,539 devices; **0** non-admin users missing a device (login's `user.devices[0]` won't crash) | ✅ |
| Real content | 313 questions, 21 tests, 182 languages, 597 timezones | ✅ production data intact |

### ⚠️ IMPORTANT — do NOT run `docs/production-schema-sync.sql` against the hosted DB

Production **already matches** the schema, and the sync script's hardcoded seeds use the **local**
ids, not the production ids:

| Reference | Local seed id | Production id |
|---|---|---|
| English | 1 | **41** |
| French | 2 | **48** |
| Kinyarwanda | 3 | **85** |
| Africa/Kigali | 1 | **30** |
| UTC | 2 | **594** |

- `languages.language_code` is UNIQUE → `INSERT IGNORE` for en/fr/rw is skipped (safe).
- `timezones.timezone_name` is **NOT** UNIQUE → the script would insert **duplicate** timezone
  rows (ids 1–3 alongside 30/594). **Do not run it.**

---

## ⚠️ LATENT BUG found — role names on the host don't match the backend's exact-match check

| role id | Hosted `role_name` (Title Case) | Backend `isAdminRoleName()` expects | Works? |
|---|---|---|---|
| 1 | `'Super Admin'` | `"super_admin"` | ❌ **exact match fails** → super-admin locked out of console |
| 2 | `'admin'` | `"admin"` | ✅ |
| 3 | `'Content Manager'` | — | n/a (not a console role) |
| 5 | `'Student'` | — | n/a |

`isAdminRoleName(roleName)` does `["admin","super_admin"].includes(roleName)` — a **case-
and-format-sensitive** exact match. Today the only real admin is role 2 (`'admin'`), which matches,
so login works. But the Android app routes admins by **numeric id** (1 or 2), so a future role 1
user would be sent to the console by the app and then **rejected with 403 by the backend**.

**Fix (recommended):** make the check tolerant — lowercase + strip spaces/underscores before
comparing, or compare by role **id** (`role === 1 || role === 2`) consistently on the backend.
See `DRIVING_SHOOL_COMPANY_LEGACY/lib/auth/roles.ts` and every `/api/admin/*` route.

---

## So what IS wrong?

The schema was never the problem. The role-based merge (two apps → one) is a **backend + Android**
change, and the hosted DB already supports it. The login failure the user reported most likely comes
from one of these — in this order of likelihood:

1. **Stale backend deployment on the host.** The hosted Next.js app must be running the current
   code: `POST /api/auth/login` returns `user.role` / `roleName` (needed for role-based routing),
   `/api/auth/register` hard-codes student role, and `/api/admin/*` has the JWT role guard. If the
   host runs an older build, admins can't be routed to the console and login responses lack `role`.
2. **Stale `DATABASE_URL` credentials.** The `.env` in `DRIVING_SHOOL_COMPANY_LEGACY/.env` is
   rejected from this machine (`Access denied for 'sxlvhdzo_admin'@'197.157.165.93'`). The hosted
   app connects from the server (localhost), which may use different/newer credentials. Update the
   `.env` (or cPanel Remote MySQL) so local dev and CI can reach the real DB.
3. **Android app pointing at the wrong backend / old API contract.** Confirm the app's base URL
   hits the deployed role-based backend.

> 📝 **Method note:** the column comparison compared column **names** (405/405). Types/nullability
> were spot-checked on key tables (`users.role int NOT NULL`, FKs present); a full type-level
> `information_schema.COLUMNS` diff is possible once the `.env` creds work (see
> `docs/live-schema-diff.sh`).

### Suggested next steps

```bash
# 1. Verify the hosted backend responds with role data (from a browser/curl):
#    POST https://<your-host>/api/auth/login  {"identifier":"0732657995","password":"..."}
#    -> expect JSON with  user: { role: 2, roleName: "admin", ... }

# 2. Redeploy the Next.js backend from DRIVING_SHOOL_COMPANY_LEGACY if the response
#    lacks "role" / "roleName" or /api/admin/* returns 404/500.

# 3. Fix DRIVING_SHOOL_COMPANY_LEGACY/.env DATABASE_URL to working hosted credentials.
```

---

## How this diff was produced (reproducible)

```bash
# 1. import the export into a scratch DB (FK checks off to load data in any order)
/c/xampp/mysql/bin/mysql.exe -h 127.0.0.1 -P 3306 -u root \
  -e "CREATE DATABASE hosted_export;"
(echo 'SET FOREIGN_KEY_CHECKS=0;'; cat docs/sxlvhdzo_driving_school.sql) | \
  /c/xampp/mysql/bin/mysql.exe -h 127.0.0.1 -P 3306 -u root hosted_export

# 2. compare table sets (case-insensitive) and column sets (single query)
#    (see docs/live-schema-diff.sh for the live-connection variant)
```

`docs/live-schema-diff.sh` still works for a **live** diff against a reachable host — the same
checks against `information_schema` — useful once the `.env` credentials are fixed.
