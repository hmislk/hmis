# Development Agent Guidelines

## High-Level Agent Roles

### Branch Management
- **Naming Convention**: Issue-based branches (`12875-implement-full-lab-workflow`)
- **PR Integration**: Auto-close with `Closes #issueNumber`
- **Status Tracking**: Project board updates

### UI/UX Development
- **Icon Management**: SVG-only, consistent sizing (80x80)
- **Theming**: Use `currentColor` for dynamic styling
- **Accessibility**: Proper tooltips and labeling

### Privilege System
- **Enum-based**: Add to `Privileges.java`
- **UI Integration**: Use `webUserController.hasPrivilege()`
- **Assignment**: Via User Privileges admin interface

### Configuration Management
- **Dynamic Values**: Use `configOptionApplicationController`
- **Color Schemes**: Centralized color configuration
- **Feature Toggles**: Boolean flags for UI elements

## Detailed Guidelines

For comprehensive implementation details, see:
- **Branch & PR Management**: [Branch workflow details](developer_docs/git/branch-management.md)
- **QA Deployment Guide**: [Bot-Friendly QA Deployment](developer_docs/deployment/qa-deployment-guide.md)
- **UI Development Handbook**: [Complete Reference](developer_docs/ui/comprehensive-ui-guidelines.md)
- **Icon & UI Standards**: [UI guidelines](developer_docs/ui/icon-management.md)
- **Privilege Implementation**: [Security patterns](developer_docs/security/privilege-system.md)
- **Configuration Patterns**: [Config usage](developer_docs/configuration/application-options.md)

## Key Principles
1. **Consistency**: Follow established patterns
2. **Security**: Validate privileges, sanitize inputs
3. **Maintainability**: Use centralized configuration
4. **Accessibility**: Ensure inclusive design
5. **JSF-only changes**: When modifying only XHTML/JSF files (no Java changes), compilation/testing is not required

---
These guidelines apply to the entire repository.

## Build & Testing
- **Detect Maven**: Prefer `./detect-maven.sh test` for running tests when needed.
- **Compile Guard**: Do not run Maven compile/build unless explicitly requested by the user.
- **When to Test**: Run tests only for Java changes and only when requested; JSF-only changes do not require builds/tests.

## Persistence & Deployment Safeguards
- **Datasource Placeholders**: Ensure `src/main/resources/META-INF/persistence.xml` uses `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}`; never commit hardcoded JNDI names.
- **No Local Paths**: Do not commit hardcoded `eclipselink.application-location` or similar local filesystem paths.
- **Pre‑Push Check**: Before pushing, verify any changes to `persistence.xml` follow the above.

## DTO Implementation
- **Non‑Breaking Changes**: Never modify existing constructors; add new ones if needed.
- **Efficient Queries**: Prefer direct DTO queries over entity‑to‑DTO conversion loops.

## UI Development Rules
- **Frontend‑Only for UI Tasks**: For UI improvements, limit work to XHTML/CSS/PrimeFaces unless backend changes are explicitly requested.
- **Reuse Controller State**: Use existing controller properties/methods; avoid introducing new filtered values/global filters or backend logic.
- **PrimeFaces Classes**: Prefer PrimeFaces button classes (e.g., `ui-button-success`, `ui-button-warning`) over Bootstrap button classes.
- **XHTML Hygiene**: Escape `&` as `&amp;` in attribute values; keep standard HTML DOCTYPE with `ui:composition` templating.
- **ERP Text Elements**: Use `h:outputText` for headings/labels rather than raw HTML heading tags.
- **Don’t Rename Components**: Avoid renaming composite components without auditing all usages across the codebase.
- **Backward Compatibility**: Do not “fix” intentional typos in model/controller property names used for DB compatibility.

## Credentials & Security
- **Never Commit Secrets**: Do not commit database credentials or sensitive configs.
- **Local Storage**: Keep DB credentials outside the repo (e.g., `C:\Credentials\credentials.txt` on Windows or `~/.config/hmis/credentials.txt` on Linux/Mac).

## Git & Project Flow
- **Commit Messages**: Use auto‑close keywords like `Closes #12345` where applicable.
- **Project Board**: Keep issue status in sync with the project board workflow.

## Wiki Writing Guidelines
- **Audience**: End users (pharmacy staff, nurses, doctors, admins), not developers.
- **Style**: User‑centric language, step‑by‑step instructions, practical examples, and actionable guidance.
- **Structure**: Overview; When to Use; How to Use; Understanding Messages; Best Practices; Troubleshooting; Configuration (user impact); FAQ.
- **Include**: UI elements/navigation, error messages and meanings, business workflows, user‑level configuration, integrations.
- **Exclude**: Code snippets, file paths, DB schema, developer debugging details, backend internals (unless writing in `developer_docs/`).
