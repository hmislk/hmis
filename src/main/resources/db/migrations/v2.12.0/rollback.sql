-- Rollback for Migration v2.12.0: Remove notification.PATIENTENCOUNTER_ID
-- Issue: hmislk/hmis#19303
--
-- Drops the foreign key and the PATIENTENCOUNTER_ID column added by v2.12.0.
-- UNIVERSAL + IDEMPOTENT: resolves the real table-name case and guards each
-- statement on INFORMATION_SCHEMA so it is safe to re-run.

SELECT 'Rollback v2.12.0 - Remove notification.PATIENTENCOUNTER_ID' AS status;

SET @notification_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'NOTIFICATION'
    LIMIT 1
);

-- STEP 1: DROP FOREIGN KEY (if present)
SET @fk_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @notification_table
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
      AND CONSTRAINT_NAME = 'FK_notification_patientencounter'
);
SET @sql_drop_fk = IF(@notification_table IS NOT NULL AND @fk_exists > 0,
    CONCAT('ALTER TABLE ', @notification_table, ' DROP FOREIGN KEY FK_notification_patientencounter'),
    'SELECT ''FK_notification_patientencounter not present — skipping'' AS info'
);
PREPARE stmt FROM @sql_drop_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 2: DROP COLUMN (if present)
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @notification_table
      AND UPPER(COLUMN_NAME) = 'PATIENTENCOUNTER_ID'
);
SET @sql_drop_col = IF(@notification_table IS NOT NULL AND @col_exists > 0,
    CONCAT('ALTER TABLE ', @notification_table, ' DROP COLUMN PATIENTENCOUNTER_ID'),
    'SELECT ''notification.PATIENTENCOUNTER_ID not present — skipping'' AS info'
);
PREPARE stmt FROM @sql_drop_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.12.0 complete' AS status;
