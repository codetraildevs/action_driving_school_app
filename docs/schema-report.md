# Schema Report — `driving_school_local` (current `schema.prisma`)

> **Source of truth:** `DRIVING_SHOOL_COMPANY_LEGACY/prisma/schema.prisma` (MySQL provider).
> The local test database (`driving_school_local`) was created with `prisma db push`, so it is an
> exact mirror of the current schema — **51 tables**.
>
> ⚠️ The live diff could not be produced: the `DATABASE_URL` in `.env`
> (`sxlvhdzo_admin@198.251.89.126`) is **rejected with Access denied**, so the production schema
> was not inspected. This report describes the *current* schema; treat the “likely newer” section
> as a checklist to verify against production.

## Table of contents
1. [Important note for this feature](#important-note)
2. [Tables by domain](#tables-by-domain)
3. [Likely newer tables (check on production)](#likely-newer-tables)
4. [How to sync production schema](#how-to-sync-production)

---

## Important note <a name="important-note"></a>

**The admin console feature made ZERO `schema.prisma` changes.** All tables it reads
(`user_roles`, `users`, `user_subscriptions`, `irembo_*`, …) already existed in the schema.
The feature was code-only (Android UI + two backend route files). So there is no *required* schema
migration for the admin console — production only needs the schema to be up to date with
`schema.prisma` in general.

> ✅ **CONFIRMED by live diff** (see [`live-schema-diff-report.md`](live-schema-diff-report.md)):
> the hosted DB export already matches the current schema — 51/51 tables, 405/405 columns, roles
> seeded, admin present. **No schema sync is needed.**

## Tables by domain <a name="tables-by-domain"></a>

**Auth & users**
`users` · `user_roles` · `devices` · `sessions` · `loginattemps` · `firebase_devices` · `forget_password_requests`

**Profile & personalization**
`languages` · `timezones` · `user_timezones` · `addresses`

**Subscriptions & access control**
`subscription_plans` · `permissions` · `user_permissions` · `user_subscriptions` ·
`user_subscriptions_request` · `user_test_access` · `transactions`

**Content — tests & questions**
`tests` · `test_questions` · `test_attempts` · `test_answers` · `test_results` · `test_translations` ·
`questions` · `question_options` · `question_translations` · `question_option_translations`

**Content — learning materials & PDFs**
`learning_materials` · `user_learning_materials` · `pdf_files` · `reading_sessions` · `bookmarks` · `ratings`

**Irembo services**
`irembo_driving_license_requests` · `irembo_special_requests`

**Engagement & notifications**
`user_activities` · `user_notifications` · `user_ratings` · `whatsappgroup`

**Compliance & legal**
`privacy_policies` · `privacy_policy_acceptances` · `terms_of_service` ·
`terms_of_service_acceptances` · `app_store_compliance` · `datadeletionrequests` · `privacyconsent`

**Admin & file management**
`files` · `folders` · `system_settings` · `permissionlogs`

## Likely newer tables (verify against production) <a name="likely-newer-tables"></a>

These back recent features and are the most likely candidates to be **missing or out of date on the
live DB** — confirm each exists with the expected columns:

| Table | Backs |
|---|---|
| `user_roles` | role-based admin console (roles 1–10) |
| `user_subscriptions_request` | test-access / subscription requests shown in admin |
| `user_test_access` | per-user test access & expiry |
| `irembo_driving_license_requests` / `irembo_special_requests` | Irembo services + admin Requests tab |
| `whatsappgroup` | WhatsApp groups tab |
| `firebase_devices` | FCM push tokens |
| `app_store_compliance` | Play Store compliance versions |
| `privacy_policy_acceptances` / `terms_of_service_acceptances` | legal acceptance tracking |
| `user_ratings` | Play Store ratings |
| `files` / `folders` | file manager |
| `loginattemps` / `permissionlogs` | audit / login tracking |

## How to sync production schema <a name="how-to-sync-production"></a>

Once the correct live `DATABASE_URL` is in `DRIVING_SHOOL_COMPANY_LEGACY/.env` (or passed inline):

```bash
cd DRIVING_SHOOL_COMPANY_LEGACY

# 1. (optional, safe) Inspect what differs — Prisma prints a diff summary without writing:
#    DATABASE_URL="mysql://user:pass@HOST:3306/sxlvhdzo_driving_school" npx prisma db pull --print

# 2. (destructive on prod tables it manages) Apply the current schema:
DATABASE_URL="mysql://user:pass@HOST:3306/sxlvhdzo_driving_school" npx prisma db push
```

> ⚠️ `prisma db push` alters the target database directly. **Back up production first**
> (mysqldump) and prefer applying one table/column at a time if you need to keep existing data.
> An alternative for controlled deploys: generate SQL migrations with `prisma migrate diff`
> and review them before executing.
>
> 📦 **Ready-to-run alternative:** [`docs/production-schema-sync.sql`](production-schema-sync.sql) — a safe,
> idempotent script generated from the local dump that adds only missing tables/columns, seeds
> roles 1–10 + languages + timezones, and fixes the 4 Windows-vs-Linux table-name casing issues.
> Tested locally: empty DB, re-run, and simulated old-production `users` upgrade.
