# Claude Code Configuration for HMIS Project

## Repository Information
- **GitHub Repository**: https://github.com/hmislk/hmis (not buddhika75/hmis)
- **Issues URL**: https://github.com/hmislk/hmis/issues
- **Project tmp Folder**: `tmp/` directory **inside the project root** (i.e., `<project-root>/tmp/`) for project-specific temporary files. Do NOT use the system `/tmp/` directory.

## Essential Rules (Always Apply)

### Working Directory
- **🚨 NEVER USE WORKTREE ISOLATION**: Always work directly in the main project checkout directory. Do NOT use `isolation: "worktree"` when spawning agents. If you find yourself in a path like `.claude/worktrees/*`, stop and perform all file edits in the main project directory instead. Worktrees cause the developer's local branch to go out of sync with remote commits, leading to confusing stale-file compilation errors. (Issue: hmislk/hmis#19944)

### Code Integrity
- **🚨 NO MOCK DATA**: NEVER use mock bills, fake entities, or temporary workarounds in business logic
- **🚨 DISCUSS UNCERTAINTIES**: ALWAYS discuss with user when uncertain about implementation approach
- **🚨 BACKWARD COMPATIBILITY**: Never "fix" intentional typos (e.g., `purcahseRate`) - database compatibility
- **🚨 COMPONENT NAMING**: Never rename composite components without checking ALL usage
- **🚨 NEVER MODIFY EXISTING CONSTRUCTORS**: Only ADD new constructors. Changing or removing existing constructor signatures breaks other callers. New constructors should delegate to the existing one via `this(...)` when possible. See [DTO Guidelines](developer_docs/dto/implementation-guidelines.md)
- **🚨 JPQL FIRST, NATIVE SQL LAST**: Always use JPQL for database queries. Native SQL (`nativeScalarQuery`, `executeNativeSql`) is only permitted when there is a significant, demonstrated performance constraint that JPQL cannot address. Never reach for native SQL just because JPQL is harder to write.
- **🚨 USE `findLongByJpql` FOR COUNT QUERIES**: Always use `findLongByJpql` (not `findDoubleByJpql`) for JPQL `COUNT(...)` queries. `COUNT` returns a `Long`; using `findDoubleByJpql` causes a silent `ClassCastException` caught internally, returning `0.0` every time and making the check always pass.
- **🚨 WIRE NEW REPORT BUTTONS INTO REPORT FAVORITES**: Any report/analytics index page that has adopted the self-service Favorites mechanism (star toggle + pinned ⭐ Favorites tab — currently `reports/index.xhtml`, being rolled out to other Analytics pages) MUST have every new report button added to BOTH its home category tab AND the Favorites tab. Read [Report Favorites Implementation Guide](developer_docs/feature/report-favorites.md) first — it covers the mandatory `reportKey` global-uniqueness rule, the exact markup pattern (`h:panelGroup`, never a raw `<div>`, for the Favorites-tab row), and the per-page empty-state gotcha.
- **🚨 NEVER HARDCODE A HOSPITAL'S NAME TO GATE BEHAVIOR**: Do NOT write `sessionController.userPreference.applicationInstitution eq 'Ruhuna'` (or `'Cooperative'`, `'Arogya'`, any other institution name) in XHTML `rendered`/Java conditionals to turn a feature on/off for one hospital. Use a `ConfigOption` boolean/value instead (resolved per-department-first, same as everywhere else) so an admin can toggle it without a code change or deploy, and so a hospital rename doesn't silently kill the branch. Read [Institution-Specific Behavior](developer_docs/configuration/institution-specific-behavior.md) first — it documents why (135 existing violations across 35 files as of 2026-08-31) and the exact pattern to use instead. Existing violations are tech debt to fix opportunistically when you're already touching that code, not something to mass-refactor uninvited.

### persistence.xml — Local JNDI Lifecycle
- **🚨 RESTORE LOCAL JNDI AFTER EVERY PUSH**: Immediately after every `git push`, replace the CI/CD placeholders in `persistence.xml` back to the local JNDI names — `${JDBC_DATASOURCE}` → `jdbc/coop` and `${JDBC_AUDIT_DATASOURCE}` → `jdbc/ruhunuAudit`. Leave the change **unstaged**. Do this without being asked. The developer needs local Payara to connect right away for testing.

### Security — Credentials & Sensitive Data
- **🚨 NEVER COMMIT CREDENTIALS OR SENSITIVE DATA**: Do NOT write passwords, API keys, database usernames, IP addresses, hostnames, or SSH connection strings into any file inside the project folder — including `developer_docs/`, `tmp/`, `wiki-docs/`, migration scripts, or any other tracked or untracked file. These belong exclusively in secure storage **outside** the project directory (e.g. `C:\Credentials\`). If a doc needs to reference how to connect to a database, write a generic description and point to the external credentials file — never inline the actual values.

### Deployment
- **🚨 NEVER DEPLOY MANUALLY AS ROOT**: NEVER use `sudo` or root to copy WARs, run `asadmin`, or touch Payara's application/log directories directly. Root-owned files in `/opt/payara5/glassfish/domains/domain1/` (applications, generated, logs) block all future CI/CD deployments — `asadmin undeploy` and `deploy` will fail with permission errors. **All deployments MUST go through GitHub Actions CI/CD.** If a manual fix is absolutely needed, use `appuser` only. See [Deployment Recovery Guide](developer_docs/deployment/deployment-recovery-guide.md).

### Testing
- **JSF-only changes** (XHTML only, no Java) do not require compilation or testing

### Git & Branching
- **Include issue closing keywords** (`Closes #N`) in commit messages
- **🚨 ALWAYS BASE FEATURE BRANCHES ON `development`**: When creating a new local branch for feature development, ALWAYS branch from `origin/development`, NEVER from `master`. The `master` branch is managed exclusively by system admins. Use: `git checkout -b <branch-name> origin/development`
- **🚨 `development` IS THE DEFAULT BRANCH**: All PRs MUST target `development`, NOT `master`. When checking what already exists in the codebase (to avoid duplicate fields/methods), ALWAYS compare against `origin/development`, not `origin/master`. The CI validates against `development`. Never reference or merge into `master` during feature development.
- **🚨 HOTFIX BRANCHES MUST END WITH `-hotfix`**: When creating a branch targeting a production branch (e.g., `coop-prod`, `ruhunu-prod`, `southernlanka-prod`), the branch name **MUST** end with `-hotfix`. CI merge validation will block PRs from branches that do not end with `-hotfix`. Format: `<description>-hotfix` (e.g., `sequence-preallocation-hotfix`, `critical-billing-fix-hotfix`). See the `/hotfix-deploy` skill.

## Situational Guidelines (Reference When Needed)

### When Working on Persistence/Deployment
- [Persistence Configuration Guide](developer_docs/deployment/persistence-verification.md) - JNDI settings for dev vs production
- [Deployment Recovery Guide](developer_docs/deployment/deployment-recovery-guide.md) - How to recover when root-owned files break CI/CD deployment
- [Windows Remote Access Tips](developer_docs/deployment/windows-remote-access-tips.md) - SSH agent gotchas, Payara admin console over a tunnel, driving remote `asadmin` from a Windows dev machine

### When Working on Database
- [Migration Development Guide § Cross-deployment case sensitivity](developer_docs/database/migration-development-guide.md#cross-deployment-case-sensitivity-must) - Migration scripts must detect actual table-name case via `INFORMATION_SCHEMA` + prepared statements; hardcoding either `UPPER` or `lower` breaks half the customer DBs. Reference: `v2.1.12/migration-universal.sql`, `v2.1.17/migration.sql`.

### When Adding Excel Export to a Report
- [Excel Export for HTML Tables](developer_docs/feature/excel-export-html-table.md) - Pattern for exporting HTML-based (non-DataTable) report tables to Excel using Apache POI via `HttpServletResponse`

### When Creating User Documentation
- **Wiki Location**: `../hmis.wiki` sibling directory (NEVER inside the main project repo)
- **Target Audience**: End users (pharmacy staff, nurses, doctors, administrators)

### When Working on Inward / Inpatient Module
- [Inward Navigation & Reference](developer_docs/navigation/inward_navigation.md) - Pages, controllers, workflow, open issues
- [Inward CC Settlement Tracking](developer_docs/billing/inward-cc-settlement-tracking.md) - Data model, settlement paths, cancellation flows, and debtor report pattern for inpatient credit company payments

### When Reviewing a PR
- **🚨 AFTER APPLYING ANY CODERABBIT/CODEX FIX**: Always verify method names exist on the actual entity before pushing. Automated tools frequently generate wrong getter names (e.g., `getCompleted()` instead of `isCompleted()` for primitive `boolean` fields). See [PR Review Workflow §4a](developer_docs/git/pr-review-workflow.md).

### When Committing Code
- [Commit Conventions](developer_docs/git/commit-conventions.md) - Message format

### When Creating a Hotfix for a Production Branch
- **Branch name MUST end with `-hotfix`** (see § Git & Branching above) — CI blocks merges otherwise
- Use the `/hotfix-deploy` skill to run the full workflow: branch from prod → fix → commit → push → PR targeting prod branch
- [Commit Conventions — Hotfix Branches](developer_docs/git/commit-conventions.md#hotfix-branches) - naming format and examples

## Common Abbreviations & Terms
- **TIA**: Thanks In Advance

---
This behavior should persist across all Claude Code sessions for this project.
