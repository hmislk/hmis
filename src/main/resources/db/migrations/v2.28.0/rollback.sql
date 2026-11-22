-- Rollback v2.28.0: drop the OPD quotation tables
-- WARNING: all quotation data is lost. Only roll back together with a build that no longer maps
-- OpdQuotation / OpdQuotationItem.

SELECT 'Rollback v2.28.0 - Drop OPD quotation tables' AS status;

-- Item table first (it references the quotation table)
SET @item_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'OPDQUOTATIONITEM' LIMIT 1
);
SET @sql = IF(@item_table IS NULL,
    'SELECT "SKIPPED: OPDQUOTATIONITEM not found" AS status',
    CONCAT('DROP TABLE ', @item_table)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @quotation_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'OPDQUOTATION' LIMIT 1
);
SET @sql = IF(@quotation_table IS NULL,
    'SELECT "SKIPPED: OPDQUOTATION not found" AS status',
    CONCAT('DROP TABLE ', @quotation_table)
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
