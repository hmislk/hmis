# Database Configuration Workflow for HMIS

## Important Note for All Repositories and Computers

### Persistence.xml Configuration Pattern

**Before Pushing to GitHub:**
- Use environment variable placeholders: `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}`
- This keeps credentials secure and allows for different environments

**After Pulling from GitHub (for local development):**
- Revert to actual JNDI names like:
  - `jdbc/coop` 
  - `jdbc/sl`
  - Or other environment-specific datasource names

### File Location
`src/main/resources/META-INF/persistence.xml`

### Example Changes

**For GitHub (before push):**
```xml
<jta-data-source>${JDBC_DATASOURCE}</jta-data-source>
<jta-data-source>${JDBC_AUDIT_DATASOURCE}</jta-data-source>
```

**For Local Development (after pull):**
```xml
<jta-data-source>jdbc/coop</jta-data-source>
<jta-data-source>jdbc/sl</jta-data-source>
```

### Workflow Steps
1. **After git pull:** Change persistence.xml to use local datasource names (jdbc/coop, jdbc/sl, etc.)
2. **Before git push:** Change persistence.xml back to environment variables (${JDBC_DATASOURCE}, ${JDBC_AUDIT_DATASOURCE})

### Warning
⚠️ **NEVER commit actual database connection details or JNDI names to the repository**
⚠️ **Always use environment variables in the committed version**

---
*Keep this file in all HMIS repository copies for reference*