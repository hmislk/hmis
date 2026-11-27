-- Migration v2.35.0: index on BILLFEE.REFERENCEBILLITEM_ID
--
-- BillItem.proFees is mapped by BillFee.referenceBillItem, so the inpatient final bill view and
-- reprint read BILLFEE by REFERENCEBILLITEM_ID. Without an index each read scans the whole table.
--
-- Changes no data. The code works with or without the index, so this can run before or after deploy.
-- Safe to re-run: the index is added only when no index with this name exists and no index already
-- starts with REFERENCEBILLITEM_ID.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.35.0 - index on BILLFEE.REFERENCEBILLITEM_ID' AS status;

SET @billfee_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILLFEE' LIMIT 1
);

SELECT CONCAT('BILLFEE: ', COALESCE(@billfee_table, 'NOT FOUND')) AS info;

SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILLFEE'
      AND (UPPER(INDEX_NAME) = 'IDX_BILLFEE_REFERENCEBILLITEM'
           OR (UPPER(COLUMN_NAME) = 'REFERENCEBILLITEM_ID' AND SEQ_IN_INDEX = 1))
);
SET @sql = IF(@billfee_table IS NULL,
    'SELECT "SKIPPED: BILLFEE table not found" AS status',
    IF(@idx_exists > 0,
        'SELECT "SKIPPED: index on REFERENCEBILLITEM_ID already exists" AS status',
        CONCAT('CREATE INDEX IDX_BILLFEE_REFERENCEBILLITEM ON ', @billfee_table, ' (REFERENCEBILLITEM_ID)')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Migration v2.35.0 complete' AS status;
