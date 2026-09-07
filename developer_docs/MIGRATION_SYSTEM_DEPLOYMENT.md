# Database Migration System Deployment Guide

## Overview
This guide provides step-by-step instructions for deploying the new database migration system along with the consumption allowed feature.

## Prerequisites
- ✅ Application code deployed to target environment
- ✅ Database backup completed
- ✅ Admin user access to application
- ✅ Database permissions for schema modifications

## Deployment Steps

### Phase 1: Application Deployment
1. **Deploy Application Code**
   ```bash
   # Deploy the updated application with migration system
   # Ensure all new files are included:
   # - DatabaseMigration entity
   # - DatabaseMigrationFacade
   # - DatabaseMigrationController
   # - MigrationDiscoveryService
   # - Admin UI (database_migration.xhtml)
   # - Migration scripts in /db/migrations/v2.1.0/
   ```

2. **Verify Application Startup**
   - Application should start successfully
   - Check logs for any entity mapping errors
   - Verify no database connection issues

### Phase 2: Database Migration System Initialization

1. **Create DatabaseMigration Table**
   ```sql
   -- The migration system table should be created automatically by JPA
   -- If not, create manually:
   CREATE TABLE database_migration (
       id BIGINT NOT NULL AUTO_INCREMENT,
       version VARCHAR(50) NOT NULL UNIQUE,
       description VARCHAR(255) NOT NULL,
       filename VARCHAR(100) NOT NULL,
       status VARCHAR(50) NOT NULL,
       executed_at DATETIME NOT NULL,
       executed_by_id BIGINT,
       execution_time_ms BIGINT,
       error_message LONGTEXT,
       execution_log LONGTEXT,
       rollback_filename VARCHAR(100),
       rollback_at DATETIME,
       rollback_by_id BIGINT,
       requires_downtime BOOLEAN DEFAULT FALSE,
       estimated_duration_ms BIGINT,
       migration_metadata LONGTEXT,
       PRIMARY KEY (id),
       FOREIGN KEY (executed_by_id) REFERENCES web_user(id),
       FOREIGN KEY (rollback_by_id) REFERENCES web_user(id)
   );
   ```

2. **Verify Configuration Options**
   - Login to application as admin
   - Check that migration configuration options are created
   - Navigate to: **Admin → Database Migration**

### Phase 3: Execute Consumption Allowed Migration

1. **Access Migration Management UI**
   - URL: `http://your-domain/context/faces/admin/database_migration.xhtml`
   - Login as administrator
   - Verify you see the migration dashboard

2. **Review Pending Migrations**
   - Should see `v2.1.0` in pending migrations
   - Review migration details:
     - Description: "Add consumption_allowed field to Item table with backfill and constraints"
     - Estimated Duration: 1 minute
     - Requires Downtime: No
     - Affected Tables: item

3. **Execute Migration**
   - Click "Execute All Pending Migrations"
   - Confirm the execution when prompted
   - Monitor progress in real-time
   - Verify successful completion

4. **Post-Migration Verification**
   ```sql
   -- Run verification queries
   source /path/to/verify-migration.sql

   -- Expected results:
   -- ✓ consumption_allowed column exists
   -- ✓ Column is NOT NULL with DEFAULT TRUE
   -- ✓ All existing items have consumption_allowed = TRUE
   -- ✓ No NULL values in the column
   ```

### Phase 4: Feature Testing

1. **Test AMP Management**
   - Navigate to: **Pharmacy → Admin → AMP**
   - Create or edit an AMP
   - Verify "Consumption Allowed" field appears
   - Test toggling the value
   - Save and verify persistence

2. **Test Pharmacy Issue**
   - Navigate to: **Pharmacy → Pharmacy Issue**
   - Test item autocomplete
   - Verify items appear in search results
   - Toggle consumption allowed for an item to FALSE
   - Verify restricted items don't appear when restriction is enabled

3. **Test Configuration**
   - Access configuration options
   - Find "Restrict Consumption to Items with Consumption Allowed Flag"
   - Test toggling this setting
   - Verify behavior changes in pharmacy issue autocomplete

### Phase 5: Production Validation

1. **Performance Verification**
   ```sql
   -- Check query performance
   EXPLAIN SELECT * FROM item WHERE consumption_allowed = 1;
   EXPLAIN SELECT * FROM database_migration ORDER BY version;

   -- Verify no performance degradation in pharmacy operations
   ```

2. **Data Integrity Check**
   ```sql
   -- Verify all items have proper values
   SELECT COUNT(*) as items_with_null_consumption
   FROM item
   WHERE consumption_allowed IS NULL;
   -- Should return 0

   -- Verify migration history
   SELECT version, status, executed_at
   FROM database_migration
   ORDER BY version;
   ```

3. **User Acceptance Testing**
   - Have pharmacy staff test the new feature
   - Verify existing workflows are not disrupted
   - Test the new consumption restriction functionality

## Rollback Procedure

### If Migration Fails
1. **Check Migration Status**
   ```sql
   SELECT * FROM database_migration WHERE version = 'v2.1.0';
   ```

2. **Manual Rollback (if needed)**
   ```sql
   -- Execute rollback script
   source /path/to/rollback.sql

   -- Update migration status
   UPDATE database_migration
   SET status = 'ROLLED_BACK', rollback_at = NOW()
   WHERE version = 'v2.1.0';
   ```

### If Application Issues Occur
1. **Revert Application Code**
   - Deploy previous version without migration system
   - Remove consumption_allowed references

2. **Database Cleanup**
   ```sql
   -- Remove consumption_allowed column
   ALTER TABLE item DROP COLUMN consumption_allowed;

   -- Drop migration table (optional)
   DROP TABLE database_migration;
   ```

## Monitoring and Maintenance

### Ongoing Monitoring
- Check migration system logs regularly
- Monitor database performance after changes
- Validate user feedback on new functionality

### Future Migrations
- Use the established migration system for future schema changes
- Follow the migration script structure in `/db/migrations/`
- Always test migrations in staging environment first

### Backup Strategy
- Schedule regular database backups before migrations
- Keep migration logs for audit purposes
- Document all schema changes in version control

## Troubleshooting

### Common Issues

1. **Migration UI Not Accessible**
   - Check user permissions
   - Verify admin role assignment
   - Check application logs for errors

2. **Migration Execution Fails**
   - Check database permissions
   - Verify SQL syntax in migration scripts
   - Review error messages in migration log

3. **Performance Issues**
   - Check database indexes
   - Monitor query execution times
   - Verify connection pool settings

4. **Feature Not Working**
   - Clear application cache
   - Verify entity reloading
   - Check JSF component updates

### Support Contacts
- **Technical Issues**: Development Team
- **Database Issues**: Database Administrator
- **User Issues**: System Administrator

## Success Criteria
- ✅ Migration system deployed and functional
- ✅ v2.1.0 migration executed successfully
- ✅ Consumption allowed feature working
- ✅ No performance degradation
- ✅ User acceptance confirmed
- ✅ Backup and rollback procedures tested

## Documentation Updates
After successful deployment:
- Update user manuals with new consumption allowed feature
- Document migration system procedures for future use
- Update system architecture documentation
- Create training materials for administrators