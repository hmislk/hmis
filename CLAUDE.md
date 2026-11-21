# HMIS — Claude Code Rules
🚨 = hard rule. [+] = why and steps; read it when the rule applies.

## Repository
- Repo: https://github.com/hmislk/hmis (not buddhika75/hmis). It is **public**.
- **Temp files.** `tmp/` (gitignored) holds small text files only; screenshots, PDFs, dumps go to an out-of-repo directory, never ad-hoc system `/tmp/` (the session scratchpad the harness provides is fine for throwaway files). [+](developer_docs/process/temp-files-policy.md)

## Essential Rules

### Working
- **🚨 No worktrees.** Never use `isolation: "worktree"`; edit in the main checkout; if under `.claude/worktrees/*`, stop and move out. [+](developer_docs/rules/working.md)
- **🚨 No Artifact tool for deliverables.** Write `.md` in `tmp/`, `developer_docs/` or `../hmis.wiki/`. [+](developer_docs/rules/working.md)
- **🚨 Discuss with the user only** decisions about PRODUCTION (merges, live data, live config), MONEY, CLINICAL meaning, and FLOW, DESIGN or BUSINESS-PROCESS changes. Decide every other (non-production, implementation-level) choice yourself, say so, and keep working. [+](developer_docs/rules/working.md)
- **🚨 Finish everything the request covers before stopping.** Never end a turn asking whether to do the next in-scope item. [+](developer_docs/rules/working.md)
- **🚨 Removing or hiding a feature? Query production usage on every production DB first,** before coding. [+](developer_docs/rules/working.md)
- **🚨 Live config: apply agreed values exactly,** check the deployed branch has the code, verify a new record. [+](.claude/skills/app-configuration/SKILL.md)
- **🚨 Retrospect after nontrivial work.** Propose the CLAUDE.md/doc/skill fix as a short rule and open the PR. [+](developer_docs/process/continuous-improvement-retrospective.md)
- **🚨 Write short and plain,** lead with the point, use screen words, ask one question at a time. Business-rule docs state current practice only; history and decisions go in issues/PRs. [+](developer_docs/process/writing-style.md)

### Code
- Line endings differ by machine (CRLF on Windows): edit with the Edit tool and check the diff for whole-file changes. [+](developer_docs/rules/code.md)
- **🚨 No mock data** or temporary workarounds in business logic.
- **🚨 Never "fix" intentional typos** (e.g. `purcahseRate`); they are DB column names.
- **🚨 Never rename composite components** without checking every usage.
- **🚨 Never modify or remove existing constructors;** add new ones delegating via `this(...)`. [+](developer_docs/dto/implementation-guidelines.md)
- **🚨 JPQL first.** Native SQL only for a demonstrated performance need JPQL can't meet.
- **🚨 `COUNT(...)` queries use `findLongByJpql`,** never `findDoubleByJpql`. [+](developer_docs/rules/code.md)
- **🚨 Date-time ranges: pass `TemporalType.TIMESTAMP`;** the default binds `DATE` and drops today's rows. That overload throws on a query error; check what the caller does with an exception. [+](developer_docs/rules/code.md)
- **🚨 Save a new entity with the facade's `create()`, never `edit()`.** [+](developer_docs/rules/code.md)
- **🚨 Bind id lists, not entity lists, for a root entity in JPQL** (`b.id in :billIds`). [+](developer_docs/rules/code.md)
- **🚨 Business errors from a `@Stateless` service use an `@ApplicationException(rollback = true)` exception.** [+](developer_docs/rules/code.md)
- **🚨 New report buttons on a Favorites-enabled index page go in both the category tab and the Favorites tab.** [+](developer_docs/feature/report-favorites.md)
- **🚨 Never change the meaning of an existing config key;** create a new key. [+](developer_docs/configuration/application-options.md)
- **🚨 Never change a bean's scope** without the lead developer's OK. [+](developer_docs/jsf/bean-scope-policy.md)
- **🚨 Never gate behavior on a hospital's name;** use a `ConfigOption`. [+](developer_docs/configuration/institution-specific-behavior.md)
- **🚨 Multiple Payment Methods never includes a credit method.** [+](developer_docs/billing/multiple-payment-methods-policy.md)
- **🚨 Cancellation is whole-bill only;** reversing some items is a Return/Refund. [+](developer_docs/billing/cancellation-vs-return-policy.md)
- **🚨 Inpatient medicines have exactly two workflows, both always on;** never add a config option to switch one off. [+](developer_docs/pharmacy/inpatient-medicine-issue-policy.md)

### Security
- **🚨 No hospital data in GitHub** (issues, PRs, comments, commits, screenshots): no record counts, production bill/BHT/PHN numbers or IDs, schema names, cutover dates, staff stats, patient/doctor details, or data-fix logs. Describe the defect instead; naming the reporting hospital is fine. [+](developer_docs/git/github-public-content-policy.md)
- **🚨 Never write credentials (passwords, API keys, DB users, IPs, hostnames, SSH strings) into any file in the project folder,** and never print a credentials file, even masked; pass the one value straight into the command. [+](developer_docs/rules/security.md)
- **🚨 No live-vulnerability detail on ANY public GitHub surface until the fix is deployed everywhere;** write only "hardening, tracked under #N". [+](developer_docs/rules/security.md)
- **🚨 Security PRs need a threat-model note before review:** forgery AND replay AND cross-purpose use. [+](developer_docs/rules/security.md)
- **🚨 DB dumps and backups NEVER go inside the repo tree,** even gitignored. [+](developer_docs/rules/security.md)

### Testing
- XHTML-only changes need no compile or test.
- **🚨 Never navigate to an inner page by URL;** open the app root and click through the menus. [+](developer_docs/testing/playwright-e2e-workflow.md)
- **🚨 Never clean up test data unasked;** list what you created; never write test data to production. [+](developer_docs/testing/playwright-e2e-workflow.md)

### Git and deployment
- **🚨 After every `git push`,** without being asked, restore local JNDI in `persistence.xml` and leave it unstaged: `${JDBC_DATASOURCE}` → `jdbc/coop`, `${JDBC_AUDIT_DATASOURCE}` → `jdbc/ruhunuAudit`. [+](developer_docs/deployment/persistence-verification.md)
- **🚨 Never deploy manually as root;** deploy only through GitHub Actions CI/CD. [+](developer_docs/deployment/deployment-recovery-guide.md)
- **🚨 Never `git add -A` or `git add .`;** stage explicit paths only. A scripted commit prints `git status --porcelain` and the file list, and refuses paths outside the set. [+](developer_docs/rules/git.md)
- **🚨 Hotfix branches sync `.gitignore` from `origin/development` first.** [+](.claude/skills/hotfix-deploy/SKILL.md)
- **🚨 Any task that WRITES to git runs in a dedicated worker session,** not the communication session. [+](developer_docs/process/worker-dispatch-template.md)
- **🚨 If junk or data was pushed to the public repo,** follow the `public-repo-leak-runbook` skill at once.
- Commit messages include `Closes #N`. [+](developer_docs/git/commit-conventions.md)
- **🚨 Branch from `origin/development`** and target PRs at `development`, never `master` (hotfixes: `/hotfix-deploy`). [+](developer_docs/rules/git.md)
- **🚨 Before opening a PR for issue #N, run `gh pr list --state all --search N`;** never open a second PR for it. [+](developer_docs/rules/git.md)
- **🚨 Deploy branches are `<hospital>-production` / `<hospital>-staging`;** never target `*-prod`, `*-stg` or `*-migrated`. [+](developer_docs/deployment/ci-cd-pipeline-overview.md)
- **🚨 Branches targeting a production/staging branch end in `-hotfix`.** [+](developer_docs/rules/git.md)
- **🚨 One open PR per production/staging branch;** add to its branch instead of opening another. [+](developer_docs/rules/git.md)
- **🚨 Before pushing to an existing PR's branch, run `gh pr view <N> --json state`;** if MERGED, use a new branch. [+](developer_docs/rules/git.md)
- **🚨 Dev-hub team sessions may open a PR unasked; merge into `development` only under the merge gate** (non-author `REV OK <sha> — <member>`, CI green, CodeRabbit on the head sha or `CR skipped`, threads resolved, `--match-head-commit`, never `--admin`). [+](developer_docs/git/merge-rules.md)
- **🚨 Never merge into `master` or any `*-production` / `*-staging` branch;** merging deploys, the user decides. Opening hotfix PRs is fine.
- **🚨 Mirror every merged hotfix into `development`** unless cherry-picked from it. [+](.claude/skills/hotfix-deploy/SKILL.md)
- **🚨 Dependabot PRs: green CI isn't enough;** triage with the dependabot-triage skill. [+](.claude/skills/dependabot-triage/SKILL.md)
- **🚨 After applying a CodeRabbit/Codex fix,** verify the getter names exist on the entity. [+](developer_docs/git/pr-review-workflow.md)
- **🚨 Decline bot suggestions that change design or flow, or use an off-pattern;** reply and resolve. [+](developer_docs/git/pr-review-workflow.md)

## Reference (read when relevant)
- Index of deploy, database, inward and wiki docs: [developer_docs/rules/reference-index.md](developer_docs/rules/reference-index.md)
- Migrations must detect table-name case: [guide](developer_docs/database/migration-development-guide.md)
- Wiki (`../hmis.wiki`) is for end users: [wiki rules](developer_docs/rules/wiki.md). "TIA" = Thanks In Advance.
