-- Rollback v2.1.19: Drop REGISTRATIONSOURCE column from PATIENT
-- Issue: #21181
--
-- WARNING: This is destructive. Dropping the column discards all recorded
-- registration sources. The legacy SELFREGISTERED column is untouched, so
-- portal self-registrations remain recoverable, but walk-in / inward / newborn
-- source data captured after the feature shipped will be lost.
--
-- UNIVERSAL: handles both case-sensitive and case-insensitive MySQL.

SELECT 'Rollback v2.1.19 - Drop REGISTRATIONSOURCE column from PATIENT' AS status;

SET @patient_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PATIENT'
    LIMIT 1
);

SELECT CONCAT('Detected patient table as: ', IFNULL(@patient_table, '(not found - nothing to drop)')) AS info;

SET @has_reg_source = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @patient_table
      AND UPPER(COLUMN_NAME) = 'REGISTRATIONSOURCE'
);

SET @sql = IF(@patient_table IS NOT NULL AND @has_reg_source > 0,
              CONCAT('ALTER TABLE ', @patient_table, ' DROP COLUMN REGISTRATIONSOURCE'),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Rollback v2.1.19 completed' AS final_status;
