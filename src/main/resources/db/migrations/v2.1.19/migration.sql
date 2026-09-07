-- Migration v2.1.19: Add REGISTRATIONSOURCE column to PATIENT and migrate legacy SELFREGISTERED
-- Issue: #21181 - Add PatientRegistrationSource field to Patient entity
-- Author: Dr M H B Ariyaratne
-- Date: 2026-06-03
-- Database: main HMIS database (hmisPU)
--
-- Purpose:
--   1. Add a STRING enum column REGISTRATIONSOURCE to the PATIENT table that
--      permanently records how a patient was first entered into the system.
--   2. Back-fill existing rows where the legacy boolean SELFREGISTERED = 1 with
--      the value 'ONLINE_SELF' (patient-portal self registrations).
--
-- Existing patients with SELFREGISTERED = 0 / NULL are intentionally left with a
-- NULL REGISTRATIONSOURCE — null means "unrecorded / pre-feature" and renders no
-- badge in the UI.
--
-- The legacy SELFREGISTERED column is NOT dropped here. It is deprecated in code
-- and its physical removal is deferred to a later migration once every deployment
-- has back-filled REGISTRATIONSOURCE.
--
-- UNIVERSAL: Detects actual table name case (PATIENT vs patient) so this script
-- works on both case-sensitive (Linux, lower_case_table_names=0) and
-- case-insensitive (Windows) MySQL instances. All DDL is built with prepared
-- statements against the real table name from INFORMATION_SCHEMA.
--
-- IDEMPOTENT: All statements check for existence first, so this migration can be
-- safely re-run without error.

SELECT 'Migration v2.1.19 - Add REGISTRATIONSOURCE to PATIENT' AS status;

-- ==========================================
-- STEP 0: DETECT ACTUAL TABLE NAME CASE
-- ==========================================

SET @patient_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PATIENT'
    LIMIT 1
);

SELECT CONCAT('Detected patient table as: ', IFNULL(@patient_table, '(not found)')) AS info;

SELECT IF(@patient_table IS NULL,
          'PATIENT table not found. Migration aborted.',
          'PATIENT table found; proceeding') AS applicability;

-- ==========================================
-- STEP 1: ADD REGISTRATIONSOURCE COLUMN
-- ==========================================

SET @has_reg_source = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @patient_table
      AND UPPER(COLUMN_NAME) = 'REGISTRATIONSOURCE'
);

SET @sql = IF(@patient_table IS NOT NULL AND @has_reg_source = 0,
              CONCAT('ALTER TABLE ', @patient_table, ' ADD COLUMN REGISTRATIONSOURCE VARCHAR(255) NULL'),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 2: BACK-FILL FROM LEGACY SELFREGISTERED
-- ==========================================
-- Only patients flagged as self-registered (portal) get ONLINE_SELF. Run only
-- when the legacy column still exists, and never overwrite a value already set.

SET @has_self_reg = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @patient_table
      AND UPPER(COLUMN_NAME) = 'SELFREGISTERED'
);

SET @sql = IF(@patient_table IS NOT NULL AND @has_self_reg > 0,
              CONCAT('UPDATE ', @patient_table,
                     ' SET REGISTRATIONSOURCE = ''ONLINE_SELF''',
                     ' WHERE SELFREGISTERED = 1 AND REGISTRATIONSOURCE IS NULL'),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 3: POST-MIGRATION VERIFICATION
-- ==========================================

SELECT 'REGISTRATIONSOURCE column' AS status;

SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @patient_table
  AND UPPER(COLUMN_NAME) = 'REGISTRATIONSOURCE';

SET @sql = IF(@patient_table IS NOT NULL,
              CONCAT('SELECT REGISTRATIONSOURCE AS registration_source, COUNT(*) AS patients FROM ',
                     @patient_table, ' GROUP BY REGISTRATIONSOURCE'),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.1.19 completed - REGISTRATIONSOURCE ready' AS final_status;
