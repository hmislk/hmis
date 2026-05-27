-- SQL Script to add new field 'consumptionAllowed' to Item entity
-- Author: Generated for HMIS project
-- Date: 2026-01-04
-- Description: Adds consumptionAllowed boolean field to the item table

-- Add the consumptionAllowed field to the item table
ALTER TABLE item
ADD COLUMN consumption_allowed TINYINT(1) NOT NULL DEFAULT 1
COMMENT 'Controls whether an item can be consumed/issued in pharmacy operations';

-- Verify the column was added successfully
-- (Uncomment the following line to check after running the script)
-- DESCRIBE item;

-- Optional: Update any existing records if needed (all will have default value 1/true)
-- UPDATE item SET consumption_allowed = 1 WHERE consumption_allowed IS NULL;

-- End of script