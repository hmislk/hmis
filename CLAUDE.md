# Claude Code Configuration for HMIS Project

## Core Workflows

### Persistence Configuration
- **File**: `src/main/resources/META-INF/persistence.xml`
- **🚨 CRITICAL QA DEPLOYMENT RULE**: Before any push to GitHub, manually verify persistence.xml uses `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}` variables, NOT hardcoded JNDI names like `jdbc/coop`
- **⚠️ DDL GENERATION BLOCKER**: Remove hardcoded `eclipselink.application-location` paths like `c:/tmp/` from persistence.xml
- **Manual Process**: Always check `git status` before pushing to see if persistence.xml has been modified
- **Pre-Push Checklist**: 
  1. Check if persistence.xml is in staged changes: `git status`
  2. If modified, verify it contains `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}`
  3. If it has hardcoded JNDI names, manually replace them before push
  4. Never push with hardcoded local JNDI names like `jdbc/coop` or `jdbc/ruhunuAudit`

### Git & GitHub Integration
- **Commit Conventions**: [Details](developer_docs/git/commit-conventions.md)
- **Project Board**: [Workflow](developer_docs/github/project-board-integration.md)
- **QA Deployment**: [Bot-Friendly QA Deployment Guide](developer_docs/deployment/qa-deployment-guide.md)
- **VM Management**: [VM Restart Guide](developer_docs/deployment/vm-restart-guide.md)
- **Auto-close keywords**: `Closes #issueNumber`, `Fixes #issueNumber`
- **QA Testing Path**: Issue should be tested via GitHub Issues → Projects → HMIS Development Board
- **PR Review Path**: Pull Requests → Files Changed → Review Required Files → Approve/Request Changes

### Testing & Build
- **Maven Commands**: [Environment Setup](developer_docs/testing/maven-commands.md)
- **Preferred**: Use `./detect-maven.sh test` auto-detection script
- **Fallback**: Machine-specific Maven paths
- **JSF-Only Changes**: When modifying only XHTML/JSF files (no Java changes), compilation/testing is not required

### DTO Implementation
- **Guidelines**: [Complete Reference](developer_docs/dto/implementation-guidelines.md)
- **CRITICAL**: Never modify existing constructors - only add new ones
- **Use direct DTO queries** - avoid entity-to-DTO conversion loops

## Essential Rules
1. **MANUAL PERSISTENCE.XML VERIFICATION**: Before any GitHub push, manually verify persistence.xml uses `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}` - NEVER commit hardcoded JNDI datasources
2. **Include issue closing keywords** in commit messages
3. **Update project board status** automatically  
4. **Run tests before committing** using detect-maven script (only for Java changes)
5. **Follow DTO patterns** to avoid breaking changes
6. **JSF-only changes** do not require compilation or testing
7. **🚨 CRITICAL QA RULE**: Before any QA deployment, verify persistence.xml uses `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}` variables
8. **🚨 DDL GENERATION RULE**: Never commit persistence.xml with hardcoded DDL generation paths (`eclipselink.application-location`)
9. **🚨 BACKWARD COMPATIBILITY RULE**: NEVER "fix" intentional typos in entity/controller properties (e.g., `purcahseRate` instead of `purchaseRate`) - these exist for database backward compatibility
10. **🚨 COMPONENT NAMING RULE**: NEVER rename composite components (e.g., `transfeRecieve_detailed`) without checking ALL usage across the entire codebase - these are referenced in multiple pages

---
This behavior should persist across all Claude Code sessions for this project.