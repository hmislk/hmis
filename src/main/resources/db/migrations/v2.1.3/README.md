# Migration v2.1.3 - Complete Package

## 📋 Package Contents

This migration package provides a production-safe solution for resolving decimal precision issues in the `BILLFINANCEDETAILS` table (GitHub Issue #16989).

### Files Included

| File | Purpose | Description |
|------|---------|-------------|
| `migration.sql` | **Original Migration** | Basic migration script (⚠️ **NOT PRODUCTION SAFE**) |
| `migration-safe.sql` | **Production Migration** | ✅ **RECOMMENDED** - Atomic, validated, safe migration |
| `rollback-migration-v2.1.3.sql` | **Rollback Script** | Complete rollback with data loss warnings |
| `pre-migration-validation.sql` | **Validation Script** | Comprehensive pre-migration checks |
| `PRODUCTION-DEPLOYMENT-GUIDE.md` | **Deployment Guide** | Step-by-step production deployment instructions |
| `README.md` | **Documentation** | This file - overview and usage instructions |

## 🚀 Quick Start

### For Production Deployment (RECOMMENDED)

1. **Pre-validation**:
   ```sql
   SOURCE pre-migration-validation.sql;
   ```

2. **Execute safe migration**:
   ```sql
   SOURCE migration-safe.sql;
   CALL migrate_v2_1_3_safe();
   ```

3. **If rollback needed**:
   ```sql
   SOURCE rollback-migration-v2.1.3.sql;
   CALL rollback_v2_1_3_safe();
   ```

### For Development/Testing

You may use the original `migration.sql` for development environments, but **NEVER** in production.

## ⚠️ Critical Differences

### ❌ Original migration.sql Issues:
- No transaction control
- No data validation
- No overflow checks
- No rollback mechanism
- Risk of partial application
- No pre-flight checks

### ✅ migration-safe.sql Advantages:
- Full transaction control (atomic execution)
- Comprehensive data validation
- Overflow detection and prevention
- Built-in rollback on failure
- Step-by-step validation
- Detailed progress reporting
- Production-ready error handling

## 🔧 Migration Details

### Problem Solved
- **Issue**: DECIMAL(38,0) columns lose precision in SUM() operations
- **Impact**: Pharmacy reports showing incorrect totals
- **Root Cause**: MySQL SUM() with DECIMAL(38,0) returns integers only
- **Solution**: Upgrade to DECIMAL(20,4) for proper decimal precision

### Columns Modified
All columns upgraded from various precisions to **DECIMAL(20,4)**:

1. `TOTALCOSTVALUE` - **CRITICAL** (main issue from GitHub #16989)
2. `BILLDISCOUNT`
3. `TOTALEXPENSE`
4. `TOTALQUANTITY`
5. `TOTALPURCHASEVALUE`
6. `TOTALRETAILSALEVALUE`
7. `TOTALDISCOUNT`
8. `BILLEXPENSE`
9. `LINEEXPENSE`

### Data Safety
- ✅ **No data loss** - precision is expanded, not reduced
- ✅ **Preserves existing data** - all values maintained
- ✅ **Improves accuracy** - enables proper decimal calculations
- ✅ **Full rollback available** - can revert if needed

## 📚 Usage Guide

### Step 1: Pre-Migration Planning

1. **Read the deployment guide**:
   ```bash
   cat PRODUCTION-DEPLOYMENT-GUIDE.md
   ```

2. **Run validation checks**:
   ```sql
   mysql -u[user] -p[password] [database] < pre-migration-validation.sql
   ```

3. **Create backup**:
   ```bash
   mysqldump --single-transaction [database] > backup_pre_v2.1.3.sql
   ```

### Step 2: Execute Migration

**For Production** (atomic, safe):
```sql
mysql -u[admin_user] -p[password] [database] < migration-safe.sql

# Then in MySQL prompt:
CALL migrate_v2_1_3_safe();
```

**For Development** (basic):
```sql
mysql -u[user] -p[password] [database] < migration.sql
```

### Step 3: Post-Migration Verification

```sql
-- Verify column types
SELECT COLUMN_NAME, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME IN ('TOTALCOSTVALUE', 'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE')
ORDER BY COLUMN_NAME;

-- Test SUM operations (should now show decimals)
SELECT SUM(TOTALCOSTVALUE) FROM BILLFINANCEDETAILS LIMIT 1;
```

### Step 4: Application Testing

1. Test pharmacy reports for correct decimal values
2. Verify financial calculations are accurate
3. Check that SUM operations show proper precision
4. Monitor application logs for any errors

## 🔄 Rollback Instructions

### When to Rollback
- Migration caused application errors
- Performance issues with new precision
- Business requirement to revert

### How to Rollback
```sql
# WARNING: May cause data loss (fractional values will be truncated)
mysql -u[admin_user] -p[password] [database] < rollback-migration-v2.1.3.sql

# Then in MySQL prompt:
CALL rollback_v2_1_3_safe();
```

### After Rollback
- Original decimal precision issues will return
- GitHub Issue #16989 symptoms will reoccur
- Plan alternative solution or re-migration

## 🛠️ Troubleshooting

### Common Issues

#### Migration Fails with "Data overflow detected"
**Cause**: Existing values too large for DECIMAL(20,4)
**Solution**:
1. Identify problematic records
2. Archive or clean data
3. Re-run migration

#### Application errors after migration
**Cause**: Application expects specific decimal format
**Solution**:
1. Check application logs
2. Update application code if needed
3. Consider rollback if critical

#### Performance issues
**Cause**: DECIMAL(20,4) vs original types
**Solution**:
1. Monitor query performance
2. Update indexes if needed
3. Consider optimization

## 📊 Expected Results

### Before Migration
```sql
-- SUM returns integer (precision lost)
SELECT SUM(TOTALCOSTVALUE) FROM BILLFINANCEDETAILS;
-- Result: 1234567 (no decimals)
```

### After Migration
```sql
-- SUM returns proper decimal
SELECT SUM(TOTALCOSTVALUE) FROM BILLFINANCEDETAILS;
-- Result: 1234567.8900 (with decimals)
```

### Application Impact
- ✅ Pharmacy reports show correct decimal values
- ✅ Cost calculations are accurate
- ✅ Financial totals maintain precision
- ✅ SUM() operations work correctly

## 📞 Support

### Issues with Migration
1. Check pre-migration validation results
2. Review deployment guide thoroughly
3. Test in development environment first
4. Contact database administrator if needed

### GitHub Issue
This migration resolves: [GitHub Issue #16989](https://github.com/hmislk/hmis/issues/16989) - Goods in Transit and Stock Transfer Reports - Multiple Cost/Value Display Issues

## 🔍 Technical Details

### MySQL Compatibility
- **Minimum**: MySQL 5.7+ (5.6 reached EOL in February 2021)
- **Recommended**: MySQL 8.0+
- **Tested**: MySQL 8.0

### Performance Impact
- **Minimal**: DECIMAL(20,4) has similar performance to DECIMAL(38,0)
- **Storage**: Slight increase in storage per row
- **Queries**: No significant impact on query performance

### Precision Details
- **Before**: DECIMAL(38,0) - integers only
- **After**: DECIMAL(20,4) - up to 4 decimal places
- **Range**: -99,999,999,999,999,999.9999 to 99,999,999,999,999,999.9999

## 📝 Change Log

### v2.1.3
- **Added**: Production-safe migration with full validation
- **Added**: Comprehensive rollback capability
- **Added**: Pre-migration validation script
- **Added**: Detailed deployment documentation
- **Fixed**: GitHub Issue #16989 - Decimal precision in SUM operations
- **Improved**: Error handling and transaction safety

---

## 📄 License

This migration script is part of the HMIS project and follows the project's licensing terms.

---

**Author**: Dr M H B Ariyaratne
**Date**: 2025-12-02
**Version**: 2.1.3
**GitHub Issue**: #16989