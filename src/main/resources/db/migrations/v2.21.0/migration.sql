-- Migration v2.21.0: Add BILLFEE.COLLECTEDBYDOCTORATBILLING
-- Issue: #17490 - Inward professional fee "Collected by Doctor" at billing
--
-- BillFee gains a boolean COLLECTEDBYDOCTORATBILLING, set only by the "Collected by Doctor"
-- switch on the inward professional fee add page. It is separate from FEECOLLECTEDBYDOCTOR,
-- which the surgery payment page sets, so fees flagged there keep their current behaviour.
-- Production persistence.xml has no DDL generation, so the column is created here.
-- Existing rows get 0 (false). No existing data is changed.
--
-- DEPLOY ORDER: run this migration BEFORE deploying the WAR that contains the new field.
-- Every BillFee query names the column, so a build without it fails on billing pages.
--
-- Safe to re-run: the column is added only when it does not already exist.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.21.0 - Add BILLFEE.COLLECTEDBYDOCTORATBILLING' AS status;

SET @billfee_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILLFEE'
    LIMIT 1
);

SELECT CONCAT('BillFee table: ', COALESCE(@billfee_table, 'NOT FOUND')) AS info;

SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
      AND UPPER(COLUMN_NAME) = 'COLLECTEDBYDOCTORATBILLING'
);

SET @sql = IF(@billfee_table IS NULL,
    'SELECT "SKIPPED: BILLFEE table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: COLLECTEDBYDOCTORATBILLING already exists" AS status',
        CONCAT('ALTER TABLE ', @billfee_table, ' ADD COLUMN COLLECTEDBYDOCTORATBILLING TINYINT(1) NOT NULL DEFAULT 0')
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.21.0 complete' AS status;
