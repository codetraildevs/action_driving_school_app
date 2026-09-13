-- ============================================================================
-- dedupe-accounts.sql — Merge duplicate phone-format account pairs (MySQL 8)
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
-- What it does per pair (each pair is ONE transaction — all or nothing):
--   1. Archives the loser `users` row into users_dedupe_archive (all columns,
--      discovered dynamically so new schema columns are preserved too).
--   2. For UNIQUE-per-user child tables: if the survivor already has a row,
--      the loser's row is archived instead of merged; otherwise re-pointed.
--   3. Re-points every regular child table from loser → survivor.
--   4. Deletes the loser user row.
--
-- SAFETY:
--   * Run the report queries at the bottom FIRST (read-only) and review.
--   * dedupe-duplicates.sh performs a mysqldump backup before executing.
--   * Every write is logged in dedupe_log; archived rows keep their original
--     ids and can be restored from the archive tables if ever needed.
--   * Idempotent: re-running skips pairs that no longer exist, and INSERT
--     IGNORE tolerates rows archived by an interrupted earlier run.
--   * MySQL 8 compatible: no ADD COLUMN IF NOT EXISTS (MariaDB-only);
--     conditional ALTERs use information_schema + prepared statements.
--
-- Usage (from /home/project3):
--   mysql -u $DB_USER -p $DB_NAME < scripts/dedupe-accounts.sql
--   Or via the wrapper: bash scripts/dedupe-duplicates.sh
-- ============================================================================

SET SESSION group_concat_max_len = 16384;

-- ----------------------------------------------------------------------------
-- 0. Archive scaffolding (idempotent, MySQL 8 compatible)
--    Created OUTSIDE the procedure: CREATE TABLE inside a transaction causes
--    an implicit commit, which would break per-pair atomicity.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users_dedupe_archive AS
  SELECT * FROM users WHERE 1 = 0;

-- MySQL 8 has no ADD COLUMN IF NOT EXISTS — add only when missing.
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

-- Archive tables for the UNIQUE-per-user child rows (empty clones).
CREATE TABLE IF NOT EXISTS user_subscriptions_dedupe_archive AS
  SELECT * FROM user_subscriptions WHERE 1 = 0;
CREATE TABLE IF NOT EXISTS user_test_access_dedupe_archive AS
  SELECT * FROM user_test_access WHERE 1 = 0;
CREATE TABLE IF NOT EXISTS user_timezones_dedupe_archive AS
  SELECT * FROM user_timezones WHERE 1 = 0;
CREATE TABLE IF NOT EXISTS reading_sessions_dedupe_archive AS
  SELECT * FROM reading_sessions WHERE 1 = 0;

-- ----------------------------------------------------------------------------
-- 1. The merge procedure
-- ----------------------------------------------------------------------------
DELIMITER //

DROP PROCEDURE IF EXISTS merge_user_pair //
CREATE PROCEDURE merge_user_pair(IN p_keep_id INT, IN p_drop_id INT)
BEGIN
  DECLARE v_moved INT DEFAULT 0;
  DECLARE v_phone VARCHAR(20);
  DECLARE v_cols TEXT;

  -- Any failure rolls the whole pair back and aborts the script loudly.
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  -- Guard: both rows must exist, otherwise the pair was already merged.
  IF NOT EXISTS (SELECT 1 FROM users WHERE id = p_keep_id)
     OR NOT EXISTS (SELECT 1 FROM users WHERE id = p_drop_id) THEN
    SELECT CONCAT('SKIP pair ', p_keep_id, '/', p_drop_id, ' — already merged or missing') AS status;
  ELSE
    SELECT phone_number INTO v_phone FROM users WHERE id = p_drop_id;

    START TRANSACTION;

    -- 1a. Archive the loser user row — ALL columns, discovered dynamically so
    --     columns added since this script was written are preserved too.
    SET v_cols = (
      SELECT GROUP_CONCAT(CONCAT('`', column_name, '`') ORDER BY ordinal_position)
      FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'users');
    SET @ddl = CONCAT(
      'INSERT IGNORE INTO users_dedupe_archive (', v_cols, ', merged_into, archived_at) ',
      'SELECT ', v_cols, ', ', p_keep_id, ', NOW() FROM users WHERE id = ', p_drop_id);
    PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

    -- 1b. UNIQUE-per-user child tables.
    --     If the survivor already has a row, archive the loser's row;
    --     otherwise the UPDATE below simply re-points it.

    -- user_subscriptions (user UNIQUE)
    INSERT IGNORE INTO user_subscriptions_dedupe_archive
      SELECT us.* FROM user_subscriptions us
      JOIN user_subscriptions k ON k.`user` = p_keep_id
      WHERE us.`user` = p_drop_id;
    DELETE us FROM user_subscriptions us
      JOIN user_subscriptions k ON k.`user` = p_keep_id
      WHERE us.`user` = p_drop_id;
    UPDATE user_subscriptions SET `user` = p_keep_id WHERE `user` = p_drop_id;
    SET v_moved = v_moved + ROW_COUNT();

    -- user_test_access (user UNIQUE)
    INSERT IGNORE INTO user_test_access_dedupe_archive
      SELECT uta.* FROM user_test_access uta
      JOIN user_test_access k ON k.`user` = p_keep_id
      WHERE uta.`user` = p_drop_id;
    DELETE uta FROM user_test_access uta
      JOIN user_test_access k ON k.`user` = p_keep_id
      WHERE uta.`user` = p_drop_id;
    UPDATE user_test_access SET `user` = p_keep_id WHERE `user` = p_drop_id;
    SET v_moved = v_moved + ROW_COUNT();

    -- user_timezones (user UNIQUE)
    INSERT IGNORE INTO user_timezones_dedupe_archive
      SELECT ut.* FROM user_timezones ut
      JOIN user_timezones k ON k.`user` = p_keep_id
      WHERE ut.`user` = p_drop_id;
    DELETE ut FROM user_timezones ut
      JOIN user_timezones k ON k.`user` = p_keep_id
      WHERE ut.`user` = p_drop_id;
    UPDATE user_timezones SET `user` = p_keep_id WHERE `user` = p_drop_id;
    SET v_moved = v_moved + ROW_COUNT();

    -- reading_sessions (user UNIQUE)
    INSERT IGNORE INTO reading_sessions_dedupe_archive
      SELECT rs.* FROM reading_sessions rs
      JOIN reading_sessions k ON k.`user` = p_keep_id
      WHERE rs.`user` = p_drop_id;
    DELETE rs FROM reading_sessions rs
      JOIN reading_sessions k ON k.`user` = p_keep_id
      WHERE rs.`user` = p_drop_id;
    UPDATE reading_sessions SET `user` = p_keep_id WHERE `user` = p_drop_id;
    SET v_moved = v_moved + ROW_COUNT();

    -- 1c. Regular child tables — straight re-point (no unique constraints).
    UPDATE addresses                       SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE devices                         SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE sessions                        SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE user_activities                 SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE user_notifications              SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE user_learning_materials         SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE user_permissions                SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE privacy_policy_acceptances      SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE terms_of_service_acceptances    SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE bookmarks                       SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE ratings                         SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE user_ratings                    SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE test_attempts                   SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE test_results                    SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE transactions                    SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE user_subscriptions_request      SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE irembo_driving_license_requests SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();
    UPDATE irembo_special_requests         SET `user` = p_keep_id WHERE `user` = p_drop_id;  SET v_moved = v_moved + ROW_COUNT();

    -- Optional tables (present in newer schemas, absent in older dumps):
    -- login_attempts, privacy_consents, data_deletion_requests, permission_logs.
    -- Guarded with information_schema so this runs on any schema version.
    SET @opt_tables = 'login_attempts,privacy_consents,data_deletion_requests,permission_logs';
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

    -- 1d. Finally, delete the loser account (children re-pointed above, so no
    --     ON DELETE CASCADE data loss occurs).
    DELETE FROM users WHERE id = p_drop_id;

    INSERT INTO dedupe_log (kept_user_id, dropped_user_id, phone_number, child_rows_moved)
    VALUES (p_keep_id, p_drop_id, v_phone, v_moved);

    COMMIT;

    SELECT CONCAT('MERGED user ', p_drop_id, ' (', v_phone, ') → ', p_keep_id,
                  ' — child rows moved: ', v_moved) AS status;
  END IF;
END //

DROP PROCEDURE IF EXISTS dedupe_all_phone_pairs //
CREATE PROCEDURE dedupe_all_phone_pairs()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE v_keep INT; DECLARE v_drop INT;
  -- Pairs grouped by normalized number: strip a leading +250 prefix and any
  -- leading 0. The derived table materializes the pair list, so deleting rows
  -- while iterating is safe.
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
--
-- SELECT TRIM(LEADING '0' FROM TRIM(LEADING '+250' FROM REPLACE(phone_number,' ',''))) AS core,
--        GROUP_CONCAT(CONCAT(id, ':', phone_number) ORDER BY id SEPARATOR ' | ') AS accounts
-- FROM users
-- GROUP BY core HAVING COUNT(*) > 1
-- ORDER BY core;
-- ============================================================================
