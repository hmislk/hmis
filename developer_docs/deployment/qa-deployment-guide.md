# QA Environment Deployment Guide

This guide provides detailed instructions for deploying the latest development code to QA1 and QA2 environments.

## Overview

The HMIS project uses GitHub Actions for automated CI/CD deployment to QA environments. Deployments are triggered by pushing code to specific branch names that correspond to each environment.

## ⚠️ Critical Configuration Requirements

**IMPORTANT**: Before any QA deployment, verify that `src/main/resources/META-INF/persistence.xml` is properly configured:

### Pre-Deployment Checklist

Run these commands before deploying:

```bash
# 1. Check JNDI datasources use environment variables
grep '<jta-data-source>' src/main/resources/META-INF/persistence.xml

# 2. Check for hardcoded DDL generation paths
grep -i "eclipselink.application-location" src/main/resources/META-INF/persistence.xml
```

### ✅ **CORRECT Configuration** (Required for deployments):

**JNDI Datasources:**
```xml
<jta-data-source>${JDBC_DATASOURCE}</jta-data-source>
<jta-data-source>${JDBC_AUDIT_DATASOURCE}</jta-data-source>
```

**DDL Generation:** Should NOT contain hardcoded paths
```xml
<!-- These lines should NOT exist in deployment persistence.xml -->
<!-- <property name="eclipselink.application-location" value="c:/tmp/"/> -->
```

### ❌ **INCORRECT Configuration** (Will cause deployment failures):

**Hardcoded JNDI:**
```xml
<jta-data-source>jdbc/coop</jta-data-source>
<jta-data-source>jdbc/ruhunuAudit</jta-data-source>
```

**Hardcoded DDL paths:**
```xml
<property name="eclipselink.application-location" value="c:/tmp/"/>
<property name="eclipselink.ddl-generation" value="create-or-extend-tables"/>
```

The GitHub Actions workflow automatically replaces environment variables with environment-specific values during build.

## Environment Details

| Environment | Branch Name | Application URL | JDBC DataSource |
|-------------|-------------|-----------------|-----------------|
| QA1 | `hims-qa1` | https://qa.carecode.org/qa1 | jdbc/qa, jdbc/qaAudit |
| QA2 | `hims-qa2` | https://qa.carecode.org/qa2 | jdbc/coop, jdbc/coopAudit |
| QA3 | `hims-qa3` | https://qa.carecode.org/qa3 | jdbc/qa3, jdbc/qa3audit |

## Deployment Process

### Prerequisites

**IMPORTANT: Complete ALL prerequisites before proceeding**

1. **Install and authenticate GitHub CLI:**
   ```bash
   # Check if gh is installed
   gh --version
   # Expected output: gh version X.X.X (YYYY-MM-DD)
   
   # If not installed, install it first, then authenticate
   gh auth login
   # Follow the prompts to authenticate with GitHub
   ```

2. **Verify repository access:**
   ```bash
   # Check current repository
   git remote -v
   # Expected output should show: origin https://github.com/hmislk/hmis.git
   
   # Test GitHub access
   gh repo view hmislk/hmis --json name
   # Expected output: {"name":"hmis"}
   ```

3. **Ensure clean working directory:**
   ```bash
   # Check git status
   git status
   # Expected output: "nothing to commit, working tree clean"
   
   # If you have uncommitted changes, stash them:
   # git stash push -m "temporary stash before deployment"
   ```

### Step-by-Step Deployment Instructions

#### Deploying to QA1 (Direct Push Method)

**Follow these steps EXACTLY in order:**

**Step 1: Switch to development branch**
```bash
git checkout development
```
Expected output: `Switched to branch 'development'` or `Already on 'development'`

**Step 2: Get latest changes**
```bash
git pull origin development
```
Expected output: Should show files updated or `Already up to date.`

**Step 3: Push to QA1 deployment branch**
```bash
git push origin development:hims-qa1 --force
```
Expected output: Should show something like:
```
To https://github.com/hmislk/hmis.git
 + abc1234...def5678 development -> hims-qa1 (forced update)
```

**Step 4: Get the deployment run ID**
```bash
gh run list --branch hims-qa1 --limit 1
```
Expected output format:
```
STATUS    TITLE    WORKFLOW    BRANCH    EVENT    ID    ELAPSED    AGE
in_progress    Deploy to QA1    QA1 Environment CI-CD Workflow    hims-qa1    push    1234567890    30s    1m
```
**Copy the ID number (e.g., 1234567890) for the next step**

**Step 5: Monitor deployment (REPLACE [RUN_ID] with actual ID from step 4)**
```bash
gh run watch [RUN_ID]
```
Example: `gh run watch 1234567890`

**Step 6: Verify deployment success**
Wait for the workflow to complete (shows ✓ symbols). Then test the application:
```bash
curl -s -o /dev/null -w "%{http_code}" https://qa.carecode.org/qa1/faces/index1.xhtml
```
Expected output: `200` (means success)

#### Deploying to QA2 (Pull Request Method - Protected Branch)

**IMPORTANT: QA2 requires pull requests due to branch protection rules**

**Follow these steps EXACTLY in order:**

**Step 1: Switch to development branch**
```bash
git checkout development
```
Expected output: `Switched to branch 'development'` or `Already on 'development'`

**Step 2: Get latest changes**
```bash
git pull origin development
```
Expected output: Should show files updated or `Already up to date.`

**Step 3: Create pull request for QA2**
```bash
gh pr create --base hims-qa2 --head development --title "Deploy latest development to QA2" --body "Automated deployment of latest development branch to QA2 environment"
```
Expected output: A URL like `https://github.com/hmislk/hmis/pull/14401`
**Copy the PR number from the URL (e.g., 14401)**

**Step 4: Merge the pull request (REPLACE [PR_NUMBER] with actual number from step 3)**
```bash
gh pr merge [PR_NUMBER] --merge
```
Example: `gh pr merge 14401 --merge`
Expected output: `✓ Merged pull request #14401`

**Step 5: Get the deployment run ID**
```bash
gh run list --branch hims-qa2 --limit 1
```
Expected output format:
```
STATUS    TITLE    WORKFLOW    BRANCH    EVENT    ID    ELAPSED    AGE
in_progress    Deploy to QA2    HIMS QA-2 Environment CI-CD Workflow    hims-qa2    push    1234567890    30s    1m
```
**Copy the ID number (e.g., 1234567890) for the next step**

**Step 6: Monitor deployment (REPLACE [RUN_ID] with actual ID from step 5)**
```bash
gh run watch [RUN_ID]
```
Example: `gh run watch 1234567890`

**Step 7: Verify deployment success**
Wait for the workflow to complete (shows ✓ symbols). Then test the application:
```bash
curl -s -o /dev/null -w "%{http_code}" https://qa.carecode.org/qa2/faces/index1.xhtml
```
Expected output: `200` (means success)

#### Deploying to QA3 (Pull Request Method - Protected Branch)

**IMPORTANT: QA3 requires pull requests due to branch protection rules**

**Follow these steps EXACTLY in order:**

**Step 1: Switch to development branch**
```bash
git checkout development
```
Expected output: `Switched to branch 'development'` or `Already on 'development'`

**Step 2: Get latest changes**
```bash
git pull origin development
```
Expected output: Should show files updated or `Already up to date.`

**Step 3: Create pull request for QA3**
```bash
gh pr create --base hims-qa3 --head development --title "Deploy latest development to QA3" --body "Automated deployment of latest development branch to QA3 environment"
```
Expected output: A URL like `https://github.com/hmislk/hmis/pull/14402`
**Copy the PR number from the URL (e.g., 14402)**

**Step 4: Merge the pull request (REPLACE [PR_NUMBER] with actual number from step 3)**
```bash
gh pr merge [PR_NUMBER] --merge
```
Example: `gh pr merge 14402 --merge`
Expected output: `✓ Merged pull request #14402`

**Step 5: Get the deployment run ID**
```bash
gh run list --branch hims-qa3 --limit 1
```
Expected output format:
```
STATUS    TITLE    WORKFLOW    BRANCH    EVENT    ID    ELAPSED    AGE
in_progress    Deploy to QA3    HIMS QA-3 Environment CI-CD Workflow    hims-qa3    push    1234567890    30s    1m
```
**Copy the ID number (e.g., 1234567890) for the next step**

**Step 6: Monitor deployment (REPLACE [RUN_ID] with actual ID from step 5)**
```bash
gh run watch [RUN_ID]
```
Example: `gh run watch 1234567890`

**Step 7: Verify deployment success**
Wait for the workflow to complete (shows ✓ symbols). Then test the application:
```bash
curl -s -o /dev/null -w "%{http_code}" https://qa.carecode.org/qa3/faces/index1.xhtml
```
Expected output: `200` (means success)

## What Happens During Deployment

### Build Phase
1. **Code Checkout**: Latest code from the target branch
2. **Java Setup**: JDK 11 with Temurin distribution
3. **Maven Cache**: Caches dependencies for faster builds
4. **JDBC Configuration**: Updates persistence.xml with environment-specific datasources
5. **Maven Build**: `mvn clean package -DskipTests`
6. **Artifact Archive**: Stores the generated WAR file

### Deploy Phase
1. **Artifact Download**: Gets the WAR file from build phase
2. **Server Connection**: SSH connection to QA server
3. **Backup**: Creates backup of existing WAR file
4. **File Transfer**: Copies new WAR to server using rsync
5. **Payara Deployment**: 
   - Undeploys existing application
   - Deploys new WAR with force flag
   - Sets correct context path
6. **Health Check**: Validates application is running and reachable
7. **Cleanup**: Removes temporary SSH keys

## Monitoring Deployment

### Using GitHub CLI
```bash
# List recent runs for a branch
gh run list --branch hims-qa1 --limit 5   # For QA1
gh run list --branch hims-qa2 --limit 5   # For QA2
gh run list --branch hims-qa3 --limit 5   # For QA3

# Watch a specific run in real-time
gh run watch [RUN_ID]

# View run details
gh run view [RUN_ID]
```

### Using GitHub Web Interface
1. Go to https://github.com/hmislk/hmis/actions
2. Filter by the deployment branch (hims-qa1, hims-qa2, or hims-qa3)
3. Click on the most recent workflow run to see details

## Error Handling and Troubleshooting

### Common Error Messages and Solutions

#### 1. **Command not found: gh**
**Error:** `gh: command not found`
**Solution:**
```bash
# Install GitHub CLI first
# For Windows: winget install --id GitHub.cli
# For Mac: brew install gh
# For Linux: Follow https://github.com/cli/cli/blob/trunk/docs/install_linux.md
```

#### 2. **Authentication Error**
**Error:** `To authenticate, please run: gh auth login`
**Solution:**
```bash
gh auth login
# Choose: GitHub.com > HTTPS > Y > Login with web browser
# Follow the browser prompts
```

#### 3. **Repository Access Denied**
**Error:** `HTTP 403: Forbidden (HTTP 403)`
**Solution:**
- Contact repository administrator for access
- Verify you're using the correct GitHub account

#### 3.1. **🚨 CRITICAL: Applications Not Starting (404 Errors)**
**Symptoms:** 
- Deployment completes successfully but applications return 404 errors
- `curl https://qa.carecode.org/qa1/faces/index1.xhtml` returns 404

**Most Common Cause:** Hardcoded JNDI datasources in persistence.xml

**Solution:**
1. **Check persistence.xml configuration:**
   ```bash
   grep '<jta-data-source>' src/main/resources/META-INF/persistence.xml
   ```
   
2. **Expected output (CORRECT):**
   ```xml
   <jta-data-source>${JDBC_DATASOURCE}</jta-data-source>
   <jta-data-source>${JDBC_AUDIT_DATASOURCE}</jta-data-source>
   ```

3. **If you see hardcoded values (INCORRECT):**
   ```xml
   <jta-data-source>jdbc/coop</jta-data-source>
   <jta-data-source>jdbc/ruhunuAudit</jta-data-source>
   ```
   
   **Fix immediately:**
   - Replace with environment variables
   - Create PR with the fix
   - Redeploy all affected QA environments

4. **Verify build logs show proper replacement:**
   ```bash
   gh run view [RUN_ID] --log | grep "Update JDBC Data Sources"
   ```

#### 4. **Force Push Rejected (All QA Environments)**
**Error:** `remote: error: GH013: Repository rule violations found`
**Solution:** This is normal for protected branches. Use the Pull Request method instead.

#### 5. **No commits between branches**
**Error:** `GraphQL: No commits between hims-qa3 and development`
**Solution:** QA3 is already up to date. No deployment needed.

#### 6. **Workflow Not Triggering**
**Symptoms:** No GitHub Actions run appears after push
**Check these:**
```bash
# Verify you pushed to correct branch
git ls-remote origin | grep hims-qa

# Check if workflow file exists
ls -la .github/workflows/*qa*.yml
```

#### 7. **Deployment Timeout or Failure**
**Symptoms:** Workflow fails or takes too long
**Actions:**
```bash
# Check workflow logs
gh run view [RUN_ID] --log

# Check server status manually (if you have access)
# curl -I https://qa.carecode.org/qa1/faces/index1.xhtml
```

### Step-by-Step Error Recovery

**If deployment fails:**

1. **Check the workflow status:**
   ```bash
   gh run list --branch hims-qa1 --limit 3
   ```

2. **View detailed error logs:**
   ```bash
   gh run view [FAILED_RUN_ID] --log
   ```

3. **If build failed, try again (often temporary):**
   ```bash
   # Wait 5 minutes, then retry the deployment
   git push origin development:hims-qa1 --force
   ```

4. **If deployment failed, check application manually:**
   ```bash
   curl -I https://qa.carecode.org/qa1/faces/index1.xhtml
   # If this returns 200, the app is working despite the workflow error
   ```

### Rollback Process

If a deployment fails or causes issues:

1. **Check previous successful deployment:**
   ```bash
   gh run list --branch hims-qa1 --status success --limit 5
   ```

2. **Redeploy from a previous commit:**
   ```bash
   # Find the commit hash from the successful deployment
   git push origin [COMMIT_HASH]:hims-qa1 --force
   ```

3. **Manual rollback on server:**
   - SSH to the QA server
   - Use the backup WAR file: `mv qa1.war.old qa1.war`
   - Redeploy using Payara asadmin

## Environment URLs

After successful deployment, applications will be available at:
- **QA1**: https://qa.carecode.org/qa1/faces/index1.xhtml
- **QA2**: https://qa.carecode.org/qa2/faces/index1.xhtml
- **QA3**: https://qa.carecode.org/qa3/faces/index1.xhtml

## Advanced Troubleshooting

For complex deployment failures involving server infrastructure issues, see the comprehensive troubleshooting guide:
📋 **[QA Troubleshooting Guide](qa-troubleshooting.md)**

Common server-side issues include:
- EclipseLink DDL generation path errors
- Payara admin authentication failures  
- Application state mismatches
- Missing server directories or files

## Security Notes

- All server credentials are stored as GitHub repository secrets
- SSH keys are temporary and cleaned up after each deployment
- Database credentials are environment-specific and managed by the server configuration

## Quick Reference Commands

```bash
# Deploy latest development to QA1 (Direct push)
git checkout development && git pull origin development
git push origin development:hims-qa1 --force
gh run list --branch hims-qa1 --limit 1

# Deploy latest development to QA2 (via PR - protected branch)
git checkout development && git pull origin development
gh pr create --base hims-qa2 --head development --title "Deploy to QA2" --body "Deploy latest development"
# Then merge the created PR via GitHub UI or: gh pr merge [PR_NUMBER] --merge

# Deploy latest development to QA3 (via PR - protected branch)
git checkout development && git pull origin development
gh pr create --base hims-qa3 --head development --title "Deploy to QA3" --body "Deploy latest development"
# Then merge the created PR via GitHub UI or: gh pr merge [PR_NUMBER] --merge

# Monitor deployment
gh run watch [RUN_ID]
```

## BOT-FRIENDLY Execution Checklists

### ✅ QA1 Deployment Checklist

Copy and execute these commands one by one, checking each output:

```
[ ] gh --version
[ ] git status
[ ] git checkout development
[ ] git pull origin development
[ ] git push origin development:hims-qa1 --force
[ ] gh run list --branch hims-qa1 --limit 1
[ ] gh run watch [COPY_RUN_ID_HERE]
[ ] curl -s -o /dev/null -w "%{http_code}" https://qa.carecode.org/qa1/faces/index1.xhtml
```

**Success criteria:** Last command returns `200`

### ✅ QA2 Deployment Checklist

Copy and execute these commands one by one, checking each output:

```
[ ] gh --version
[ ] git status
[ ] git checkout development
[ ] git pull origin development
[ ] gh pr create --base hims-qa2 --head development --title "Deploy latest development to QA2" --body "Automated deployment"
[ ] gh pr merge [COPY_PR_NUMBER_HERE] --merge
[ ] gh run list --branch hims-qa2 --limit 1
[ ] gh run watch [COPY_RUN_ID_HERE]
[ ] curl -s -o /dev/null -w "%{http_code}" https://qa.carecode.org/qa2/faces/index1.xhtml
```

**Success criteria:** Last command returns `200`

### ✅ QA3 Deployment Checklist

Copy and execute these commands one by one, checking each output:

```
[ ] gh --version
[ ] git status
[ ] git checkout development
[ ] git pull origin development
[ ] gh pr create --base hims-qa3 --head development --title "Deploy latest development to QA3" --body "Automated deployment"
[ ] gh pr merge [COPY_PR_NUMBER_HERE] --merge
[ ] gh run list --branch hims-qa3 --limit 1
[ ] gh run watch [COPY_RUN_ID_HERE]
[ ] curl -s -o /dev/null -w "%{http_code}" https://qa.carecode.org/qa3/faces/index1.xhtml
```

**Success criteria:** Last command returns `200`

### 🚨 Emergency Commands

If something goes wrong:
```bash
# Check recent workflow runs
gh run list --limit 5

# View error logs (replace RUN_ID)
gh run view [RUN_ID] --log

# Check if app is actually working
curl -I https://qa.carecode.org/qa1/faces/index1.xhtml
curl -I https://qa.carecode.org/qa2/faces/index1.xhtml
curl -I https://qa.carecode.org/qa3/faces/index1.xhtml
```

---
*Last updated: 2025-07-31*
*This document is designed for both human operators and automated bots*
*For questions or issues, contact the development team or create an issue in the HMIS repository.*