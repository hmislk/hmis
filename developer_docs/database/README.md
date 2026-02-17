# Database Documentation

This directory contains comprehensive database documentation for the HMIS project.

## Documents

### [MySQL Developer Guide](mysql-developer-guide.md) 🆕
**Primary database development reference**
- Credential management and security
- Debugging techniques and troubleshooting
- Performance optimization
- Schema understanding
- Common query patterns
- Environment-specific setup

### [Backup and Restore Operations](backup-restore-operations.md)
**Production and QA database operations**
- Automated backup processes
- Environment synchronization
- GitHub Actions workflows

## Quick Reference

### Credential Locations (NEVER commit to git)
- **Windows**: `C:\Credentials\credentials.txt`
- **Linux/Mac**: `~/.config/hmis/credentials.txt`

### Essential Database Commands
```bash
# Connect to database
mysql -u root -p[password] -h localhost coop

# Check Purchase Order completion
SELECT id, deptId, completed, completedAt FROM bill 
WHERE billTypeAtomic = 'PHARMACY_ORDER_APPROVAL' 
ORDER BY createdAt DESC LIMIT 10;
```

### Emergency Contacts & Resources
- See [MySQL Developer Guide](mysql-developer-guide.md) for detailed troubleshooting
- Reference [CLAUDE.md](../../CLAUDE.md) for project rules and workflows

## Security Notice
⚠️ **WARNING**: All database operations should follow security guidelines outlined in the MySQL Developer Guide. Never commit credentials or sensitive data to version control.