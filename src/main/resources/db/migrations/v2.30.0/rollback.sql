-- Rollback v2.30.0: drop the patient complaints register tables
-- WARNING: all complaint data is lost. Only roll back together with a build that no longer maps
-- PatientComplaint / PatientComplaintLog.

SELECT 'Rollback v2.30.0 - Drop patient complaints register tables' AS status;

-- Log table first (it references the complaint table)
SET @log_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTCOMPLAINTLOG' LIMIT 1
);
SET @sql = IF(@log_table IS NULL,
    'SELECT "SKIPPED: PATIENTCOMPLAINTLOG not found" AS status',
    CONCAT('DROP TABLE ', @log_table)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @complaint_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTCOMPLAINT' LIMIT 1
);
SET @sql = IF(@complaint_table IS NULL,
    'SELECT "SKIPPED: PATIENTCOMPLAINT not found" AS status',
    CONCAT('DROP TABLE ', @complaint_table)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
