-- Rollback v2.1.20: Drop ENCOUNTERREGISTRATIONFLAG column from PATIENTENCOUNTER
-- Issue: #21182
--
-- WARNING: This is destructive. Dropping the column discards every recorded
-- encounter flag (e.g. On Admission Death markers captured after the feature
-- shipped).
--
-- UNIVERSAL: handles both case-sensitive and case-insensitive MySQL.

SELECT 'Rollback v2.1.20 - Drop ENCOUNTERREGISTRATIONFLAG column from PATIENTENCOUNTER' AS status;

SET @pe_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER'
    LIMIT 1
);

SELECT CONCAT('Detected encounter table as: ', IFNULL(@pe_table, '(not found - nothing to drop)')) AS info;

SET @has_flag = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @pe_table
      AND UPPER(COLUMN_NAME) = 'ENCOUNTERREGISTRATIONFLAG'
);

SET @sql = IF(@pe_table IS NOT NULL AND @has_flag > 0,
              CONCAT('ALTER TABLE ', @pe_table, ' DROP COLUMN ENCOUNTERREGISTRATIONFLAG'),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.1.20 completed' AS final_status;
