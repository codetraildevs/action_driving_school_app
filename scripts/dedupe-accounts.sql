-- ============================================================================
-- dedupe-accounts.sql — Merge duplicate phone-format account pairs
--
-- The production DB contains pairs of accounts for the SAME phone number,
-- stored once as a legacy `07…` row and once as a `+250…` row (numbers
-- registered before phone normalization was introduced). Login is now
-- credential-aware so these pairs can no longer cause cross-user logins,
-- but they must still be merged so each number maps to exactly one account.
--
-- Policy (consistent with the backend login fix):
--   SURVIVOR = the row with the LOWEST id (the legacy account that owns the
--   user's historical data: subscriptions, test access, attempts, results).
--   The higher-id `+250…` twin is archived and deleted.
--
-- What it does per pair:
--   1. Archives the loser `users` row into users_dedupe_archive.
--   2. Re-points every child table from loser → survivor.
--   3. For UNIQUE-per-user child tables (user_subscriptions, user_test_access,
--      user_timezones, reading_sessions): keeps the survivor's row, archives
--      the loser's row, and never overwrites existing survivor data.
--   4. Deletes the loser user row (children are re-pointed first, so no
--      ON DELETE CASCADE data loss occurs).
--
-- SAFETY:
--   * Run the report queries at the bottom FIRST (read-only) and review.
--   * dedupe-duplicates.sh performs a mysqldump backup before executing.
--   * Every write is logged in dedupe_log; archived rows keep their original
--     ids and can be restored from the archive tables if ever needed.
--   * Idempotent: re-running skips pairs that no longer exist.
--
-- Usage (from /home/project3):
--   mysql -u $DB_USER -p $DB_NAME < sql/dedupe-accounts.sql            # full run
--   Or via the wrapper: bash sql/dedupe-duplicates.sh
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. Archive scaffolding (IF NOT EXISTS → idempotent)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users_dedupe_archive AS
  SELECT * FROM users WHERE 1 = 0;
ALTER TABLE users_dedupe_archive
  ADD COLUMN IF NOT EXISTS archived_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS merged_into INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS dedupe_log (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  kept_user_id INT  NOT NULL,
  dropped_user_id INT NOT NULL,
  phone_number VARCHAR(20) NOT NULL,
  child_rows_moved INT NOT NULL DEFAULT 0,
  executed_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 1. The merge procedure
-- ----------------------------------------------------------------------------
DELIMITER //

DROP PROCEDURE IF EXISTS merge_user_pair //
CREATE PROCEDURE merge_user_pair(IN p_keep_id INT, IN p_drop_id INT)
BEGIN
  DECLARE v_moved INT DEFAULT 0;
  DECLARE v_phone VARCHAR(20);

  -- Guard: both rows must exist, otherwise the pair was already merged.
  IF NOT EXISTS (SELECT 1 FROM users WHERE id = p_keep_id)
     OR NOT EXISTS (SELECT 1 FROM users WHERE id = p_drop_id) THEN
    SELECT CONCAT('SKIP pair ', p_keep_id, '/', p_drop_id, ' — already merged or missing') AS status;
  ELSE
    SELECT phone_number INTO v_phone FROM users WHERE id = p_drop_id;

    -- 1a. Archive the loser user row.
    INSERT INTO users_dedupe_archive
      (id, first_name, middle_name, last_name, dob, phone_number, is_active, role,
       profile_picture, email, last_login, created_at, updated_at, password, language,
       userTimezone, pendingLanguage, merged_into)
    SELECT id, first_name, middle_name, last_name, dob, phone_number, is_active, role,
       profile_picture, email, last_login, created_at, updated_at, password, language,
       userTimezone, pendingLanguage, p_keep_id
    FROM users WHERE id = p_drop_id;

    -- 1b. UNIQUE-per-user child tables.
    --     Survivor's row wins; the loser's row is archived, not merged.

    -- user_subscriptions (user UNIQUE)
    CREATE TABLE IF NOT EXISTS user_subscriptions_dedupe_archive AS
      SELECT * FROM user_subscriptions WHERE 1 = 0;
    INSERT INTO user_subscriptions_dedupe_archive
      SELECT us.* FROM user_subscriptions us
      JOIN user_subscriptions k ON k.`user` = p_keep_id
      WHERE us.`user` = p_drop_id;                       -- survivor already has one
    DELETE us FROM user_subscriptions us
      JOIN user_subscriptions k ON k.`user` = p_keep_id
      WHERE us.`user` = p_drop_id;
    UPDATE user_subscriptions SET `user` = p_keep_id WHERE `user` = p_drop_id;
    SET v_moved = v_moved + ROW_COUNT();

    -- user_test_access (user UNIQUE)
    CREATE TABLE IF NOT EXISTS user_test_access_dedupe_archive AS
      SELECT * FROM user_test_access WHERE 1 = 0;
    INSERT INTO user_test_access_dedupe_archive
      SELECT uta.* FROM user_test_access uta
      JOIN user_test_access k ON k.`user` = p_keep_id
      WHERE uta.`user` = p_drop_id;
    DELETE uta FROM user_test_access uta
      JOIN user_test_access k ON k.`user` = p_keep_id
      WHERE uta.`user` = p_drop_id;
    UPDATE user_test_access SET `user` = p_keep_id WHERE `user` = p_drop_id;
    SET v_moved = v_moved + ROW_COUNT();

    -- user_timezones (user UNIQUE)
    CREATE TABLE IF NOT EXISTS user_timezones_dedupe_archive AS
      SELECT * FROM user_timezones WHERE 1 = 0;
    INSERT INTO user_timezones_dedupe_archive
      SELECT ut.* FROM user_timezones ut
      JOIN user_timezones k ON k.`user` = p_keep_id
      WHERE ut.`user` = p_drop_id;
    DELETE ut FROM user_timezones ut
      JOIN user_timezones k ON k.`user` = p_keep_id
      WHERE ut.`user` = p_drop_id;
    UPDATE user_timezones SET `user` = p_keep_id WHERE `user` = p_drop_id;
    SET v_moved = v_moved + ROW_COUNT();

    -- reading_sessions (user UNIQUE)
    CREATE TABLE IF NOT EXISTS reading_sessions_dedupe_archive AS
      SELECT * FROM reading_sessions WHERE 1 = 0;
    INSERT INTO reading_sessions_dedupe_archive
      SELECT rs.* FROM reading_sessions rs
      JOIN reading_sessions k ON k.`user` = p_keep_id
      WHERE rs.`user` = p_drop_id;
    DELETE rs FROM reading_sessions rs
      JOIN reading_sessions k ON k.`user` = p_keep_id
      WHERE rs.`user` = p_drop_id;
    UPDATE reading_sessions SET `user` = p_keep_id WHERE `user` = p_drop_id;
    SET v_moved = v_moved + ROW_COUNT();

    -- 1c. Regular child tables — straight re-point (no unique constraints).
    UPDATE addresses                    SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE devices                      SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE sessions                     SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE user_activities              SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE user_notifications           SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE user_learning_materials      SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE user_permissions             SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE privacy_policy_acceptances   SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE terms_of_service_acceptances SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE bookmarks                    SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE ratings                      SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE user_ratings                 SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE test_attempts                SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE test_results                 SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE transactions                 SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE user_subscriptions_request   SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE irembo_driving_license_requests SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE irembo_special_requests         SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();

    -- Optional tables (present in newer schemas, absent in older dumps):
    -- login_attempts, privacy_consents, data_deletion_requests, permission_logs.
    -- Guarded with information_schema so this runs on any schema version.
    SET @opt_tables = 'login_attempts,privacy_consents,data_deletion_requests,permission_logs';
    SET @t = NULL;
    opt_loop: REPEAT
      SET @t = SUBSTRING_INDEX(@opt_tables, ',', 1);
      SET @opt_tables = SUBSTRING(@opt_tables, LENGTH(@t) + 2);
      IF @t IS NOT NULL AND @t <> '' THEN
        IF EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = DATABASE() AND table_name = @t) THEN
          SET @ddl = CONCAT('UPDATE `', @t, '` SET `user` = ', p_keep_id,
                            ' WHERE `user` = ', p_drop_id);
          PREPARE s FROM @ddl; EXECUTE s;
          SET v_moved = v_moved + ROW_COUNT();
          DEALLOCATE PREPARE s;
        END IF;
      END IF;
    UNTIL @opt_tables IS NULL OR @opt_tables = '' END REPEAT opt_loop;

    -- 1d. Finally, delete the loser account.
    DELETE FROM users WHERE id = p_drop_id;

    INSERT INTO dedupe_log (kept_user_id, dropped_user_id, phone_number, child_rows_moved)
    VALUES (p_keep_id, p_drop_id, v_phone, v_moved);

    SELECT CONCAT('MERGED user ', p_drop_id, ' (', v_phone, ') → ', p_keep_id,
                  ' — child rows moved: ', v_moved) AS status;
  END IF;
END //

DROP PROCEDURE IF EXISTS dedupe_all_phone_pairs //
CREATE PROCEDURE dedupe_all_phone_pairs()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE v_keep INT; DECLARE v_drop INT;
  -- Pairs grouped by normalized number: strip +250 prefix and leading 0.
  DECLARE cur CURSOR FOR
    SELECT t.keep_id, t.drop_id FROM (
      SELECT
        CASE WHEN u1.id < u2.id THEN u1.id ELSE u2.id END AS keep_id,
        CASE WHEN u1.id < u2.id THEN u2.id ELSE u1.id END AS drop_id
      FROM users u1
      JOIN users u2
        ON u1.id < u2.id
       AND TRIM(LEADING '0' FROM TRIM(LEADING '+250' FROM REPLACE(u1.phone_number, ' ', '')))
         = TRIM(LEADING '0' FROM TRIM(LEADING '+250' FROM REPLACE(u2.phone_number, ' ', '')))
    ) t;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur;
  pair_loop: LOOP
    FETCH cur INTO v_keep, v_drop;
    IF done = 1 THEN LEAVE pair_loop; END IF;
    CALL merge_user_pair(v_keep, v_drop);
  END LOOP;
  CLOSE cur;
END //

DELIMITER ;

-- ----------------------------------------------------------------------------
-- 2. EXECUTE THE MERGE
--    (Comment this out and run only the report below for a dry run.)
-- ----------------------------------------------------------------------------
CALL dedupe_all_phone_pairs();

-- ----------------------------------------------------------------------------
-- 3. Post-merge verification
-- ----------------------------------------------------------------------------
SELECT CONCAT('Remaining variant collisions: ',
       (SELECT COUNT(*) FROM (
          SELECT TRIM(LEADING '0' FROM TRIM(LEADING '+250' FROM REPLACE(phone_number,' ',''))) AS core
          FROM users GROUP BY core HAVING COUNT(*) > 1
        ) x)) AS verification;
SELECT kept_user_id, dropped_user_id, phone_number, child_rows_moved, executed_at FROM dedupe_log ORDER BY id;

-- ============================================================================
-- READ-ONLY REPORT — run these BEFORE the merge to preview what will happen.
-- (Equivalent of diag-phone-audit.js, pure SQL.)
--
-- SELECT u.id, u.phone_number, u.is_active, u.created_at
-- FROM users u
-- JOIN (SELECT TRIM(LEADING '0' FROM TRIM(LEADING '+250' FROM REPLACE(phone_number,' ',''))) AS core
--       FROM users GROUP BY core HAVING COUNT(*) > 1) d
--   ON TRIM(LEADING '0' FROM TRIM(LEADING '+250' FROM REPLACE(u.phone_number,' ',''))) = d.core
-- ORDER BY d.core, u.id;
-- ============================================================================
