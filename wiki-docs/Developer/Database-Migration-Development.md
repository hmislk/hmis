# Database Migration Development Guide

## Overview

This guide explains how developers can create and manage incremental database migrations in the HMIS system. The migration system uses semantic versioning to track and execute database schema changes safely across different environments.

## Quick Start

### 1. Check Current Version

Before creating a new migration, check the latest version:

```bash
ls src/main/resources/db/migrations/
# Output: v2.1.0, v2.1.1, v2.2.0, etc.
```

### 2. Determine Next Version

Choose the appropriate version increment:

- **Bug Fix**: v2.1.0 → v2.1.1 (patch)
- **New Feature**: v2.1.1 → v2.2.0 (minor)
- **Breaking Change**: v2.2.0 → v3.0.0 (major)

### 3. Create Migration Directory

```bash
mkdir src/main/resources/db/migrations/v2.2.0
cd src/main/resources/db/migrations/v2.2.0
```

### 4. Create Required Files

Every migration needs these files:

- `migration-info.json` - Metadata about the migration
- `migration.sql` - The actual database changes
- `rollback.sql` - How to undo the changes (optional but recommended)

## File Templates

### migration-info.json

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
        "Database backup completed"
    ],
    "migrationSteps": [
        {
            "description": "Add consumptionAllowed column to item table"
        },
        {
            "description": "Update existing items to set default value"
        }
    ]
}
```

### migration.sql

```sql
-- Migration v2.2.0: Add consumption allowed field
-- Author: Dr M H B Ariyaratne
-- Date: 2024-11-09

-- Add the new column with default value
ALTER TABLE item
ADD COLUMN consumption_allowed BOOLEAN NOT NULL DEFAULT TRUE;

-- Update any existing NULL values (safety measure)
UPDATE item
SET consumption_allowed = TRUE
WHERE consumption_allowed IS NULL;

-- Add index for better query performance
CREATE INDEX idx_item_consumption_allowed
ON item(consumption_allowed);
```

### rollback.sql

```sql
-- Rollback v2.2.0: Remove consumption allowed field
-- Author: Dr M H B Ariyaratne

-- Remove the index first
DROP INDEX IF EXISTS idx_item_consumption_allowed;

-- Remove the column (⚠️ This will delete data!)
ALTER TABLE item DROP COLUMN consumption_allowed;
```

## Version Numbering Rules

### Semantic Versioning Format: v{major}.{minor}.{patch}

| Change Type | Example | When to Use |
|-------------|---------|-------------|
| **Patch** (v2.1.0 → v2.1.1) | Bug fixes, data corrections | - Fix incorrect default values<br>- Correct data types<br>- Minor index optimizations |
| **Minor** (v2.1.1 → v2.2.0) | New features, additions | - Add new columns<br>- Create new tables<br>- Add new indexes<br>- New functionality |
| **Major** (v2.2.0 → v3.0.0) | Breaking changes | - Remove columns/tables<br>- Change data types<br>- Restructure relationships<br>- Incompatible changes |

### Examples by Feature Type

#### Adding a New Field (Minor Version)
```
Current: v2.1.5
New:     v2.2.0
Reason:  Adding new functionality
```

#### Fixing Data Issue (Patch Version)
```
Current: v2.2.0
New:     v2.2.1
Reason:  Correcting existing data
```

#### Removing Old System (Major Version)
```
Current: v2.9.5
New:     v3.0.0
Reason:  Breaking compatibility
```

## Development Workflow

### Step 1: Create Feature Branch

```bash
git checkout -b feature/consumption-allowed
```

### Step 2: Implement Code Changes

Make your Java/JSF code changes first, then create the migration to support them.

### Step 3: Create Migration Files

1. Determine next version number
2. Create migration directory and files
3. Write SQL scripts
4. Document metadata in JSON

### Step 4: Test Migration

Test the migration on your local development database:

1. **Backup your database first**
2. **Run migration through Admin UI**
3. **Verify changes work correctly**
4. **Test rollback functionality**
5. **Restore backup and test again**

### Step 5: Code Review

Include migration files in your pull request. Reviewers should check:

- SQL syntax and logic
- Rollback script completeness
- Version number correctness
- Impact on existing data

## Best Practices

### ✅ Do This

1. **Always backup before testing migrations**
2. **Use transaction-safe operations when possible**
3. **Test both forward and rollback migrations**
4. **Document breaking changes clearly**
5. **Use NULL-safe operations for existing data**
6. **Add indexes for new queryable columns**

### ❌ Avoid This

1. **Don't skip version numbers**
2. **Don't use the same version in multiple branches**
3. **Don't forget rollback scripts for important changes**
4. **Don't make breaking changes in minor versions**
5. **Don't assume existing data is clean**

### Safe SQL Patterns

#### Adding Columns Safely
```sql
-- ✅ Good: Provides default value
ALTER TABLE item
ADD COLUMN consumption_allowed BOOLEAN NOT NULL DEFAULT TRUE;

-- ❌ Bad: Might fail with existing data
ALTER TABLE item
ADD COLUMN consumption_allowed BOOLEAN NOT NULL;
```

#### Updating Existing Data
```sql
-- ✅ Good: NULL-safe update
UPDATE item
SET consumption_allowed = TRUE
WHERE consumption_allowed IS NULL;

-- ✅ Good: Conditional update
UPDATE item
SET status = 'ACTIVE'
WHERE status IS NULL OR status = '';
```

#### Creating Indexes
```sql
-- ✅ Good: Handle existing index gracefully
CREATE INDEX IF NOT EXISTS idx_item_consumption_allowed
ON item(consumption_allowed);

-- ✅ Good: Drop before recreate in rollback
DROP INDEX IF EXISTS idx_item_consumption_allowed;
```

## Running Migrations

### Through Admin Interface

1. **Login as Administrator**
2. **Navigate to Admin → Database Migration**
3. **Review pending migrations**
4. **Click "Execute All Pending Migrations"**
5. **Monitor progress in real-time**
6. **Verify successful completion**

### Migration Status

The system tracks migration status automatically:

- **Pending**: Not yet executed
- **Executing**: Currently running
- **Success**: Completed successfully
- **Failed**: Execution failed
- **Rolled Back**: Successfully undone

## Troubleshooting

### Common Issues

#### Version Conflict
**Problem**: Another developer used the same version number
**Solution**:
1. Communicate with team about version assignments
2. Rename your migration to next available version
3. Update migration-info.json with new version

#### Migration Fails
**Problem**: SQL error during execution
**Solution**:
1. Check execution logs in admin interface
2. Fix SQL syntax or logic errors
3. Test again on local database
4. Update migration files and retry

#### Can't Rollback
**Problem**: Rollback script missing or fails
**Solution**:
1. Create proper rollback script
2. Test rollback on development database
3. For data loss operations, document risks clearly

### Getting Help

1. **Check migration logs** in the admin interface
2. **Review execution history** for similar migrations
3. **Test on development database** before production
4. **Ask team members** for code review
5. **Consult database administrator** for complex changes

## Example: Complete Migration

Let's walk through creating a migration to add a "priority" field to the Patient entity:

### 1. Create Files

```bash
mkdir src/main/resources/db/migrations/v2.3.0
cd src/main/resources/db/migrations/v2.3.0
```

### 2. migration-info.json

```json
{
    "version": "v2.3.0",
    "description": "Add priority field to Patient entity",
    "author": "Dr M H B Ariyaratne",
    "estimatedDurationMinutes": 3,
    "requiresDowntime": false,
    "affectedTables": ["patient"],
    "affectedModules": ["Patient Management", "OPD"],
    "preRequisites": [
        "Database backup completed"
    ],
    "migrationSteps": [
        {
            "description": "Add priority column to patient table"
        },
        {
            "description": "Set default priority for existing patients"
        },
        {
            "description": "Add index for priority-based queries"
        }
    ]
}
```

### 3. migration.sql

```sql
-- Migration v2.3.0: Add priority field to Patient entity
-- Author: Dr M H B Ariyaratne
-- Date: 2024-11-09

-- Add priority column (Normal priority by default)
ALTER TABLE patient
ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

-- Update existing patients to have normal priority
UPDATE patient
SET priority = 'NORMAL'
WHERE priority IS NULL OR priority = '';

-- Add index for priority-based filtering
CREATE INDEX idx_patient_priority
ON patient(priority);

-- Add constraint to ensure valid priority values
ALTER TABLE patient
ADD CONSTRAINT chk_patient_priority
CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'));
```

### 4. rollback.sql

```sql
-- Rollback v2.3.0: Remove priority field from Patient entity
-- Author: Dr M H B Ariyaratne

-- Drop constraint first
ALTER TABLE patient DROP CONSTRAINT IF EXISTS chk_patient_priority;

-- Drop index
DROP INDEX IF EXISTS idx_patient_priority;

-- Remove column
ALTER TABLE patient DROP COLUMN priority;
```

### 5. Test and Deploy

1. Test migration on development database
2. Verify rollback works correctly
3. Create pull request with migration files
4. Deploy to staging/production environment
5. Execute migration through admin interface

## Integration with Development Process

### Branch Management

- **Feature branches**: Create migrations in feature branches
- **Version coordination**: Coordinate with team to avoid version conflicts
- **Merge conflicts**: Resolve by renaming versions sequentially

### Release Process

- **Version alignment**: Align migration versions with application releases
- **Testing cycles**: Include migration testing in QA process
- **Production deployment**: Execute migrations as part of deployment process

This guide provides everything you need to create safe, reliable database migrations for the HMIS system. Always test thoroughly and coordinate with your team!