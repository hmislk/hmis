# Claude Code Configuration for HMIS Project

## Core Workflows

### Persistence Configuration
- **Database Push Workflow**: [Complete Instructions](developer_docs/persistence/persistence-workflow.md)
- **AUTOMATION SCRIPTS**: Use `scripts\safe-push.bat` (Windows) or `./scripts/safe-push.sh` (Linux) instead of `git push` to automatically handle JNDI replacement
- **Manual Scripts**: `scripts\prepare-for-push.bat` / `scripts\restore-local-jndi.bat` (Windows) or bash equivalents for step-by-step control
- **File**: `src/main/resources/META-INF/persistence.xml`
- **⚠️ QA DEPLOYMENT BLOCKER**: Must use `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}` variables, NOT hardcoded JNDI names
- **⚠️ DDL GENERATION BLOCKER**: Remove hardcoded `eclipselink.application-location` paths like `c:/tmp/` from persistence.xml

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
1. **Use automation scripts for GitHub pushes**: `scripts\safe-push.bat` (Windows) or `./scripts/safe-push.sh` (Linux) instead of `git push` to prevent JNDI issues
2. **Always use environment variables** in pushed persistence.xml - NEVER commit hardcoded JNDI datasources
3. **Include issue closing keywords** in commit messages
4. **Update project board status** automatically
5. **Run tests before committing** using detect-maven script (only for Java changes)
6. **Follow DTO patterns** to avoid breaking changes
7. **JSF-only changes** do not require compilation or testing
8. **🚨 CRITICAL QA RULE**: Before any QA deployment, verify persistence.xml uses `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}` variables
9. **🚨 DDL GENERATION RULE**: Never commit persistence.xml with hardcoded DDL generation paths (`eclipselink.application-location`)
10. **🚨 BACKWARD COMPATIBILITY RULE**: NEVER "fix" intentional typos in entity/controller properties (e.g., `purcahseRate` instead of `purchaseRate`) - these exist for database backward compatibility
11. **🚨 COMPONENT NAMING RULE**: NEVER rename composite components (e.g., `transfeRecieve_detailed`) without checking ALL usage across the entire codebase - these are referenced in multiple pages

---
This behavior should persist across all Claude Code sessions for this project.