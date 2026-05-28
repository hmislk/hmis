-- Migration v2.9.0: Restore AUTO_INCREMENT on every ID primary key that lost it
-- Author: H.K. Damith Deshan
--
-- Background:
--   Entities map @GeneratedValue(strategy = IDENTITY); EclipseLink omits the ID on
--   INSERT and reads it back via SELECT LAST_INSERT_ID(). That only works when the
--   underlying ID column is AUTO_INCREMENT. Some databases were restored from dumps
--   that dropped the AUTO_INCREMENT attribute, so every INSERT into those tables fails
--   with: java.sql.SQLException: Field 'ID' doesn't have a default value (MySQL 1364).
--
--   Observed on a Roseth QA database where bill, billitem, itembatcharchive,
--   patientitem, paymentscheme, shiftstaffrequirement, stockarchive and
--   stockhistoryarchive had lost AUTO_INCREMENT, breaking inward bill settlement.
--
-- What this does:
--   Scans INFORMATION_SCHEMA for EVERY table in the current schema whose single-column
--   BIGINT primary key named ID is missing AUTO_INCREMENT, and re-adds it (preserving
--   the existing column type). Healthy tables are never selected, so they are skipped.
--   If a single table cannot be altered (e.g. lock timeout or missing privilege) the
--   per-table CONTINUE HANDLER skips it so the remaining tables are still attempted.
--   AFTER the loop the procedure re-counts the columns that are STILL missing
--   AUTO_INCREMENT and, if any remain, raises SIGNAL SQLSTATE '45000'. That error
--   propagates through CALL to the Java migration runner, which records the migration
--   as FAILED — so a partially-applied fix can never be recorded as SUCCESS.
--
-- Idempotent: a no-op on healthy databases (the scan returns no rows). Safe to re-run.
-- MySQL auto-sets each table's next value to MAX(id)+1, so no row data is modified.
--
-- UNIVERSAL: detects the actual stored case of the ID column per table, so this works on
-- both case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows) MySQL.
--
-- Requires the datasource user to have CREATE ROUTINE (a temporary stored procedure is
-- created and dropped). For the duration of the rebuild the procedure disables FK checks
-- (so child-table ALTERs succeed) and relaxes sql_mode (so tables whose OTHER columns hold
-- legacy invalid defaults such as TIMESTAMP DEFAULT '0000-00-00 00:00:00' do not fail with
-- error 1067). Both session settings are saved and restored.

SELECT 'Migration v2.9.0 - Restore AUTO_INCREMENT on all ID primary keys missing it' AS status;

-- ── BEFORE: tables that will be fixed ────────────────────────────────────────

SELECT 'BEFORE: ID primary keys missing AUTO_INCREMENT (these will be fixed)' AS status;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND UPPER(COLUMN_NAME) = 'ID'
  AND COLUMN_KEY = 'PRI'
  AND DATA_TYPE = 'bigint'
  AND EXTRA NOT LIKE '%auto_increment%'
ORDER BY TABLE_NAME;

-- ── Build a temporary procedure that loops over every affected table ──────────

DROP PROCEDURE IF EXISTS hmis_fix_autoincrement_v290;

DELIMITER $$
CREATE PROCEDURE hmis_fix_autoincrement_v290()
BEGIN
    DECLARE v_remaining INT DEFAULT 0;

    -- Save and relax session state for the rebuild:
    --   * sql_mode='' so rebuilding a table whose OTHER columns carry legacy invalid
    --     defaults (e.g. TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00') does not
    --     raise error 1067 — strict NO_ZERO_DATE/STRICT_TRANS_TABLES re-validate every
    --     column when MODIFY copies the table.
    --   * foreign_key_checks=0 so altering a column referenced by child FKs succeeds.
    SET @hmis_old_sql_mode  = @@SESSION.sql_mode;
    SET @hmis_old_fk_checks = @@SESSION.foreign_key_checks;
    SET SESSION sql_mode = '';
    SET FOREIGN_KEY_CHECKS = 0;

    -- Per-table fix loop lives in its OWN block so its tolerant SQLEXCEPTION
    -- handler (skip one failing table, keep going) does NOT also swallow the
    -- verification SIGNAL raised in the outer block below.
    fix_block: BEGIN
        DECLARE v_done    INT DEFAULT 0;
        DECLARE v_table   VARCHAR(255);
        DECLARE v_col     VARCHAR(255);
        DECLARE v_coltype VARCHAR(255);

        -- Only tables whose single-column BIGINT PK named ID lacks AUTO_INCREMENT.
        DECLARE cur CURSOR FOR
            SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND UPPER(COLUMN_NAME) = 'ID'
              AND COLUMN_KEY = 'PRI'
              AND DATA_TYPE = 'bigint'
              AND EXTRA NOT LIKE '%auto_increment%';

        DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;
        -- Skip a single un-alterable table (lock/timeout/privilege) and continue;
        -- the outer verification below still fails the migration if any remain.
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO v_table, v_col, v_coltype;
            IF v_done = 1 THEN
                LEAVE read_loop;
            END IF;
            SET @ddl = CONCAT('ALTER TABLE `', v_table, '` MODIFY COLUMN `', v_col, '` ', v_coltype, ' NOT NULL AUTO_INCREMENT');
            PREPARE st FROM @ddl;
            EXECUTE st;
            DEALLOCATE PREPARE st;
        END LOOP;
        CLOSE cur;
    END fix_block;

    -- Restore the original session state so the pooled connection is left clean.
    SET SESSION sql_mode = @hmis_old_sql_mode;
    SET FOREIGN_KEY_CHECKS = @hmis_old_fk_checks;

    -- Verification (outer block — NO SQLEXCEPTION handler here): count the ID
    -- primary keys that are STILL missing AUTO_INCREMENT. If any table could not
    -- be fixed, fail loudly so the runner records the migration as FAILED rather
    -- than SUCCESS. The Java runner never inspects SELECT output, so the failure
    -- must be raised here as a SIGNAL that propagates out through CALL.
    SELECT COUNT(*) INTO v_remaining
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(COLUMN_NAME) = 'ID'
      AND COLUMN_KEY = 'PRI'
      AND DATA_TYPE = 'bigint'
      AND EXTRA NOT LIKE '%auto_increment%';

    IF v_remaining > 0 THEN
        -- MESSAGE_TEXT is capped at 128 chars by MySQL; keep this short.
        SET @hmis_fail_msg = CONCAT('v2.9.0 FAILED: ', v_remaining,
            ' ID PK(s) lack AUTO_INCREMENT after fix; resolve lock/privilege & re-run.');
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = @hmis_fail_msg;
    END IF;
END$$
DELIMITER ;

CALL hmis_fix_autoincrement_v290();

DROP PROCEDURE IF EXISTS hmis_fix_autoincrement_v290;

-- ── AFTER: should return no rows ─────────────────────────────────────────────

SELECT 'AFTER: ID primary keys still missing AUTO_INCREMENT (should be empty)' AS status;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND UPPER(COLUMN_NAME) = 'ID'
  AND COLUMN_KEY = 'PRI'
  AND DATA_TYPE = 'bigint'
  AND EXTRA NOT LIKE '%auto_increment%'
ORDER BY TABLE_NAME;

SELECT 'Migration v2.9.0 completed' AS final_status;
