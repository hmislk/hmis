-- Migration v2.12.0: Add notification.PATIENTENCOUNTER_ID for discharge notifications
-- Issue: hmislk/hmis#19303
--
-- Background:
--   Three-stage inpatient discharge notifications (clinical, room, final) key the
--   Notification on a PatientEncounter rather than a Bill or PatientRoom. This adds
--   the backing @ManyToOne column notification.PATIENTENCOUNTER_ID and its FK to
--   patientencounter(ID). Production persistence.xml has no DDL generation, so the
--   column must be created explicitly here.
--
-- UNIVERSAL: Detects actual table name case via INFORMATION_SCHEMA so this script
--   works on both case-sensitive (Linux) and case-insensitive (Windows) MySQL.
--
-- IDEMPOTENT: The ADD COLUMN and ADD FOREIGN KEY statements are gated on
--   INFORMATION_SCHEMA existence checks so the script can be safely re-run.

SELECT 'Migration v2.12.0 - Add notification.PATIENTENCOUNTER_ID' AS status;

-- ==========================================
-- STEP 0: RESOLVE ACTUAL TABLE NAMES
-- ==========================================

SET @notification_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'NOTIFICATION'
    LIMIT 1
);

SET @patientencounter_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER'
    LIMIT 1
);

SELECT CONCAT('Resolved notification table as: ', IFNULL(@notification_table, '(not found)')) AS info;
SELECT CONCAT('Resolved patientencounter table as: ', IFNULL(@patientencounter_table, '(not found)')) AS info;

SELECT IF(@notification_table IS NULL,
          'notification table not found; v2.12.0 is not applicable and completes as a successful no-op',
          'notification table found; ensuring PATIENTENCOUNTER_ID column and FK exist') AS applicability;

-- ==========================================
-- STEP 1: ADD COLUMN PATIENTENCOUNTER_ID (if missing)
-- ==========================================

SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @notification_table
      AND UPPER(COLUMN_NAME) = 'PATIENTENCOUNTER_ID'
);

SET @sql_add_col = IF(@notification_table IS NOT NULL AND @col_exists = 0,
    CONCAT('ALTER TABLE ', @notification_table, ' ADD COLUMN PATIENTENCOUNTER_ID BIGINT NULL'),
    'SELECT ''notification.PATIENTENCOUNTER_ID already exists or table missing — skipping column add'' AS info'
);
PREPARE stmt FROM @sql_add_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 2: ADD FOREIGN KEY to patientencounter(ID) (if missing)
-- ==========================================

SET @fk_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @notification_table
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
      AND CONSTRAINT_NAME = 'FK_notification_patientencounter'
);

SET @sql_add_fk = IF(
    @notification_table IS NOT NULL
    AND @patientencounter_table IS NOT NULL
    AND @fk_exists = 0,
    CONCAT('ALTER TABLE ', @notification_table,
           ' ADD CONSTRAINT FK_notification_patientencounter',
           ' FOREIGN KEY (PATIENTENCOUNTER_ID) REFERENCES ', @patientencounter_table, '(ID)'),
    'SELECT ''FK_notification_patientencounter already exists or a table is missing — skipping FK add'' AS info'
);
PREPARE stmt FROM @sql_add_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.12.0 complete' AS status;
