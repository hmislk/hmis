# Database Migration Development Guide

## Overview

The HMIS Database Migration System provides a comprehensive framework for managing database schema changes across development, testing, and production environments. This guide covers the technical implementation details for developers working with the migration system.

## Architecture Components

### Core Classes

1. **DatabaseMigration Entity** (`com.divudi.core.entity.DatabaseMigration`)
   - JPA entity for tracking migration execution history
   - Stores version, status, execution metadata, and audit information

2. **DatabaseMigrationFacade** (`com.divudi.core.facade.DatabaseMigrationFacade`)
   - EJB facade for database operations
   - CRUD operations and query methods for migration records

3. **DatabaseMigrationController** (`com.divudi.bean.common.DatabaseMigrationController`)
   - JSF managed bean for UI interactions
   - Business logic for migration execution and progress tracking

4. **MigrationDiscoveryService** (`com.divudi.core.util.MigrationDiscoveryService`)
   - Service for discovering migration files from filesystem
   - Handles version comparison and file loading

5. **MigrationInfo** (`com.divudi.core.util.MigrationInfo`)
   - DTO for migration metadata from JSON files

### Migration Status Enumeration

```java
public enum MigrationStatus {
    PENDING,        // Not yet executed
    EXECUTING,      // Currently running
    SUCCESS,        // Completed successfully
    FAILED,         // Execution failed
    ROLLED_BACK,    // Successfully rolled back
    ROLLBACK_FAILED // Rollback attempt failed
}
```

## File Structure

### Migration Directory Layout

```
src/main/resources/db/migrations/
├── v2.1.0/
│   ├── migration-info.json
│   ├── migration.sql
│   └── rollback.sql
├── v2.1.1/
│   ├── migration-info.json
│   ├── migration.sql
│   └── rollback.sql
└── v2.2.0/
    ├── migration-info.json
    ├── migration.sql
    └── rollback.sql
```

### Required Files

1. **migration-info.json**: Migration metadata
2. **migration.sql**: Forward migration script (required)
3. **rollback.sql**: Rollback script (optional but recommended)

## Version Numbering System

### Semantic Versioning Format

- **Format**: `v{major}.{minor}.{patch}`
- **Examples**: v2.1.0, v2.1.1, v2.2.0, v3.0.0

### Version Increment Rules

- **Patch** (v2.1.0 → v2.1.1): Bug fixes, data corrections, minor schema adjustments
- **Minor** (v2.1.1 → v2.2.0): New features, new tables, new columns, indexes
- **Major** (v2.2.0 → v3.0.0): Breaking changes, significant restructuring, data model changes

### Version Comparison Implementation

The system uses integer-per-segment comparison for proper semantic version ordering:

```java
private int compareVersions(String version1, String version2) {
    // Handles null cases and removes 'v' prefix
    // Splits by '.' and compares each segment as integer
    // Falls back to string comparison for non-numeric segments
}
```

## Migration Metadata Schema

### migration-info.json Structure

```json
{
    "version": "v2.2.0",
    "description": "Add consumption allowed field to Item entity",
    "author": "Dr M H B Ariyaratne",
    "estimatedDurationMinutes": 5,
    "requiresDowntime": false,
    "affectedTables": ["item"],
    "affectedModules": ["Pharmacy", "Inventory"],
    "preRequisites": [
        "Database backup completed",
        "Ensure no active transactions on item table"
    ],
    "migrationSteps": [
        {
            "description": "Add consumptionAllowed column to item table"
        },
        {
            "description": "Update existing items to set consumptionAllowed = true"
        },
        {
            "description": "Add index for performance optimization"
        }
    ]
}
```

### Field Definitions

- **version**: Must match directory name
- **description**: Brief summary of changes
- **author**: Developer responsible for migration
- **estimatedDurationMinutes**: Expected execution time
- **requiresDowntime**: Whether application downtime is needed
- **affectedTables**: List of database tables modified
- **affectedModules**: List of application modules impacted
- **preRequisites**: Prerequisites before execution
- **migrationSteps**: Detailed breakdown of migration actions

## Development Workflow

### 1. Feature Branch Creation

```bash
git checkout -b feature/consumption-allowed
```

### 2. Version Determination

- Check current latest version: Look in existing migration directories
- Determine increment type based on change scope
- Assign next sequential version number

### 3. Migration Creation

```bash
mkdir -p src/main/resources/db/migrations/v2.2.0
cd src/main/resources/db/migrations/v2.2.0
touch migration-info.json migration.sql rollback.sql
```

### 4. SQL Script Development

#### Forward Migration (migration.sql)

```sql
-- Migration v2.2.0: Add consumption allowed field
-- Author: Dr M H B Ariyaratne
-- Date: 2024-11-09

-- Add consumptionAllowed column to item table
ALTER TABLE item ADD COLUMN consumption_allowed BOOLEAN NOT NULL DEFAULT TRUE;

-- Null-safe backfill for existing data
UPDATE item
SET consumption_allowed = TRUE
WHERE consumption_allowed IS NULL;

-- Performance optimization
CREATE INDEX idx_item_consumption_allowed
ON item(consumption_allowed);

-- Verify data integrity
-- (Optional verification queries can be added)
```

#### Rollback Script (rollback.sql)

```sql
-- Rollback v2.2.0: Remove consumption allowed field
-- Author: Dr M H B Ariyaratne

-- Drop performance index
DROP INDEX IF EXISTS idx_item_consumption_allowed;

-- Remove column (will lose data - document this risk)
ALTER TABLE item DROP COLUMN consumption_allowed;
```

### 5. Testing Strategy

#### Local Testing

```bash
# Backup database
mysqldump -u root -p hmis_dev > backup_before_v2.2.0.sql

# Test migration execution through admin UI
# Verify data integrity
# Test rollback functionality

# Restore from backup if needed
mysql -u root -p hmis_dev < backup_before_v2.2.0.sql
```

#### Automated Testing

Consider adding tests to verify:
- Migration executes without errors
- Expected schema changes are applied
- Data integrity is maintained
- Rollback functionality works correctly

## Integration Points

### Configuration Management

The migration system integrates with `ConfigOptionApplicationController`:

```java
// Latest executed version is stored as configuration
configController.setLongTextValueByKey("Database Schema Version", latestVersion);

// Migration behavior can be configured
getBooleanValueByKey("Auto Execute Database Migrations", false);
getBooleanValueByKey("Show Migration Management to Admins", true);
```

### Session and Security

- Only administrators can execute migrations
- User actions are logged with audit trail
- Migration execution requires proper authentication

### Performance Considerations

- Migrations run in database transactions
- Large migrations may require batch processing
- Consider downtime requirements for schema changes
- Monitor execution time and system resources

## Error Handling

### Migration Failures

```java
// Automatic rollback on SQL errors
try {
    executeSqlScript(sql, logBuilder);
    migration.setStatus(MigrationStatus.SUCCESS);
} catch (SQLException e) {
    connection.rollback(); // Automatic transaction rollback
    migration.setStatus(MigrationStatus.FAILED);
    migration.setErrorMessage(e.getMessage());
}
```

### Recovery Strategies

1. **Failed Migration**: Review logs, fix issues, retry execution
2. **Partial Execution**: Use transaction rollback, investigate state
3. **Rollback Failure**: Manual intervention required, restore from backup

## Deployment Integration

### Continuous Integration

```yaml
# Example CI pipeline steps
- name: Run Migration Tests
  run: |
    ./detect-maven.sh test -Dtest=*MigrationTest*

- name: Validate Migration Scripts
  run: |
    # Syntax validation
    # Schema validation
    # Rollback script verification
```

### Production Deployment

1. **Pre-deployment**: Database backup
2. **Migration Execution**: Through admin UI or automated process
3. **Verification**: Data integrity checks
4. **Monitoring**: Performance and error monitoring
5. **Rollback Plan**: Ready if issues detected

## Best Practices

### SQL Script Guidelines

1. **Atomic Operations**: Each migration should be atomic
2. **Idempotent Scripts**: Handle re-execution gracefully
3. **Data Preservation**: Avoid data loss operations
4. **Performance Impact**: Consider query execution time
5. **Index Management**: Add/drop indexes strategically

### Version Management

1. **Sequential Numbering**: Don't skip version numbers
2. **Branch Coordination**: Coordinate versions across feature branches
3. **Release Planning**: Align versions with release cycles

### Documentation Standards

1. **Clear Descriptions**: Explain what and why
2. **Impact Assessment**: Document affected systems
3. **Rollback Risks**: Document potential data loss
4. **Prerequisites**: List all requirements

## Troubleshooting

### Common Issues

1. **Version Conflicts**: Multiple developers using same version number
2. **Dependency Issues**: Migration requires previous migration not yet executed
3. **Schema Conflicts**: Migration conflicts with existing schema
4. **Performance Issues**: Long-running migrations causing timeouts

### Debugging Tools

1. **Migration Logs**: Detailed execution logs in admin UI
2. **Database State**: Query migration tracking table
3. **File System**: Verify migration files are deployed correctly
4. **Configuration**: Check migration-related configuration options

## Advanced Features

### Custom Migration Logic

For complex migrations that require Java code:

1. Create specialized service beans
2. Inject into migration controller
3. Call from migration execution logic
4. Maintain transaction boundaries

### Migration Dependencies

For migrations that depend on external systems:

1. Document dependencies in migration-info.json
2. Add prerequisite checks in migration logic
3. Implement retry mechanisms for transient failures

### Batch Processing

For large data migrations:

1. Implement batch processing in SQL scripts
2. Add progress tracking for long operations
3. Consider memory and transaction log implications

---

This guide provides the technical foundation for developing and maintaining database migrations in the HMIS system. Always test thoroughly and coordinate with the team before implementing schema changes.