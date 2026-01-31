# MySQL Performance Configuration for HMIS

## Overview

This document provides technical implementation details for optimizing MySQL configuration settings for HMIS performance. These settings are instance-dependent and should be configured based on available system resources.

## Configuration Analysis Results

Based on performance investigation (2025-12-25), the following critical configuration issues were identified:

### Current Configuration Issues

1. **InnoDB Buffer Pool**: 128MB (severely undersized)
2. **Log File Size**: 50MB (needs optimization)
3. **Transaction Commit**: Set to 1 (performance impact)
4. **Query Cache**: Not explicitly configured

### Database Workload Profile

- **ITEM Table**: 10,404 rows, 12.19MB
- **STOCK Table**: 24,065 rows, 7.78MB
- **ITEMBATCH Table**: 13,159 rows, 3.95MB
- **Total Working Set**: ~25MB+ with indexes
- **Query Pattern**: Heavy autocomplete/search operations with JOINs

## Recommended Configuration Settings

### 1. InnoDB Buffer Pool Optimization

```ini
# /etc/mysql/mysql.conf.d/mysqld.cnf or /etc/my.cnf

[mysqld]
# Buffer Pool: 50-80% of available RAM for dedicated MySQL servers
# For servers with other applications, use 25-50% of RAM

# For 4GB RAM systems:
innodb_buffer_pool_size = 1024M

# For 8GB RAM systems:
innodb_buffer_pool_size = 2048M

# For 16GB+ RAM systems:
innodb_buffer_pool_size = 4096M
```

### 2. InnoDB Log File Configuration

```ini
# Log files for write performance and crash recovery
innodb_log_file_size = 256M
innodb_log_buffer_size = 16M

# Flush behavior - balance between performance and durability
# 1 = full ACID compliance (current setting)
# 2 = write to log but flush every second (recommended for performance)
innodb_flush_log_at_trx_commit = 2
```

### 3. Connection and Query Optimization

```ini
# Connection handling
max_connections = 200
max_connect_errors = 10000

# Query cache (if MySQL version < 8.0)
query_cache_type = 1
query_cache_size = 128M
query_cache_limit = 2M

# Thread handling
thread_cache_size = 16
```

### 4. Table and Index Optimization

```ini
# File per table for better management
innodb_file_per_table = ON

# Index optimization
innodb_sort_buffer_size = 2M
myisam_sort_buffer_size = 8M

# Table scan optimization
join_buffer_size = 2M
sort_buffer_size = 2M
read_buffer_size = 128K
read_rnd_buffer_size = 256K
```

## Implementation Patterns

### 1. Configuration Deployment Script

```bash
#!/bin/bash
# mysql-performance-config.sh

# Backup current configuration
cp /etc/mysql/mysql.conf.d/mysqld.cnf /etc/mysql/mysql.conf.d/mysqld.cnf.backup.$(date +%Y%m%d)

# Calculate optimal buffer pool size (50% of RAM)
TOTAL_RAM_KB=$(grep MemTotal /proc/meminfo | awk '{print $2}')
BUFFER_POOL_MB=$((TOTAL_RAM_KB / 2 / 1024))

# Ensure minimum 512MB, maximum 8GB for HMIS workload
if [ $BUFFER_POOL_MB -lt 512 ]; then
    BUFFER_POOL_MB=512
elif [ $BUFFER_POOL_MB -gt 8192 ]; then
    BUFFER_POOL_MB=8192
fi

# Apply configuration
cat >> /etc/mysql/mysql.conf.d/mysqld.cnf << EOF

# HMIS Performance Optimization - $(date)
[mysqld]
innodb_buffer_pool_size = ${BUFFER_POOL_MB}M
innodb_log_file_size = 256M
innodb_log_buffer_size = 16M
innodb_flush_log_at_trx_commit = 2
max_connections = 200
query_cache_size = 128M
join_buffer_size = 2M
sort_buffer_size = 2M
EOF

echo "Configuration applied. Restart MySQL service to take effect."
```

### 2. Performance Monitoring Queries

```sql
-- Monitor buffer pool usage
SELECT
    FORMAT_BYTES(innodb_buffer_pool_size) AS buffer_pool_size,
    FORMAT_BYTES(innodb_buffer_pool_pages_total * innodb_page_size) AS total_pages,
    ROUND(innodb_buffer_pool_pages_dirty / innodb_buffer_pool_pages_total * 100, 2) AS dirty_page_pct,
    ROUND(innodb_buffer_pool_read_requests / innodb_buffer_pool_reads * 100, 2) AS buffer_pool_hit_rate
FROM information_schema.GLOBAL_STATUS
WHERE VARIABLE_NAME IN (
    'innodb_buffer_pool_size',
    'innodb_buffer_pool_pages_total',
    'innodb_buffer_pool_pages_dirty',
    'innodb_buffer_pool_read_requests',
    'innodb_buffer_pool_reads'
);

-- Monitor query cache effectiveness (if enabled)
SHOW STATUS LIKE 'Qcache_%';

-- Check connection usage
SHOW STATUS LIKE 'Connections';
SHOW STATUS LIKE 'Threads_connected';
```

### 3. Index Analysis Patterns

```sql
-- Analyze index usage after configuration changes
SELECT
    table_name,
    index_name,
    cardinality,
    non_unique,
    index_type
FROM information_schema.statistics
WHERE table_schema = DATABASE()
    AND table_name IN ('ITEM', 'STOCK', 'ITEMBATCH')
ORDER BY table_name, seq_in_index;

-- Identify unused indexes
SELECT
    object_schema,
    object_name,
    index_name
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE index_name IS NOT NULL
    AND count_star = 0
    AND object_schema = DATABASE();
```

## Environment-Specific Considerations

### Development Environment

```ini
# Smaller settings for development machines
[mysqld]
innodb_buffer_pool_size = 512M
innodb_log_file_size = 128M
innodb_flush_log_at_trx_commit = 0  # Fastest, but no durability
max_connections = 50
```

### Production Environment

```ini
# Optimized for production workloads
[mysqld]
innodb_buffer_pool_size = 2048M  # Adjust based on server RAM
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2  # Good balance
max_connections = 200

# Additional production settings
innodb_flush_method = O_DIRECT
innodb_io_capacity = 200
innodb_io_capacity_max = 400
```

### Cloud/Container Environment

```ini
# Container-optimized settings
[mysqld]
innodb_buffer_pool_size = 1024M
innodb_use_native_aio = 0  # Disable for containers
innodb_flush_method = fsync
max_connections = 100
```

## Testing and Validation

### 1. Configuration Testing Script

```bash
#!/bin/bash
# test-mysql-performance.sh

echo "Testing MySQL performance configuration..."

# Test connection pool
mysql -e "SHOW STATUS LIKE 'Threads_connected';"

# Test buffer pool
mysql -e "SHOW ENGINE INNODB STATUS\G" | grep -A 10 "BUFFER POOL"

# Test query performance
time mysql -e "
SELECT COUNT(*) FROM STOCK s
INNER JOIN ITEMBATCH ib ON s.ITEMBATCH_ID = ib.ID
INNER JOIN ITEM i ON ib.ITEM_ID = i.ID
WHERE s.STOCK > 0 AND s.DEPARTMENT_ID = 1;
"
```

### 2. Performance Benchmarking

```sql
-- Benchmark autocomplete queries before/after configuration
SET @start_time = NOW(6);

SELECT COUNT(*) FROM (
    SELECT s.* FROM STOCK s
    INNER JOIN ITEMBATCH ib ON s.ITEMBATCH_ID = ib.ID
    INNER JOIN ITEM i ON ib.ITEM_ID = i.ID
    WHERE s.STOCK > 0
        AND s.DEPARTMENT_ID = 1
        AND (i.NAME LIKE 'para%' OR i.CODE LIKE 'para%')
    ORDER BY i.NAME, ib.DATEOFEXPIRE
    LIMIT 20
) AS benchmark_query;

SELECT TIMESTAMPDIFF(MICROSECOND, @start_time, NOW(6)) / 1000 AS execution_time_ms;
```

## Troubleshooting Common Issues

### 1. Memory Allocation Issues

```bash
# Check available memory
free -h

# Monitor MySQL memory usage
ps aux | grep mysql
pmap $(pidof mysqld) | tail -1
```

### 2. Configuration Validation

```sql
-- Verify configuration is applied
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';
SHOW VARIABLES LIKE 'innodb_log_file_size';
SHOW VARIABLES LIKE 'innodb_flush_log_at_trx_commit';
```

### 3. Performance Regression Detection

```sql
-- Monitor query execution time
SET SESSION profiling = 1;
-- Execute your query
SHOW PROFILES;
SHOW PROFILE FOR QUERY 1;
```

## Integration with HMIS Application

### 1. Connection Pool Configuration

Update `persistence.xml` to match MySQL configuration:

```xml
<!-- Adjust connection pool to match max_connections -->
<property name="hibernate.c3p0.max_size" value="50"/>
<property name="hibernate.c3p0.min_size" value="10"/>
<property name="hibernate.c3p0.timeout" value="300"/>
```

### 2. Query Optimization Hints

```java
// Use in PharmacySaleForCashierController
@Query(hints = @QueryHint(name = "org.hibernate.cacheMode", value = "NORMAL"))
public List<StockDTO> findOptimizedStock(String query, Department dept);
```

## Monitoring and Maintenance

### 1. Regular Performance Checks

```bash
#!/bin/bash
# weekly-performance-check.sh

# Check slow query log
mysql -e "SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;"

# Analyze table statistics
mysql -e "ANALYZE TABLE ITEM, STOCK, ITEMBATCH;"

# Update table statistics
mysql -e "OPTIMIZE TABLE ITEM, STOCK, ITEMBATCH;"
```

### 2. Automated Monitoring

Set up monitoring for key metrics:
- Buffer pool hit ratio (target >95%)
- Query execution time (autocomplete <100ms)
- Connection usage (target <80% of max)
- Disk I/O patterns

---

This configuration guide provides the technical foundation for optimizing MySQL performance for HMIS. Always test configuration changes in development environment before applying to production.