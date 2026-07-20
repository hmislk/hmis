-- Migration v2.15.0: Add patienttransferrequest.THEATREROOM_ID for theatre stay billing
-- Issue: hmislk/hmis#22213
--
-- Background:
--   PatientTransferRequest gained a new @ManyToOne field `theatreRoom` (a PatientRoom
--   subtype, TheatreRoom) so theatre stays get billed alongside the still-active ward
--   room, instead of being silently dropped from Room Stay History / Interim Bill /
--   Summary / Charges. Production persistence.xml has no DDL generation, so the
--   column and its FK must be created explicitly here.
--
-- UNIVERSAL: Detects actual table name case via INFORMATION_SCHEMA so this script
--   works on both case-sensitive (Linux) and case-insensitive (Windows) MySQL.
--
-- IDEMPOTENT: The ADD COLUMN and ADD FOREIGN KEY statements are gated on
--   INFORMATION_SCHEMA existence checks so the script can be safely re-run.

SELECT 'Migration v2.15.0 - Add patienttransferrequest.THEATREROOM_ID' AS status;

-- ==========================================
-- STEP 0: RESOLVE ACTUAL TABLE NAMES
-- ==========================================

SET @transferrequest_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PATIENTTRANSFERREQUEST'
    LIMIT 1
);

SET @patientroom_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PATIENTROOM'
    LIMIT 1
);

SELECT CONCAT('Resolved patienttransferrequest table as: ', IFNULL(@transferrequest_table, '(not found)')) AS info;
SELECT CONCAT('Resolved patientroom table as: ', IFNULL(@patientroom_table, '(not found)')) AS info;

SELECT IF(@transferrequest_table IS NULL,
          'patienttransferrequest table not found; v2.15.0 is not applicable and completes as a successful no-op',
          'patienttransferrequest table found; ensuring THEATREROOM_ID column and FK exist') AS applicability;

-- ==========================================
-- STEP 1: ADD COLUMN THEATREROOM_ID (if missing)
-- ==========================================

SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @transferrequest_table
      AND UPPER(COLUMN_NAME) = 'THEATREROOM_ID'
);

SET @sql_add_col = IF(@transferrequest_table IS NOT NULL AND @col_exists = 0,
    CONCAT('ALTER TABLE ', @transferrequest_table, ' ADD COLUMN THEATREROOM_ID BIGINT NULL'),
    'SELECT ''patienttransferrequest.THEATREROOM_ID already exists or table missing — skipping column add'' AS info'
);
PREPARE stmt FROM @sql_add_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- STEP 2: ADD FOREIGN KEY to patientroom(ID) (if missing)
-- ==========================================

SET @fk_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @transferrequest_table
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
      AND CONSTRAINT_NAME = 'FK_patienttransferrequest_theatreroom'
);

SET @sql_add_fk = IF(
    @transferrequest_table IS NOT NULL
    AND @patientroom_table IS NOT NULL
    AND @fk_exists = 0,
    CONCAT('ALTER TABLE ', @transferrequest_table,
           ' ADD CONSTRAINT FK_patienttransferrequest_theatreroom',
           ' FOREIGN KEY (THEATREROOM_ID) REFERENCES ', @patientroom_table, '(ID)'),
    'SELECT ''FK_patienttransferrequest_theatreroom already exists or a table is missing — skipping FK add'' AS info'
);
PREPARE stmt FROM @sql_add_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.15.0 complete' AS status;
