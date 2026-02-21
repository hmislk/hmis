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

### Git & Documentation
9. **Include issue closing keywords** (`Closes #N`) in commit messages
10. **JSF-only changes** (XHTML only, no Java) do not require compilation or testing

## Situational Guidelines (Reference When Needed)

### When Working on Persistence/Deployment
- [Persistence Configuration Guide](developer_docs/deployment/persistence-verification.md) - JNDI settings for dev vs production

### When Working on UI/XHTML
- [UI Development Handbook](developer_docs/ui/comprehensive-ui-guidelines.md) - Complete UI reference
- [Icon Management](developer_docs/ui/icon-management.md) - Standard icons and sizing

### When Working on JSF/AJAX
- [JSF AJAX Update Guidelines](developer_docs/jsf/ajax-update-guidelines.md) - Critical AJAX rules
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

### When Committing Code
- [Commit Conventions](developer_docs/git/commit-conventions.md) - Message format

## Common Abbreviations & Terms
- **TIA**: Thanks In Advance

---
This behavior should persist across all Claude Code sessions for this project.
