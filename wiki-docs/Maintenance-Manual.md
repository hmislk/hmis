# HMIS Maintenance Manual

## Overview
This manual provides essential maintenance procedures for HMIS production environments, including log management, performance monitoring, and system health checks.

## Table of Contents
1. [Log Management](#log-management)
2. [Performance Monitoring](#performance-monitoring)
3. [Database Maintenance](#database-maintenance)
4. [System Health Checks](#system-health-checks)
5. [Backup Procedures](#backup-procedures)
6. [Common Issues and Solutions](#common-issues-and-solutions)

---

## Log Management

### Payara Server Logs

#### Log File Locations
```
/path/to/payara/glassfish/domains/domain1/logs/
├── server.log          # Main application log
├── server.log_*        # Archived logs
└── access/            # HTTP access logs
```

#### Viewing Current Logs
```bash
# View real-time logs
tail -f /path/to/payara/glassfish/domains/domain1/logs/server.log

# View last 100 lines
tail -n 100 /path/to/payara/glassfish/domains/domain1/logs/server.log

# Search for errors
grep -i "error\|exception\|severe" /path/to/payara/glassfish/domains/domain1/logs/server.log
```

#### Deleting Old Logs

**⚠️ Important**: Always backup logs before deletion, especially if investigating issues.

**Manual Deletion:**
```bash
# Navigate to logs directory
cd /path/to/payara/glassfish/domains/domain1/logs/

# Delete logs older than 30 days
find . -name "server.log_*" -mtime +30 -delete

# Delete access logs older than 30 days
find ./access -name "*.log" -mtime +30 -delete
```

**Automated Cleanup Script:**

Create a script `/usr/local/bin/cleanup-payara-logs.sh`:
```bash
#!/bin/bash
LOG_DIR="/path/to/payara/glassfish/domains/domain1/logs"
RETENTION_DAYS=30

# Archive and delete old server logs
find $LOG_DIR -name "server.log_*" -mtime +$RETENTION_DAYS -delete

# Delete old access logs
find $LOG_DIR/access -name "*.log" -mtime +$RETENTION_DAYS -delete

echo "$(date): Cleaned logs older than $RETENTION_DAYS days"
```

Make it executable:
```bash
chmod +x /usr/local/bin/cleanup-payara-logs.sh
```

**Schedule with Cron:**
```bash
# Edit crontab
crontab -e

# Add this line to run daily at 2 AM
0 2 * * * /usr/local/bin/cleanup-payara-logs.sh >> /var/log/payara-cleanup.log 2>&1
```

#### Configure Log Rotation in Payara

Using Payara Admin Console:
1. Navigate to: **Configurations → server-config → Logger Settings**
2. Set **File Rotation Limit**: 10 (number of files to keep)
3. Set **File Rotation Limit in Bytes**: 2000000 (2MB)
4. Enable **Rotation on Date Change**: true
5. Click **Save**

Using asadmin command:
```bash
./asadmin set-log-attributes \
  com.sun.enterprise.server.logging.GFFileHandler.rotationLimitInBytes=2000000

./asadmin set-log-attributes \
  com.sun.enterprise.server.logging.GFFileHandler.maxHistoryFiles=10
```

---

## Performance Monitoring

### System Resource Monitoring

#### Check CPU and Memory Usage
```bash
# Overall system resources
top

# Payara-specific memory usage
ps aux | grep payara

# Detailed JVM memory
jstat -gc <payara-pid> 1000

# Get Payara PID
jps -l | grep payara
```

#### Monitor Disk Space
```bash
# Check disk usage
df -h

# Check logs directory size
du -sh /path/to/payara/glassfish/domains/domain1/logs/

# Find large files
find /path/to/payara -type f -size +100M -exec ls -lh {} \;
```

### Application Performance Monitoring

#### Check Thread Pools
Using Payara Admin Console:
1. Navigate to: **Configurations → server-config → Thread Pools**
2. Monitor **http-thread-pool**:
   - Current Thread Count
   - Max Thread Pool Size
   - Current Threads Busy

Recommended settings:
- **Min Pool Size**: 5
- **Max Pool Size**: 200
- **Max Queue Size**: 4096

#### Check Connection Pools

Using Admin Console:
1. Navigate to: **Resources → JDBC → Connection Pools**
2. Select your pool (e.g., `jdbc/hmisDB`)
3. Click **Monitoring** tab
4. Check:
   - **NumConnUsed**: Current connections in use
   - **NumConnFree**: Available connections
   - **WaitQueueLength**: Requests waiting for connection

Using asadmin:
```bash
./asadmin get-monitoring-level --monitor=jdbc-connection-pool
./asadmin list-jdbc-connection-pools
```

#### Enable Performance Monitoring
```bash
# Enable monitoring
./asadmin set server.monitoring-service.module-monitoring-levels.jdbc-connection-pool=HIGH
./asadmin set server.monitoring-service.module-monitoring-levels.jvm=HIGH
./asadmin set server.monitoring-service.module-monitoring-levels.web-container=HIGH
```

### JVM Heap Analysis

#### Monitor Heap Usage
```bash
# Get heap information
jmap -heap <payara-pid>

# Generate heap dump for analysis
jmap -dump:format=b,file=/tmp/heap-dump.hprof <payara-pid>
```

#### Analyze Heap Dumps
Use tools like:
- **Eclipse MAT** (Memory Analyzer Tool)
- **VisualVM**
- **JProfiler**

#### Optimal JVM Settings
Edit `domain.xml` or use asadmin:
```bash
# Set heap size (example for 16GB server)
./asadmin delete-jvm-options "-Xmx512m"
./asadmin create-jvm-options "-Xmx8g"
./asadmin create-jvm-options "-Xms4g"

# Set metaspace
./asadmin create-jvm-options "-XX:MaxMetaspaceSize=512m"

# Enable GC logging
./asadmin create-jvm-options "-Xlog:gc*:file=/path/to/payara/glassfish/domains/domain1/logs/gc.log:time,uptime,level,tags"
```

### Response Time Monitoring

#### Enable Access Logging
1. Navigate to: **Configurations → server-config → HTTP Service → Access Log**
2. Enable **Access Logging**
3. Format: `%client.name% %auth-user-name% %datetime% %request% %status% %response.length%`

#### Analyze Slow Requests
```bash
# Find requests taking more than 5 seconds
awk '$NF > 5000' /path/to/payara/glassfish/domains/domain1/logs/access/server_access_log
```

---

## Database Maintenance

### Regular Database Checks

#### Check Database Size
```sql
-- Check database size
SELECT
    table_schema AS 'Database',
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'hmis'
GROUP BY table_schema;

-- Check individual table sizes
SELECT
    table_name AS 'Table',
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'hmis'
ORDER BY (data_length + index_length) DESC;
```

#### Optimize Tables
```sql
-- Optimize specific table
OPTIMIZE TABLE bill;

-- Optimize all tables (run during off-peak hours)
SELECT CONCAT('OPTIMIZE TABLE ', table_name, ';')
FROM information_schema.tables
WHERE table_schema = 'hmis';
```

#### Check and Repair Tables
```sql
-- Check table integrity
CHECK TABLE bill;

-- Repair if needed
REPAIR TABLE bill;
```

### Audit Table Maintenance

#### Archive Old Audit Data
```sql
-- Check audit table size
SELECT COUNT(*) FROM audit_event;

-- Archive data older than 1 year
CREATE TABLE audit_event_archive_2024 AS
SELECT * FROM audit_event
WHERE created_at < '2024-01-01';

-- Delete archived data
DELETE FROM audit_event
WHERE created_at < '2024-01-01';

-- Optimize table after deletion
OPTIMIZE TABLE audit_event;
```

### Index Maintenance

#### Identify Missing Indexes
```sql
-- Find tables without indexes
SELECT DISTINCT
    t.table_name
FROM information_schema.tables t
LEFT JOIN information_schema.statistics s
    ON t.table_name = s.table_name
    AND t.table_schema = s.table_schema
WHERE t.table_schema = 'hmis'
    AND s.index_name IS NULL
    AND t.table_type = 'BASE TABLE';
```

#### Rebuild Indexes
```sql
-- Rebuild specific index
ALTER TABLE bill DROP INDEX idx_bill_date, ADD INDEX idx_bill_date(bill_date);

-- Analyze table to update statistics
ANALYZE TABLE bill;
```

---

## System Health Checks

### Daily Checks

#### 1. Check Application Accessibility
```bash
# Test application endpoint
curl -I https://your-hmis-domain.com/hmis/

# Expected: HTTP 200 OK
```

#### 2. Check Payara Server Status
```bash
# Check if domain is running
./asadmin list-domains

# Check application deployment
./asadmin list-applications
```

#### 3. Check Database Connectivity
```bash
# Test database connection
./asadmin ping-connection-pool jdbc/hmisDB

# Expected: "Command ping-connection-pool executed successfully."
```

#### 4. Review Error Logs
```bash
# Check for critical errors in last 24 hours
grep -i "severe\|critical" /path/to/payara/glassfish/domains/domain1/logs/server.log | tail -50
```

### Weekly Checks

#### 1. Review System Resources
- CPU usage trends
- Memory consumption
- Disk space availability
- Network bandwidth

#### 2. Database Performance
- Slow query log review
- Table growth analysis
- Index usage statistics

#### 3. Backup Verification
- Verify latest backup exists
- Test backup restoration (on test environment)

### Monthly Checks

#### 1. Security Updates
- Check for Payara security patches
- Review MySQL security advisories
- Update system packages

#### 2. Performance Analysis
- Review response time trends
- Analyze peak usage patterns
- Identify optimization opportunities

#### 3. Capacity Planning
- Database growth projections
- Server resource trends
- Storage expansion needs

---

## Backup Procedures

### Database Backups

#### Automated Daily Backup Script
Create `/usr/local/bin/backup-hmis-db.sh`:
```bash
#!/bin/bash
BACKUP_DIR="/backups/hmis/database"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="hmis"
DB_USER="hmis_backup_user"
DB_PASS="your_password"  # Use secure method to store password

# Create backup directory
mkdir -p $BACKUP_DIR

# Perform backup
mysqldump -u $DB_USER -p$DB_PASS \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  $DB_NAME | gzip > $BACKUP_DIR/hmis_$DATE.sql.gz

# Delete backups older than 30 days
find $BACKUP_DIR -name "hmis_*.sql.gz" -mtime +30 -delete

echo "$(date): Database backup completed: hmis_$DATE.sql.gz"
```

Schedule with cron:
```bash
# Run daily at 1 AM
0 1 * * * /usr/local/bin/backup-hmis-db.sh >> /var/log/hmis-backup.log 2>&1
```

#### Restore from Backup
```bash
# Decompress and restore
gunzip < /backups/hmis/database/hmis_20240115_010000.sql.gz | \
  mysql -u root -p hmis
```

### Application Backups

#### Backup Payara Configuration
```bash
# Backup domain directory
tar -czf /backups/hmis/payara/domain1_$(date +%Y%m%d).tar.gz \
  /path/to/payara/glassfish/domains/domain1/config/
```

#### Backup Deployed Application
```bash
# Backup WAR file
cp /path/to/payara/glassfish/domains/domain1/applications/hmis.war \
  /backups/hmis/application/hmis_$(date +%Y%m%d).war
```

---

## Common Issues and Solutions

### Issue 1: High Memory Usage

**Symptoms:**
- Application becomes slow or unresponsive
- Out of Memory errors in logs
- Java heap space errors

**Solutions:**
```bash
# 1. Check current heap usage
jmap -heap <payara-pid>

# 2. Generate heap dump for analysis
jmap -dump:format=b,file=/tmp/heap-dump.hprof <payara-pid>

# 3. Increase heap size if needed
./asadmin delete-jvm-options "-Xmx512m"
./asadmin create-jvm-options "-Xmx8g"
./asadmin restart-domain
```

### Issue 2: Database Connection Pool Exhausted

**Symptoms:**
- "Unable to acquire connection" errors
- Application timeouts
- High wait queue length

**Solutions:**
1. Increase pool size in Admin Console:
   - Navigate to: **Resources → JDBC → Connection Pools**
   - Increase **Max Pool Size** (e.g., from 32 to 64)
   - Increase **Pool Resize Quantity**: 2

2. Check for connection leaks:
```sql
-- Show active connections
SHOW PROCESSLIST;

-- Kill long-running queries
KILL <process_id>;
```

### Issue 3: Slow Application Performance

**Investigation Steps:**
```bash
# 1. Check system resources
top
iostat -x 1 5

# 2. Check database slow queries
tail -f /var/log/mysql/mysql-slow.log

# 3. Enable SQL logging in Payara
# Add to persistence.xml:
# <property name="eclipselink.logging.level.sql" value="FINE"/>
```

**Solutions:**
- Optimize slow queries
- Add missing indexes
- Clear old data from large tables
- Increase server resources

### Issue 4: Application Won't Start

**Check:**
```bash
# 1. View server logs
tail -100 /path/to/payara/glassfish/domains/domain1/logs/server.log

# 2. Check port conflicts
netstat -tlnp | grep 8080

# 3. Verify Java version
java -version

# 4. Test database connectivity
./asadmin ping-connection-pool jdbc/hmisDB
```

### Issue 5: Disk Space Full

**Immediate Actions:**
```bash
# 1. Check disk usage
df -h

# 2. Find large log files
find /path/to/payara/glassfish/domains/domain1/logs -type f -size +100M

# 3. Delete old logs
find /path/to/payara/glassfish/domains/domain1/logs -name "server.log_*" -mtime +7 -delete

# 4. Clear temporary files
rm -rf /tmp/*
rm -rf /path/to/payara/glassfish/domains/domain1/generated/*
```

---

## Maintenance Schedule

### Daily
- ✅ Monitor application accessibility
- ✅ Check error logs
- ✅ Verify database backups

### Weekly
- ✅ Review system resource usage
- ✅ Analyze slow queries
- ✅ Clean old log files
- ✅ Check disk space

### Monthly
- ✅ Optimize database tables
- ✅ Archive old audit data
- ✅ Review security patches
- ✅ Performance trend analysis
- ✅ Test backup restoration

### Quarterly
- ✅ Comprehensive security audit
- ✅ Capacity planning review
- ✅ Update documentation
- ✅ Disaster recovery drill

---

## Emergency Contacts

**System Administrator:**
- Name: [Your Name]
- Phone: [Contact Number]
- Email: [Email Address]

**Database Administrator:**
- Name: [DBA Name]
- Phone: [Contact Number]
- Email: [Email Address]

**Escalation:**
- Manager: [Manager Name]
- Phone: [Contact Number]
- Email: [Email Address]

---

## Useful Commands Reference

### Payara Management
```bash
# Start domain
./asadmin start-domain domain1

# Stop domain
./asadmin stop-domain domain1

# Restart domain
./asadmin restart-domain domain1

# Deploy application
./asadmin deploy /path/to/hmis.war

# Undeploy application
./asadmin undeploy hmis

# List applications
./asadmin list-applications
```

### Database Management
```bash
# Connect to MySQL
mysql -u root -p hmis

# Export database
mysqldump -u root -p hmis > backup.sql

# Import database
mysql -u root -p hmis < backup.sql

# Show processlist
mysql -u root -p -e "SHOW PROCESSLIST;"
```

### System Monitoring
```bash
# System resources
htop

# Disk I/O
iostat -x 1

# Network connections
netstat -an | grep 8080

# Check logs in real-time
tail -f /path/to/payara/glassfish/domains/domain1/logs/server.log
```

---

## Additional Resources

- [Payara Server Documentation](https://docs.payara.fish/)
- [MySQL Performance Tuning](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)
- [HMIS GitHub Repository](https://github.com/hmislk/hmis)
- [VM Restart Guide](https://github.com/hmislk/hmis/wiki/VM-Restart-Guide)

---

**Last Updated:** 2025-11-23
**Version:** 1.0
