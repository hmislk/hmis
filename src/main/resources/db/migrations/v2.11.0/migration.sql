-- Migration v2.11.0: Create patient merge audit tables
-- Issue: hmislk/hmis#21209
--
-- Background:
--   Introduces the two tables that back the patient duplicate merge/unmerge
--   system: patientmergerecord (one row per merge event) and
--   patientmergeaffectedrecord (one row per entity re-pointed during a merge).
--   These tables are the foundation for #21210–#21214.
--
-- UNIVERSAL: Detects actual table name case via INFORMATION_SCHEMA so this
--   script works on both case-sensitive (Linux) and case-insensitive (Windows)
--   MySQL instances.
--
-- IDEMPOTENT: Both CREATE TABLE statements are gated on INFORMATION_SCHEMA
--   existence checks so the script can be safely re-run.

SELECT 'Migration v2.11.0 - Create patient merge audit tables' AS status;

-- ==========================================
-- STEP 1: CREATE patientmergerecord
-- ==========================================

SET @pmr_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PATIENTMERGERECORD'
);

SET @sql_pmr = IF(@pmr_exists = 0,
    'CREATE TABLE patientmergerecord (
        ID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        MERGEDATE DATETIME,
        MERGEDBY_ID BIGINT,
        PRIMARYPATIENT_ID BIGINT,
        SECONDARYPATIENT_ID BIGINT,
        PRIMARYSNAPSHOTJSON LONGTEXT,
        SECONDARYSNAPSHOTJSON LONGTEXT,
        MERGEREASON VARCHAR(500),
        MERGETYPE VARCHAR(30),
        STATUS VARCHAR(20),
        REVERSEDBY_ID BIGINT,
        REVERSEDAT DATETIME
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4',
    'SELECT ''patientmergerecord already exists — skipping'' AS info'
);

PREPARE stmt_pmr FROM @sql_pmr;
EXECUTE stmt_pmr;
DEALLOCATE PREPARE stmt_pmr;

-- ==========================================
-- STEP 2: CREATE patientmergeaffectedrecord
-- ==========================================

SET @pmar_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'PATIENTMERGEAFFECTEDRECORD'
);

SET @sql_pmar = IF(@pmar_exists = 0,
    'CREATE TABLE patientmergeaffectedrecord (
        ID BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
        MERGERECORD_ID BIGINT NOT NULL,
        ENTITYCLASS VARCHAR(100),
        ENTITYID BIGINT,
        OLDPATIENTID BIGINT,
        NEWPATIENTID BIGINT,
        FOREIGN KEY (MERGERECORD_ID) REFERENCES patientmergerecord(ID)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4',
    'SELECT ''patientmergeaffectedrecord already exists — skipping'' AS info'
);

PREPARE stmt_pmar FROM @sql_pmar;
EXECUTE stmt_pmar;
DEALLOCATE PREPARE stmt_pmar;

SELECT 'Migration v2.11.0 complete' AS status;
