-- Migration v2.1.20: Add ENCOUNTERREGISTRATIONFLAG column to PATIENTENCOUNTER and back-fill legacy rows
-- Issue: #21182 - Add On Admission Death flag to PatientEncounter/Admission
-- Author: Dr M H B Ariyaratne
-- Date: 2026-06-03
-- Database: main HMIS database (hmisPU)
--
-- Purpose:
--   1. Add a STRING enum column ENCOUNTERREGISTRATIONFLAG to the PATIENTENCOUNTER
--      table that records an operational/clinical flag for each encounter at
--      registration time (STANDARD, ON_ADMISSION_DEATH, RAPID_TEMP_AE).
--   2. Back-fill all existing rows (NULL flag) with 'STANDARD'. Unlike the
--      registration source, an encounter has no meaningful "unknown" flag state:
--      every legacy admission was a standard admission, so NULL normalises to
--      STANDARD both here and in the entity (getter + @PrePersist/@PreUpdate).
--
-- PATIENTENCOUNTER is a SINGLE_TABLE inheritance root, so this single column also
-- covers Admission and every other PatientEncounter subclass.
--
-- UNIVERSAL: Detects actual table name case (PATIENTENCOUNTER vs patientencounter)
-- so this script works on both case-sensitive (Linux, lower_case_table_names=0)
-- and case-insensitive (Windows) MySQL instances. All DDL is built with prepared
-- statements against the real table name from INFORMATION_SCHEMA.
--
-- IDEMPOTENT: All statements check for existence first, so this migration can be
-- safely re-run without error.

SELECT 'Migration v2.1.20 - Add ENCOUNTERREGISTRATIONFLAG to PATIENTENCOUNTER' AS status;

-- ==========================================
-- STEP 0: DETECT ACTUAL TABLE NAME CASE
-- ==========================================

SET @pe_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER'
    LIMIT 1
);

SELECT CONCAT('Detected encounter table as: ', IFNULL(@pe_table, '(not found)')) AS info;

SELECT IF(@pe_table IS NULL,
          'PATIENTENCOUNTER table not found. Migration aborted.',
          'PATIENTENCOUNTER table found; proceeding') AS applicability;

-- ==========================================
-- STEP 1: ADD ENCOUNTERREGISTRATIONFLAG COLUMN
-- ==========================================
-- DEFAULT 'STANDARD' so any direct insert that omits the column lands on the
-- default. Column is left nullable to avoid blocking legacy raw inserts; the
-- entity lifecycle hooks and STEP 2 keep values non-null in practice.

SET @has_flag = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @pe_table
      AND UPPER(COLUMN_NAME) = 'ENCOUNTERREGISTRATIONFLAG'
);

SET @sql = IF(@pe_table IS NOT NULL AND @has_flag = 0,
              CONCAT('ALTER TABLE ', @pe_table,
                     ' ADD COLUMN ENCOUNTERREGISTRATIONFLAG VARCHAR(255) DEFAULT ''STANDARD'''),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 2: BACK-FILL LEGACY NULL ROWS
-- ==========================================
-- Every existing encounter with a NULL flag was a standard admission.

SET @sql = IF(@pe_table IS NOT NULL,
              CONCAT('UPDATE ', @pe_table,
                     ' SET ENCOUNTERREGISTRATIONFLAG = ''STANDARD''',
                     ' WHERE ENCOUNTERREGISTRATIONFLAG IS NULL'),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 3: POST-MIGRATION VERIFICATION
-- ==========================================

SELECT 'ENCOUNTERREGISTRATIONFLAG column' AS status;

SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @pe_table
  AND UPPER(COLUMN_NAME) = 'ENCOUNTERREGISTRATIONFLAG';

SET @sql = IF(@pe_table IS NOT NULL,
              CONCAT('SELECT ENCOUNTERREGISTRATIONFLAG AS encounter_flag, COUNT(*) AS encounters FROM ',
                     @pe_table, ' GROUP BY ENCOUNTERREGISTRATIONFLAG'),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.1.20 completed - ENCOUNTERREGISTRATIONFLAG ready' AS final_status;
