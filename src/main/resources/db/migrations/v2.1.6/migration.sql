-- Migration v2.1.6: Create REPORTLOG table in audit database
-- Description: Add missing REPORTLOG table to track report generation activities
-- Author: Dr M H B Ariyaratne
-- Date: 2025-12-16
-- Issue: Production error - Table 'rhaudit.reportlog' doesn't exist
-- Database: rhaudit (audit database)
--
-- Purpose: Create audit table for report generation logging to track
-- who generated which reports, when, and execution performance metrics

-- ==========================================
-- PRE-MIGRATION VERIFICATION
-- ==========================================

-- Step 1: Verify we're connected to the audit database
-- This verification step ensures the migration runs on the correct database
SELECT DATABASE() AS current_database;

-- Step 2: Check if REPORTLOG table already exists (should not exist)
SELECT COUNT(*) AS table_exists
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'reportlog';

-- ==========================================
-- TABLE CREATION
-- ==========================================

-- Step 3: Create REPORTLOG table
-- This table tracks all report generation activities with performance metrics
CREATE TABLE IF NOT EXISTS reportlog (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reportType VARCHAR(255) NOT NULL COMMENT 'Type of report generated (e.g., PharmacyReports, FinanceReports)',
    reportName VARCHAR(255) NOT NULL COMMENT 'Specific name of the report',
    createdAt DATETIME NOT NULL COMMENT 'Timestamp when the log entry was created',
    generatedById BIGINT NOT NULL COMMENT 'ID of the user who generated the report',
    executionTimeInMillis BIGINT NULL COMMENT 'Time taken to generate the report in milliseconds',
    startTime DATETIME NOT NULL COMMENT 'When report generation started',
    endTime DATETIME NULL COMMENT 'When report generation completed',

    -- Primary Key
    PRIMARY KEY (id),

    -- Indexes for common query patterns
    INDEX idx_reportlog_created (createdAt DESC) COMMENT 'For chronological queries',
    INDEX idx_reportlog_user (generatedById) COMMENT 'For user-specific report tracking',
    INDEX idx_reportlog_type (reportType) COMMENT 'For report type filtering',
    INDEX idx_reportlog_name (reportName) COMMENT 'For specific report queries',
    INDEX idx_reportlog_composite (generatedById, createdAt DESC) COMMENT 'For user activity timeline'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit log for report generation activities and performance tracking';

-- ==========================================
-- POST-MIGRATION VERIFICATION
-- ==========================================

-- Step 4: Verify table was created successfully
SELECT COUNT(*) AS table_created
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'reportlog';

-- Step 5: Verify all columns exist with correct types
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'reportlog'
ORDER BY ORDINAL_POSITION;

-- Step 6: Verify indexes were created
SELECT
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    INDEX_TYPE,
    INDEX_COMMENT
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'reportlog'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- Step 7: Verify table is empty (fresh start)
SELECT COUNT(*) AS initial_row_count
FROM reportlog;

-- ==========================================
-- PERFORMANCE VERIFICATION
-- ==========================================

-- Step 8: Test index usage for common queries
-- Query 1: Recent reports by user
EXPLAIN SELECT id, reportName, startTime, endTime, executionTimeInMillis
FROM reportlog
WHERE generatedById = 1
  AND createdAt >= DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY createdAt DESC
LIMIT 100;

-- Query 2: Reports by type
EXPLAIN SELECT id, reportName, createdAt, executionTimeInMillis
FROM reportlog
WHERE reportType = 'PharmacyReports'
  AND createdAt >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY createdAt DESC;

-- Query 3: Performance metrics for specific report
EXPLAIN SELECT
    reportName,
    COUNT(*) AS generation_count,
    AVG(executionTimeInMillis) AS avg_execution_ms,
    MAX(executionTimeInMillis) AS max_execution_ms,
    MIN(executionTimeInMillis) AS min_execution_ms
FROM reportlog
WHERE reportName = 'Daily Sales Report'
  AND endTime IS NOT NULL
  AND createdAt >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY reportName;

-- ==========================================
-- MIGRATION COMPLETE
-- ==========================================
-- Expected outcome:
-- - REPORTLOG table created in audit database (rhaudit)
-- - All required columns present with correct types and constraints
-- - Indexes created for optimal query performance
-- - Ready to track report generation activities
-- - Fixes: javax.persistence.PersistenceException for missing table
-- - Application can now log report generation without errors
-- ==========================================
