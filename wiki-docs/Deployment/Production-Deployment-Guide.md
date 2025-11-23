# HMIS Production Deployment Guide

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Production Environments](#production-environments)
- [Pre-Deployment Checklist](#pre-deployment-checklist)
- [Deployment Process](#deployment-process)
- [Validation and Health Checks](#validation-and-health-checks)
- [Rollback Procedures](#rollback-procedures)
- [Server Management](#server-management)
- [Troubleshooting](#troubleshooting)
- [Security Considerations](#security-considerations)

## Overview

The HMIS (Hospital Management Information System) uses an automated CI/CD pipeline built on GitHub Actions to deploy to multiple production healthcare environments. Each production environment serves a specific healthcare institution with its own isolated database and application server.

### Deployment Philosophy
- **Automated**: Deployments are triggered automatically when code is pushed to designated production branches
- **Validated**: Multiple validation steps ensure code quality and configuration correctness
- **Traceable**: Every deployment is logged and can be monitored in real-time
- **Reversible**: Automatic backup creation enables quick rollback if needed
- **Environment-Specific**: Each environment has its own JDBC datasources and configuration

## Architecture

### Technology Stack
- **Application Server**: Payara 5 (Java EE 8 certified)
- **Java Version**: JDK 11 (Temurin distribution)
- **Build Tool**: Apache Maven 3.x
- **Database**: MySQL with JDBC connection pools
- **Deployment Method**: WAR file deployment via Payara asadmin CLI
- **CI/CD Platform**: GitHub Actions
- **File Transfer**: rsync over SSH

### Deployment Flow

```
Developer Push → GitHub Actions → Build (Maven) → Deploy (SSH/rsync) → Payara Server → Health Check
       ↓              ↓                ↓                 ↓                    ↓              ↓
   Branch      Validation      WAR Package        File Transfer      App Deployment    Verify
  (prod)       (Tests)         (artifact)         (with backup)      (asadmin)        (HTTP 200)
```

## Prerequisites

### Required Tools and Access
1. **Git**: Version control system installed and configured
2. **GitHub Access**: Write permissions to hmislk/hmis repository
3. **GitHub CLI** (optional but recommended): For monitoring deployments
4. **SSH Access** (DevOps only): Direct server access for troubleshooting

### Required Knowledge
- Understanding of Git branching and merging
- Basic knowledge of Java EE applications
- Familiarity with CI/CD concepts
- Understanding of healthcare IT security requirements (HIPAA compliance)

## Production Environments

### Active Production Environments

The HMIS system is deployed to multiple production environments, each serving a specific healthcare institution:

| Environment | Branch Name | Institution | JDBC Datasource | Application URL |
|-------------|-------------|-------------|-----------------|-----------------|
| **RH-PROD** | `ruhunu-prod` | Ruhunu Hospital | `jdbc/ruhunu`, `jdbc/ruhunuAudit` | https://rh.carecode.org/rh |
| **COOP-PROD** | `coop-prod` | Cooperative Hospital | `jdbc/coop`, `jdbc/coopaudit` | https://coop.carecode.org/coop |
| **MP-PROD** | `mp-prod` | Medical Practice | `jdbc/mp`, `jdbc/mpAudit` | https://mp.carecode.org/mp |
| **RMH-PROD** | `rmh-prod` | RMH Hospital | `jdbc/rmh`, `jdbc/rmhAudit` | https://rmh.carecode.org/rmh |
| **SLH-PROD** | `southernlanka-prod` | Southern Lanka Hospital | `jdbc/slh`, `jdbc/slhAudit` | https://slh.carecode.org/slh |
| **KML-PROD** | `kml-prod` | KML Healthcare | `jdbc/kml`, `jdbc/kmlAudit` | https://kml.carecode.org/kml |
| **HORIZON-PROD** | `horizon-prod` | Horizon Healthcare | `jdbc/horizon`, `jdbc/horizonAudit` | https://horizon.carecode.org/horizon |
| **DIGASIRI-PROD** | `digasiri-prod` | Digasiri Medical Center | `jdbc/digasiri`, `jdbc/digasiriAudit` | https://digasiri.carecode.org/digasiri |
| **ASIRI-PROD** | `asiripharmacy-prod` | Asiri Pharmacy | `jdbc/asiripharmacy`, `jdbc/asiripharmacyAudit` | https://asiri.carecode.org/asiripharmacy |
| **EWC-PROD** | `engage-wellness-center-prod` | Engage Wellness Center | `jdbc/ewc`, `jdbc/ewcAudit` | https://ewc.carecode.org/ewc |

### Staging Environments

| Environment | Branch Name | Purpose | JDBC Datasource |
|-------------|-------------|---------|-----------------|
| **COOP-STG** | `coop-stg` | Staging for COOP | `jdbc/coopStg`, `jdbc/coopStgAudit` |
| **COOP-STG-PROD** | `coop-prod-stg` | Production staging | `jdbc/coopProd`, `jdbc/coopProdAudit` |
| **RH-STG** | `rh-stg` | Staging for RH | `jdbc/rhStg`, `jdbc/rhStgAudit` |
| **MP-STG** | `mp-stg` | Staging for MP | `jdbc/mpStg`, `jdbc/mpStgAudit` |
| **SLH-STG** | `southernlanka-stg` | Staging for SLH | `jdbc/slhStg`, `jdbc/slhStgAudit` |

### Development and QA Environments

| Environment | Branch Name | Purpose | Application URL |
|-------------|-------------|---------|-----------------|
| **QA1** | `hims-qa1` | Primary QA testing | https://qa.carecode.org/qa1 |
| **QA2** | `hims-qa2` | Secondary QA testing | https://qa.carecode.org/qa2 |
| **QA3** | `hims-qa3` | Tertiary QA testing | https://qa.carecode.org/qa3 |
| **RH-DEV** | `rh-dev` | Development environment | https://dev.carecode.org/rhdev |
| **COOP-DEV** | `coop-dev` | COOP development | https://dev.carecode.org/coopdev |

## Pre-Deployment Checklist

### 🚨 CRITICAL: Pre-Deployment Validation

**BEFORE deploying to production, ALL of the following must be verified:**

#### 1. Code Quality Validation

```bash
# Verify code compiles without errors
mvn clean compile

# Check for security vulnerabilities
# Review dependency-check reports if configured
```

#### 2. Persistence.xml Verification (MANDATORY)

This is the **most critical** pre-deployment check. Failure to verify this will cause deployment failures.

**Check JDBC datasource configuration:**
```bash
grep '<jta-data-source>' src/main/resources/META-INF/persistence.xml
```

**Required output (CORRECT):**
```xml
<jta-data-source>${JDBC_DATASOURCE}</jta-data-source>
<jta-data-source>${JDBC_AUDIT_DATASOURCE}</jta-data-source>
```

**INCORRECT configurations that MUST be fixed:**
```xml
<!-- ❌ DO NOT DEPLOY with hardcoded values like these: -->
<jta-data-source>jdbc/coop</jta-data-source>
<jta-data-source>jdbc/ruhunuAudit</jta-data-source>
```

**Why this matters:**
- The CI/CD pipeline replaces `${JDBC_DATASOURCE}` with environment-specific values
- Hardcoded values will cause the application to fail to connect to the database
- This will result in HTTP 404 errors even though deployment "succeeds"

See [Persistence Verification Guide](../developer_docs/deployment/persistence-verification.md) for detailed instructions.

#### 3. Security Validation

**Check grantAllPrivilegesToAllUsersForTesting flag:**
```bash
grep "grantAllPrivilegesToAllUsersForTesting" src/main/java/com/divudi/bean/common/WebUserController.java
```

**Required**: Must be set to `false` or not present for production deployments.

The PR validation workflow automatically checks this, but verify manually before deployment.

#### 4. Branch Protection Verification

**Production branches require:**
- Code must come from `staging` branch OR
- Code must be in a hotfix branch (ending with `-hotfix`)

**Verify branch source:**
```bash
# Check current branch
git branch --show-current

# Verify merge source for production
git log --oneline -5
```

#### 5. Testing Verification

**Before production deployment, ensure:**
- ✅ All unit tests pass (if running tests)
- ✅ QA environment testing completed successfully
- ✅ User acceptance testing (UAT) completed
- ✅ Performance testing shows acceptable results
- ✅ Security testing completed (especially for SQL injection, XSS)

#### 6. Documentation Verification

**Ensure the following are documented:**
- Changes included in this deployment
- Database schema changes (if any)
- Configuration changes required
- Known issues or limitations
- Rollback plan

#### 7. Change Communication

**Before deploying, notify:**
- ✅ Hospital IT administrators
- ✅ End users (if significant changes)
- ✅ Support team
- ✅ DevOps team

**Include in notification:**
- Deployment date and time
- Expected downtime (if any)
- New features or bug fixes
- Any required actions from users

## Deployment Process

### Standard Production Deployment

Production deployments are **fully automated** via GitHub Actions. The process is triggered by pushing code to the appropriate production branch.

#### Step 1: Prepare Code for Deployment

**1a. Ensure development code is ready:**
```bash
# Switch to development branch
git checkout development

# Pull latest changes
git pull origin development

# Verify code compiles and tests pass
mvn clean compile
```

**1b. Merge to staging (if required):**
```bash
# Staging is typically updated from QA environments
# This step is environment-specific
```

#### Step 2: Create Deployment Branch/Merge

**For hotfixes:**
```bash
# Create hotfix branch
git checkout -b feature-name-hotfix

# Make necessary changes
# ... edit files ...

# Commit changes
git add .
git commit -m "Hotfix: Description of urgent fix

Closes #issue-number"

# Push hotfix branch
git push origin feature-name-hotfix
```

**For regular deployments:**
```bash
# Merge staging to production branch
# This should be done via Pull Request for audit trail
```

#### Step 3: Deploy to Production

**Direct push method (for allowed branches):**
```bash
# Push to production branch
git push origin staging:ruhunu-prod --force
```

**Pull Request method (recommended for audit trail):**
```bash
# Create pull request via GitHub CLI
gh pr create --base ruhunu-prod --head staging \
  --title "Deploy staging to RH Production" \
  --body "Production deployment of tested staging code.

Changes included:
- Feature 1
- Bug fix 2
- Performance improvement 3

Testing completed:
- QA1, QA2, QA3 testing passed
- UAT completed and approved
- Security review completed

Closes #issue-number"

# Merge the pull request (after approval)
gh pr merge [PR_NUMBER] --merge
```

#### Step 4: Monitor Deployment

**List recent deployment runs:**
```bash
# View recent workflows for production branch
gh run list --branch ruhunu-prod --limit 5
```

**Watch deployment in real-time:**
```bash
# Get the run ID from the list command above
gh run watch [RUN_ID]
```

**Example output:**
```
✓ build Checkout Code                                    3s
✓ build Set up JDK 11                                    8s
✓ build Cache Maven Packages                             2s
✓ build Update JDBC Data Sources in persistence.xml     1s
✓ build Verify JDBC Data Sources in persistence.xml     1s
✓ build Build with Maven                               145s
✓ build Archive Build Artifacts                         5s
✓ deploy Checkout Code                                   2s
✓ deploy Download Build Artifact                         3s
✓ deploy Deploy to Payara                              45s
```

### Deployment Pipeline Details

Each production deployment goes through the following automated phases:

#### Phase 1: Build (runs on GitHub Actions runner)

**1. Code Checkout**
- Checks out code from the production branch
- Uses `actions/checkout@v4`

**2. Java Environment Setup**
- Installs JDK 11 (Temurin distribution)
- Configures Maven with Java 11

**3. Maven Cache**
- Caches `~/.m2` directory for faster builds
- Cache key based on `pom.xml` hash

**4. JDBC Configuration**
- **CRITICAL STEP**: Replaces environment variables in `persistence.xml`
- Example for RH-PROD:
  ```bash
  sed -i 's|${JDBC_DATASOURCE}|jdbc/ruhunu|' persistence.xml
  sed -i 's|${JDBC_AUDIT_DATASOURCE}|jdbc/ruhunuAudit|' persistence.xml
  ```
- Verifies the replacement was successful

**5. Maven Build**
- Runs `mvn clean package -DskipTests`
- Compiles source code
- Packages into WAR file
- Skips unit tests for faster deployment (tests should run in CI before this)

**6. Artifact Archiving**
- Uploads WAR file as GitHub Actions artifact
- Artifact name: `build-artifacts`
- Location: `target/*.war`

#### Phase 2: Deploy (runs on GitHub Actions runner, connects to production server)

**1. Artifact Download**
- Downloads the WAR file from the build phase
- Stores in current directory

**2. SSH Key Setup**
- Retrieves SSH private key from GitHub Secrets
- Creates temporary `private_key.pem` file
- Sets correct permissions (600)

**3. Server Preparation**
- Connects to production server via SSH
- Creates deployment directory if it doesn't exist: `/home/appuser/app/latest`
- Sets correct ownership: `appuser:appuser`

**4. Backup Creation**
- Removes old backup (`*.war.old`) if it exists
- Creates backup of current WAR file
  ```bash
  mv current.war current.war.old
  ```

**5. File Transfer**
- Uses `rsync` to copy new WAR file to server
- Shows progress during transfer
- Command: `rsync -aL --progress -e "ssh -i private_key.pem" ./*.war SERVER:PATH/app.war`

**6. File Permissions**
- Sets ownership of new WAR file to `appuser:appuser`

**7. Payara Deployment**
- Creates temporary admin password file
- Undeploys existing application (if running)
  ```bash
  asadmin --user admin --passwordfile /tmp/payara-admin-pass.txt undeploy app-name
  ```
- Deploys new WAR file
  ```bash
  asadmin --user admin --passwordfile /tmp/payara-admin-pass.txt \
    deploy --force=true --contextroot app-name /path/to/app.war
  ```
- Removes temporary password file

**8. Deployment Verification**
- Checks if application is listed in Payara
  ```bash
  asadmin list-applications | grep 'app-name'
  ```
- Reports success or failure

**9. Health Check**
- Performs HTTP health check (5 attempts with 10-second intervals)
- Checks application endpoint: `https://subdomain.carecode.org/app/faces/index1.xhtml`
- Expected response: HTTP 200
- Reports reachability status

**10. Cleanup**
- Removes temporary SSH private key
- Cleans up temporary files

### Environment-Specific Deployment Examples

#### Example 1: Deploying to RH-PROD (Ruhunu Hospital)

```bash
# 1. Verify pre-deployment checklist completed
grep '<jta-data-source>' src/main/resources/META-INF/persistence.xml

# 2. Push to production branch
git push origin staging:ruhunu-prod

# 3. Monitor deployment
gh run list --branch ruhunu-prod --limit 1
gh run watch [RUN_ID]

# 4. Verify deployment
curl -s -o /dev/null -w "%{http_code}" https://rh.carecode.org/rh/faces/index1.xhtml
# Expected: 200
```

#### Example 2: Deploying Hotfix to COOP-PROD

```bash
# 1. Create hotfix branch
git checkout -b critical-billing-fix-hotfix

# 2. Make the fix
# ... edit files ...

# 3. Commit
git add .
git commit -m "Hotfix: Fix critical billing calculation error

Closes #12345"

# 4. Push hotfix
git push origin critical-billing-fix-hotfix

# 5. Create PR to production
gh pr create --base coop-prod --head critical-billing-fix-hotfix \
  --title "HOTFIX: Critical billing calculation fix" \
  --body "Emergency fix for billing calculation error affecting invoices.

This is a critical hotfix that needs immediate deployment.

Closes #12345"

# 6. Merge (after review)
gh pr merge [PR_NUMBER] --merge

# 7. Monitor deployment
gh run watch $(gh run list --branch coop-prod --limit 1 --json databaseId --jq '.[0].databaseId')
```

## Validation and Health Checks

### Automated Health Checks

The deployment pipeline automatically performs the following health checks:

#### 1. Payara Application List Check
```bash
asadmin list-applications | grep 'app-name'
```
- Verifies application is deployed in Payara
- Confirms application name matches expected value

#### 2. HTTP Health Check
```bash
curl -s -o /dev/null -w "%{http_code}" https://subdomain.carecode.org/app/faces/index1.xhtml
```
- Checks if application is reachable
- Verifies HTTP 200 response
- Retries up to 5 times with 10-second intervals

### Manual Validation Steps

After deployment completes, perform the following manual validations:

#### 1. Application Access
```bash
# Test main application URL
curl -I https://[subdomain].carecode.org/[app]/faces/index1.xhtml

# Expected response
HTTP/2 200
```

#### 2. Database Connectivity
- Log in to the application
- Navigate to a module that reads from database (e.g., Patient Search)
- Verify data loads correctly

#### 3. Audit Trail
- Perform an action that should be audited (e.g., create a patient record)
- Verify audit record is created in audit database

#### 4. Critical Workflows
Test critical healthcare workflows:
- **Patient Registration**: Create a new patient record
- **Pharmacy**: Create a prescription and dispense medication
- **Billing**: Generate an invoice
- **Laboratory**: Create a lab test request
- **Reports**: Generate a financial or clinical report

#### 5. Performance Check
- Monitor response times for critical operations
- Check server resource utilization
  ```bash
  # If you have SSH access
  ssh user@server "top -bn1 | head -20"
  ssh user@server "df -h"
  ```

#### 6. Log Review
```bash
# Check Payara server logs for errors (requires SSH access)
ssh user@server "tail -100 /opt/payara5/glassfish/domains/domain1/logs/server.log"
```

Look for:
- ❌ `ERROR` messages
- ❌ `Exception` stack traces
- ❌ Database connection errors
- ✅ Successful deployment messages
- ✅ Application startup confirmation

### Health Check Failure Response

If health checks fail:

**1. Check deployment logs:**
```bash
gh run view [RUN_ID] --log
```

**2. Check application status:**
```bash
# Via GitHub Actions (if configured)
gh run view [RUN_ID] --log | grep -A 10 "Validate if the application is running"
```

**3. Review persistence.xml configuration:**
```bash
# Check if environment variables were replaced correctly
gh run view [RUN_ID] --log | grep "Update JDBC Data Sources"
```

**4. If application is not responding:**
- Wait 2-3 minutes (application may still be starting)
- Check server logs for errors
- Verify database connectivity
- Check Payara server status

**5. If issue persists:**
- Initiate rollback procedure (see [Rollback Procedures](#rollback-procedures))
- Notify DevOps team
- Document the issue for investigation

## Rollback Procedures

### Automatic Backup

The deployment pipeline **automatically creates a backup** of the current WAR file before deploying a new version:

```bash
# Backup location on server
/home/appuser/app/latest/[app-name].war.old
```

### Rollback Methods

#### Method 1: Redeploy Previous Version (Recommended)

**Step 1: Identify last working commit**
```bash
# View recent successful deployments
gh run list --branch ruhunu-prod --status success --limit 10

# View specific run details
gh run view [PREVIOUS_SUCCESSFUL_RUN_ID]
```

**Step 2: Redeploy from previous commit**
```bash
# Get commit hash from successful deployment
git log --oneline ruhunu-prod -10

# Force push previous commit to production branch
git push origin [PREVIOUS_COMMIT_HASH]:ruhunu-prod --force
```

**Step 3: Monitor rollback deployment**
```bash
gh run watch $(gh run list --branch ruhunu-prod --limit 1 --json databaseId --jq '.[0].databaseId')
```

#### Method 2: Manual Server-Side Rollback (DevOps Only)

**⚠️ Requires SSH access to production server**

**Step 1: Connect to server**
```bash
ssh -i private_key.pem appuser@[SERVER_IP]
```

**Step 2: Navigate to deployment directory**
```bash
cd /home/appuser/app/latest
ls -lah
```

**Step 3: Verify backup exists**
```bash
# Check backup file
ls -lh [app-name].war.old

# Check timestamps
stat [app-name].war [app-name].war.old
```

**Step 4: Undeploy current application**
```bash
# Create admin password file
echo "AS_ADMIN_PASSWORD=[PAYARA_ADMIN_PASSWORD]" > /tmp/payara-pass.txt

# Undeploy current version
/opt/payara5/bin/asadmin --user admin --passwordfile /tmp/payara-pass.txt \
  undeploy [app-name]
```

**Step 5: Restore backup**
```bash
# Replace current with backup
mv [app-name].war [app-name].war.failed
mv [app-name].war.old [app-name].war
```

**Step 6: Redeploy backup version**
```bash
/opt/payara5/bin/asadmin --user admin --passwordfile /tmp/payara-pass.txt \
  deploy --force=true --contextroot [app-name] \
  /home/appuser/app/latest/[app-name].war
```

**Step 7: Verify rollback**
```bash
# Check application is deployed
/opt/payara5/bin/asadmin --user admin --passwordfile /tmp/payara-pass.txt \
  list-applications

# Test HTTP endpoint
curl -I https://[subdomain].carecode.org/[app]/faces/index1.xhtml
```

**Step 8: Cleanup**
```bash
rm /tmp/payara-pass.txt
```

### Rollback Decision Matrix

| Scenario | Recommended Method | Urgency |
|----------|-------------------|---------|
| Application won't start | Method 2 (Manual) | CRITICAL |
| HTTP 500 errors | Method 2 (Manual) | HIGH |
| Database connection failure | Check DB first, then Method 1 | HIGH |
| Performance degradation | Monitor first, then Method 1 | MEDIUM |
| Minor UI issues | Create hotfix instead | LOW |
| Feature not working as expected | Create hotfix instead | LOW |

### Post-Rollback Actions

**1. Notify stakeholders:**
- Hospital IT administrators
- End users (if they noticed the issue)
- Development team
- Management (if critical)

**2. Document the incident:**
- What went wrong
- When the issue was detected
- Rollback actions taken
- Current system status

**3. Root cause analysis:**
- Investigate why the deployment failed
- Review what was missed in pre-deployment checks
- Update deployment checklist if needed

**4. Plan forward:**
- Fix the issue in development
- Test thoroughly in QA
- Plan redeployment

## Server Management

### Server Restart Operations

The HMIS system includes automated server management workflows for maintenance and troubleshooting.

#### Restart All Servers

**Purpose:** Restart all production Payara servers (typically for maintenance or resource cleanup)

**Trigger:** Manual via GitHub Actions

**Access:**
```
GitHub Repository → Actions → "Restart All Servers" workflow → Run workflow
```

**Configuration Options:**
- **Exclude Development Server** (4.240.39.63): Yes/No
- **Exclude QA Server** (4.240.43.211): Yes/No
- **Exclude Shared Server 01** (52.172.158.159): Yes/No
- **Exclude Shared Server 02** (20.204.129.229): Yes/No
- **Exclude D01 Server** (4.213.180.217): Yes/No

**Process:**
1. Connects to observability server via SSH
2. Executes restart script: `/home/azureuser/utils/server_utils/restart_all_servers.sh`
3. Restarts Payara on all selected servers
4. Logs restart operations

**When to use:**
- Scheduled maintenance windows
- After major configuration changes
- Memory leak cleanup
- Performance issues affecting multiple servers

**⚠️ Important:**
- Coordinate with hospital IT before restarting
- Avoid during peak hours (typically 8 AM - 5 PM local time)
- Allow 5-10 minutes for all applications to fully start

#### Restart Individual Servers

**Purpose:** Restart specific production servers without affecting others

**Trigger:** Manual via GitHub Actions

**Access:**
```
GitHub Repository → Actions → "Restart Individual Servers" workflow → Run workflow
```

**When to use:**
- Single environment performance issues
- After environment-specific configuration changes
- Testing after hotfix deployment
- Isolated application issues

### Database Export and Import

#### Scheduled Database Export/Import

**Purpose:** Copy production data to QA environments for testing with real data

**Trigger:** Manual scheduling via GitHub Actions

**Access:**
```
GitHub Repository → Actions → "Database Export and Import Scheduler" → Run workflow
```

**Configuration:**
- **From Environment**: Select production environment to export
  - COOP_Dev, COOP_Prod, Ruhunu_Prod, MP_Prod, RMH_Prod, SLH_Prod
- **To Environment**: Select QA environment to import
  - QA1, QA2
- **Scheduling Date**: YYYY-MM-DD format

**Process:**
1. Schedules export/import for 2:00 AM Sri Lanka time (UTC+5:30)
2. Connects to observability server
3. Uses `at` command to schedule execution
4. Executes export/import script at scheduled time
5. Sends email notification on success/failure

**Notification Recipients:**
- Development team leads
- QA team
- Database administrators

**⚠️ Important:**
- Ensures QA environments have realistic test data
- Scheduled during off-peak hours
- Backs up target database before import
- Sanitizes sensitive patient data (if configured)

**Use Cases:**
- QA testing with production-like data
- Reproducing production issues in QA
- Performance testing with realistic data volumes
- Training environments

### Server Infrastructure

#### Production Server Details

**Shared Server 01** (52.172.158.159)
- Hosts: Multiple production applications
- Purpose: Shared hosting for smaller institutions
- Managed by: DevOps team

**Shared Server 02** (20.204.129.229)
- Hosts: Multiple production applications
- Purpose: Shared hosting for smaller institutions
- Managed by: DevOps team

**D01 Server** (4.213.180.217)
- Purpose: Dedicated hosting
- Managed by: DevOps team

**QA Server** (4.240.43.211)
- Hosts: QA1, QA2, QA3 environments
- Purpose: Quality assurance testing
- Managed by: QA and DevOps teams

**Development Server** (4.240.39.63)
- Hosts: Development environments
- Purpose: Active development and testing
- Managed by: Development team

#### Observability Server

**Purpose:**
- Centralized monitoring
- Automated script execution
- Database utilities
- Email notification service

**Capabilities:**
- Server health monitoring
- Automated restarts
- Database backup and restore
- Email notifications for deployments and incidents

## Troubleshooting

### Common Deployment Issues

#### Issue 1: Application Returns HTTP 404 After Deployment

**Symptoms:**
- Deployment completes successfully
- Health check fails with HTTP 404
- Application not accessible

**Most Common Cause:** Hardcoded JNDI datasources in `persistence.xml`

**Diagnosis:**
```bash
# Check deployment logs for JDBC replacement
gh run view [RUN_ID] --log | grep "Update JDBC Data Sources"

# Verify what was deployed
gh run view [RUN_ID] --log | grep "jta-data-source"
```

**Solution:**
1. Fix `persistence.xml` to use `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}`
2. Commit and push the fix
3. Redeploy

**Prevention:**
- Always verify `persistence.xml` before pushing (see [Pre-Deployment Checklist](#pre-deployment-checklist))
- Use pre-commit hooks to validate configuration

#### Issue 2: Build Fails During Maven Package

**Symptoms:**
- Build phase fails
- Error in `mvn clean package` step

**Common Causes:**
1. Compilation errors in Java code
2. Missing dependencies
3. Dependency version conflicts
4. Insufficient memory for Maven build

**Diagnosis:**
```bash
# View build error logs
gh run view [RUN_ID] --log | grep -A 20 "Build with Maven"
```

**Solutions:**
- **Compilation errors:** Fix code errors and repush
- **Missing dependencies:** Check `pom.xml`, run `mvn dependency:tree`
- **Version conflicts:** Review dependency management section in `pom.xml`
- **Memory issues:** Typically auto-resolved by GitHub Actions; retry deployment

#### Issue 3: Deployment Fails During File Transfer

**Symptoms:**
- Build succeeds
- Deployment fails during rsync step

**Common Causes:**
1. SSH connection issues
2. Insufficient disk space on server
3. Permission issues
4. Network timeout

**Diagnosis:**
```bash
# Check deployment logs
gh run view [RUN_ID] --log | grep -A 10 "Copy new WAR file"
```

**Solutions:**
- **SSH issues:** Verify server is accessible; contact DevOps
- **Disk space:** DevOps to clean up old files on server
- **Permissions:** DevOps to verify `/home/appuser/app/latest` permissions
- **Network timeout:** Retry deployment

#### Issue 4: Payara Deployment Fails

**Symptoms:**
- File transfer succeeds
- `asadmin deploy` command fails

**Common Causes:**
1. Payara server is down
2. Insufficient memory on server
3. Application startup errors
4. Port conflicts

**Diagnosis:**
```bash
# Check Payara deployment logs
gh run view [RUN_ID] --log | grep -A 20 "Deploy the WAR using asadmin"
```

**Solutions:**
- **Payara down:** DevOps to restart Payara server
- **Memory issues:** DevOps to increase heap size or restart server
- **Startup errors:** Review application logs; fix code issues
- **Port conflicts:** DevOps to check port assignments

#### Issue 5: Application Starts But Doesn't Respond

**Symptoms:**
- Deployment succeeds
- Application listed in `asadmin list-applications`
- HTTP health check fails or times out

**Common Causes:**
1. Database connection pool exhaustion
2. Application still initializing
3. Network/firewall issues
4. SSL certificate issues

**Diagnosis:**
```bash
# Check if application is actually running
curl -I https://[subdomain].carecode.org/[app]/faces/index1.xhtml

# Check application logs (requires SSH)
ssh user@server "tail -100 /opt/payara5/glassfish/domains/domain1/logs/server.log"
```

**Solutions:**
- **Still initializing:** Wait 2-5 minutes and recheck
- **Database issues:** Check database server status and connection pool
- **Network issues:** Contact DevOps to check firewall rules
- **SSL issues:** DevOps to verify SSL certificates

#### Issue 6: Branch Protection Prevents Push

**Symptoms:**
- Git push is rejected
- Error: "GH013: Repository rule violations found"

**Cause:** Trying to push directly to protected production branch

**Solution:**
```bash
# Use pull request method instead
gh pr create --base [production-branch] --head [source-branch] \
  --title "Deploy to production" \
  --body "Production deployment description"

# Merge PR (after approval)
gh pr merge [PR_NUMBER] --merge
```

#### Issue 7: Deployment Workflow Not Triggering

**Symptoms:**
- Push to production branch succeeds
- No GitHub Actions workflow starts

**Common Causes:**
1. Workflow file doesn't exist for this branch
2. Workflow file has syntax errors
3. GitHub Actions is disabled
4. Branch name doesn't match workflow trigger

**Diagnosis:**
```bash
# Verify workflow file exists
ls -la .github/workflows/*prod*.yml

# Check workflow syntax
cat .github/workflows/[environment]_prod_ci_cd.yml | head -20

# View recent runs
gh run list --limit 10
```

**Solutions:**
- **Missing workflow:** Create workflow file for this environment
- **Syntax errors:** Fix YAML syntax in workflow file
- **Disabled Actions:** Contact repository admin
- **Branch mismatch:** Verify branch name matches workflow trigger

### Emergency Contacts

**For Production Issues:**
- **DevOps Team:** [Contact via configured channels]
- **Database Team:** [Contact via configured channels]
- **Project Lead:** Dr. M H B Ariyaratne

**Escalation Path:**
1. Developer notices issue → Attempts fix
2. If unresolved in 15 minutes → Contact DevOps
3. If impacting patient care → Escalate to Project Lead immediately

## Security Considerations

### Secrets Management

**All sensitive credentials are stored as GitHub Secrets:**

| Secret Name | Purpose | Environment-Specific |
|-------------|---------|---------------------|
| `[ENV]_SERVER_IP` | Production server IP address | Yes |
| `[ENV]_SERVER_USER` | SSH username for deployment | Yes |
| `[ENV]_SSH_PRIVATE_KEY` | SSH private key for authentication | Yes |
| `[ENV]_PAYARA_ADMIN_PASS` | Payara admin password | Yes |
| `OBSERVABILITY_SERVER_IP` | Monitoring server IP | No |
| `OBSERVABILITY_SSH_PRIVATE_KEY` | Monitoring server SSH key | No |

**Security Best Practices:**

1. **Never commit secrets to Git:**
   - Database passwords
   - SSH keys
   - API keys
   - Admin passwords

2. **Rotate credentials regularly:**
   - SSH keys: Every 6 months
   - Passwords: Every 90 days
   - Review access: Monthly

3. **Limit access:**
   - Only authorized DevOps personnel have SSH access
   - Production branch push access limited to senior developers
   - Audit all access regularly

4. **Secure communication:**
   - All deployments use SSH with key-based authentication
   - All application traffic uses HTTPS/TLS
   - Database connections encrypted

### HIPAA Compliance

**Healthcare data protection requirements:**

1. **Data Encryption:**
   - ✅ Data in transit: HTTPS/TLS for all connections
   - ✅ Data at rest: Database encryption enabled
   - ✅ Backup encryption: All backups encrypted

2. **Access Control:**
   - ✅ Role-based access control (RBAC) in application
   - ✅ Multi-factor authentication for admin access
   - ✅ Audit trail for all data access

3. **Audit Logging:**
   - ✅ All deployments logged in GitHub Actions
   - ✅ Application maintains audit trail in `hmisAuditPU` database
   - ✅ Server access logs retained for compliance period

4. **Data Integrity:**
   - ✅ Automated backups before deployment
   - ✅ Rollback capability preserves data
   - ✅ No data loss during deployments

5. **Incident Response:**
   - Defined rollback procedures
   - Emergency contacts established
   - Incident documentation required

### Security Validations

**Automated security checks:**

1. **Pre-deployment validation:**
   - ✅ `grantAllPrivilegesToAllUsersForTesting` must be `false`
   - ✅ No hardcoded credentials in code
   - ✅ No development/debug flags enabled

2. **Dependency security:**
   - Jackson library updated for CVE-2019-10202
   - Log4j configured to prevent Log4Shell
   - Regular dependency vulnerability scans

3. **SQL Injection Prevention:**
   - All database queries use parameterized statements
   - User input validation and sanitization
   - Regular security code reviews

4. **Access Control:**
   - Session timeout configured
   - Failed login attempt monitoring
   - Password complexity requirements enforced

## Appendix

### Related Documentation

- **[QA Deployment Guide](../developer_docs/deployment/qa-deployment-guide.md)** - Deploying to QA environments
- **[Persistence Verification Guide](../developer_docs/deployment/persistence-verification.md)** - Detailed persistence.xml checks
- **[VM Restart Guide](../developer_docs/deployment/vm-restart-guide.md)** - Server restart procedures
- **[Git Commit Conventions](../developer_docs/git/commit-conventions.md)** - Commit message standards
- **[Production Deployment Checklist](../developer_docs/production-deployment-checklist.md)** - Comprehensive checklist

### Quick Reference Commands

#### Deployment Commands
```bash
# Deploy to production (via PR)
gh pr create --base [prod-branch] --head staging --title "Deploy to production"
gh pr merge [PR_NUMBER] --merge

# Monitor deployment
gh run list --branch [prod-branch] --limit 1
gh run watch [RUN_ID]

# Verify deployment
curl -I https://[subdomain].carecode.org/[app]/faces/index1.xhtml
```

#### Validation Commands
```bash
# Check persistence.xml
grep '<jta-data-source>' src/main/resources/META-INF/persistence.xml

# Verify security flags
grep "grantAllPrivilegesToAllUsersForTesting" src/main/java/com/divudi/bean/common/WebUserController.java

# Test compilation
mvn clean compile
```

#### Rollback Commands
```bash
# Rollback to previous commit
git log --oneline [prod-branch] -10
git push origin [previous-commit]:[ prod-branch] --force

# Monitor rollback
gh run watch $(gh run list --branch [prod-branch] --limit 1 --json databaseId --jq '.[0].databaseId')
```

### Production Branch Reference

| Branch | Institution | JDBC Primary | JDBC Audit |
|--------|-------------|--------------|------------|
| `ruhunu-prod` | Ruhunu Hospital | jdbc/ruhunu | jdbc/ruhunuAudit |
| `coop-prod` | Cooperative Hospital | jdbc/coop | jdbc/coopaudit |
| `mp-prod` | Medical Practice | jdbc/mp | jdbc/mpAudit |
| `rmh-prod` | RMH Hospital | jdbc/rmh | jdbc/rmhAudit |
| `southernlanka-prod` | Southern Lanka | jdbc/slh | jdbc/slhAudit |
| `kml-prod` | KML Healthcare | jdbc/kml | jdbc/kmlAudit |
| `horizon-prod` | Horizon Healthcare | jdbc/horizon | jdbc/horizonAudit |
| `digasiri-prod` | Digasiri Medical | jdbc/digasiri | jdbc/digasiriAudit |
| `asiripharmacy-prod` | Asiri Pharmacy | jdbc/asiripharmacy | jdbc/asiripharmacyAudit |
| `engage-wellness-center-prod` | Engage Wellness | jdbc/ewc | jdbc/ewcAudit |

### Workflow File Reference

| Workflow File | Branch Trigger | Environment |
|---------------|----------------|-------------|
| `rh_prod_ci_cd.yml` | `ruhunu-prod` | RH Production |
| `coop_prod_ci_cd.yml` | `coop-prod` | COOP Production |
| `mp_prod_ci_cd.yml` | `mp-prod` | MP Production |
| `rmh_prod_ci_cd.yml` | `rmh-prod` | RMH Production |
| `southernlanka_prod_ci_cd.yml` | `southernlanka-prod` | SLH Production |
| `kml_prod_ci_cd.yml` | `kml-prod` | KML Production |
| `horizon_prod_ci_cd.yml` | `horizon-prod` | Horizon Production |
| `digasiri_prod_ci_cd.yml` | `digasiri-prod` | Digasiri Production |
| `asiripharmacy_prod_ci_cd.yml` | `asiripharmacy-prod` | Asiri Production |
| `engage_wellness_center_prod_ci_cd.yml` | `engage-wellness-center-prod` | EWC Production |

---

**Document Version:** 1.0
**Last Updated:** 2025-11-23
**Maintained By:** HMIS Development Team
**Review Schedule:** Quarterly or after major infrastructure changes

**For questions or issues:**
- Create an issue: https://github.com/hmislk/hmis/issues
- Contact: Development team via configured channels
