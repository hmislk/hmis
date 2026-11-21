-- Rollback v2.26.0: drop the OPD credit company claim tables
-- WARNING: all claim data is lost. Only roll back together with a build that no longer maps
-- CreditCompanyClaim / CreditCompanyClaimItem.

SELECT 'Rollback v2.26.0 - Drop OPD credit company claim tables' AS status;

-- Item table first (it references the claim table)
SET @item_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYCLAIMITEM' LIMIT 1
);
SET @sql = IF(@item_table IS NULL,
    'SELECT "SKIPPED: CREDITCOMPANYCLAIMITEM not found" AS status',
    CONCAT('DROP TABLE ', @item_table)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @claim_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYCLAIM' LIMIT 1
);
SET @sql = IF(@claim_table IS NULL,
    'SELECT "SKIPPED: CREDITCOMPANYCLAIM not found" AS status',
    CONCAT('DROP TABLE ', @claim_table)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
