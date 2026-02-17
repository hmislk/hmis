# Database Migration System

The HMIS Database Migration System provides administrators with a powerful, self-contained solution for managing database schema changes and version control. This system allows you to safely execute database migrations, track execution history, and maintain database consistency across different environments.

## Overview

The Database Migration System is built directly into the HMIS application, eliminating the need for external migration tools. It provides:

- **Version Control**: Track database schema versions alongside application versions
- **Safe Execution**: Atomic migrations with rollback capabilities
- **Progress Monitoring**: Real-time tracking of migration execution
- **Audit Trail**: Complete history of all migration activities
- **Admin Control**: Secure, administrator-controlled execution

## Accessing the Migration System

1. **Login** as an administrator to the HMIS system
2. **Navigate** to: **Admin → Database Migration**
3. **URL**: `http://your-domain/context/faces/admin/database_migration.xhtml`

![Migration Dashboard](images/migration-dashboard.png)

## Migration Dashboard

The migration dashboard provides a comprehensive overview of your database migration status:

### Status Cards
- **Current Version**: Shows the current database schema version
- **Application Version**: Displays the latest application version available
- **Pending Migrations**: Number of migrations waiting to be executed
- **Status**: Current system status (Ready/In Progress)

### Migration Progress
When migrations are running, you'll see:
- **Progress Bar**: Visual indicator of completion percentage
- **Current Migration**: Which migration is currently executing
- **Step Information**: Current step and total steps remaining

## Managing Migrations

### Pending Migrations

When new application versions include database changes, they appear in the **Pending Migrations** section:

1. **Review Migration Details**:
   - Version number and description
   - Estimated execution time
   - Whether downtime is required
   - Affected database tables

2. **Execute Migrations**:
   - Click **"Execute All Pending Migrations"**
   - Confirm the action when prompted
   - Monitor progress in real-time

### Viewing Available Migrations

The **Available Migrations** tab shows all migrations in the system:
- **Version**: Migration version number
- **Description**: Brief description of changes
- **Author**: Who created the migration
- **Affects**: Which modules are impacted
- **Status**: Executed or Pending

### Execution History

The **Execution History** tab provides a complete audit trail:
- **Version**: Which migration was executed
- **Status**: Success, Failed, or Rolled Back
- **Executed At**: When the migration ran
- **Duration**: How long it took to complete
- **Executed By**: Which administrator ran it
- **Actions**: Options to rollback successful migrations

### Migration Logs

The **Execution Log** tab shows detailed logs from migration runs:
- Complete SQL execution details
- Error messages (if any)
- Performance information
- Verification results

## Safety Features

### Migration Locking
- Only one migration can run at a time
- System prevents concurrent executions
- Automatic cleanup if migrations are interrupted

### Transaction Safety
- Each migration runs in a database transaction
- Automatic rollback on failure
- Data integrity protection

### Rollback Capabilities
- Safe rollback of successfully executed migrations
- Rollback scripts provided for each migration
- Complete audit trail of rollback activities

## Migration Versioning

The system uses **semantic versioning** for migrations:
- Format: `v2.1.0`, `v2.1.1`, `v2.2.0`
- Automatic discovery from filesystem
- Chronological execution order

### Version Components
- **Major**: Significant database changes
- **Minor**: New features or schema additions
- **Patch**: Bug fixes or minor adjustments

## Configuration Options

Administrators can configure migration behavior:

1. **Navigate** to: **Admin → System Configuration**
2. **Find** migration-related options:
   - **Auto Execute Database Migrations**: Enable automatic execution
   - **Show Migration Management to Admins**: Control UI visibility
   - **Require Migration Confirmation**: Force confirmation dialogs
   - **Enable Migration Progress Tracking**: Show real-time progress
   - **Log Migration Execution Details**: Enable detailed logging

## Troubleshooting

### Migration Fails to Execute

**Symptoms**: Migration shows as failed in execution history

**Solutions**:
1. Check the **Execution Log** tab for error details
2. Verify database permissions
3. Ensure sufficient disk space
4. Contact system administrator if needed

### Migration UI Not Accessible

**Symptoms**: Cannot access migration management page

**Solutions**:
1. Verify you have administrator privileges
2. Check if "Show Migration Management to Admins" is enabled
3. Clear browser cache and try again
4. Contact system administrator

### Slow Migration Performance

**Symptoms**: Migrations take longer than expected

**Solutions**:
1. Check database server performance
2. Verify network connectivity
3. Review migration complexity in logs
4. Consider scheduling during low-usage periods

## Best Practices

### Before Executing Migrations

1. **Backup Database**: Always create a backup before running migrations
2. **Review Changes**: Understand what each migration does
3. **Check Dependencies**: Ensure all prerequisites are met
4. **Plan Downtime**: Schedule during low-usage periods if required

### During Migration Execution

1. **Monitor Progress**: Watch the real-time progress indicators
2. **Don't Interrupt**: Allow migrations to complete naturally
3. **Check Logs**: Review execution logs for any warnings
4. **Verify Results**: Confirm changes were applied correctly

### After Migration Completion

1. **Test Functionality**: Verify all system features work correctly
2. **Check Performance**: Monitor system performance
3. **Review Logs**: Examine execution logs for any issues
4. **Document Changes**: Note any operational changes needed

## Security Considerations

### Access Control
- Only administrators can execute migrations
- User actions are logged and auditable
- Role-based access controls enforced

### Data Protection
- All migrations run in transactions
- Automatic rollback on failure
- Complete audit trail maintained

### System Safety
- Migration locking prevents conflicts
- Progress monitoring prevents interruption
- Error handling protects data integrity

## Support and Maintenance

### Regular Monitoring
- Check migration status periodically
- Review execution logs for warnings
- Monitor system performance after migrations

### Backup Strategy
- Schedule regular database backups
- Test backup restoration procedures
- Keep migration logs for audit purposes

### Getting Help
- Check execution logs for error details
- Review this documentation for guidance
- Contact system administrator for assistance
- Report issues through the support system

## Advanced Features

### Manual Migration Creation
Advanced users can create custom migration scripts following the established format structure.

### Batch Processing
The system can handle multiple migrations in sequence, executing them in the correct order automatically.

### Integration Monitoring
The migration system integrates with the overall HMIS monitoring infrastructure for comprehensive system oversight.

---

## Summary

The Database Migration System provides a robust, secure, and user-friendly way to manage database schema changes in your HMIS installation. By following the guidelines in this documentation, administrators can safely maintain database consistency while deploying new application features.

For additional assistance or questions about the migration system, please contact your system administrator or refer to the technical documentation.