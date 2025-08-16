-- Migration Script: Add consignment flag to Bill
-- Adds boolean column 'consignment' to table Bill with default false.
-- Author: ChatGPT
-- Date: 2025-08-01

ALTER TABLE Bill ADD COLUMN consignment BOOLEAN DEFAULT FALSE;
