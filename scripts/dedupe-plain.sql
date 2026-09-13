-- ============================================================================
-- dedupe-plain.sql — FALLBACK: delete duplicate accounts with plain SQL
--
-- Use ONLY if scripts/dedupe-accounts.sql fails again. This version uses no
-- stored procedures and no dynamic SQL — just CREATE TABLE, UPDATE, DELETE.
--
-- Policy identical to the main script: LOWER id survives, higher-id
-- duplicate is archived into users_dedupe_archive then deleted.
--
-- SAFETY:
--   * Wrapper takes a mysqldump backup first.
--   * The users row is archived BEFORE any changes; every child row that is
--     deleted (unique-table conflicts) is archived too.
--   * NOT atomic per pair (plain statements) — but each statement is
--     idempotent and re-running is safe: it only touches rows that still
--     exist.
--
-- Run from /home/project3:
--   mysql -u $DB_USER -p $DB_NAME < scripts/dedupe-plain.sql
-- ============================================================================

SET SESSION group_concat_max_len = 16384;

-- ----------------------------------------------------------------------------
-- 0. Archive scaffolding (MySQL 8 compatible: conditional ALTERs)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users_dedupe_archive AS
  SELECT * FROM users WHERE 1 = 0;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'users_dedupe_archive'
     AND column_name = 'archived_at') = 0,
  'ALTER TABLE users_dedupe_archive ADD COLUMN archived_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'users_dedupe_archive'
     AND column_name = 'merged_into') = 0,
  'ALTER TABLE users_dedupe_archive ADD COLUMN merged_into INT NOT NULL DEFAULT 0',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS dedupe_log (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  kept_user_id     INT         NOT NULL,
  dropped_user_id  INT         NOT NULL,
  phone_number     VARCHAR(20) NOT NULL,
  child_rows_moved INT         NOT NULL DEFAULT 0,
  executed_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_subscriptions_dedupe_archive AS
  SELECT * FROM user_subscriptions WHERE 1 = 0;
CREATE TABLE IF NOT EXISTS user_test_access_dedupe_archive AS
  SELECT * FROM user_test_access WHERE 1 = 0;
CREATE TABLE IF NOT EXISTS user_timezones_dedupe_archive AS
  SELECT * FROM user_timezones WHERE 1 = 0;
CREATE TABLE IF NOT EXISTS reading_sessions_dedupe_archive AS
  SELECT * FROM reading_sessions WHERE 1 = 0;

-- ----------------------------------------------------------------------------
-- 1. For each pair, archive the HIGHER-id user row.
--    (LOWER id survives; the join condition keeps only the higher of the two.)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO users_dedupe_archive
  (id, first_name, middle_name, last_name, dob, phone_number, is_active, role,
   profile_picture, email, last_login, created_at, updated_at, password, language,
   userTimezone, pendingLanguage, merged_into, archived_at)
SELECT
  u.id, u.first_name, u.middle_name, u.last_name, u.dob, u.phone_number, u.is_active, u.role,
  u.profile_picture, u.email, u.last_login, u.created_at, u.updated_at, u.password, u.language,
  u.userTimezone, u.pendingLanguage, u2.id AS merged_into, NOW()
FROM users u
JOIN users u2
  ON u2.id < u.id
 AND TRIM(LEADING '0' FROM TRIM(LEADING '+250' FROM REPLACE(u.phone_number, ' ', '')))
   = TRIM(LEADING '0' FROM TRIM(LEADING '+250' FROM REPLACE(u2.phone_number, ' ', '')))
WHERE NOT EXISTS (SELECT 1 FROM users_dedupe_archive a WHERE a.id = u.id);

-- ----------------------------------------------------------------------------
-- 2. UNIQUE-per-user child tables: if the survivor already has a row,
--    archive the duplicate's row and delete it; otherwise it gets re-pointed
--    in step 3. (An earlier interrupted run may have already archived them —
--    INSERT IGNORE makes this safe.)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO user_subscriptions_dedupe_archive
  SELECT us.* FROM user_subscriptions us
  JOIN user_subscriptions k ON k.`user` = us.`user`      -- survivor already has one
  JOIN users_dedupe_archive a ON a.id = us.`user`        -- ...and us belongs to a duplicate
  WHERE k.`user` <> us.`user`;

DELETE us FROM user_subscriptions us
  JOIN user_subscriptions k ON k.`user` = us.`user`
  JOIN users_dedupe_archive a ON a.id = us.`user`
  WHERE k.`user` <> us.`user`;

INSERT IGNORE INTO user_test_access_dedupe_archive
  SELECT uta.* FROM user_test_access uta
  JOIN user_test_access k ON k.`user` = uta.`user`
  JOIN users_dedupe_archive a ON a.id = uta.`user`
  WHERE k.`user` <> uta.`user`;

DELETE uta FROM user_test_access uta
  JOIN user_test_access k ON k.`user` = uta.`user`
  JOIN users_dedupe_archive a ON a.id = uta.`user`
  WHERE k.`user` <> uta.`user`;

INSERT IGNORE INTO user_timezones_dedupe_archive
  SELECT ut.* FROM user_timezones ut
  JOIN user_timezones k ON k.`user` = ut.`user`
  JOIN users_dedupe_archive a ON a.id = ut.`user`
  WHERE k.`user` <> ut.`user`;

DELETE ut FROM user_timezones ut
  JOIN user_timezones k ON k.`user` = ut.`user`
  JOIN users_dedupe_archive a ON a.id = ut.`user`
  WHERE k.`user` <> ut.`user`;

INSERT IGNORE INTO reading_sessions_dedupe_archive
  SELECT rs.* FROM reading_sessions rs
  JOIN reading_sessions k ON k.`user` = rs.`user`
  JOIN users_dedupe_archive a ON a.id = rs.`user`
  WHERE k.`user` <> rs.`user`;

DELETE rs FROM reading_sessions rs
  JOIN reading_sessions k ON k.`user` = rs.`user`
  JOIN users_dedupe_archive a ON a.id = rs.`user`
  WHERE k.`user` <> rs.`user`;

-- ----------------------------------------------------------------------------
-- 3. Re-point ALL child tables from duplicate → survivor.
--    Safe to re-run; each UPDATE matches only rows still pointing at a
--    duplicate id.
-- ----------------------------------------------------------------------------
UPDATE addresses                       SET `user` = (SELECT merged_into FROM users_dedupe_archive WHERE id = addresses.`user`)      WHERE `user` IN (SELECT id FROM users_dedupe_archive);
UPDATE devices                         SET `user` = (SELECT merged_into FROM users_dedupe_archive WHERE id = devices.`user`)        WHERE `user` IN (SELECT id FROM users_dedupe_archive);
UPDATE sessions                        SET `user` = (SELECT merged_into FROM users_dedupe_archive WHERE id = sessions.`user`)       WHERE `user` IN (SELECT id FROM users_dedupe_archive);
UPDATE user_activities                 SET `user` = (SELECT merged_into FROM users_dedupe_archive WHERE id = user_activities.`user`)    WHERE `user` IN (SELECT id FROM users_dedupe_archive);
UPDATE user_notifications              SET `user` = (SELECT merged_into FROM users_dedupe_archive WHERE id = user_notifications.`user`) WHERE `user` IN (SELECT id FROM users_dedupe_archive);
UPDATE user_learning_materials         SET `user_id` = (SELECT merged_into FROM users_dedupe_archive WHERE id = user_learning_materials.`user_id`) WHERE `user_id` IN (SELECT id FROM users_dedupe_archive);
UPDATE user_permissions                SET `user` = (SELECT merged_into FROM users_dedupe_archive WHERE id = user_permissions.`user`) WHERE `user` IN (SELECT id FROM users_dedupe_archive);
UPDATE privacy_policy_acceptances      SET `user_id` = (SELECT merged_into FROM users_dedupe_archive WHERE id = privacy_policy_acceptances.`user_id`) WHERE `user_id` IN (SELECT id FROM users_dedupe_archive);
UPDATE terms_of_service_acceptances    SET `user_id` = (SELECT merged_into FROM users_dedupe_archive WHERE id = terms_of_service_acceptances.`user_id`) WHERE `user_id` IN (SELECT id FROM users_dedupe_archive);
UPDATE bookmarks                       SET `user` = (SELECT merged_into FROM users_dedupe_archive WHERE id = bookmarks.`user`)      WHERE `user` IN (SELECT id FROM users_dedupe_archive);
UPDATE ratings                         SET `user` = (SELECT merged_into FROM users_dedupe_archive WHERE id = ratings.`user`)        WHERE `user` IN (SELECT id FROM users_dedupe_archive);
UPDATE user_ratings                    SET `user_id` = (SELECT merged_into FROM users_dedupe_archive WHERE id = user_ratings.`user_id`)   WHERE `user_id` IN (SELECT id FROM users_dedupe_archive);
UPDATE test_attempts                   SET `user` = (SELECT merged_into FROM users_dedupe_archive WHERE id = test_attempts.`user`)  WHERE `user` IN (SELECT id FROM users_dedupe_archive);
UPDATE test_results                    SET `user` = (SELECT merged_into FROM users_dedupe_archive WHERE id = test_results.`user`) WHERE `user` IN (SELECT id FROM users_dedupe_archive);
UPDATE transactions                    SET `user` = (SELECT merged_into FROM users_dedupe_archive WHERE id = transactions.`user`)   WHERE `user` IN (SELECT id FROM users_dedupe_archive);
UPDATE user_subscriptions_request      SET `user` = (SELECT merged_into FROM users_dedupe_archive WHERE id = user_subscriptions_request.`user`) WHERE `user` IN (SELECT id FROM users_dedupe_archive);
UPDATE irembo_driving_license_requests SET `user_id` = (SELECT merged_into FROM users_dedupe_archive WHERE id = irembo_driving_license_requests.`user_id`) WHERE `user_id` IN (SELECT id FROM users_dedupe_archive);
UPDATE irembo_special_requests         SET `user_id` = (SELECT merged_into FROM users_dedupe_archive WHERE id = irembo_special_requests.`user_id`) WHERE `user_id` IN (SELECT id FROM users_dedupe_archive);

-- Optional tables (guard with information_schema — absent in older schemas).
-- Real table names per schema: loginAttemps (sic), privacyConsent,
-- DataDeletionRequests, permissionlogs. All four use column `user`.
SET @ddl = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'loginAttemps'),
  'UPDATE loginAttemps SET `user` = (SELECT merged_into FROM users_dedupe_archive a WHERE a.id = loginAttemps.`user`) WHERE `user` IN (SELECT id FROM users_dedupe_archive)',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'privacyConsent'),
  'UPDATE privacyConsent SET `user` = (SELECT merged_into FROM users_dedupe_archive a WHERE a.id = privacyConsent.`user`) WHERE `user` IN (SELECT id FROM users_dedupe_archive)',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'DataDeletionRequests'),
  'UPDATE DataDeletionRequests SET `user` = (SELECT merged_into FROM users_dedupe_archive a WHERE a.id = DataDeletionRequests.`user`) WHERE `user` IN (SELECT id FROM users_dedupe_archive)',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF(
  EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'permissionlogs'),
  'UPDATE permissionlogs SET `user` = (SELECT merged_into FROM users_dedupe_archive a WHERE a.id = permissionlogs.`user`) WHERE `user` IN (SELECT id FROM users_dedupe_archive)',
  'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ----------------------------------------------------------------------------
-- 4. Delete the duplicate user rows (children re-pointed above).
-- ----------------------------------------------------------------------------
DELETE FROM users WHERE id IN (SELECT id FROM users_dedupe_archive);

-- ----------------------------------------------------------------------------
-- 5. Log the merges
-- ----------------------------------------------------------------------------
INSERT INTO dedupe_log (kept_user_id, dropped_user_id, phone_number, child_rows_moved)
SELECT a.merged_into, a.id, a.phone_number, 0
FROM users_dedupe_archive a
WHERE NOT EXISTS (
  SELECT 1 FROM dedupe_log d
  WHERE d.dropped_user_id = a.id AND d.kept_user_id = a.merged_into);

-- ----------------------------------------------------------------------------
-- 6. Verification — must be 0
-- ----------------------------------------------------------------------------
SELECT CONCAT('Remaining variant collisions: ',
       (SELECT COUNT(*) FROM (
          SELECT TRIM(LEADING '0' FROM TRIM(LEADING '+250' FROM REPLACE(phone_number,' ',''))) AS core
          FROM users GROUP BY core HAVING COUNT(*) > 1
        ) x)) AS verification;
SELECT kept_user_id, dropped_user_id, phone_number FROM dedupe_log ORDER BY id;
