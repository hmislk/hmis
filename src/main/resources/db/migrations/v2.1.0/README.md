# Migration v2.1.0: Consumption Allowed Feature

## Overview
This migration adds the `consumption_allowed` field to the `item` table to support selective item consumption restrictions in the pharmacy module.

## Files in this Migration
- `migration.sql` - Forward migration script
- `rollback.sql` - Rollback script (removes the column)
- `migration-info.json` - Migration metadata and execution details
- `verify-migration.sql` - Post-migration verification queries
- `README.md` - This documentation

## Migration Details

### Purpose
- Add `consumptionAllowed` boolean attribute to Item entity
- Enable pharmacy staff to mark items as allowed/blocked for consumption
- Support system-wide configuration for consumption restriction

### Database Changes
1. **Add Column**: `consumption_allowed BOOLEAN`
2. **Backfill Data**: Set all existing items to `TRUE` (allowed)
3. **Add Constraints**: `NOT NULL DEFAULT TRUE`

### Application Changes (Already Implemented)
- Updated `Item.java` entity with `@Column(nullable = false)`
- Added consumption allowed toggle in AMP management UI
- Created configuration option for system-wide restriction
- Enhanced pharmacy issue autocomplete with consumption filtering

## Execution Instructions

### Prerequisites
1. **Backup Database** - Always backup before migration
2. **Test Environment** - Run on staging/test environment first
3. **Application Deployment** - Deploy application code before running migration

### Step 1: Execute Migration
```sql
-- Connect to database and run:
source /path/to/migration.sql
```

### Step 2: Verify Migration
```sql
-- Run verification queries:
source /path/to/verify-migration.sql
```

### Step 3: Test Application
1. Access AMP management: `http://localhost:8080/rh/faces/pharmacy/admin/amp.xhtml`
2. Verify "Consumption Allowed" field appears and defaults to "Yes"
3. Test pharmacy issue page: `http://localhost:8080/rh/faces/pharmacy/pharmacy_issue.xhtml`
4. Confirm item autocomplete works with new filtering

## Expected Results

### Database State After Migration
- All existing items have `consumption_allowed = TRUE`
- New items automatically get `consumption_allowed = TRUE`
- Column cannot accept NULL values
- No impact on existing pharmacy workflows

### Application Behavior
- **Default**: All items remain available for consumption
- **Configurable**: Admin can enable system-wide consumption restriction
- **Selective**: Individual items can be marked as not allowed for consumption
- **Backward Compatible**: Existing workflows continue unchanged

## Rollback Procedure

### When to Rollback
- Migration fails midway
- Unexpected application issues
- Need to revert to previous version

### Rollback Steps
```sql
-- 1. Backup current state (if needed)
-- 2. Execute rollback script
source /path/to/rollback.sql

-- 3. Verify rollback
DESCRIBE item; -- consumption_allowed column should not exist
```

### Post-Rollback Application Updates
After database rollback, also revert these application changes:
1. Remove `consumptionAllowed` property from `Item.java`
2. Remove consumption allowed UI from `amp.xhtml`
3. Remove config option from `ConfigOptionApplicationController.java`
4. Revert pharmacy issue autocomplete changes
5. Update `pharmacy_issue.xhtml` to use original autocomplete method

## Troubleshooting

### Common Issues

#### Migration Fails at Step 2 (Backfill)
```sql
-- Check for any constraint violations
SELECT * FROM item WHERE consumption_allowed IS NULL;
-- Manually update problematic records
UPDATE item SET consumption_allowed = TRUE WHERE id = [problem_id];
```

#### Migration Fails at Step 3 (Add Constraint)
```sql
-- Verify all records are non-NULL first
SELECT COUNT(*) FROM item WHERE consumption_allowed IS NULL;
-- If count > 0, run backfill again
UPDATE item SET consumption_allowed = TRUE WHERE consumption_allowed IS NULL;
```

#### Application Startup Issues
- Check entity mapping: `@Column(nullable = false)` annotation
- Verify JPA can load existing records
- Check for any custom SQL queries that need updating

## Performance Impact
- **Negligible**: Single column addition
- **Fast Execution**: Estimated < 1 minute for typical HMIS database
- **No Downtime**: Migration can run while application is active

## Future Considerations
This migration serves as a pilot for the planned database migration system. Lessons learned will inform the design of:
- Automated migration execution
- Progress monitoring
- Admin UI for migration management
- Version tracking system

## Related Resources
- **GitHub Issue**: https://github.com/hmislk/hmis/issues/16415
- **Branch**: `16413-consumption-allowed-or-not-to-be-marked-and-marked-available-selectively`
- **Documentation**: [Database Migration System Planning](link-to-future-docs)

## Contact
For issues or questions about this migration, please refer to GitHub issue #16415 or contact the development team.