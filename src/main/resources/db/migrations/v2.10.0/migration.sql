-- Migration v2.10.0: Backfill registrationSource from selfRegistered and drop selfRegistered column
-- Issue: hmislk/hmis#21199
--
-- Background:
--   Patient.selfRegistered (Boolean) was superseded by Patient.registrationSource
--   (PatientRegistrationSource enum) introduced in #21181. v2.1.19 back-filled
--   REGISTRATIONSOURCE = 'ONLINE_SELF' for selfRegistered=1 rows at the time of
--   that migration. This migration mops up any rows created since then with the
--   legacy path, then drops the column entirely now that all Java code has been
--   migrated to use registrationSource.
--
-- UNIVERSAL: Detects actual table name case (PATIENT vs patient) so this script
-- works on both case-sensitive (Linux, lower_case_table_names=0) and
-- case-insensitive (Windows) MySQL instances.
--
-- IDEMPOTENT: Both steps gate on INFORMATION_SCHEMA existence checks, so this
-- migration can be safely re-run after the column has already been dropped.

SELECT 'Migration v2.10.0 - Backfill registrationSource and drop selfRegistered column' AS status;

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

-- ==========================================
-- STEP 1: BACKFILL REMAINING ONLINE_SELF ROWS
-- ==========================================
-- Only runs when the legacy column still exists so this step is safe to skip on
-- databases where the column was already removed.

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
                     ' WHERE SELFREGISTERED = 1 AND (REGISTRATIONSOURCE IS NULL OR REGISTRATIONSOURCE = '''')'),
              'SELECT 1 /* selfRegistered column already absent, skipping backfill */');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IF(@has_self_reg > 0,
          CONCAT('Rows backfilled to ONLINE_SELF: ', ROW_COUNT()),
          'Backfill skipped — selfRegistered column not present') AS backfill_status;

-- ==========================================
-- STEP 2: DROP THE REDUNDANT COLUMN
-- ==========================================

SET @sql = IF(@patient_table IS NOT NULL AND @has_self_reg > 0,
              CONCAT('ALTER TABLE ', @patient_table, ' DROP COLUMN SELFREGISTERED'),
              'SELECT 1 /* selfRegistered column already absent, skipping drop */');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 3: VERIFY
-- ==========================================

SET @sql = IF(@patient_table IS NOT NULL,
              CONCAT('SELECT REGISTRATIONSOURCE AS registration_source, COUNT(*) AS patients FROM ',
                     @patient_table, ' GROUP BY REGISTRATIONSOURCE'),
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.10.0 completed' AS final_status;
