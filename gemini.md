# Gemini Code Configuration for HMIS Project

## Core Workflows

### Persistence Configuration
- **Database Push Workflow**: [Complete Instructions](developer_docs/persistence/persistence-workflow.md)
- **CRITICAL**: Automatic JNDI replacement before/after GitHub pushes
- **File**: `src/main/resources/META-INF/persistence.xml`

### Git & GitHub Integration
- **Commit Conventions**: [Details](developer_docs/git/commit-conventions.md)
- **Project Board**: [Workflow](developer_docs/github/project-board-integration.md)
- **GitHub CLI Usage**: [Complete GitHub CLI Guide](developer_docs/github/gh-cli-usage.md)
- **Auto-close keywords**: `Closes #issueNumber`, `Fixes #issueNumber`
- **QA Testing Path**: Issue should be tested via GitHub Issues → Projects → HMIS Development Board
- **PR Review Path**: Pull Requests → Files Changed → Review Required Files → Approve/Request Changes

### Testing & Build
- **Maven Commands**: [Environment Setup](developer_docs/testing/maven-commands.md)
- **Preferred**: Use `./detect-maven.sh test` auto-detection script
- **Fallback**: Machine-specific Maven paths

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
- **Icon & UI Standards**: [UI guidelines](developer_docs/ui/icon-management.md)
- **Privilege Implementation**: [Security patterns](developer_docs/security/privilege-system.md)
- **Configuration Patterns**: [Config usage](developer_docs/config/application-options.md)

## Key Principles
1. **Consistency**: Follow established patterns
2. **Security**: Validate privileges, sanitize inputs
3. **Maintainability**: Use centralized configuration
4. **Accessibility**: Ensure inclusive design

## Essential Rules
1. **Always use environment variables** in pushed persistence.xml
2. **Include issue closing keywords** in commit messages
3. **Update project board status** automatically
4. **Run tests before committing** using detect-maven script
5. **Follow DTO patterns** to avoid breaking changes

---
This behavior should persist across all Gemini Code sessions for this project.
