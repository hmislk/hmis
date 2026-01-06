# Persistence.xml Verification Guide

## Overview

The `persistence.xml` file configures database connections for the HMIS application. This file requires different settings for local development versus QA/production deployment. **Hardcoded local settings must NEVER be pushed to GitHub** as they will break QA deployments.

## Critical Rule

**🚨 BEFORE ANY PUSH TO GITHUB**: Manually verify `persistence.xml` uses environment variables, NOT hardcoded JNDI datasource names.

## File Location

```
src/main/resources/META-INF/persistence.xml
```

## Pre-Push Verification Checklist

Run these checks **before every `git push`**:

### Step 1: Check if persistence.xml Was Modified

```bash
git status
```

Look for `persistence.xml` in the output. If you see:
```
modified:   src/main/resources/META-INF/persistence.xml
```

Then proceed to Step 2. If not modified, you can skip this checklist.

### Step 2: Verify Datasource Configuration

Open the file and check the `<jta-data-source>` elements:

```bash
grep -A 1 "jta-data-source" src/main/resources/META-INF/persistence.xml
```

### Step 3: Confirm Correct Configuration

#### ✅ CORRECT (Uses environment variables):
```xml
<persistence-unit name="hmisPU" transaction-type="JTA">
    <jta-data-source>${JDBC_DATASOURCE}</jta-data-source>
    <!-- ... -->
</persistence-unit>

<persistence-unit name="hmisPU_AUDIT" transaction-type="JTA">
    <jta-data-source>${JDBC_AUDIT_DATASOURCE}</jta-data-source>
    <!-- ... -->
</persistence-unit>
```

#### ❌ WRONG (Hardcoded local JNDI names):
```xml
<persistence-unit name="hmisPU" transaction-type="JTA">
    <jta-data-source>jdbc/coop</jta-data-source>
    <!-- ... -->
</persistence-unit>

<persistence-unit name="hmisPU_AUDIT" transaction-type="JTA">
    <jta-data-source>jdbc/ruhunuAudit</jta-data-source>
    <!-- ... -->
</persistence-unit>
```

### Step 4: Verify DDL Generation Configuration

Check for hardcoded file paths in DDL generation settings:

```bash
grep -i "application-location" src/main/resources/META-INF/persistence.xml
```

#### ✅ CORRECT (No hardcoded paths OR commented out):
```xml
<!-- DDL generation disabled for production -->
```

OR with environment variable:
```xml
<property name="eclipselink.application-location" value="${DDL_OUTPUT_PATH}"/>
```

#### ❌ WRONG (Hardcoded local path):
```xml
<property name="eclipselink.application-location" value="c:/tmp/"/>
```

OR:
```xml
<property name="eclipselink.application-location" value="/home/buddhika/tmp/"/>
```

### Step 5: Fix if Necessary

If you found hardcoded values in Steps 3 or 4:

1. **DO NOT COMMIT** the current changes
2. Open `persistence.xml` in your editor
3. Replace hardcoded values with environment variables:
   - `jdbc/coop` → `${JDBC_DATASOURCE}`
   - `jdbc/ruhunuAudit` → `${JDBC_AUDIT_DATASOURCE}`
4. Remove or comment out hardcoded DDL paths
5. Save the file
6. Verify again with Step 2 and Step 4
7. Now you can safely commit and push

## Why This Matters

### Local Development vs. QA Deployment

**Local Development:**
- Uses local database instances
- JNDI names like `jdbc/coop`, `jdbc/ruhunuAudit`
- Configured in local Payara server
- DDL generation enabled for schema updates

**QA/Production Deployment:**
- Uses shared database servers
- JNDI names configured via environment variables
- Managed by deployment automation
- DDL generation disabled (schema managed separately)

### Consequences of Pushing Hardcoded Values

If hardcoded local values are pushed to GitHub:

1. **QA Deployment Fails**: Automated deployment cannot find `jdbc/coop` datasource
2. **Application Won't Start**: Database connection fails on startup
3. **Manual Intervention Required**: DevOps must manually fix the deployment
4. **Delayed Testing**: QA team cannot test the feature
5. **DDL Files Generated in Wrong Location**: May cause deployment failures

## Common Scenarios

### Scenario 1: Switching Between Databases Locally

When testing with different local databases, you might change datasource names in `persistence.xml`:

```xml
<!-- Testing with alternate local database -->
<jta-data-source>jdbc/testdb</jta-data-source>
```

**REMEMBER**: Change it back to `${JDBC_DATASOURCE}` before committing!

### Scenario 2: Enabling DDL Generation for Schema Updates

During development, you might enable DDL generation:

```xml
<property name="eclipselink.ddl-generation" value="create-or-extend-tables"/>
<property name="eclipselink.ddl-generation.output-mode" value="both"/>
<property name="eclipselink.application-location" value="/home/buddhika/tmp/"/>
```

**REMEMBER**: Remove or comment out the `application-location` before committing!

### Scenario 3: Debugging Connection Issues

You might temporarily change connection settings to debug issues:

```xml
<property name="eclipselink.logging.level.sql" value="FINE"/>
<property name="eclipselink.logging.parameters" value="true"/>
```

**VERIFY**: These are generally safe to commit, but review with the team first.

## Quick Command Reference

### Check if persistence.xml is modified:
```bash
git status | grep persistence.xml
```

### View current datasource configuration:
```bash
grep "jta-data-source" src/main/resources/META-INF/persistence.xml
```

### View changes made to persistence.xml:
```bash
git diff src/main/resources/META-INF/persistence.xml
```

### Discard local changes (if you decide not to commit them):
```bash
git checkout src/main/resources/META-INF/persistence.xml
```

### Unstage persistence.xml (if accidentally staged):
```bash
git reset src/main/resources/META-INF/persistence.xml
```

## Automation (Future Enhancement)

Consider implementing:

- **Pre-commit hook**: Automatically verify datasource configuration
- **CI/CD validation**: Reject pushes with hardcoded datasources
- **Template-based approach**: Separate local and deployment configurations

See [Git Hooks](../git/git-hooks.md) for implementation guidance.

## Related Documentation

- [QA Deployment Guide](qa-deployment-guide.md) - Understanding the deployment process
- [Environment Variables](environment-variables.md) - Configuring deployment variables
- [Database Configuration](../database/mysql-developer-guide.md) - Local database setup

## Troubleshooting

### I Already Pushed Hardcoded Values - What Now?

1. **Immediate action**: Create a new commit fixing the issue
   ```bash
   # Fix persistence.xml to use ${JDBC_DATASOURCE}
   git add src/main/resources/META-INF/persistence.xml
   git commit -m "Fix: Use environment variables in persistence.xml

   Replace hardcoded JNDI datasource names with environment variables
   for proper QA deployment.

   Refs #[issue-number]"
   git push
   ```

2. **Notify team**: Inform QA team that deployment may have failed

3. **Learn**: Add this to your pre-push mental checklist

### How Do I Set Up Environment Variables Locally?

You don't need to! For local development:

1. Keep `${JDBC_DATASOURCE}` in the committed version
2. Configure your local Payara server's JNDI resources to match
3. Or use a local `.gitignore`d copy for development

See [Local Development Setup](../setup/local-environment.md) for details.

---

**Remember**: This verification is CRITICAL for successful QA deployments. Make it a habit to check persistence.xml before every push!
