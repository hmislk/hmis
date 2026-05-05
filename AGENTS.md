# Codex Configuration for HMIS Project

## Repository Information
- **GitHub Repository**: https://github.com/hmislk/hmis
- **Issues URL**: https://github.com/hmislk/hmis/issues
- **Project tmp Folder**: Use `tmp/` inside project root (`<project-root>/tmp/`), not system `/tmp/`.

## Essential Rules (Always Apply)

### Working Directory
0. **🚨 NEVER USE WORKTREE ISOLATION**: Always work directly in the main project checkout directory.

### User Control
1. **🚨 NO AUTO-ACTIONS**: Do not commit, build, run, or push unless explicitly requested.
2. **🚨 EXPLICIT COMMANDS ONLY**: Wait for user confirmation before Git operations, Maven builds, or deployments.
3. **🚨 NO AUTO-COMPILE**: Never run Maven compile unless explicitly requested.

### Code Integrity
4. **🚨 NO MOCK DATA**: Never use mock bills, fake entities, or temporary business-logic workarounds.
5. **🚨 DISCUSS UNCERTAINTIES**: Discuss with user when uncertain about implementation approach.
6. **🚨 BACKWARD COMPATIBILITY**: Never “fix” intentional typos (e.g., `purcahseRate`) used for DB compatibility.
7. **🚨 COMPONENT NAMING**: Never rename composite components without checking all usages.
8. **🚨 NEVER MODIFY EXISTING CONSTRUCTORS**: Only add new constructors; do not change/remove existing signatures.
9. **🚨 JPQL FIRST, NATIVE SQL LAST**: Use JPQL by default; use native SQL only with demonstrated constraints.
10. **🚨 USE `findLongByJpql` FOR COUNT QUERIES**: JPQL `COUNT(...)` should use `findLongByJpql`.

### Deployment
11. **🚨 NEVER DEPLOY MANUALLY AS ROOT**: Never use root/sudo for Payara deployment paths; use CI/CD.

### Git & Branching
12. **Issue Auto-Close**: Include issue closing keywords (`Closes #N`) in commits/PRs.
13. **JSF-only changes**: XHTML/JSF-only changes do not require compile/tests.
14. **🚨 BASE FEATURE BRANCHES ON `development`**.
15. **🚨 `development` IS DEFAULT PR TARGET** (not `master`).
16. **🚨 HOTFIX BRANCHES MUST END WITH `-hotfix`** for production branch targets.

---
These guidelines apply to the entire repository.

## Build & Testing
- **Detect Maven**: Prefer `./detect-maven.sh test` when tests are needed.
- **Compile Guard**: Do not run Maven compile/build unless explicitly requested.
- **When to Test**: Run tests for Java changes only when requested; JSF-only changes do not require tests.

## Persistence & Deployment Safeguards
- Ensure `src/main/resources/META-INF/persistence.xml` uses `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}`.
- Never commit hardcoded JNDI names or local `eclipselink.application-location` paths.
- Before push, verify any `persistence.xml` changes meet these rules.

## UI Development Rules
- For UI improvements, stay frontend-only (XHTML/CSS/PrimeFaces) unless backend is explicitly requested.
- Reuse existing controller state/methods; avoid new backend/global filter logic unless requested.
- Prefer PrimeFaces button classes (`ui-button-success`, `ui-button-warning`) over Bootstrap button classes.
- Escape `&` as `&amp;` in attribute values; keep standard HTML DOCTYPE with `ui:composition`.
- Use `h:outputText` for headings/labels in ERP pages.

## Security & Credentials
- Never commit secrets or database credentials.
- Keep credentials outside repo (`~/.config/hmis/credentials.txt` or equivalent).

## Wiki Writing Guidelines
- Audience: End users (pharmacy staff, nurses, doctors, admins), not developers.
- Style: User-centric, step-by-step, practical examples, actionable guidance.
- Structure: Overview → When to Use → How to Use → Messages → Best Practices → Troubleshooting → Configuration → FAQ.
- Exclude deep developer internals unless writing in `developer_docs/`.

## Reference Docs
- Branch workflow: `developer_docs/git/branch-management.md`
- Commit conventions: `developer_docs/git/commit-conventions.md`
- QA deployment: `developer_docs/deployment/qa-deployment-guide.md`
- UI handbook: `developer_docs/ui/comprehensive-ui-guidelines.md`
- Icon standards: `developer_docs/ui/icon-management.md`
- Privilege system: `developer_docs/security/privilege-system.md`
- DTO guidelines: `developer_docs/dto/implementation-guidelines.md`
- PR review workflow: `developer_docs/git/pr-review-workflow.md`
- REST API dev guide: `developer_docs/api/rest-api-development-guide.md`
- Wiki publishing: `developer_docs/github/wiki-publishing.md`

---
This behavior should persist across all Codex sessions for this project.
