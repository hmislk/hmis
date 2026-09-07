-- Migration v2.1.15: Set departmentType for pharmaceutical items (LOWERCASE VERSION)
-- Author: Dr M H B Ariyaratne
-- Date: 2026-02-04
-- Issue: #18359 - Pharmaceutical items need departmentType = 'Pharmacy'
-- NOTE: Use this version for databases with lowercase table names

-- Get the actual table name from the database
SELECT 'Starting migration v2.1.15 (lowercase version)' AS status;

-- Show what table name we found
SELECT 'Using table:' AS info, TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND LOWER(TABLE_NAME) = 'item'
LIMIT 1;

-- For lowercase tables
UPDATE item SET departmenttype = 'Pharmacy'
WHERE dtype = 'PharmaceuticalItem' AND departmenttype IS NULL;

UPDATE item SET departmenttype = 'Pharmacy'
WHERE dtype = 'Amp' AND departmenttype IS NULL;

UPDATE item SET departmenttype = 'Pharmacy'
WHERE dtype = 'Vmp' AND departmenttype IS NULL;

UPDATE item SET departmenttype = 'Pharmacy'
WHERE dtype = 'Vmpp' AND departmenttype IS NULL;

UPDATE item SET departmenttype = 'Pharmacy'
WHERE dtype = 'Ampp' AND departmenttype IS NULL;

UPDATE item SET departmenttype = 'Pharmacy'
WHERE dtype = 'Vtm' AND departmenttype IS NULL;

UPDATE item SET departmenttype = 'Pharmacy'
WHERE dtype = 'Atm' AND departmenttype IS NULL;

UPDATE item SET departmenttype = 'Pharmacy'
WHERE dtype = 'Antibiotic' AND departmenttype IS NULL;

SELECT 'Migration v2.1.15 completed (lowercase version)' AS status;