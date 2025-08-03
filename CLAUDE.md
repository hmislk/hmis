# Claude Code Configuration for HMIS Project

## Core Workflows

### Persistence Configuration
- **Database Push Workflow**: [Complete Instructions](developer_docs/persistence/persistence-workflow.md)
- **CRITICAL**: Automatic JNDI replacement before/after GitHub pushes
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
1. **Always use environment variables** in pushed persistence.xml - NEVER commit hardcoded JNDI datasources
2. **Include issue closing keywords** in commit messages
3. **Update project board status** automatically
4. **Run tests before committing** using detect-maven script (only for Java changes)
5. **Follow DTO patterns** to avoid breaking changes
6. **JSF-only changes** do not require compilation or testing
7. **🚨 CRITICAL QA RULE**: Before any QA deployment, verify persistence.xml uses `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}` variables
8. **🚨 DDL GENERATION RULE**: Never commit persistence.xml with hardcoded DDL generation paths (`eclipselink.application-location`)

---
This behavior should persist across all Claude Code sessions for this project.