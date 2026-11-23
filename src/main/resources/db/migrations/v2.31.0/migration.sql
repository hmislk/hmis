-- Migration v2.31.0: close columns on CREDITCOMPANYCLAIM
-- Issue: #25847 (OPD credit company claims, close a partially settled claim)
--
-- Adds CLOSEREASON, CLOSEDAT and CLOSEDBY_ID (with a foreign key to WEBUSER) to the claim table
-- created by v2.26.0. The CreditCompanyClaim entity maps them, so without them every claim load fails.
-- Run this by hand BEFORE deploying a build that maps these fields.
--
-- Safe to re-run: each column and the foreign key are added only when missing.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.31.0 - close columns on CREDITCOMPANYCLAIM' AS status;

SET @claim_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYCLAIM' LIMIT 1
);
SET @webuser_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'WEBUSER' LIMIT 1
);

SELECT CONCAT('CREDITCOMPANYCLAIM: ', COALESCE(@claim_table, 'NOT FOUND'),
              ', WEBUSER: ', COALESCE(@webuser_table, 'NOT FOUND')) AS info;

-- STEP 1: CLOSEREASON
SET @exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYCLAIM' AND UPPER(COLUMN_NAME) = 'CLOSEREASON'
);
SET @sql = IF(@claim_table IS NULL,
    'SELECT "SKIPPED: CREDITCOMPANYCLAIM table not found" AS status',
    IF(@exists > 0,
        'SELECT "SKIPPED: CLOSEREASON already exists" AS status',
        CONCAT('ALTER TABLE ', @claim_table, ' ADD COLUMN CLOSEREASON VARCHAR(255) DEFAULT NULL')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 2: CLOSEDAT
SET @exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYCLAIM' AND UPPER(COLUMN_NAME) = 'CLOSEDAT'
);
SET @sql = IF(@claim_table IS NULL,
    'SELECT "SKIPPED: CREDITCOMPANYCLAIM table not found" AS status',
    IF(@exists > 0,
        'SELECT "SKIPPED: CLOSEDAT already exists" AS status',
        CONCAT('ALTER TABLE ', @claim_table, ' ADD COLUMN CLOSEDAT DATETIME DEFAULT NULL')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 3: CLOSEDBY_ID
SET @exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYCLAIM' AND UPPER(COLUMN_NAME) = 'CLOSEDBY_ID'
);
SET @sql = IF(@claim_table IS NULL,
    'SELECT "SKIPPED: CREDITCOMPANYCLAIM table not found" AS status',
    IF(@exists > 0,
        'SELECT "SKIPPED: CLOSEDBY_ID already exists" AS status',
        CONCAT('ALTER TABLE ', @claim_table, ' ADD COLUMN CLOSEDBY_ID BIGINT DEFAULT NULL')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- STEP 4: foreign key CLOSEDBY_ID -> WEBUSER (index is created by MySQL with the constraint)
SET @fk_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYCLAIM'
      AND UPPER(CONSTRAINT_NAME) = 'FK_CREDITCOMPANYCLAIM_CLOSEDBY_ID'
);
SET @sql = IF(@claim_table IS NULL OR @webuser_table IS NULL,
    'SELECT "SKIPPED: CREDITCOMPANYCLAIM or WEBUSER table not found" AS status',
    IF(@fk_exists > 0,
        'SELECT "SKIPPED: FK_CREDITCOMPANYCLAIM_CLOSEDBY_ID already exists" AS status',
        CONCAT('ALTER TABLE ', @claim_table, ' ADD CONSTRAINT FK_CREDITCOMPANYCLAIM_CLOSEDBY_ID FOREIGN KEY (CLOSEDBY_ID) REFERENCES ', @webuser_table, ' (ID)')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
