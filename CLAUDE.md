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
- **Wiki Publishing**: [Guide](developer_docs/github/wiki-publishing.md)
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

### Database Development
- **MySQL Guide**: [Complete Reference](developer_docs/database/mysql-developer-guide.md)
- **🚨 CREDENTIALS SECURITY**: MySQL credentials MUST be stored in separate folder (NOT in git)
- **Location**: `C:\Credentials\credentials.txt` (Windows) or `~/.config/hmis/credentials.txt` (Linux/Mac)
- **Never commit database credentials** to version control
- **Database debugging techniques** and performance optimization guidelines in MySQL guide

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
11. **🚨 DATABASE CREDENTIALS RULE**: NEVER commit database credentials to git - store them in environment-specific folders outside the project directory (see MySQL guide)

## Wiki Writing Guidelines {#wiki-writing-guidelines}

### Purpose and Audience
- **Wiki is for end users**, not developers
- Focus on **how to use features**, not how they were implemented
- Write for pharmacy staff, nurses, doctors, administrators - not programmers

### Writing Style
- **User-centric language**: "How to substitute items" not "Implementation of substitute functionality"
- **Step-by-step instructions**: Clear, numbered procedures
- **Practical examples**: Real scenarios users encounter
- **Actionable guidance**: What to do when problems occur

### Content Structure
1. **Overview**: What the feature does and why users need it
2. **When to Use**: Specific scenarios and use cases
3. **How to Use**: Step-by-step procedures with screenshots if possible
4. **Understanding Messages**: What system messages mean and how to respond
5. **Best Practices**: Tips for effective use
6. **Troubleshooting**: Common problems and solutions
7. **Configuration**: Admin settings that affect the feature (user impact only)
8. **FAQ**: Common user questions

### What NOT to Include
- ❌ Code snippets, file paths, line numbers
- ❌ Technical implementation details
- ❌ Database schema information
- ❌ Developer debugging information
- ❌ Backend process descriptions

### What TO Include
- ✅ User interface elements and navigation
- ✅ Error messages and their meanings
- ✅ Business process workflows
- ✅ Configuration options (from user perspective)
- ✅ Integration with other modules

### Exception
Only include technical details when specifically requested for developer documentation or when writing for the developer_docs/ directory.

---
This behavior should persist across all Claude Code sessions for this project.