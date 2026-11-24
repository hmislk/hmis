-- Migration v2.33.0: cancellation columns on PATIENTENCOUNTER
-- Issue: #25745 (cancelled admissions should track required information)
--
-- Adds CANCELLATIONREASON_ID (the coded reason, a row of CATEGORY) and CANCELLATIONDECIDEDBY_ID
-- (a STAFF row) to the admission table. PatientEncounter maps them, so without them every admission
-- load fails. Run this by hand BEFORE deploying a build that maps these fields.
-- The reason list itself reuses the existing CATEGORY table and needs no change.
-- Old cancelled admissions keep both columns empty.
--
-- Safe to re-run: each column is added only when missing.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.33.0 - cancellation columns on PATIENTENCOUNTER' AS status;

SET @enc_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER' LIMIT 1
);

SELECT CONCAT('PATIENTENCOUNTER: ', COALESCE(@enc_table, 'NOT FOUND')) AS info;

-- STEP 1: CANCELLATIONREASON_ID
SET @exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER' AND UPPER(COLUMN_NAME) = 'CANCELLATIONREASON_ID'
);
SET @sql = IF(@enc_table IS NULL,
    'SELECT "SKIPPED: PATIENTENCOUNTER table not found" AS status',
    IF(@exists > 0,
        'SELECT "SKIPPED: CANCELLATIONREASON_ID already exists" AS status',
        CONCAT('ALTER TABLE ', @enc_table, ' ADD COLUMN CANCELLATIONREASON_ID BIGINT DEFAULT NULL')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 2: CANCELLATIONDECIDEDBY_ID
SET @exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER' AND UPPER(COLUMN_NAME) = 'CANCELLATIONDECIDEDBY_ID'
);
SET @sql = IF(@enc_table IS NULL,
    'SELECT "SKIPPED: PATIENTENCOUNTER table not found" AS status',
    IF(@exists > 0,
        'SELECT "SKIPPED: CANCELLATIONDECIDEDBY_ID already exists" AS status',
        CONCAT('ALTER TABLE ', @enc_table, ' ADD COLUMN CANCELLATIONDECIDEDBY_ID BIGINT DEFAULT NULL')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.33.0 complete' AS status;
