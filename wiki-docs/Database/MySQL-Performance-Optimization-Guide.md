# MySQL Performance Optimization Guide for HMIS Administrators

## Overview

This guide helps system administrators optimize MySQL database performance to improve HMIS application speed, especially for pharmacy module operations like autocomplete searches and stock management.

## When to Use This Guide

Apply these optimizations if you experience:
- Slow autocomplete dropdowns in pharmacy sales
- Long loading times when selecting items or stock
- General application slowness during peak usage
- Database timeouts or connection issues

## Before You Begin

### Prerequisites
- Administrative access to the MySQL server
- Ability to restart MySQL service
- At least 1GB of available RAM on the server
- Database backup completed (recommended)

### System Requirements Check

1. **Check available memory:**
   ```bash
   free -h
   ```
   You need at least 2GB total RAM for optimal performance.

2. **Check current MySQL configuration:**
   ```bash
   mysql -u root -p -e "SHOW VARIABLES LIKE 'innodb_buffer_pool_size';"
   ```

3. **Verify MySQL version:**
   ```bash
   mysql -u root -p -e "SELECT VERSION();"
   ```

## Step 1: Database Index Optimization (Automatic)

The HMIS system includes automatic database migration for performance indexes.

### Using the Admin Interface

1. **Access the Migration Page:**
   - Navigate to: `http://your-server:8080/hmis/faces/admin/database_migration.xhtml`
   - Login with administrator credentials

2. **Execute Migration v2.1.11:**
   - Look for "Performance optimization indexes" migration
   - Click "Execute All Pending Migrations"
   - Wait for completion (approximately 5-15 minutes)

3. **Verify Success:**
   - Check that migration status shows "Executed"
   - No errors in the execution log

### Expected Results
- 50-80% faster autocomplete searches
- Improved stock filtering performance
- Better overall pharmacy module responsiveness

## Step 2: MySQL Configuration Optimization (Manual)

### Important Notice
⚠️ **These changes require MySQL service restart and will cause temporary downtime.**
Plan the restart during maintenance hours.

### Configuration Steps

#### 1. Backup Current Configuration
```bash
sudo cp /etc/mysql/mysql.conf.d/mysqld.cnf /etc/mysql/mysql.conf.d/mysqld.cnf.backup.$(date +%Y%m%d)
```

#### 2. Calculate Optimal Settings

Choose your server type and apply the corresponding configuration:

##### Small Server (2-4GB RAM)
```ini
# Add to /etc/mysql/mysql.conf.d/mysqld.cnf
[mysqld]
# Memory optimization
innodb_buffer_pool_size = 1024M
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2

# Connection optimization
max_connections = 100
query_cache_size = 64M
```

##### Medium Server (4-8GB RAM)
```ini
# Add to /etc/mysql/mysql.conf.d/mysqld.cnf
[mysqld]
# Memory optimization
innodb_buffer_pool_size = 2048M
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2

# Connection optimization
max_connections = 150
query_cache_size = 128M
```

##### Large Server (8GB+ RAM)
```ini
# Add to /etc/mysql/mysql.conf.d/mysqld.cnf
[mysqld]
# Memory optimization
innodb_buffer_pool_size = 4096M
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2

# Connection optimization
max_connections = 200
query_cache_size = 256M
```

#### 3. Edit Configuration File

1. **Open the configuration file:**
   ```bash
   sudo nano /etc/mysql/mysql.conf.d/mysqld.cnf
   ```

2. **Add the appropriate settings** from above at the end of the file

3. **Save and exit** (Ctrl+X, then Y, then Enter in nano)

#### 4. Restart MySQL Service

##### Ubuntu/Debian:
```bash
sudo systemctl restart mysql
sudo systemctl status mysql
```

##### CentOS/RHEL:
```bash
sudo systemctl restart mysqld
sudo systemctl status mysqld
```

##### Traditional init systems:
```bash
sudo service mysql restart
sudo service mysql status
```

## Step 3: Verify Optimization Results

### 1. Check Configuration Applied
```bash
mysql -u root -p -e "SHOW VARIABLES LIKE 'innodb_buffer_pool_size';"
mysql -u root -p -e "SHOW VARIABLES LIKE 'max_connections';"
```

### 2. Test Application Performance

1. **Access HMIS pharmacy module**
2. **Test autocomplete functionality:**
   - Go to pharmacy sales page
   - Try typing in item search boxes
   - Notice improved response time

3. **Monitor for any issues:**
   - Check for connection errors
   - Verify normal application functionality

### 3. Performance Monitoring

Check these metrics weekly:

```bash
# Check database connections
mysql -u root -p -e "SHOW STATUS LIKE 'Threads_connected';"

# Check buffer pool efficiency
mysql -u root -p -e "SHOW ENGINE INNODB STATUS;" | grep -A 5 "BUFFER POOL"
```

## Troubleshooting

### MySQL Won't Start After Configuration Changes

1. **Check error logs:**
   ```bash
   sudo tail -f /var/log/mysql/error.log
   ```

2. **Common issues and fixes:**

   **Insufficient memory error:**
   - Reduce `innodb_buffer_pool_size` to 512M
   - Restart MySQL service

   **Log file size error:**
   - Remove old log files: `sudo rm /var/lib/mysql/ib_logfile*`
   - Restart MySQL service

### Application Still Slow

1. **Verify migration executed:**
   - Check admin migration page
   - Ensure v2.1.11 shows as "Executed"

2. **Check available memory:**
   ```bash
   free -h
   top
   ```

3. **Review MySQL settings:**
   - Ensure configuration changes were saved
   - Verify MySQL restart completed successfully

### Connection Issues

If you see "too many connections" errors:

1. **Temporary fix:**
   ```bash
   mysql -u root -p -e "SET GLOBAL max_connections = 200;"
   ```

2. **Permanent fix:**
   - Increase `max_connections` in configuration file
   - Restart MySQL service

## Regular Maintenance

### Monthly Tasks

1. **Optimize database tables:**
   ```bash
   mysql -u root -p -e "OPTIMIZE TABLE coop.ITEM, coop.STOCK, coop.ITEMBATCH;"
   ```

2. **Check disk space:**
   ```bash
   df -h
   ```

3. **Review slow query log (if enabled)**

### Monitor Performance Indicators

- **Autocomplete response time:** Should be under 1 second
- **Page load times:** Pharmacy pages should load within 3-5 seconds
- **Database connections:** Should stay under 80% of maximum
- **Memory usage:** MySQL should use allocated buffer pool efficiently

## Expected Performance Improvements

After completing all optimizations:

- **Autocomplete searches:** 50-80% faster response time
- **Stock filtering:** 60-90% performance improvement
- **Page loading:** 25-50% faster overall
- **User experience:** Significantly more responsive interface

## Support and Documentation

For additional technical details, refer to:
- Developer documentation in `developer_docs/database/mysql-performance-configuration.md`
- MySQL official documentation for your version
- Contact development team for complex issues

## Safety Reminders

- Always backup your database before making configuration changes
- Test configuration changes during low-usage periods
- Monitor system performance after changes
- Keep backup copies of working configurations
- Document any custom modifications for future reference

---

This guide provides step-by-step instructions for optimizing HMIS database performance. Follow the steps carefully and monitor results to ensure optimal system operation.