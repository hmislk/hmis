-- Migration v2.27.0: OPD credit company claim settlement link table
-- Issue: #25847 (OPD credit company claims, part 2)
--
-- Creates CREDITCOMPANYCLAIMSETTLEMENT, mapped by the CreditCompanyClaimSettlement entity. It links
-- a credit collection bill to the claim and the claimed bill it paid. Production has no DDL
-- generation, so run this before deploying a build that includes that entity. Requires v2.26.0.
--
-- Safe to re-run: the table is created only when missing.
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.
-- The new table is created in the same case as the existing WEBUSER table.

SELECT 'Migration v2.27.0 - OPD credit company claim settlement table' AS status;

SET @webuser_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'WEBUSER' LIMIT 1
);
SET @bill_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL' LIMIT 1
);
SET @claim_actual = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYCLAIM' LIMIT 1
);

SELECT CONCAT('WEBUSER: ', COALESCE(@webuser_table, 'NOT FOUND'),
              ', BILL: ', COALESCE(@bill_table, 'NOT FOUND'),
              ', CREDITCOMPANYCLAIM: ', COALESCE(@claim_actual, 'NOT FOUND')) AS info;

-- Case for the new table follows the existing WEBUSER table
SET @settlement_table = IF(@webuser_table = UPPER(@webuser_table), 'CREDITCOMPANYCLAIMSETTLEMENT', 'creditcompanyclaimsettlement');

SET @exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'CREDITCOMPANYCLAIMSETTLEMENT'
);
SET @sql = IF(@webuser_table IS NULL OR @bill_table IS NULL OR @claim_actual IS NULL,
    'SELECT "SKIPPED: WEBUSER, BILL or CREDITCOMPANYCLAIM table not found (run v2.26.0 first)" AS status',
    IF(@exists > 0,
        'SELECT "SKIPPED: CREDITCOMPANYCLAIMSETTLEMENT already exists" AS status',
        CONCAT('CREATE TABLE ', @settlement_table, ' (
            ID BIGINT NOT NULL AUTO_INCREMENT,
            AMOUNT DOUBLE DEFAULT NULL,
            CREATEDAT DATETIME DEFAULT NULL,
            RETIRED TINYINT(1) DEFAULT 0,
            RETIREDAT DATETIME DEFAULT NULL,
            CLAIM_ID BIGINT DEFAULT NULL,
            CLAIMEDBILL_ID BIGINT DEFAULT NULL,
            CREATER_ID BIGINT DEFAULT NULL,
            RETIRER_ID BIGINT DEFAULT NULL,
            SETTLEMENTBILL_ID BIGINT DEFAULT NULL,
            PRIMARY KEY (ID),
            KEY FK_CREDITCOMPANYCLAIMSETTLEMENT_CLAIM_ID (CLAIM_ID),
            KEY FK_CREDITCOMPANYCLAIMSETTLEMENT_CLAIMEDBILL_ID (CLAIMEDBILL_ID),
            KEY FK_CREDITCOMPANYCLAIMSETTLEMENT_CREATER_ID (CREATER_ID),
            KEY FK_CREDITCOMPANYCLAIMSETTLEMENT_RETIRER_ID (RETIRER_ID),
            KEY FK_CREDITCOMPANYCLAIMSETTLEMENT_SETTLEMENTBILL_ID (SETTLEMENTBILL_ID),
            CONSTRAINT FK_CREDITCOMPANYCLAIMSETTLEMENT_CLAIM_ID FOREIGN KEY (CLAIM_ID) REFERENCES ', @claim_actual, ' (ID),
            CONSTRAINT FK_CREDITCOMPANYCLAIMSETTLEMENT_CLAIMEDBILL_ID FOREIGN KEY (CLAIMEDBILL_ID) REFERENCES ', @bill_table, ' (ID),
            CONSTRAINT FK_CREDITCOMPANYCLAIMSETTLEMENT_CREATER_ID FOREIGN KEY (CREATER_ID) REFERENCES ', @webuser_table, ' (ID),
            CONSTRAINT FK_CREDITCOMPANYCLAIMSETTLEMENT_RETIRER_ID FOREIGN KEY (RETIRER_ID) REFERENCES ', @webuser_table, ' (ID),
            CONSTRAINT FK_CREDITCOMPANYCLAIMSETTLEMENT_SETTLEMENTBILL_ID FOREIGN KEY (SETTLEMENTBILL_ID) REFERENCES ', @bill_table, ' (ID)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4')
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
