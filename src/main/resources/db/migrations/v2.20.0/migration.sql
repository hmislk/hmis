-- Migration v2.20.0: Add the WebUser two-factor columns
-- Author: Dr M H B Ariyaratne
-- Issue: #25075 - Login and API key lookup fail silently when the database is missing new WebUser columns
--
-- WebUser maps eleven TWOFACTOR* fields (authenticator app, #24869; SMS option, #25056).
-- Every JPQL query that loads a WebUser names these columns, so on a database that lacks
-- them every web login shows "Invalid User" and every API key lookup fails.
-- This script adds any of the columns that are missing, with the types and defaults
-- EclipseLink generates for the entity fields. Existing users get 0/false for the flags
-- and the failed-attempt counter, and NULL for the rest, which means "two-factor off".
--
-- Safe to re-run: each column is added only when it does not already exist.
-- No existing data is changed.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.20.0 - Add WebUser two-factor columns' AS status;

SET @webuser_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'WEBUSER'
    LIMIT 1
);

SELECT CONCAT('WebUser table: ', COALESCE(@webuser_table, 'NOT FOUND')) AS info;

-- TWOFACTORREQUIRED (#24869)
SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORREQUIRED'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: TWOFACTORREQUIRED already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN TWOFACTORREQUIRED TINYINT(1) DEFAULT 0')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TWOFACTORENROLLED (#24869)
SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORENROLLED'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: TWOFACTORENROLLED already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN TWOFACTORENROLLED TINYINT(1) DEFAULT 0')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TWOFACTORSECRET (#24869)
SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORSECRET'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: TWOFACTORSECRET already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN TWOFACTORSECRET VARCHAR(255)')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TWOFACTORENROLLEDAT (#24869)
SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORENROLLEDAT'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: TWOFACTORENROLLEDAT already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN TWOFACTORENROLLEDAT DATETIME')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TWOFACTORLASTUSEDSTEP (#24869)
SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORLASTUSEDSTEP'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: TWOFACTORLASTUSEDSTEP already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN TWOFACTORLASTUSEDSTEP BIGINT')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TWOFACTORFAILEDATTEMPTS (#24869)
SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORFAILEDATTEMPTS'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: TWOFACTORFAILEDATTEMPTS already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN TWOFACTORFAILEDATTEMPTS INTEGER DEFAULT 0')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TWOFACTORLOCKED (#24869)
SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORLOCKED'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: TWOFACTORLOCKED already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN TWOFACTORLOCKED TINYINT(1) DEFAULT 0')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TWOFACTORLOCKEDAT (#24869)
SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORLOCKEDAT'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: TWOFACTORLOCKEDAT already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN TWOFACTORLOCKEDAT DATETIME')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TWOFACTORSMSALLOWED (#25056)
SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORSMSALLOWED'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: TWOFACTORSMSALLOWED already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN TWOFACTORSMSALLOWED TINYINT(1) DEFAULT 0')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TWOFACTORMOBILE (#25056)
SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORMOBILE'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: TWOFACTORMOBILE already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN TWOFACTORMOBILE VARCHAR(255)')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- TWOFACTORMOBILEVERIFIEDAT (#25056)
SET @has_col = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
      AND UPPER(COLUMN_NAME) = 'TWOFACTORMOBILEVERIFIEDAT'
);
SET @sql = IF(@webuser_table IS NULL,
    'SELECT "SKIPPED: WEBUSER table not found" AS status',
    IF(@has_col > 0,
        'SELECT "SKIPPED: TWOFACTORMOBILEVERIFIEDAT already exists" AS status',
        CONCAT('ALTER TABLE ', @webuser_table, ' ADD COLUMN TWOFACTORMOBILEVERIFIEDAT DATETIME')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT CONCAT('WebUser two-factor columns after migration: ', COUNT(*), ' of 11') AS status
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @webuser_table
  AND UPPER(COLUMN_NAME) IN ('TWOFACTORREQUIRED', 'TWOFACTORENROLLED', 'TWOFACTORSECRET', 'TWOFACTORENROLLEDAT', 'TWOFACTORLASTUSEDSTEP', 'TWOFACTORFAILEDATTEMPTS', 'TWOFACTORLOCKED', 'TWOFACTORLOCKEDAT', 'TWOFACTORSMSALLOWED', 'TWOFACTORMOBILE', 'TWOFACTORMOBILEVERIFIEDAT');
