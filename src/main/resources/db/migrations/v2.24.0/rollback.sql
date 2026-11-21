-- Rollback v2.24.0: drop the maintenance window tables
-- WARNING: deletes all maintenance windows. Roll back only together with a build that no
-- longer has the MaintenanceWindow entity, or the banner lookup will fail.

SELECT 'Rollback v2.24.0 - Drop maintenance window tables' AS status;

SET @t = (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'MAINTENANCEWINDOW_INSTITUTION' LIMIT 1);
SET @sql = IF(@t IS NULL, 'SELECT "SKIPPED: maintenancewindow_institution not found" AS status', CONCAT('DROP TABLE ', @t));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @t = (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'MAINTENANCEWINDOW_DEPARTMENT' LIMIT 1);
SET @sql = IF(@t IS NULL, 'SELECT "SKIPPED: maintenancewindow_department not found" AS status', CONCAT('DROP TABLE ', @t));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @t = (SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'MAINTENANCEWINDOW' LIMIT 1);
SET @sql = IF(@t IS NULL, 'SELECT "SKIPPED: MAINTENANCEWINDOW not found" AS status', CONCAT('DROP TABLE ', @t));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
