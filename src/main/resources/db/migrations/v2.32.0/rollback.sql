-- Rollback v2.32.0: drop ADMISSIONTYPEHISTORY
-- WARNING: the dated history of changed admission types is lost. Admissions that were changed then
-- use their current type for the whole stay again. Only roll back together with a build that no
-- longer maps AdmissionTypeHistory.

SELECT 'Rollback v2.32.0 - drop ADMISSIONTYPEHISTORY' AS status;

SET @history_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'ADMISSIONTYPEHISTORY' LIMIT 1
);
SET @sql = IF(@history_table IS NULL,
    'SELECT "SKIPPED: ADMISSIONTYPEHISTORY not found" AS status',
    CONCAT('DROP TABLE ', @history_table)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
