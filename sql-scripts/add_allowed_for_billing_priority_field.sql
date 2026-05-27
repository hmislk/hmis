-- SQL Script to add 'allowedForBillingPriority' field to Item entity
-- Author: Generated for HMIS project
-- Date: 2026-01-04
-- Description: Adds allowedForBillingPriority boolean field to the item table

-- Add the allowedForBillingPriority field to the item table
ALTER TABLE item
ADD COLUMN allowed_for_billing_priority TINYINT(1) DEFAULT NULL
COMMENT 'Controls whether an item is allowed for billing priority operations';

-- Optional: Set default value for existing records if needed
-- UPDATE item SET allowed_for_billing_priority = 1 WHERE allowed_for_billing_priority IS NULL;

-- Verify the column was added successfully
-- (Uncomment the following line to check after running the script)
-- DESCRIBE item;

-- End of script