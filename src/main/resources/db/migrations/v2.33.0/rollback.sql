-- Rollback v2.33.0: drop the cancellation columns from PATIENTENCOUNTER
-- WARNING: the coded reasons and decision makers of cancelled admissions are lost. Only roll back
-- together with a build that no longer maps PatientEncounter.cancellationReason / cancellationDecidedBy.

SELECT 'Rollback v2.33.0 - drop cancellation columns from PATIENTENCOUNTER' AS status;

SET @enc_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER' LIMIT 1
);

SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER' AND UPPER(COLUMN_NAME) = 'CANCELLATIONDECIDEDBY_ID');
SET @sql = IF(@enc_table IS NULL OR @exists = 0, 'SELECT "SKIPPED: CANCELLATIONDECIDEDBY_ID not found" AS status',
    CONCAT('ALTER TABLE ', @enc_table, ' DROP COLUMN CANCELLATIONDECIDEDBY_ID'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER' AND UPPER(COLUMN_NAME) = 'CANCELLATIONREASON_ID');
SET @sql = IF(@enc_table IS NULL OR @exists = 0, 'SELECT "SKIPPED: CANCELLATIONREASON_ID not found" AS status',
    CONCAT('ALTER TABLE ', @enc_table, ' DROP COLUMN CANCELLATIONREASON_ID'));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- The reason list rows are CATEGORY rows with DTYPE = AdmissionCancellationReason.
SET @cat_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CATEGORY' LIMIT 1
);
SET @sql = IF(@cat_table IS NULL, 'SELECT "SKIPPED: CATEGORY table not found" AS status',
    CONCAT('DELETE FROM ', @cat_table, ' WHERE DTYPE = ''AdmissionCancellationReason'''));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
