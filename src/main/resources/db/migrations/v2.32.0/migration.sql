-- Migration v2.32.0: dated admission type history
-- Issue: #25814 (Change Admission Type)
--
-- Creates ADMISSIONTYPEHISTORY, mapped by the AdmissionTypeHistory entity. Production has no
-- DDL generation, so run this before deploying a build that includes that entity.
-- An admission with no row keeps its current admission type for the whole stay (same as before),
-- so no data is written here.
--
-- Safe to re-run: the table is created only when missing.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.
-- The new table is created in the same case as the existing WEBUSER table.

SELECT 'Migration v2.32.0 - ADMISSIONTYPEHISTORY' AS status;

SET @webuser_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'WEBUSER' LIMIT 1
);
SET @encounter_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'PATIENTENCOUNTER' LIMIT 1
);
SET @category_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CATEGORY' LIMIT 1
);

SELECT CONCAT('WEBUSER: ', COALESCE(@webuser_table, 'NOT FOUND'),
              ', PATIENTENCOUNTER: ', COALESCE(@encounter_table, 'NOT FOUND'),
              ', CATEGORY: ', COALESCE(@category_table, 'NOT FOUND')) AS info;

-- Case for the new table follows the existing WEBUSER table
SET @history_table = IF(@webuser_table = UPPER(@webuser_table), 'ADMISSIONTYPEHISTORY', 'admissiontypehistory');

-- STEP 1: ADMISSIONTYPEHISTORY (AdmissionType is a Category subclass, so ADMISSIONTYPE_ID points at CATEGORY)
SET @exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'ADMISSIONTYPEHISTORY'
);
SET @sql = IF(@webuser_table IS NULL OR @encounter_table IS NULL OR @category_table IS NULL,
    'SELECT "SKIPPED: WEBUSER, PATIENTENCOUNTER or CATEGORY table not found" AS status',
    IF(@exists > 0,
        'SELECT "SKIPPED: ADMISSIONTYPEHISTORY already exists" AS status',
        CONCAT('CREATE TABLE ', @history_table, ' (
            ID BIGINT NOT NULL AUTO_INCREMENT,
            CREATEDAT DATETIME DEFAULT NULL,
            FROMDATE DATETIME DEFAULT NULL,
            REASON VARCHAR(500) DEFAULT NULL,
            TODATE DATETIME DEFAULT NULL,
            ADMISSIONTYPE_ID BIGINT DEFAULT NULL,
            CREATER_ID BIGINT DEFAULT NULL,
            PATIENTENCOUNTER_ID BIGINT DEFAULT NULL,
            PRIMARY KEY (ID),
            KEY FK_ADMISSIONTYPEHISTORY_ADMISSIONTYPE_ID (ADMISSIONTYPE_ID),
            KEY FK_ADMISSIONTYPEHISTORY_CREATER_ID (CREATER_ID),
            KEY FK_ADMISSIONTYPEHISTORY_PATIENTENCOUNTER_ID (PATIENTENCOUNTER_ID),
            CONSTRAINT FK_ADMISSIONTYPEHISTORY_ADMISSIONTYPE_ID FOREIGN KEY (ADMISSIONTYPE_ID) REFERENCES ', @category_table, ' (ID),
            CONSTRAINT FK_ADMISSIONTYPEHISTORY_CREATER_ID FOREIGN KEY (CREATER_ID) REFERENCES ', @webuser_table, ' (ID),
            CONSTRAINT FK_ADMISSIONTYPEHISTORY_PATIENTENCOUNTER_ID FOREIGN KEY (PATIENTENCOUNTER_ID) REFERENCES ', @encounter_table, ' (ID)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
