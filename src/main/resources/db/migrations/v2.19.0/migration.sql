-- Migration v2.19.0: Index BILL.ONLINEBOOKING_ID
-- Author: Dr M H B Ariyaratne
-- Issue: #24923 — Channel API /complete takes about 25 s under load
--
-- Root cause:
--   ChannelService.findBillFromOnlineBooking() filters Bill by onlineBooking, and
--   OnlineBooking.bill is an EAGER @OneToOne(mappedBy = "onlineBooking"), so every
--   load of an OnlineBooking (API /save, /complete, /edit, /cancellation) also
--   queries BILL by ONLINEBOOKING_ID. The column has no index, so each of these
--   is a full scan of the bill table.
--
-- Pure performance change: no data is modified. Safe to re-run: the index is
-- created only when no index already starts with ONLINEBOOKING_ID.
-- InnoDB builds a secondary index online (reads and writes continue).
--
-- UNIVERSAL: detects actual table-name case via INFORMATION_SCHEMA so this works on both
-- case-sensitive (Linux, lower_case_table_names=0) and case-insensitive (Windows/Azure) MySQL.

SELECT 'Migration v2.19.0 - Index BILL.ONLINEBOOKING_ID' AS status;

SET @bill_table = (
    SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'BILL'
    LIMIT 1
);

SET @ob_column = (
    SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND UPPER(COLUMN_NAME) = 'ONLINEBOOKING_ID'
    LIMIT 1
);

SET @existing_index = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
      AND UPPER(COLUMN_NAME) = 'ONLINEBOOKING_ID' AND SEQ_IN_INDEX = 1
);

SELECT CONCAT('Bill table: ', COALESCE(@bill_table, 'NOT FOUND'),
              ', column: ', COALESCE(@ob_column, 'NOT FOUND'),
              ', existing indexes on it: ', @existing_index) AS info;

SET @create_sql = IF(@bill_table IS NULL OR @ob_column IS NULL,
    'SELECT "SKIPPED: bill table or ONLINEBOOKING_ID column not found" AS status',
    IF(@existing_index > 0,
        'SELECT "SKIPPED: an index on ONLINEBOOKING_ID already exists" AS status',
        CONCAT('CREATE INDEX idx_bill_onlinebooking ON ', @bill_table, ' (', @ob_column, ')')
    )
);
PREPARE stmt FROM @create_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT CONCAT('Indexes on ONLINEBOOKING_ID after migration: ', COUNT(*)) AS status
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @bill_table
  AND UPPER(COLUMN_NAME) = 'ONLINEBOOKING_ID' AND SEQ_IN_INDEX = 1;
