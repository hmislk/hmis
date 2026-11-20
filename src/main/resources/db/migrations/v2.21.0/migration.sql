-- Migration v2.21.0: Add AdmissionNumber.COUNTERYEAR
-- Issue: #22142 - BHT number series shared across types / reset every year (code merged in #25654)
--
-- The AdmissionNumber entity maps counterYear (nullable Integer). The BHT number
-- counter query names this column, so on a database without it every admission
-- fails with a missing-column error. #25654 shipped without a migration.
-- NULL means the single, never-resetting series, so existing rows stay valid
-- and keep working exactly as before. No data is changed.
--
-- Safe to re-run: the column is added only when it does not already exist.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.21.0 - Add AdmissionNumber.COUNTERYEAR' AS status;

SET @an_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'ADMISSIONNUMBER'
    LIMIT 1
);

SELECT CONCAT('AdmissionNumber table: ', COALESCE(@an_table, 'NOT FOUND')) AS info;

SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @an_table
      AND UPPER(COLUMN_NAME) = 'COUNTERYEAR'
);
SET @sql = IF(@an_table IS NULL,
    'SELECT "SKIPPED: ADMISSIONNUMBER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: COUNTERYEAR already exists" AS status',
        CONCAT('ALTER TABLE ', @an_table, ' ADD COLUMN COUNTERYEAR INT NULL')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
