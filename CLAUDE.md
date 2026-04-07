# Claude Code Configuration for HMIS Project

## Repository Information
- **GitHub Repository**: https://github.com/hmislk/hmis (not buddhika75/hmis)
- **Issues URL**: https://github.com/hmislk/hmis/issues
- **Project tmp Folder**: `tmp/` directory **inside the project root** (i.e., `<project-root>/tmp/`) for project-specific temporary files. Do NOT use the system `/tmp/` directory.

## Essential Rules (Always Apply)

### User Control
1. **🚨 NO AUTO-ACTIONS**: Do NOT commit, build, run, or push code unless the user explicitly requests it
2. **🚨 EXPLICIT COMMANDS ONLY**: Wait for user confirmation before executing Git operations, Maven builds, or deployment commands
3. **🚨 NO AUTO-COMPILE**: Never run Maven compile unless explicitly requested

### Code Integrity
4. **🚨 NO MOCK DATA**: NEVER use mock bills, fake entities, or temporary workarounds in business logic
5. **🚨 DISCUSS UNCERTAINTIES**: ALWAYS discuss with user when uncertain about implementation approach
6. **🚨 BACKWARD COMPATIBILITY**: Never "fix" intentional typos (e.g., `purcahseRate`) - database compatibility
7. **🚨 COMPONENT NAMING**: Never rename composite components without checking ALL usage
8. **🚨 NEVER MODIFY EXISTING CONSTRUCTORS**: Only ADD new constructors. Changing or removing existing constructor signatures breaks other callers. New constructors should delegate to the existing one via `this(...)` when possible. See [DTO Guidelines](developer_docs/dto/implementation-guidelines.md)
9. **🚨 JPQL FIRST, NATIVE SQL LAST**: Always use JPQL for database queries. Native SQL (`nativeScalarQuery`, `executeNativeSql`) is only permitted when there is a significant, demonstrated performance constraint that JPQL cannot address. Never reach for native SQL just because JPQL is harder to write.
10. **🚨 USE `findLongByJpql` FOR COUNT QUERIES**: Always use `findLongByJpql` (not `findDoubleByJpql`) for JPQL `COUNT(...)` queries. `COUNT` returns a `Long`; using `findDoubleByJpql` causes a silent `ClassCastException` caught internally, returning `0.0` every time and making the check always pass.

### Git & Branching
11. **Include issue closing keywords** (`Closes #N`) in commit messages
12. **JSF-only changes** (XHTML only, no Java) do not require compilation or testing
13. **🚨 ALWAYS BASE FEATURE BRANCHES ON `development`**: When creating a new local branch for feature development, ALWAYS branch from `origin/development`, NEVER from `master`. The `master` branch is managed exclusively by system admins. Use: `git checkout -b <branch-name> origin/development`
14. **🚨 `development` IS THE DEFAULT BRANCH**: All PRs MUST target `development`, NOT `master`. When checking what already exists in the codebase (to avoid duplicate fields/methods), ALWAYS compare against `origin/development`, not `origin/master`. The CI validates against `development`. Never reference or merge into `master` during feature development.

## Situational Guidelines (Reference When Needed)

### When Working on Persistence/Deployment
- [Persistence Configuration Guide](developer_docs/deployment/persistence-verification.md) - JNDI settings for dev vs production

### When Working on UI/XHTML
- [UI Development Handbook](developer_docs/ui/comprehensive-ui-guidelines.md) - Complete UI reference
- [Icon Management](developer_docs/ui/icon-management.md) - Standard icons and sizing

### When Working on JSF/AJAX
- [JSF AJAX Update Guidelines](developer_docs/jsf/ajax-update-guidelines.md) - Critical AJAX rules
- [Navigation Patterns](developer_docs/jsf/navigation-patterns.md) - viewAction anti-pattern, initialization in navigation methods
- [DataTable Selection Guide](developer_docs/jsf/primefaces-datatable-selection.md) - Selection patterns

### When Working with DTOs
- [DTO Implementation Guidelines](developer_docs/dto/implementation-guidelines.md) - Constructor and query patterns

### When Working with Database
- [MySQL Developer Guide](developer_docs/database/mysql-developer-guide.md) - Credentials and debugging

### When Creating User Documentation
- [Wiki Publishing Workflow](developer_docs/github/wiki-publishing.md) - Sibling folder approach
- [Wiki Writing Guidelines](developer_docs/github/wiki-writing-guidelines.md) - Content standards
- **Wiki Location**: `../hmis.wiki` sibling directory (NEVER inside the main project repo)
- **Target Audience**: End users (pharmacy staff, nurses, doctors, administrators)

### When Working on Inward / Inpatient Module
- [Inward Navigation & Reference](developer_docs/navigation/inward_navigation.md) - Pages, controllers, workflow, open issues

### When Adding a New Privilege
- [Privilege System Guide](developer_docs/security/privilege-system.md) - **All 3 steps required**: enum value + `getCategory()` case + `UserPrivilageController` tree node. Adding only the enum is NOT sufficient — the privilege will be invisible in the admin UI. This was missed for `InpatientClinicalDischarge` (PR #19658, issue #19677).

### When Developing a REST API
- [REST API Development Guide](developer_docs/api/rest-api-development-guide.md) - **All 4 registration steps required**: `ApplicationConfig` + `CapabilityStatementResource` + `AnthropicApiService.buildSystemPrompt` (module listing) + `AnthropicApiService.buildToolsArray`/`executeToolCall` (tool handler). Skipping any step means the API is invisible to the AI chat or undiscoverable via `/api/capabilities`.

### When Committing Code
- [Commit Conventions](developer_docs/git/commit-conventions.md) - Message format

## Common Abbreviations & Terms
- **TIA**: Thanks In Advance

---
This behavior should persist across all Claude Code sessions for this project.
