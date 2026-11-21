-- Migration v2.25.0: Maintenance window tables
-- Issue: #25838 (maintenance banner)
--
-- Creates the tables behind the MaintenanceWindow entity when they do not exist:
--   MAINTENANCEWINDOW                 (entity table)
--   maintenancewindow_institution     (join table, name is lower case in the entity mapping)
--   maintenancewindow_department      (join table, name is lower case in the entity mapping)
-- Add Missing Fields does not create new tables, so a database without them fails on the
-- first banner lookup. Run this before deploying a build that includes this change.
--
-- Safe to re-run: each table is created only when missing. No data is changed.
--
-- UNIVERSAL: detects the actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive MySQL.

SELECT 'Migration v2.25.0 - Maintenance window tables' AS status;

SET @mw_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'MAINTENANCEWINDOW'
    LIMIT 1
);
SET @sql = IF(@mw_table IS NOT NULL,
    'SELECT "SKIPPED: MAINTENANCEWINDOW already exists" AS status',
    'CREATE TABLE MAINTENANCEWINDOW (
        ID BIGINT NOT NULL AUTO_INCREMENT,
        TITLE VARCHAR(255) DEFAULT NULL,
        MESSAGE LONGTEXT,
        STARTAT DATETIME DEFAULT NULL,
        ENDAT DATETIME DEFAULT NULL,
        SHOWFROMAT DATETIME DEFAULT NULL,
        CREATEDAT DATETIME DEFAULT NULL,
        SOURCE VARCHAR(255) DEFAULT NULL,
        EXTERNALREFERENCE VARCHAR(255) DEFAULT NULL,
        RETIRED TINYINT(1) DEFAULT 0,
        RETIREDAT DATETIME DEFAULT NULL,
        CREATEDBY_ID BIGINT DEFAULT NULL,
        RETIRER_ID BIGINT DEFAULT NULL,
        PRIMARY KEY (ID)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @mwi_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'MAINTENANCEWINDOW_INSTITUTION'
    LIMIT 1
);
SET @sql = IF(@mwi_table IS NOT NULL,
    'SELECT "SKIPPED: maintenancewindow_institution already exists" AS status',
    'CREATE TABLE maintenancewindow_institution (
        MaintenanceWindow_ID BIGINT NOT NULL,
        institutions_ID BIGINT NOT NULL,
        PRIMARY KEY (MaintenanceWindow_ID, institutions_ID)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @mwd_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'MAINTENANCEWINDOW_DEPARTMENT'
    LIMIT 1
);
SET @sql = IF(@mwd_table IS NOT NULL,
    'SELECT "SKIPPED: maintenancewindow_department already exists" AS status',
    'CREATE TABLE maintenancewindow_department (
        MaintenanceWindow_ID BIGINT NOT NULL,
        departments_ID BIGINT NOT NULL,
        PRIMARY KEY (MaintenanceWindow_ID, departments_ID)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.25.0 complete' AS status;
