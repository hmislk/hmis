-- Rollback v2.9.0: intentionally a NO-OP.
-- Author: H.K. Damith Deshan
--
-- Migration v2.9.0 only RE-ADDS AUTO_INCREMENT to ID primary keys that were missing it.
-- Removing AUTO_INCREMENT again would immediately re-break every INSERT into those tables
-- (MySQL error 1364, "Field 'ID' doesn't have a default value") — the very bug v2.9.0 fixes.
-- There is also no record of which tables this database had altered, so a blanket removal
-- across all ID primary keys would be destructive. Therefore this rollback does nothing.
--
-- If you genuinely must revert a specific table, run manually and accept that inserts break:
--   ALTER TABLE `<table>` MODIFY COLUMN `ID` BIGINT NOT NULL;

SELECT 'Rollback v2.9.0 - no action taken (re-adding AUTO_INCREMENT is not safely reversible)' AS status;
