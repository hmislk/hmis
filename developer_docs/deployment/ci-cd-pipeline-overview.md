# CI/CD Pipeline Documentation - HMIS Project

## Overview

The HMIS (Health Management Information System) project utilizes an extensive GitHub Actions-based CI/CD pipeline with 37+ active workflows supporting multiple healthcare clients across development, staging, and production environments.

## Branch Strategy

### Primary Branches
- **Development**: `development` (default branch)
- **Main**: Used for pull requests and general development work

### Environment-Specific Branches
- **Development Branches**: 
  - `ruhunu-dev` → dev.carecode.org
  - `coop-dev` → Custom development environment

- **Staging Branches**:
  - `rh-stg` → stg.carecode.org
  - `coop-stg` → Cooperative staging environment
  - `southernlanka-stg` → Southern Lanka staging
  - `mp-stg` → Matara Pharmacy staging

- **Production Branches**:
  - `ruhunu-prod` → rh.carecode.org
  - `coop-prod` → Cooperative production
  - `digasiri-prod` → Digasiri production
  - `horizon-prod` → Horizon production
  - `kml-prod` → KML production
  - `mp-prod` → Matara Pharmacy production
  - `rmh-prod` → RMH production
  - `southernlanka-prod` → Southern Lanka production
  - `asiripharmacy-prod` → Asiri Pharmacy production

## Deployment Environments

### Server Infrastructure
- **Development Server**: <DEV_SERVER_IP>
- **QA Servers**: 
  - QA1: <QA1_IP>
  - Multiple QA environments (QA1, QA2, QA3)
- **Shared Servers**:
  - Shared01: <SHARED01_IP>
  - Shared02: <SHARED02_IP>
- **Dedicated Servers**: 
  - D01: <D01_IP>

### Application Stack
- **Application Server**: Payara 5
- **Java Version**: JDK 11 (Temurin distribution)
- **Build Tool**: Maven
- **Database**: JDBC with environment-specific datasources
- **Web Server**: NGINX (reverse proxy)
- **OS**: Linux (Ubuntu)

## CI/CD Workflow Details

### Build Process
1. **Code Checkout**: Uses `actions/checkout@v4`
2. **Java Setup**: JDK 11 with Temurin distribution
3. **Maven Caching**: Caches `~/.m2` for faster builds
4. **Database Configuration**: 
   - Replaces environment variables in `persistence.xml`
   - Maps `${JDBC_DATASOURCE}` to client-specific JNDI names
   - Example: `jdbc/ruhunu` for Ruhunu client
5. **Build Execution**: `mvn clean verify`
   - Use `-DskipTests` only for emergency deployments; see “Troubleshooting → Build Failures”.
6. **Artifact Storage**: Uploads WAR files as build artifacts
### Deployment Process
1. **Artifact Download**: Retrieves WAR file from build job
2. **SSH Key Setup**: Configures private key for server access
3. **Backup Strategy**: 
   - Backs up existing WAR as `.old` file
   - Removes previous backup before creating new one
4. **File Transfer**: Uses `rsync` for efficient WAR deployment
5. **Payara Deployment**:
   - Undeploys existing application
   - Deploys new WAR with force flag
   - Sets appropriate context root
6. **Health Validation**:
   - Checks application deployment status via `asadmin`
   - Performs HTTP health checks (5 retries with 10s intervals)
   - Validates specific endpoint: `/faces/index1.xhtml`

### Security Configuration
- **SSH Keys**: Environment-specific private keys stored as GitHub secrets
- **Admin Passwords**: Payara admin passwords secured as secrets
- **Server Access**: Uses dedicated service accounts (`appuser`)
- **File Permissions**: Ensures proper ownership and permissions

## Server Management Actions

### Restart All Servers Workflow
**File**: `.github/workflows/restart_all_servers.yml`
- **Trigger**: Manual dispatch only (`workflow_dispatch`)
- **Selective Restart**: Allows exclusion of specific servers
- **Server Options**:
  - Development (<DEV_SERVER_IP>)
  - QA (<QA1_IP>)
  - Shared01 (<SHARED01_IP>)
  - Shared02 (<SHARED02_IP>)
  - D01 (<D01_IP>)
- **Remote Execution**: Calls `/home/azureuser/utils/server_utils/restart_all_servers.sh`

### Restart Individual Servers Workflow
**File**: `.github/workflows/restart_individual_servers.yml`
- **Trigger**: Manual dispatch only
- **Available Actions**:
  - Reload NGINX
  - Restart Payara Service
  - Restart VM
- **Server Selection**: Same server pool as restart all
- **Remote Execution**: Calls `/home/azureuser/utils/server_utils/restart_individual_servers.sh`

### Observability Server
- **Management Server IP**: Stored as `OBSERVABILITY_SERVER_IP` secret
- **SSH Access**: Uses `OBSERVABILITY_SSH_PRIVATE_KEY`
- **User Account**: `azureuser`
- **Utility Scripts**: Located in `/home/azureuser/utils/server_utils/`

## Validation Workflows

### Branch Merge Validation
- **File**: `.github/workflows/branch_merge_validation.yml`
- **Purpose**: Validates merges between branches

### Development PR Validation
- **File**: `.github/workflows/development_pr_validation.yml`
- **Purpose**: Validates pull requests to development branch

### Production Merge Validation
- **Purpose**: Additional validation for production deployments

## Database Management

### Export/Import Scheduler
- **File**: `.github/workflows/database_export_import_scheduler.yml`
- **Purpose**: Automated database backup and migration tasks

## Client-Specific Configurations

Each client has dedicated CI/CD pipelines with environment-specific configurations:

### Example: Ruhunu Hospital (RH)
- **Development**: `ruhunu-dev` → dev.carecode.org/rh
- **Staging**: `rh-stg` → stg.carecode.org/rh
- **Production**: `ruhunu-prod` → rh.carecode.org/rh
- **JNDI Resources**: `jdbc/ruhunu`, `jdbc/ruhunuAudit`

### Example: Cooperative (COOP)
- **Development**: `coop-dev`
- **Staging**: `coop-stg`
- **Production**: `coop-prod`
- **Special**: Combined staging-production workflow available

## Monitoring and Quality Assurance

### Code Quality
- **CodeQL Analysis**: Automated security and quality scanning
- **Dependency Management**: Dependabot for dependency updates

### Testing Strategy
- **Unit Tests**: Available but currently skipped (`-DskipTests`)
- **Integration Tests**: Commented out in most workflows
- **Health Checks**: Post-deployment HTTP validation

## Best Practices

### Deployment Safety
1. **Blue-Green Deployment**: Available for critical production environments
2. **Rollback Strategy**: Previous WAR files backed up as `.old`
3. **Health Validation**: Mandatory health checks before marking deployment successful
4. **Gradual Rollout**: Manual approval workflows for production

### Environment Management
1. **Environment Variables**: Used for database connections
2. **Secrets Management**: All sensitive data stored as GitHub secrets
3. **Environment Isolation**: Separate branches and servers per environment
4. **Client Isolation**: Dedicated pipelines prevent cross-client interference

### Maintenance
1. **Server Restart Capabilities**: Both bulk and individual server management
2. **Database Maintenance**: Scheduled export/import workflows
3. **Artifact Management**: Automatic cleanup and archival
4. **Monitoring**: Health check integration for proactive issue detection

## Troubleshooting

### Common Issues
1. **Build Failures**: Check Maven dependencies and Java version compatibility
2. **Deployment Failures**: Verify SSH keys and server connectivity
3. **Health Check Failures**: Validate application startup and database connections
4. **Permission Issues**: Ensure proper file ownership (`appuser:appuser`)

### Workflow Debugging
1. **View Workflow Runs**: `gh run list --workflow=workflow-name`
2. **Check Logs**: `gh run view run-id --log`
3. **Manual Deployment**: Use custom deployment workflow for troubleshooting

## Future Enhancements

### Recommendations
1. **Enable Unit Testing**: Remove `-DskipTests` flag and implement comprehensive test suite
2. **Container Strategy**: Consider containerization for improved deployment consistency
3. **Infrastructure as Code**: Implement Terraform or similar for infrastructure management
4. **Enhanced Monitoring**: Integrate application performance monitoring (APM)
5. **Automated Rollback**: Implement automatic rollback on health check failures

---

*Last Updated: 2025-01-27*
*This documentation reflects the current state of CI/CD pipelines as of the analysis date.*