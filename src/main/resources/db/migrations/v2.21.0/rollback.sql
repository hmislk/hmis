-- Rollback v2.21.0: drop AdmissionNumber.COUNTERYEAR
-- WARNING: only roll back together with a build that no longer maps
-- AdmissionNumber.counterYear (before #25654), or every BHT number read fails again.
-- Any per-year counter rows are kept but lose their year.

SELECT 'Rollback v2.21.0 - Drop AdmissionNumber.COUNTERYEAR' AS status;

SET @an_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'ADMISSIONNUMBER'
    LIMIT 1
);

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @an_table
      AND UPPER(COLUMN_NAME) = 'COUNTERYEAR'
    LIMIT 1
);
SET @sql = IF(@an_table IS NULL OR @col IS NULL,
    'SELECT "SKIPPED: COUNTERYEAR not found" AS status',
    CONCAT('ALTER TABLE ', @an_table, ' DROP COLUMN ', @col)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
