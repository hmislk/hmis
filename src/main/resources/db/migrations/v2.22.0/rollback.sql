-- Rollback v2.22.0: drop BILLFEE.COLLECTEDBYDOCTORATBILLING
-- Idempotent and case-insensitive: only drops what exists. The flag values are lost.
--
-- DEPLOY ORDER: stop every instance that maps BILLFEE.COLLECTEDBYDOCTORATBILLING (the current
-- build reads it when loading BillFee and in its billing queries) and restore the prior WAR
-- BEFORE running this script. Run while the current WAR still serves, those queries fail.
-- WARNING: dropping the column loses every saved "Collected by Doctor" flag.

SET @billfee_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILLFEE'
    LIMIT 1
);

SET @col = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @billfee_table
      AND UPPER(COLUMN_NAME) = 'COLLECTEDBYDOCTORATBILLING'
    LIMIT 1
);

SET @sql = IF(@billfee_table IS NOT NULL AND @col IS NOT NULL,
    CONCAT('ALTER TABLE ', @billfee_table, ' DROP COLUMN ', @col),
    'SELECT "COLLECTEDBYDOCTORATBILLING not found - nothing to drop" AS info'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.22.0 complete' AS status;
