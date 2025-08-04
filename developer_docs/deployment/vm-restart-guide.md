# VM Restart Guide

This guide provides instructions for restarting production and development VMs using GitHub Actions workflows.

## Overview

The HMIS project includes automated VM management through GitHub Actions workflows that can restart VMs, reload NGINX, or restart Payara services on various environments.

## Available Actions

| Action | Description |
|--------|-------------|
| **Restart VM** | Completely restarts the virtual machine using Azure CLI |
| **Restart Payara Service** | Restarts only the Payara application server service |
| **Reload NGINX** | Reloads NGINX configuration without downtime |

## Available Server Environments

The system supports multiple server environments. When using the restart workflow, you'll need to specify:
- Server identifier (ask system administrator for current server mappings)
- Server IP address 
- Server purpose (Development/QA/Production/Shared)

**Note**: Server IPs and instance names change over time. Always verify current server mappings with the system administrator before executing restart commands.

## Method 1: GitHub Web Interface

### Step-by-Step Instructions

1. **Navigate to GitHub Actions**
   - Go to: https://github.com/hmislk/hmis/actions

2. **Find the Workflow**
   - Look for: "Restart Individual Servers"
   - Click on the workflow name

3. **Run the Workflow**
   - Click **"Run workflow"** button
   - Select your options:
     - **Action**: Choose from dropdown (Restart VM, Restart Payara Service, Reload NGINX)
     - **Select Server**: Choose target server from available options
   - Click **"Run workflow"** to execute

4. **Monitor Progress**
   - The workflow will appear in the actions list
   - Click on the running workflow to see real-time logs
   - Wait for completion (typically 30-60 seconds)

## Method 2: GitHub CLI

### Prerequisites

Ensure GitHub CLI is installed and authenticated:
```bash
# Check if gh is installed
gh --version

# Authenticate if needed
gh auth login
```

### Execute VM Restart

```bash
# Template command
gh workflow run "Restart Individual Servers" \
  --field selectedAction="Restart VM" \
  --field selectedServer="[SERVER_IDENTIFIER]"

# Example for restarting Payara service instead
gh workflow run "Restart Individual Servers" \
  --field selectedAction="Restart Payara Service" \
  --field selectedServer="[SERVER_IDENTIFIER]"
```

### Monitor Workflow Execution

```bash
# Check latest workflow runs
gh run list --workflow="Restart Individual Servers" --limit 5

# View specific run details (replace RUN_ID with actual ID)
gh run view [RUN_ID]

# Watch workflow in real-time
gh run watch [RUN_ID]
```

## What Happens During VM Restart

### VM Restart Process
1. **Azure Authentication**: Script authenticates with Azure using managed identity
2. **VM Shutdown**: Azure CLI executes `az vm restart` command
3. **VM Boot**: Virtual machine restarts and boots up
4. **Service Startup**: All configured services start automatically
5. **Application Deployment**: Payara server and applications initialize

### Service Restart Process (Payara Only)
1. **SSH Connection**: Connects to target server via SSH
2. **Service Stop**: Executes `sudo systemctl restart payara_domain1.service`
3. **Service Start**: Payara service restarts with existing deployment
4. **Verification**: Confirms service is running

### NGINX Reload Process
1. **SSH Connection**: Connects to target server via SSH  
2. **Configuration Reload**: Executes `sudo systemctl reload nginx`
3. **Zero Downtime**: NGINX reloads configuration without dropping connections

## Post-Restart Verification

### Application Health Check

After VM restart, verify application accessibility:

```bash
# Check if application responds (replace with actual domain)
curl -s -o /dev/null -w "%{http_code}" https://[DOMAIN]/[APP_CONTEXT]/faces/index1.xhtml

# Expected output: 200 (success)
# Other codes: 000 (still starting), 5xx (error)
```

### Timing Expectations

| Action | Expected Duration | Application Downtime |
|--------|------------------|---------------------|
| VM Restart | 2-5 minutes | 2-5 minutes |
| Payara Restart | 30-90 seconds | 30-90 seconds |
| NGINX Reload | 5-10 seconds | None |

### Troubleshooting

**If application doesn't respond after 5 minutes:**

1. **Check workflow logs**:
   ```bash
   gh run view [RUN_ID] --log
   ```

2. **Verify server status** (if you have direct access):
   ```bash
   # Check if VM is running in Azure portal
   # Or using Azure CLI:
   az vm get-instance-view --name [VM_NAME] --resource-group [RESOURCE_GROUP]
   ```

3. **Check Payara service**:
   ```bash
   # SSH to server and check service status
   sudo systemctl status payara_domain1.service
   ```

## Production vs Development Environments

### Production Considerations
- **Blue-Green Deployment**: Production uses blue-green deployment with load balancer
- **Multiple VMs**: May have multiple production VMs behind load balancer
- **Backup Strategy**: Always ensure recent backups before restart
- **Maintenance Window**: Consider scheduling during low-traffic periods

### Development/QA Considerations
- **Single VM**: Usually one VM per environment
- **Faster Recovery**: Less complex infrastructure, quicker restart
- **Testing Impact**: May affect ongoing development/testing activities

## Security Notes

- All server credentials are stored as GitHub repository secrets
- SSH keys are temporary and cleaned up after each operation
- Azure authentication uses managed identity
- No sensitive information is exposed in workflow logs

## Emergency Procedures

### If Workflow Fails

1. **Check Recent Runs**:
   ```bash
   gh run list --limit 5
   ```

2. **View Error Details**:
   ```bash
   gh run view [FAILED_RUN_ID] --log
   ```

3. **Retry Operations**:
   ```bash
   # Wait 2-3 minutes, then retry
   gh workflow run "Restart Individual Servers" \
     --field selectedAction="Restart VM" \
     --field selectedServer="[SERVER_IDENTIFIER]"
   ```

### Manual Intervention Required

If automated restart fails, contact system administrator for:
- Direct Azure portal access
- SSH access to servers
- Manual service restart procedures
- Rollback procedures if needed

## Best Practices

1. **Verify Target**: Always double-check server selection before restart
2. **Timing**: Avoid restarts during peak usage hours
3. **Communication**: Notify team members of planned restarts
4. **Monitoring**: Monitor application logs after restart
5. **Documentation**: Update this guide when server configurations change

## Quick Reference Commands

```bash
# List available workflows
gh workflow list

# Run VM restart (template)
gh workflow run "Restart Individual Servers" \
  --field selectedAction="Restart VM" \
  --field selectedServer="[ASK_ADMIN_FOR_CURRENT_SERVER]"

# Check recent workflow runs
gh run list --workflow="Restart Individual Servers" --limit 3

# Verify application health
curl -I https://[DOMAIN]/[APP]/faces/index1.xhtml
```

---
*Last updated: 2025-07-31*
*Always verify current server mappings with system administrator before executing restart commands*