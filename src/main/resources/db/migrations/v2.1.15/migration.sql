-- Migration v2.1.15: Set departmentType for pharmaceutical items (BACKTICK VERSION)
-- Author: Dr M H B Ariyaratne
-- Date: 2026-02-04
-- Issue: #18359 - Pharmaceutical items need departmentType = 'Pharmacy'

SELECT 'Starting migration v2.1.15 - Using backticks for case insensitivity' AS status;

-- Show what tables we have
SHOW TABLES LIKE '%item%';

-- Use backticks to handle case sensitivity - MySQL should resolve to actual table name
UPDATE `item` SET `departmenttype` = 'Pharmacy' WHERE `dtype` = 'PharmaceuticalItem' AND `departmenttype` IS NULL;
UPDATE `item` SET `departmenttype` = 'Pharmacy' WHERE `dtype` = 'Amp' AND `departmenttype` IS NULL;
UPDATE `item` SET `departmenttype` = 'Pharmacy' WHERE `dtype` = 'Vmp' AND `departmenttype` IS NULL;
UPDATE `item` SET `departmenttype` = 'Pharmacy' WHERE `dtype` = 'Vmpp' AND `departmenttype` IS NULL;
UPDATE `item` SET `departmenttype` = 'Pharmacy' WHERE `dtype` = 'Ampp' AND `departmenttype` IS NULL;
UPDATE `item` SET `departmenttype` = 'Pharmacy' WHERE `dtype` = 'Vtm' AND `departmenttype` IS NULL;
UPDATE `item` SET `departmenttype` = 'Pharmacy' WHERE `dtype` = 'Atm' AND `departmenttype` IS NULL;
UPDATE `item` SET `departmenttype` = 'Pharmacy' WHERE `dtype` = 'Antibiotic' AND `departmenttype` IS NULL;

SELECT 'Migration v2.1.15 completed using backticks' AS final_status;