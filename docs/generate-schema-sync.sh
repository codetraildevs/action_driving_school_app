#!/usr/bin/env bash
# Generates docs/production-schema-sync.sql from local-db/driving_school_local.sql
# (the local DB is an exact mirror of the current role-based Prisma schema).
# The generated script is SAFE for production: it only ADDs missing tables/columns,
# never drops, truncates, or alters data.

set -euo pipefail

DUMP="local-db/driving_school_local.sql"
OUT="docs/production-schema-sync.sql"

# The local dump was created on Windows (case-insensitive) so these four tables
# were stored lowercase. Prisma expects the properly-cased names, and Linux
# production is case-sensitive — so we must CREATE them under the correct case.
declare -A CASE_FIX=(
  [datadeletionrequests]=DataDeletionRequests
  [loginattemps]=loginAttemps
  [privacyconsent]=privacyConsent
  [whatsappgroup]=WhatsAppGroup
)

# --- Extract all CREATE TABLE blocks (strip the mysqldump DROP + SET noise) ---
# --- and rewrite the four table names to their Prisma-expected casing.      ---
awk '
  /^CREATE TABLE/ { capture=1 }
  capture { print }
  capture && /^\) ENGINE=/ { capture=0; print "" }
' "$DUMP" \
  | sed 's/^CREATE TABLE/CREATE TABLE IF NOT EXISTS/' \
  | sed \
      -e 's/`datadeletionrequests`/`DataDeletionRequests`/g' \
      -e 's/`loginattemps`/`loginAttemps`/g' \
      -e 's/`privacyconsent`/`privacyConsent`/g' \
      -e 's/`whatsappgroup`/`WhatsAppGroup`/g' \
  > /tmp/tables_ddl.sql

TABLE_COUNT=$(grep -c '^CREATE TABLE IF NOT EXISTS' /tmp/tables_ddl.sql)

cat > "$OUT" <<'EOF'
-- ============================================================================
-- PRODUCTION SCHEMA SYNC — role-based app (safe, idempotent)
-- ============================================================================
-- Purpose : Bring the hosted DB (sxlvhdzo_driving_school) in line with the
--           current role-based schema: users.role + user_roles table (roles
--           1–10), pendingLanguage / userTimezone columns, and every table the
--           current backend/Android app expects.
-- Safety  : This script ONLY ADDS what is missing (CREATE TABLE IF NOT EXISTS,
--           guarded ALTER TABLE ADD COLUMN, INSERT IGNORE seeds). It never
--           drops tables, truncates data, or overwrites existing rows.
--           -> Existing production data (real users, exams, subscriptions) is
--              preserved.
-- Run     : 1) Back up first:  mysqldump -u <user> -p sxlvhdzo_driving_school > backup.sql
--           2) Apply:          mysql -u <user> -p sxlvhdzo_driving_school < production-schema-sync.sql
--              (or paste into phpMyAdmin -> SQL tab)
-- Idempotent: safe to re-run any number of times.
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET @sync_db = DATABASE();

-- ----------------------------------------------------------------------------
-- PART 0 — Case-sensitivity fixes (Windows-created lowercase names vs Linux)
-- ----------------------------------------------------------------------------
-- The local dump was created on Windows (case-insensitive table names). On the
-- hosted Linux MySQL these names must match Prisma exactly or queries fail with
-- "table doesn't exist". Renames are ONLY attempted on case-sensitive
-- filesystems (lower_case_table_names = 0) and only when the lowercase table
-- exists while the properly-cased one does not. On Windows this procedure is a
-- no-op (safe to re-run).
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sync_rename_if_needed;
DELIMITER $$
CREATE PROCEDURE sync_rename_if_needed(IN old_name VARCHAR(64), IN new_name VARCHAR(64))
BEGIN
  DECLARE lc INT DEFAULT 1;
  DECLARE old_exists INT DEFAULT 0;
  DECLARE new_exists INT DEFAULT 0;
  SELECT @@lower_case_table_names INTO lc;
  IF lc = 0 THEN
    SELECT COUNT(*) INTO old_exists FROM information_schema.tables
      WHERE table_schema = @sync_db AND table_name = old_name COLLATE utf8mb4_bin;
    SELECT COUNT(*) INTO new_exists FROM information_schema.tables
      WHERE table_schema = @sync_db AND table_name = new_name COLLATE utf8mb4_bin;
    IF old_exists = 1 AND new_exists = 0 THEN
      SET @ddl = CONCAT('RENAME TABLE `', old_name, '` TO `', new_name, '`');
      PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
      SELECT CONCAT('RENAMED ', old_name, ' -> ', new_name) AS sync_action;
    ELSE
      SELECT CONCAT('SKIP rename ', old_name, ' -> ', new_name) AS sync_action;
    END IF;
  ELSE
    SELECT CONCAT('SKIP rename ', old_name, ' (case-insensitive filesystem)') AS sync_action;
  END IF;
END$$
DELIMITER ;

CALL sync_rename_if_needed('datadeletionrequests', 'DataDeletionRequests');
CALL sync_rename_if_needed('loginattemps',        'loginAttemps');
CALL sync_rename_if_needed('privacyconsent',      'privacyConsent');
CALL sync_rename_if_needed('whatsappgroup',       'WhatsAppGroup');
DROP PROCEDURE IF EXISTS sync_rename_if_needed;

-- ----------------------------------------------------------------------------
-- PART 1 — Missing tables (current schema; no-op if already present)
-- ----------------------------------------------------------------------------
EOF

# Append the extracted CREATE TABLE IF NOT EXISTS statements
cat /tmp/tables_ddl.sql >> "$OUT"

cat >> "$OUT" <<'EOF'

-- ----------------------------------------------------------------------------
-- PART 2 — users: role-based columns (guarded ADD COLUMN)
-- ----------------------------------------------------------------------------
-- These are the critical role-based columns. If the hosted `users` table was
-- created before the role-based merge it will lack them, and the login route
-- (which reads user.role.roleName) would fail. Each ADD is guarded so it only
-- runs when the column is missing.
-- ----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sync_add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE sync_add_column_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(512))
BEGIN
  DECLARE col_exists INT DEFAULT 0;
  SELECT COUNT(*) INTO col_exists FROM information_schema.columns
    WHERE table_schema = @sync_db AND table_name = tbl AND column_name = col;
  IF col_exists = 0 THEN
    SET @ddl = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN ', ddl);
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    SELECT CONCAT('ADDED ', tbl, '.', col) AS sync_action;
  ELSE
    SELECT CONCAT('SKIP ', tbl, '.', col, ' (exists)') AS sync_action;
  END IF;
END$$
DELIMITER ;

-- users.role            -> FK to user_roles (role-based routing; required by login)
--                        DEFAULT 5 (student) so existing production rows get a valid role
--                        instead of 0 (0 is not in user_roles 1-10 and would break the FK
--                        and demote existing admins). Promote admins afterwards:
--                        UPDATE users SET role = 2 WHERE <your admin criteria>;
-- users.pendingLanguage -> FK to languages (pending language-switch)
-- users.userTimezone    -> FK to timezones (per-user timezone)
CALL sync_add_column_if_missing('users', 'role',            '`role` int NOT NULL DEFAULT 5');
CALL sync_add_column_if_missing('users', 'pendingLanguage', '`pendingLanguage` int NULL');
CALL sync_add_column_if_missing('users', 'userTimezone',    '`userTimezone` int NOT NULL DEFAULT 1');
DROP PROCEDURE IF EXISTS sync_add_column_if_missing;

-- Add the FKs (guarded against duplicates) once the columns exist.
DROP PROCEDURE IF EXISTS sync_add_fk_if_missing;
DELIMITER $$
-- NOTE: parameter names deliberately avoid shadowing information_schema columns
-- (table_name / constraint_name) — a shadowed name makes the existence check
-- always match and silently skips the ALTER.
CREATE PROCEDURE sync_add_fk_if_missing(IN p_tbl VARCHAR(64), IN p_fk VARCHAR(64), IN p_ddl VARCHAR(512))
BEGIN
  DECLARE fk_exists INT DEFAULT 0;
  SELECT COUNT(*) INTO fk_exists FROM information_schema.table_constraints
    WHERE constraint_schema = @sync_db AND table_name = p_tbl AND constraint_name = p_fk;
  IF fk_exists = 0 THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_tbl, '` ADD CONSTRAINT `', p_fk, '` ', p_ddl);
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    SELECT CONCAT('ADDED FK ', p_fk) AS sync_action;
  ELSE
    SELECT CONCAT('SKIP FK ', p_fk, ' (exists)') AS sync_action;
  END IF;
END$$
DELIMITER ;

CALL sync_add_fk_if_missing('users', 'users_role_fkey',
  'FOREIGN KEY (`role`) REFERENCES `user_roles` (`id`) ON DELETE CASCADE ON UPDATE CASCADE');
CALL sync_add_fk_if_missing('users', 'users_pendingLanguage_fkey',
  'FOREIGN KEY (`pendingLanguage`) REFERENCES `languages` (`id`) ON UPDATE CASCADE');
CALL sync_add_fk_if_missing('users', 'users_userTimezone_fkey',
  'FOREIGN KEY (`userTimezone`) REFERENCES `timezones` (`id`) ON UPDATE CASCADE');
DROP PROCEDURE IF EXISTS sync_add_fk_if_missing;

-- ----------------------------------------------------------------------------
-- PART 3 — Seed reference data (INSERT IGNORE: never overwrites existing rows)
-- ----------------------------------------------------------------------------

-- Roles 1–10 (role-based console). Critical: login reads user.role.roleName.
INSERT IGNORE INTO `user_roles` (`id`, `role_name`, `description`, `created_at`, `updated_at`) VALUES
(1,  'super_admin',    'Full access to the entire platform', NOW(3), NOW(3)),
(2,  'admin',          'Platform administrator',             NOW(3), NOW(3)),
(3,  'content_manager','Manages learning content',           NOW(3), NOW(3)),
(4,  'teacher',        'Driving instructor',                 NOW(3), NOW(3)),
(5,  'student',        'Standard app user',                  NOW(3), NOW(3)),
(6,  'premium_user',   'User with premium subscription',     NOW(3), NOW(3)),
(7,  'free_user',      'User on the free tier',              NOW(3), NOW(3)),
(8,  'moderator',      'Moderates content and activity',     NOW(3), NOW(3)),
(9,  'support_staff',  'Customer support',                   NOW(3), NOW(3)),
(10, 'guest',          'Guest visitor',                      NOW(3), NOW(3));

-- Languages (en / fr / rw)
INSERT IGNORE INTO `languages` (`id`, `language_code`, `language_name`, `native_name`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 'en', 'English',     'English',     1, NOW(3), NOW(3)),
(2, 'fr', 'French',      'Français',    1, NOW(3), NOW(3)),
(3, 'rw', 'Kinyarwanda', 'Kinyarwanda', 1, NOW(3), NOW(3));

-- Timezones (Africa/Kigali primary)
INSERT IGNORE INTO `timezones` (`id`, `timezone_name`, `utc_offset`, `offset_in_minutes`, `country_code`, `country_name`, `region`, `is_dst`, `created_at`, `updated_at`) VALUES
(1, 'Africa/Kigali', '+02:00', 120, 'RW', 'Rwanda', 'Africa/Kigali', 0, NOW(3), NOW(3)),
(2, 'UTC',           '+00:00',   0, 'US', 'Coordinated Universal Time', 'UTC', 0, NOW(3), NOW(3)),
(3, 'Europe/Brussels','+01:00', 60, 'BE', 'Belgium', 'Europe/Brussels', 0, NOW(3), NOW(3));

-- ----------------------------------------------------------------------------
-- PART 4 — Verification
-- ----------------------------------------------------------------------------
SELECT 'SYNC COMPLETE' AS status;
SELECT COUNT(*) AS user_roles_count FROM `user_roles`;
SELECT COUNT(*) AS languages_count FROM `languages`;
SELECT COUNT(*) AS timezones_count FROM `timezones`;
SELECT COUNT(*) AS users_with_role FROM `users` WHERE `role` IS NOT NULL AND `role` > 0;

SET FOREIGN_KEY_CHECKS = 1;
EOF

echo "Generated $OUT with $TABLE_COUNT CREATE TABLE IF NOT EXISTS statements."
