# Gemini Code Configuration for HMIS Project

## Core Workflows

### Persistence Configuration
- **Database Push Workflow**: [Complete Instructions](developer_docs/persistence/persistence-workflow.md)
- **CRITICAL**: Automatic JNDI replacement before/after GitHub pushes
- **File**: `src/main/resources/META-INF/persistence.xml`
- **Environment Variables**: Replace `jdbc/coop` with `${JDBC_DATASOURCE}` and `jdbc/ruhunuAudit` with `${JDBC_AUDIT_DATASOURCE}`

### Git & GitHub Integration
- **Commit Conventions**: [Details](developer_docs/git/commit-conventions.md)
- **Project Board**: [Workflow](developer_docs/github/project-board-integration.md)
- **GitHub CLI Usage**: [Complete GitHub CLI Guide](developer_docs/github/gh-cli-usage.md)
- **QA Deployment**: [Bot-Friendly QA Deployment Guide](developer_docs/deployment/qa-deployment-guide.md)
- **Auto-close keywords**: `Closes #issueNumber`, `Fixes #issueNumber`

#### Troubleshooting `gh pr create`
- **Quoting Issues**: When using `gh pr create` with `--title` or `--body` flags, ensure that the entire string for the title and body is properly quoted to avoid shell interpretation issues, especially on Windows. For multi-line bodies or complex strings, consider using `--body-file` to read content from a temporary file.
- **Base Branch**: Always verify the correct base branch for the pull request. Use `git remote show origin` to identify the `HEAD branch` (e.g., `development` instead of `main`) and specify it with `--base <branch_name>`.
- **Seamless PR Creation**: The most reliable method for `gh pr create` is often to use the `--fill` flag after ensuring the local branch is up-to-date with the remote base branch.

##### Concrete Command Examples
```bash
# Check default branch
git remote show origin

# Create PR with proper base branch
gh pr create --base development --fill

# Create PR with specific title and body file
echo "Fix CodeRabbit AI issues" > /tmp/pr_title.txt
echo "- Fixed JPQL query duplicates\n- Removed debug code" > /tmp/pr_body.txt
gh pr create --title-file /tmp/pr_title.txt --body-file /tmp/pr_body.txt

# Handle permission errors
gh auth refresh --scopes repo

# Check current authentication status
gh auth status
```

### Testing & Build
- **Maven Commands**: [Environment Setup](developer_docs/testing/maven-commands.md)
- **Preferred**: Use `./detect-maven.sh test` auto-detection script
- **Fallback**: Machine-specific Maven paths
- **JSF-Only Changes**: When modifying only XHTML/JSF files (no Java changes), compilation/testing is not required

### DTO Implementation
- **Guidelines**: [Complete Reference](developer_docs/dto/implementation-guidelines.md)
- **CRITICAL**: Never modify existing constructors - only add new ones
- **Use direct DTO queries** - avoid entity-to-DTO conversion loops

## Development Agent Guidelines

### High-Level Agent Roles

#### Branch Management
- **Naming Convention**: Issue-based branches (`12875-implement-full-lab-workflow`)
- **PR Integration**: Auto-close with `Closes #issueNumber`
- **Status Tracking**: Project board updates

#### UI/UX Development
- **Icon Management**: SVG-only, consistent sizing (80x80)
- **Theming**: Use `currentColor` for dynamic styling
- **Accessibility**: Proper tooltips and labeling

#### Privilege System
- **Enum-based**: Add to `Privileges.java`
- **UI Integration**: Use `webUserController.hasPrivilege()`
- **Assignment**: Via User Privileges admin interface

#### Configuration Management
- **Dynamic Values**: Use `configOptionApplicationController`
- **Color Schemes**: Centralized color configuration
- **Feature Toggles**: Boolean flags for UI elements

## Detailed Guidelines

For comprehensive implementation details, see:
- **Branch & PR Management**: [Branch workflow details](developer_docs/git/branch-management.md)
- **GitHub CLI Usage**: [Complete GitHub CLI Guide](developer_docs/github/gh-cli-usage.md)
- **UI Development Handbook**: [Complete Reference](developer_docs/ui/comprehensive-ui-guidelines.md)
- **Icon & UI Standards**: [UI guidelines](developer_docs/ui/icon-management.md)
- **Privilege Implementation**: [Security patterns](developer_docs/security/privilege-system.md)
- **Configuration Patterns**: [Config usage](developer_docs/configuration/application-options.md)

## Key Principles
1. **Consistency**: Follow established patterns
2. **Security**: Validate privileges, sanitize inputs
3. **Maintainability**: Use centralized configuration
4. **Accessibility**: Ensure inclusive design

## Essential Rules
1. **Always use environment variables** in pushed persistence.xml
2. **Include issue closing keywords** in commit messages
3. **Update project board status** automatically
4. **Run tests before committing** using detect-maven script (only for Java changes)
5. **Follow DTO patterns** to avoid breaking changes
6. **JSF-only changes** do not require compilation or testing
