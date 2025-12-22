# Claude Code Configuration for HMIS Project

## Repository Information
- **GitHub Repository**: https://github.com/hmislk/hmis
- **Issues URL**: https://github.com/hmislk/hmis/issues
- **Main Repository**: hmislk/hmis (not buddhika75/hmis)

## Core Workflows

### Persistence Configuration
- **🚨 CRITICAL**: Before any push, verify persistence.xml uses environment variables
- **File**: `src/main/resources/META-INF/persistence.xml`
- **Required**: `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}` (NOT hardcoded JNDI names)
- **Pre-Push Checklist**: [Detailed Verification Guide](developer_docs/deployment/persistence-verification.md)

### Git & GitHub Integration
- **Commit Conventions**: [Guide](developer_docs/git/commit-conventions.md)
- **Project Board**: [Workflow](developer_docs/github/project-board-integration.md)
- **QA Deployment**: [Guide](developer_docs/deployment/qa-deployment-guide.md)
- **VM Management**: [Guide](developer_docs/deployment/vm-restart-guide.md)
- **Auto-close keywords**: `Closes #issueNumber`, `Fixes #issueNumber`

### Wiki Publishing
- **🚨 CRITICAL**: ALWAYS publish to GitHub Wiki immediately after creating user documentation
- **Directory**: Create files in `wiki-docs/` (e.g., `wiki-docs/Pharmacy/Feature-Name.md`)
- **Publishing Workflow**: [Complete Guide](developer_docs/github/wiki-publishing.md)
- **Writing Guidelines**: [Content Standards](developer_docs/github/wiki-writing-guidelines.md)
- **Target Audience if not explicitly mentioned**: End users (pharmacy staff, nurses, doctors, administrators)

### Developer Documentation Guidelines
- **🚨 TECHNICAL FOCUS ONLY**: Developer documentation should contain only technical implementation patterns, not narrative "before/after" stories
- **🚨 NO PROGRESS STORIES**: Avoid "we implemented this because...", "the user requested...", "this fixes the issue..." - focus on HOW to implement
- **🚨 IMPLEMENTATION PATTERNS**: Show code examples, method signatures, component usage, configuration patterns
- **🚨 CURRENT STATE ONLY**: Document the final implementation state, not the journey to get there
- **Target Audience**: Developers implementing similar features

### Testing & Build
- **🚨 COMPILE RULE**: Do NOT run `./detect-maven.sh compile` or Maven compile commands unless explicitly requested by user. Ask the user to compile and provide feedback first.
- **Maven Commands**: [Environment Setup](developer_docs/testing/maven-commands.md)
- **Preferred**: Use `./detect-maven.sh test` auto-detection script
- **Fallback**: Machine-specific Maven paths
- **JSF-Only Changes**: When modifying only XHTML/JSF files (no Java changes), compilation/testing is not required


### DTO Implementation
- **Guidelines**: [Complete Reference](developer_docs/dto/implementation-guidelines.md)
- **CRITICAL**: Try not to modify existing constructors - only add new ones
- **Use direct DTO queries** - avoid entity-to-DTO conversion loops

### UI Development Guidelines
- **🚨 UI-ONLY CHANGES**: When UI improvements are requested, make ONLY frontend/XHTML changes
- **NO BACKEND MODIFICATIONS**: Do NOT add new controller properties, methods, or backend dependencies unless explicitly requested
- **KEEP IT SIMPLE**: Use existing controller properties and methods - avoid introducing filteredValues, globalFilter, or new backend logic
- **FRONTEND FOCUS**: Stick to HTML/CSS styling, PrimeFaces component attributes, and layout improvements
- **UI Development Handbook**: [Complete Reference](developer_docs/ui/comprehensive-ui-guidelines.md)
- **Icon Management**: [Standard Actions & Sizing](developer_docs/ui/icon-management.md)

### JSF Development Guidelines
- **JSF AJAX Updates**: [Critical Guidelines](developer_docs/jsf/ajax-update-guidelines.md)
- **🚨 AJAX UPDATE RULE**: NEVER use plain HTML elements (div, span, etc.) with id attributes for AJAX updates - use JSF components (h:panelGroup, p:outputPanel, etc.) instead
- **🚨 RENDERED ATTRIBUTE RULE**: NEVER use `rendered` attribute on plain HTML elements - JSF ignores it; use JSF components like `h:panelGroup` with `layout="block"` instead
- **PrimeFaces DataTable Selection**: [Implementation Guide](developer_docs/jsf/primefaces-datatable-selection.md)
- **🚨 DATATABLE SELECTION**: Use `selectionMode="multiple"` on dataTable and `selectionBox="true"` on column (NOT `selectionMode` on column)

### Database Development
- **MySQL Guide**: [Complete Reference](developer_docs/database/mysql-developer-guide.md)
- **🚨 CREDENTIALS SECURITY**: MySQL credentials MUST be stored in separate folder (NOT in git)
- **Location**: `C:\Credentials\credentials.txt` (Windows) or `~/.config/hmis/credentials.txt` (Linux/Mac)
- **Never commit database credentials** to version control
- **Database debugging techniques** and performance optimization guidelines in MySQL guide

## Essential Rules

### User Control & Automation
1. **🚨 NO AUTO-ACTIONS**: Do NOT commit, build, run, or push code unless the user explicitly requests it
2. **🚨 EXPLICIT COMMANDS ONLY**: Wait for user confirmation before executing Git operations, Maven builds, or deployment commands
3. **🚨 WIKI EXCEPTION**: Wiki publishing requires push - follow [Publishing Workflow](developer_docs/github/wiki-publishing.md) exactly

### Deployment & Configuration
3. **🚨 PERSISTENCE.XML**: Verify environment variables before push - [Guide](developer_docs/deployment/persistence-verification.md)
4. **🚨 DATABASE CREDENTIALS**: Never commit credentials to git - [MySQL Guide](developer_docs/database/mysql-developer-guide.md)

### Git & Documentation
5. **Include issue closing keywords** (`Closes #N`) in commit messages
6. **🚨 WIKI PUBLISHING**: Publish to GitHub Wiki immediately - [Guide](developer_docs/github/wiki-publishing.md)

### Build & Testing
5. **Run tests before committing** using `./detect-maven.sh test` (Java changes only, when user requests)
6. **🚨 NO AUTO-COMPILE**: Never run Maven compile unless explicitly requested
7. **JSF-only changes** do not require compilation or testing

### Code Integrity
8. **Follow DTO patterns** to avoid breaking changes - [Guide](developer_docs/dto/implementation-guidelines.md)
9. **🚨 BACKWARD COMPATIBILITY**: Never "fix" intentional typos (e.g., `purcahseRate`) - database compatibility unless explicitly requested by the user
10. **🚨 COMPONENT NAMING**: Never rename composite components without checking ALL usage
11. **🚨 NO MOCK DATA**: NEVER use mock bills, fake entities, or temporary workarounds in business logic
12. **🚨 DISCUSS UNCERTAINTIES**: ALWAYS discuss with user when uncertain about implementation approach - never assume or create workarounds

### UI Development
11. **🚨 UI-ONLY CHANGES**: Frontend only - no backend modifications unless requested - [Guide](developer_docs/ui/comprehensive-ui-guidelines.md)
12. **🚨 ERP UI**: Use `h:outputText` instead of HTML headings (h1-h6)
13. **🚨 PRIMEFACES CSS**: Use PrimeFaces button classes, not Bootstrap
14. **🚨 XHTML STRUCTURE**: HTML DOCTYPE with `ui:composition` and template inside `h:body`
15. **🚨 XML ENTITIES**: Always escape ampersands as `&amp;` in XHTML attributes

### JSF Development
16. **🚨 JSF AJAX UPDATES**: Never use plain HTML elements for AJAX updates - [Guide](developer_docs/jsf/ajax-update-guidelines.md)
17. **🚨 PRIMEFACES COMPONENT REFERENCES**: Use PrimeFaces `p:resolveFirstComponentWithId` function for component updates: `update=":#{p:resolveFirstComponentWithId('componentId',view).clientId}"` or `render=":#{p:resolveFirstComponentWithId('pDetails',view).clientId} :#{p:resolveFirstComponentWithId('pPreview',view).clientId}"` for multiple components
18. **🚨 DATATABLE SELECTION**: Use `selectionMode="multiple"` on dataTable element, `selectionBox="true"` on column, and array property (not List) for selection binding - [Guide](developer_docs/jsf/primefaces-datatable-selection.md)
19. **🚨 JSF RENDERED ATTRIBUTE**: Never use `rendered` attribute on plain HTML elements (div, span, etc.) - JSF ignores it; use JSF components like `h:panelGroup` with `layout="block"` instead

---
This behavior should persist across all Claude Code sessions for this project.
