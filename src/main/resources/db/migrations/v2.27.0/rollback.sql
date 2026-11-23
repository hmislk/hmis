-- Rollback v2.27.0: drop the OPD credit company claim settlement table
-- WARNING: all claim settlement links are lost. Only roll back together with a build that no longer
-- maps CreditCompanyClaimSettlement.

SELECT 'Rollback v2.27.0 - Drop OPD credit company claim settlement table' AS status;

SET @settlement_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYCLAIMSETTLEMENT' LIMIT 1
);
SET @sql = IF(@settlement_table IS NULL,
    'SELECT "SKIPPED: CREDITCOMPANYCLAIMSETTLEMENT not found" AS status',
    CONCAT('DROP TABLE ', @settlement_table)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
