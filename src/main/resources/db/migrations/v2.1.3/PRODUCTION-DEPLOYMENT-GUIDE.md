# Migration v2.1.3 - Production Deployment Guide

## Overview
This migration resolves critical decimal precision issues in the `BILLFINANCEDETAILS` table that cause incorrect SUM() calculations, particularly affecting pharmacy reports (GitHub Issue #16989).

## 📋 Pre-Deployment Checklist

### Required Resources
- [ ] Database Administrator access
- [ ] Application maintenance window (15-30 minutes)
- [ ] Full database backup completed
- [ ] Rollback plan approved
- [ ] Stakeholder notifications sent

### Environment Verification
- [ ] Verify this is the correct database environment
- [ ] Confirm application version compatibility
- [ ] Check disk space (ensure 20% free space for migration)
- [ ] Verify MySQL version compatibility (5.7+ recommended)

## ⚠️ Critical Warnings

### Data Safety
- **BACKUP REQUIRED**: Full database backup is **MANDATORY** before execution
- **DOWNTIME REQUIRED**: 15-30 minutes application downtime needed
- **DATA LOSS RISK**: **LOW** - Precision is expanded, not reduced
- **ROLLBACK AVAILABLE**: Complete rollback script provided

### Impact Assessment
- **Tables Affected**: `BILLFINANCEDETAILS` only
- **Columns Modified**: 9 financial columns upgraded to DECIMAL(20,4)
- **Application Impact**: Pharmacy reports will show correct decimal values
- **Performance Impact**: Minimal - DECIMAL(20,4) vs DECIMAL(38,0)

## 🚀 Deployment Steps

### Step 1: Pre-Deployment Backup
```bash
# Create full database backup
mysqldump -u[username] -p[password] --single-transaction --routines --triggers [database_name] > backup_pre_migration_v2.1.3_$(date +%Y%m%d_%H%M%S).sql

# Verify backup integrity
mysql -u[username] -p[password] -e "SELECT COUNT(*) FROM BILLFINANCEDETAILS;" [database_name]
```

### Step 2: Application Maintenance Mode
```bash
# Put application in maintenance mode
# Method depends on your deployment setup:
# - Update load balancer to show maintenance page
# - Set application maintenance flag
# - Stop application servers (if using dedicated maintenance window)
```

### Step 3: Execute Migration
```sql
-- Connect to MySQL as admin user
mysql -u[admin_username] -p[admin_password] [database_name]

-- Load and execute the safe migration
SOURCE /path/to/migration-safe.sql;

-- Execute the migration procedure
CALL migrate_v2_1_3_safe();

-- Verify execution
SELECT 'Migration completed successfully' AS status;
```

### Step 4: Verification
```sql
-- Verify column types are correct
SELECT COLUMN_NAME, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME IN (
    'TOTALCOSTVALUE', 'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE',
    'BILLDISCOUNT', 'TOTALDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY',
    'BILLEXPENSE', 'LINEEXPENSE'
  )
ORDER BY COLUMN_NAME;

-- Expected result: All 9 columns should show 'decimal(20,4)'

-- Test SUM operations (the original issue)
SELECT
    SUM(TOTALCOSTVALUE) as total_cost,
    SUM(TOTALPURCHASEVALUE) as total_purchase,
    COUNT(*) as record_count
FROM BILLFINANCEDETAILS
WHERE TOTALCOSTVALUE IS NOT NULL
LIMIT 1;

-- Verify no data loss
SELECT COUNT(*) as total_records FROM BILLFINANCEDETAILS;
-- Should match pre-migration count
```

### Step 5: Application Testing
```bash
# Start application services
# Test critical functionality:

# 1. Test pharmacy reports
# - Navigate to pharmacy cost reports
# - Verify decimal values show correctly (not rounded to integers)
# - Check SUM calculations are accurate

# 2. Test financial reports
# - Check BILLFINANCEDETAILS dependent reports
# - Verify cost calculations
# - Ensure no application errors

# 3. Spot check data
# - Compare key financial totals before/after
# - Verify decimal precision is maintained
```

### Step 6: Remove Maintenance Mode
```bash
# Remove application from maintenance mode
# - Update load balancer to resume normal traffic
# - Clear maintenance flags
# - Monitor application logs for any issues
```

## 🔧 Troubleshooting

### Common Issues

#### Issue: Migration Procedure Fails with Overflow Error
**Symptom**: `Data overflow detected in [COLUMN] - migration aborted`
**Cause**: Existing data exceeds DECIMAL(20,4) capacity
**Resolution**:
1. Identify problematic records:
   ```sql
   SELECT * FROM BILLFINANCEDETAILS
   WHERE ABS(TOTALCOSTVALUE) >= 9999999999999999.9999;
   ```
2. Clean or archive problematic data
3. Re-run migration

#### Issue: Record Count Mismatch
**Symptom**: `Data integrity check failed - record count mismatch`
**Cause**: Concurrent data modifications during migration
**Resolution**:
1. Ensure application is in maintenance mode
2. Verify no background processes are modifying data
3. Re-run migration

#### Issue: Application Shows Errors After Migration
**Symptom**: Application errors when accessing financial reports
**Cause**: Application may expect specific decimal precision
**Resolution**:
1. Check application logs for specific errors
2. Consider rollback if critical functionality affected
3. Update application code if needed

### Rollback Procedure
If migration causes issues:
```sql
-- Execute rollback (WARNING: May cause data loss)
SOURCE /path/to/rollback-migration-v2.1.3.sql;
CALL rollback_v2_1_3_safe();
```

## 📊 Success Criteria

### Technical Validation
- [ ] All 9 target columns are DECIMAL(20,4)
- [ ] No DECIMAL(38,0) columns remain in target list
- [ ] Record count matches pre-migration count
- [ ] SUM() operations return proper decimal values
- [ ] No application errors in logs

### Functional Validation
- [ ] Pharmacy reports show correct decimal values
- [ ] Cost calculations are accurate
- [ ] Financial totals match expected values
- [ ] No user-reported calculation errors

### Performance Validation
- [ ] Query performance unchanged or improved
- [ ] Report generation time acceptable
- [ ] No memory or disk space issues

## 📝 Post-Deployment Tasks

### Immediate (0-2 hours)
- [ ] Monitor application error logs
- [ ] Verify pharmacy report accuracy
- [ ] Check system performance metrics
- [ ] Confirm backup was successful

### Short-term (24-48 hours)
- [ ] Gather user feedback on report accuracy
- [ ] Monitor for any calculation discrepancies
- [ ] Verify all financial reports work correctly
- [ ] Update documentation with deployment results

### Long-term (1 week)
- [ ] Close GitHub Issue #16989
- [ ] Archive deployment artifacts
- [ ] Update production runbooks
- [ ] Schedule backup cleanup

## 📞 Emergency Contacts

### During Deployment Window
- **DBA Lead**: [Contact Information]
- **Application Lead**: [Contact Information]
- **DevOps Lead**: [Contact Information]
- **Business Stakeholder**: [Contact Information]

### Escalation Path
1. **Level 1**: Database Administrator
2. **Level 2**: Senior Developer/DBA
3. **Level 3**: System Architect
4. **Level 4**: CTO/Technical Director

## 📈 Monitoring and Alerts

### Key Metrics to Monitor
- Database connection count
- Query response times
- Application error rates
- Memory usage
- Disk space utilization

### Alert Thresholds
- Error rate > 1% of normal
- Response time > 150% of baseline
- Memory usage > 85%
- Disk space < 15%

## 📄 Documentation Updates Required

### After Successful Deployment
- [ ] Update database schema documentation
- [ ] Update API documentation if affected
- [ ] Update troubleshooting guides
- [ ] Update backup/recovery procedures

---

## 🔒 Security Considerations

- Migration scripts contain no sensitive data
- Database credentials should be secured
- Backup files should be encrypted
- Access logs should be maintained

## 📋 Compliance Notes

- This migration improves data accuracy
- No PII or sensitive data is modified
- Audit trail is maintained
- Rollback capability preserves compliance

---

**Deployment Date**: _______________
**Deployed By**: ___________________
**Approved By**: ___________________
**Verification By**: _______________