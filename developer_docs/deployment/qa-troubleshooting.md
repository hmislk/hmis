# QA Deployment Troubleshooting Guide

## 🚨 CRITICAL: Application 404 Errors After Deployment

### Symptoms
- GitHub Actions deployment completes successfully 
- Build phase shows ✅ success
- Deploy phase shows ✅ success
- BUT: Applications return 404 errors when accessed
- `curl https://qa.carecode.org/qa*/faces/index1.xhtml` returns 404

### Root Cause Analysis

**90% of QA deployment failures are caused by:**
Hardcoded JNDI datasources in `persistence.xml` instead of environment variables.

### Immediate Diagnostic Steps

1. **Check persistence.xml configuration:**
   ```bash
   grep '<jta-data-source>' src/main/resources/META-INF/persistence.xml
   ```

2. **What you should see (CORRECT):**
   ```xml
   <jta-data-source>${JDBC_DATASOURCE}</jta-data-source>
   <jta-data-source>${JDBC_AUDIT_DATASOURCE}</jta-data-source>
   ```

3. **Red flags (INCORRECT - will cause failures):**
   ```xml
   <jta-data-source>jdbc/coop</jta-data-source>
   <jta-data-source>jdbc/ruhunuAudit</jta-data-source>
   <jta-data-source>jdbc/qa</jta-data-source>
   ```

### Step-by-Step Fix

#### Step 1: Fix persistence.xml
```bash
# Edit the file to replace hardcoded values with environment variables
# Replace ANY hardcoded JNDI name with ${JDBC_DATASOURCE}
# Replace ANY hardcoded audit JNDI name with ${JDBC_AUDIT_DATASOURCE}
```

#### Step 2: Create and merge fix PR
```bash
# Create branch for fix
git checkout -b fix-qa-jndi-datasources

# Commit the changes
git add src/main/resources/META-INF/persistence.xml
git commit -m "Fix QA JNDI datasource configuration

- Replace hardcoded JNDI datasources with environment variables
- Ensures proper replacement during GitHub Actions deployment

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>"

# Push and create PR
git push origin fix-qa-jndi-datasources
gh pr create --base development --head fix-qa-jndi-datasources --title "Fix QA JNDI datasource configuration" --body "Fix hardcoded JNDI datasources"

# Merge after checks pass
gh pr merge [PR_NUMBER] --merge --admin
```

#### Step 3: Redeploy all QA environments
```bash
# Switch back to development and pull latest
git checkout development
git pull origin development

# Create deployment PRs for all QA environments
gh pr create --base hims-qa1 --head development --title "Deploy JNDI fix to QA1" --body "Deploy with fixed JNDI configuration"
gh pr create --base hims-qa2 --head development --title "Deploy JNDI fix to QA2" --body "Deploy with fixed JNDI configuration"  
gh pr create --base hims-qa3 --head development --title "Deploy JNDI fix to QA3" --body "Deploy with fixed JNDI configuration"

# Merge all PRs (after checks pass)
gh pr merge [QA1_PR] --merge --admin
gh pr merge [QA2_PR] --merge --admin
gh pr merge [QA3_PR] --merge --admin
```

### Verification Steps

#### 1. Verify build logs show proper JNDI replacement
```bash
# Get the run ID from deployment
gh run list --branch hims-qa1 --limit 1

# Check that build logs show replacement working
gh run view [RUN_ID] --log | grep -A 5 "Update JDBC Data Sources"
```

**Expected output:**
```
Update JDBC Data Sources in persistence.xml
sed -i 's|<jta-data-source>${JDBC_DATASOURCE}</jta-data-source>|<jta-data-source>jdbc/qa</jta-data-source>|'
sed -i 's|<jta-data-source>${JDBC_AUDIT_DATASOURCE}</jta-data-source>|<jta-data-source>jdbc/qaAudit</jta-data-source>|'

Verify JDBC Data Sources in persistence.xml  
        <jta-data-source>jdbc/qa</jta-data-source>
        <jta-data-source>jdbc/qaAudit</jta-data-source>
```

#### 2. Test application availability (after ~5 minutes for startup)
```bash
# Test all QA environments
curl -s -o /dev/null -w "%{http_code}" https://qa.carecode.org/qa1/faces/index1.xhtml
curl -s -o /dev/null -w "%{http_code}" https://qa.carecode.org/qa2/faces/index1.xhtml  
curl -s -o /dev/null -w "%{http_code}" https://qa.carecode.org/qa3/faces/index1.xhtml

# Expected output: 200 for all environments
```

## Prevention Guidelines

### For Developers
1. **NEVER commit hardcoded JNDI datasources** to persistence.xml
2. **Always use environment variables**: `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}`
3. **Before any QA deployment**, run: `grep '<jta-data-source>' src/main/resources/META-INF/persistence.xml`

### For Automated Systems
- Consider adding a pre-commit hook to check for hardcoded JNDI values
- Add validation step in GitHub Actions to fail builds with hardcoded values

## Environment-Specific JNDI Mappings

| Environment | JDBC_DATASOURCE | JDBC_AUDIT_DATASOURCE |
|-------------|-----------------|----------------------|
| QA1 | jdbc/qa | jdbc/qaAudit |
| QA2 | jdbc/coop | jdbc/coopAudit |
| QA3 | jdbc/qa3 | jdbc/qa3audit |

These mappings are handled automatically by the GitHub Actions workflow when environment variables are used correctly.

## Server-Side Infrastructure Failures

### Payara Deployment Failures

#### Issue: EclipseLink DDL Generation Path Error
**Error:**
```
Exception [EclipseLink-7018] File error.
java.io.FileNotFoundException: c:/tmp/createDDL.jdbc (No such file or directory)
```

**Root Cause:** Hardcoded DDL generation paths in persistence.xml

**Most Common Cause:** Developer accidentally used persistence.xml file configured for DDL generation (with hardcoded paths like `c:/tmp/`) instead of the deployment-ready version.

**Solution:**
1. **Check persistence.xml for DDL generation properties:**
   ```bash
   grep -i "eclipselink.application-location" src/main/resources/META-INF/persistence.xml
   ```

2. **Remove or fix DDL generation properties:**
   ```xml
   <!-- REMOVE or update these lines: -->
   <property name="eclipselink.application-location" value="c:/tmp/"/>
   <property name="eclipselink.ddl-generation" value="create-or-extend-tables"/>
   <property name="eclipselink.ddl-generation.output-mode" value="sql-script"/>
   ```

3. **Ensure logging is appropriate for deployment:**
   ```xml
   <!-- Use SEVERE instead of FINEST for production -->
   <property name="eclipselink.logging.level" value="SEVERE"/>
   ```

4. **Alternative for DDL generation**: Use separate persistence configuration file for DDL generation, not the deployment persistence.xml

#### Issue: Payara Admin Authentication Failure  
**Error:**
```
Invalid file for option: --passwordfile: java.io.FileNotFoundException: /tmp/payara-admin-pass.txt
```

**Root Cause:** Missing Payara admin password file on server

**Solution:**
1. **Server administrator** needs to restore `/tmp/payara-admin-pass.txt` file
2. **Check GitHub repository secrets** for correct password file content
3. **Verify deployment workflow** is properly creating the password file

#### Issue: Application State Mismatch
**Error:**
```
remote failure: Application qa2 is not deployed on this target [server]
Command undeploy failed.
```

**Root Cause:** Application registry is out of sync with actual deployment state

**Solution:**
1. **Manual server cleanup** required by administrator
2. **Clean deployment approach:**
   ```bash
   # Skip undeploy and force new deployment
   asadmin deploy --force=true --name=qa2 path/to/application.war
   ```

### When to Escalate to Server Administrator

**Escalate immediately if you see:**
- File permission errors (`/tmp/` directory issues)
- Payara admin authentication failures  
- EclipseLink DDL generation path errors
- Application registry mismatches

**These are infrastructure issues beyond deployment workflow scope.**

---
*This guide was created based on real deployment failures encountered on 2025-08-03*