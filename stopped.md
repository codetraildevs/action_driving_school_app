# ✅ RESOLVED — schema was NEVER the problem (live diff done)

> See [`docs/live-schema-diff-report.md`](docs/live-schema-diff-report.md) for the definitive
> comparison of the hosted DB export (`docs/sxlvhdzo_driving_school.sql`) vs the current schema.
>
> **Verdict: the hosted DB is already fully role-based — 51/51 tables, 405/405 columns, roles 1–10
> seeded, real admin (role 2) + 3,538 students, all devices present. Do NOT run
> `docs/production-schema-sync.sql` on production** (its hardcoded language/timezone ids 1–3 clash
> with production's 41/48/85 and 30/594 — it would create duplicate timezone rows).
>
> The real fix is a **backend redeploy** (ensure `/api/auth/login` returns `role`/`roleName` and
> `/api/admin/*` guards are live) and **working `DATABASE_URL` credentials** in `.env`.

---

# (Earlier analysis, superseded by the live diff above)

# Schema comparison complete → schema-sync script ready ⚠️ now known unnecessary

## What the user's issue was

> "I have a problem — before [the role-based merge] it was working. Check what you changed on
> the SQL tables, basically `users` and related tables. Login was separate and there was no
> role-based — it was two separate apps and now it's one. I think what to do is upload the
> modified schema."

**What actually changed with role-based:** the app went from two separate experiences (learner
app + admin console) to **one role-based app**. That is a *backend + Android* change, not a schema
change — the schema already contained `users.role`, `user_roles`, etc. The reason it can break on
the hosted server is that the **hosted DB may be missing the role-based tables/columns** (or has
them under Windows-created lowercase names), so the login route (which reads `user.role.roleName`)
fails.

## What I verified

| Check | Result |
|---|---|
| Local `driving_school_local` vs `schema.prisma` | ✅ 51/51 tables, `users` 17/17 cols incl. `role`, `pendingLanguage`, `userTimezone` |
| Role seed data (`user_roles` 1–10) | ✅ present locally |
| Hosted MySQL `198.251.89.126:3306` | ✅ **port reachable** — but `.env` creds rejected (`Access denied for 'sxlvhdzo_admin'`) |

> ⚠️ I could **not** inspect the hosted schema directly because the `DATABASE_URL` credentials in
> `DRIVING_SHOOL_COMPANY_LEGACY/.env` are rejected (Access denied). Fix the password (or add
> remote-access IP in cPanel → Remote MySQL) to run the sync script against the real DB.

## ✅ Deliverable: `docs/production-schema-sync.sql` (safe, idempotent)

Generated from the local dump (an exact mirror of the current role-based schema). It:

1. **Renames 4 tables** to Prisma's expected casing for Linux/cPanel
   (`datadeletionrequests`→`DataDeletionRequests`, `loginattemps`→`loginAttemps`,
   `privacyconsent`→`privacyConsent`, `whatsappgroup`→`WhatsAppGroup`) — guarded so it's a no-op
   on Windows / when the correctly-cased table already exists.
2. **Creates all 51 tables** with `CREATE TABLE IF NOT EXISTS` (only adds missing ones).
3. **Adds the role-based columns** to an existing `users` table if missing:
   `role` (int NOT NULL **DEFAULT 5** — student, FK→`user_roles`), `pendingLanguage`
   (FK→`languages`), `userTimezone` (FK→`timezones`). Defaulting to 5 (not 0) keeps the FK
   valid and existing production users as regular students.

   > **After syncing:** promote real admins so role-based routing grants them the console:
   > `UPDATE users SET role = 2 WHERE phone_number IN ('+2507...admin...');`
4. **Seeds reference data** with `INSERT IGNORE`: roles 1–10, languages (en/fr/rw), timezones.
5. **Never drops, truncates, or overwrites existing data** — safe to re-run.

**Verified locally** (XAMPP MySQL):
- ✅ Empty DB → 51 tables + roles seeded, no errors
- ✅ Re-run → idempotent, no errors
- ✅ Simulated old production `users` (no role cols) → columns + FKs added, existing user row preserved

## How to apply (once hosted creds work)

```bash
# 1. back up the hosted DB
mysqldump -u <user> -p sxlvhdzo_driving_school > backup_prod_$(date +%F).sql
# 2. apply the sync script
mysql -u <user> -p sxlvhdzo_driving_school < docs/production-schema-sync.sql
# (or paste the file into cPanel → phpMyAdmin → SQL tab)
```

> Don't do a full restore of `local-db/driving_school_local.sql` onto hosted — it contains only
> test data (4 users, empty question bank) and would wipe real production data.

## Regenerating the script

```bash
bash docs/generate-schema-sync.sh   # rewrites docs/production-schema-sync.sql from the local dump
```
