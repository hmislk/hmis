# Persistence.xml Database Configuration Workflow

## AUTOMATION SCRIPTS (RECOMMENDED)

### One-Command Solution

**Windows (Primary Development Environment):**
```cmd
REM Instead of: git push
REM Use this:
scripts\safe-push.bat

REM Or with parameters:
scripts\safe-push.bat origin main
```

**Linux (Server Environment):**
```bash
# Instead of: git push
# Use this:
./scripts/safe-push.sh

# Or with parameters:
./scripts/safe-push.sh origin main
```

This script automatically:
1. Backs up your current local JNDI names
2. Replaces them with environment variables
3. Pushes to GitHub
4. Restores your local JNDI names

### Manual Step-by-Step Control

**Windows:**
```cmd
REM 1. Prepare for push (backs up and replaces JNDI names)
scripts\prepare-for-push.bat

REM 2. Push your changes
git push

REM 3. Restore local configuration
scripts\restore-local-jndi.bat
```

**Linux:**
```bash
# 1. Prepare for push (backs up and replaces JNDI names)
./scripts/prepare-for-push.sh

# 2. Push your changes
git push

# 3. Restore local configuration
./scripts/restore-local-jndi.sh
```

## LEGACY: Manual Git Push Behavior

When asked to push changes to GitHub, Claude should AUTOMATICALLY:

1. **Before pushing:**
   - Change current JNDI names → `${JDBC_DATASOURCE}` in persistence.xml
   - Change current audit JNDI names → `${JDBC_AUDIT_DATASOURCE}` in persistence.xml

2. **Push changes** to GitHub

3. **After pushing:**
   - Revert `${JDBC_DATASOURCE}` → back to the original JNDI name that was there before
   - Revert `${JDBC_AUDIT_DATASOURCE}` → back to the original audit JNDI name that was there before

## File Location
`src/main/resources/META-INF/persistence.xml`

## Variable JNDI Names
The JNDI names change based on environment and will be manually updated:
- Examples: `jdbc/asiri`, `jdbc/ruhunu`, `jdbc/coop`, etc.
- Always preserve whatever the current local configuration is

## Security Note
**NEVER commit actual JNDI names to the repository - always use environment variables in pushed commits**

## Key Principle
Use the **last choice in history** - whatever JNDI names are currently in the file should be restored after pushing.

## Script Details

### Available Scripts

**Windows (Primary Development):**
- **`scripts\safe-push.bat`** - Complete automation (recommended)
- **`scripts\prepare-for-push.bat`** - Prepare persistence.xml for GitHub push
- **`scripts\restore-local-jndi.bat`** - Restore local JNDI configuration

**Linux (Server Environment):**
- **`scripts/safe-push.sh`** - Complete automation (recommended)
- **`scripts/prepare-for-push.sh`** - Prepare persistence.xml for GitHub push
- **`scripts/restore-local-jndi.sh`** - Restore local JNDI configuration

### How It Works
1. Scripts detect current JNDI names in persistence.xml
2. Create backup files (.jndi-backup-main, .jndi-backup-audit)
3. Replace with environment variables for GitHub compatibility
4. After push, restore original local names from backup
5. Clean up backup files

### Benefits
- ✅ Eliminates QA deployment blockers
- ✅ Maintains local development functionality  
- ✅ Prevents manual errors
- ✅ One-command simplicity
