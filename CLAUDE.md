# HMIS — Claude Code Rules

## Repository
- Repo: https://github.com/hmislk/hmis (not buddhika75/hmis). It is **public**.
- Temp files go in `<project-root>/tmp/`, never system `/tmp/`.

## Essential Rules

### Working
- **🚨 No worktrees.** Never use `isolation: "worktree"`; edit in the main checkout. If you are under `.claude/worktrees/*`, stop and move there. (Worktrees desync the developer's branch.)
- **🚨 No Artifact tool for deliverables.** Recipients can't open them without a Claude account. Write `.md` in `tmp/` (working), `developer_docs/` (tracked) or `../hmis.wiki/` (wiki; never inside this repo).
- **🚨 Discuss uncertainties** about the implementation approach with the user before coding.
- **🚨 Retrospect after nontrivial work.** If a documented fact would have prevented a mistake, propose the CLAUDE.md/doc/skill fix and ask before opening a PR. Write it as a short rule, not a story. See [Retrospective](developer_docs/process/continuous-improvement-retrospective.md).

### Code
- **🚨 No mock data** or temporary workarounds in business logic.
- **🚨 Never "fix" intentional typos** (e.g. `purcahseRate`); they are DB column names.
- **🚨 Never rename composite components** without checking every usage.
- **🚨 Never modify or remove existing constructors.** Only add new ones, delegating via `this(...)`. See [DTO Guidelines](developer_docs/dto/implementation-guidelines.md).
- **🚨 JPQL first.** Native SQL only for a demonstrated performance need JPQL can't meet.
- **🚨 `COUNT(...)` queries use `findLongByJpql`**, never `findDoubleByJpql` (silently returns 0.0).
- **🚨 New report buttons on a Favorites-enabled index page** (e.g. `reports/index.xhtml`) go in both the category tab and the ⭐ Favorites tab. Read [Report Favorites](developer_docs/feature/report-favorites.md) first.
- **🚨 Never gate behavior on a hospital's name** (`applicationInstitution eq 'Ruhuna'` etc.). Use a `ConfigOption`. See [Institution-Specific Behavior](developer_docs/configuration/institution-specific-behavior.md). Fix existing violations only when already touching that code.
- **🚨 Cancellation is whole-bill only.** Never build item-level cancellation; reversing some items is a Return/Refund, which most bill types already have. See [Cancellation vs. Return](developer_docs/billing/cancellation-vs-return-policy.md).

### persistence.xml
- **🚨 After every `git push`**, without being asked, restore local JNDI and leave it unstaged: `${JDBC_DATASOURCE}` → `jdbc/coop`, `${JDBC_AUDIT_DATASOURCE}` → `jdbc/ruhunuAudit`.

### Security
- **🚨 No hospital data in GitHub** (issues, PRs, comments, commits, screenshots): no record counts, production bill/BHT/PHN numbers or IDs, schema names, cutover dates, staff stats, patient/doctor details, or data-fix logs. Describe the defect instead; naming the reporting hospital is fine. See [Public Content Policy](developer_docs/git/github-public-content-policy.md).
- **🚨 Never write credentials** (passwords, API keys, DB users, IPs, hostnames, SSH strings) into any file in the project folder, tracked or not. Point to the external credentials file instead.

### Deployment
- **🚨 Never deploy manually as root.** Deploy only through GitHub Actions CI/CD; if a manual fix is unavoidable, use `appuser`. See [Deployment Recovery](developer_docs/deployment/deployment-recovery-guide.md).

### Testing
- XHTML-only changes need no compile or test.
- **🚨 Never navigate to an inner page by URL.** Open only the app root, then click through the menus. A URL-loaded page shows artifacts, not defects; a page with no menu path is itself the finding. Record the menu path in the issue/PR. See [Playwright E2E §2](developer_docs/testing/playwright-e2e-workflow.md#-never-navigate-by-typing-a-page-url).
- **🚨 Never clean up test data unasked.** Leave records in place and list what you created. Never write test data to production without confirming the environment. See [§15a](developer_docs/testing/playwright-e2e-workflow.md#15a-leave-test-data-where-it-is--never-clean-up-unasked).

### Git
- Commit messages include `Closes #N`. See [Commit Conventions](developer_docs/git/commit-conventions.md).
- **🚨 Branch from `origin/development`** and target PRs at `development`, never `master`. Check for existing code against `origin/development`.
- **🚨 Branches targeting a production/staging branch end in `-hotfix`** (CI blocks others). Use the `/hotfix-deploy` skill.
- **🚨 One open PR per production/staging branch.** Check `gh pr list --state open --base <branch>`; if one exists, add to its branch and update its title/body.
- **🚨 Mirror every merged hotfix into `development`** unless its commits were cherry-picked from `development` (hotfix-deploy Step 9a).
- **🚨 After applying a CodeRabbit/Codex fix**, verify the getter names exist on the entity (e.g. `isCompleted()`, not `getCompleted()`). See [PR Review §4a](developer_docs/git/pr-review-workflow.md).

## Reference (read when relevant)
- Persistence/deployment: [JNDI dev vs prod](developer_docs/deployment/persistence-verification.md) · [Windows remote access](developer_docs/deployment/windows-remote-access-tips.md) · [Migrating a stale hospital DB](developer_docs/deployment/migrating-a-stale-hospital-to-development.md) (always E2E-test a real OPD bill settle)
- Migrations must detect table-name case: [Migration Guide § case sensitivity](developer_docs/database/migration-development-guide.md#cross-deployment-case-sensitivity-must)
- Excel export of HTML tables: [guide](developer_docs/feature/excel-export-html-table.md)
- Inward: [navigation](developer_docs/navigation/inward_navigation.md) · [CC settlement](developer_docs/billing/inward-cc-settlement-tracking.md)
- User docs go to `../hmis.wiki`, written for end users (pharmacy staff, nurses, doctors, admins).
- "TIA" = Thanks In Advance.
