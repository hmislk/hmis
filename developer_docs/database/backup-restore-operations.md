# Database Backup and Restore Operations

## Overview
This document describes the automated process for backing up and restoring databases between different environments, specifically designed for bot automation and manual operations.

## Coop Production Database Backup to QA2

### Automated Process via GitHub Actions

The system provides an automated way to backup the Coop Production database and restore it to QA2 using GitHub Actions workflow.

#### Using GitHub Actions Workflow (Recommended for Bots)

1. **Navigate to GitHub Actions**
   - Go to your repository on GitHub
   - Click on "Actions" tab
   - Find "Database Export and Import Scheduler" workflow

2. **Trigger the Workflow**
   - Click "Run workflow" button
   - Select the following parameters:
     - **fromEnv**: `COOP_Prod`
     - **toEnv**: `QA2`
     - **date**: Schedule date in `YYYY-MM-DD` format

3. **Workflow Execution**
   - The workflow schedules the backup/restore operation for 2:00 AM Sri Lanka time on the specified date
   - Email notifications are sent to the team upon success/failure

#### Available Source Environments
- `COOP_Dev` - Coop Development
- `COOP_Prod` - Coop Production
- `Ruhunu_Prod` - Ruhunu Production
- `MP_Prod` - MP Production
- `RMH_Prod` - RMH Production
- `SLH_Prod` - SLH Production

#### Available Target Environments
- `QA1` - QA Environment 1
- `QA2` - QA Environment 2

### Manual Process (Advanced Users)

For direct execution, you can use the underlying script:

```bash
# Execute on the observability server
/home/azureuser/utils/db_utils/db_export_import_scheduler.sh COOP_Prod QA2
```

## Process Details

### How the Backup/Restore Works

1. **Backup Creation**
   - Creates mysqldump of source database
   - Stores backup in `/opt/db_export_import_backups/myBackup/backup.sql`
   - Manages backup file rotation (keeps previous backup as backup-old.sql)

2. **Transfer Process**
   - Copies backup file from source server to intermediate location
   - Transfers to target server's import directory
   - Uses secure SCP with SSH keys for authentication

3. **Database Restoration**
   - Drops existing target database if it exists
   - Creates new empty database
   - Imports the backup SQL file
   - Verifies restoration success

4. **Notification System**
   - Sends email notifications to development team
   - Reports success or failure status
   - Includes environment details and timestamp

### Configuration Requirements

The system relies on a configuration file located at:
```
/home/azureuser/utils/secrets/server_config.json
```

This file contains:
- Server IP addresses for each environment
- SSH key paths for secure connections
- Database connection details (IPs, usernames, passwords, database names)

### Security Features

- All connections use SSH key authentication
- Database passwords are stored securely in configuration files
- Backup files are owned by azureuser with proper permissions (644)
- Cleanup removes temporary files after operations

## Bot Integration

### For Automated Systems

Bots can trigger database backup/restore operations by:

1. **GitHub API Integration**
   - Use GitHub API to trigger the "Database Export and Import Scheduler" workflow
   - Pass required parameters: fromEnv, toEnv, and date
   - Monitor workflow status via GitHub API

2. **Example API Call**
   ```bash
   curl -X POST \
     -H "Authorization: token YOUR_GITHUB_TOKEN" \
     -H "Accept: application/vnd.github.v3+json" \
     https://api.github.com/repos/YOUR_REPO/actions/workflows/database_export_import_scheduler.yml/dispatches \
     -d '{"ref":"main","inputs":{"fromEnv":"COOP_Prod","toEnv":"QA2","date":"2024-01-15"}}'
   ```

### Monitoring and Verification

- Check email notifications for operation status
- Verify QA2 application functionality after restore
- Monitor logs on observability server if issues occur

## Emergency Procedures

### If Backup/Restore Fails

1. Check email notifications for error details
2. Verify server connectivity and SSH key access
3. Check disk space on backup directories
4. Ensure database services are running on target environment
5. Contact system administrators if persistent issues occur

### Rollback Procedure

If a restore causes issues:
1. The system automatically keeps the previous backup as `backup-old.sql`
2. Manual restoration from the old backup can be performed if needed
3. Contact database administrators for complex rollback scenarios

## Related Files

- Script: `.github/scripts/db_export_import_scheduler.sh`
- Workflow: `.github/workflows/database_export_import_scheduler.yml`
- QA2 Deployment: `.github/workflows/hims_qa2_ci_cd.yml`

## Contact Information

For issues or questions regarding database operations:
- Email notifications go to: imeshranawella00@gmail.com, geeth.gsm@gmail.com, deshanipubudu0415@gmail.com, iranimadushika28@gmail.com
- Check GitHub Actions logs for detailed execution information