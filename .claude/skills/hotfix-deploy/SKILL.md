---
name: hotfix-deploy
description: >
  Full hotfix workflow for deploying urgent fixes to a production branch
  (coop-prod, ruhunu-prod, southernlanka-prod, etc.). Covers branch creation,
  fix, commit, push, and PR targeting the production branch. Use when you need
  to apply an urgent fix directly to a production environment without going
  through the normal development → QA → prod pipeline. The agent may invoke
  this on its own judgment to prepare a hotfix (branch, fix, commit, push,
  PR) — merging the PR, which is what actually triggers CI/CD deployment,
  remains a manual human action.
allowed-tools: Bash, Read, Grep, Edit, Write
argument-hint: "<prod-branch> <description>"
---

# Hotfix Deployment Workflow

Deploy an urgent fix directly to a production branch.

## Arguments

- `$0` — Target production branch, or staging branch that serves production (e.g., `coop-prod`, `ruhunu-prod`, `southernlanka-prod`, `coop-stg-migrated`, `ruhunu-prod-migrated`)
- `$1` — Short description of the fix (e.g., `sequence-preallocation`, `critical-billing-fix`)

## Critical Rule

**The branch name MUST end with `-hotfix`.**
CI merge validation will block PRs from branches that do not end with `-hotfix`.

**One open PR per production branch** (including staging branches that serve production). If a PR targeting `$0` is already
open, do NOT create a second one. Add the new fix to that PR's head branch,
push, and update the PR title/body so it lists every fix it carries (Step 1a).
Parallel PRs against the same production branch conflict with each other,
deploy in an unpredictable order, and each merge triggers a separate
production deployment.

**Confirm `$0` is the exact branch actually deployed to the environment that
needs the fix before doing anything else.** Similarly-named branches serve
different environments and are not interchangeable — e.g. for COOP,
`coop-prod`, `coop-prod-migrated` and `coop-stg-migrated` are three distinct
branches/deployments. If there is any doubt which branch backs the environment
in question, ask the user rather than guess from the name. See the Common
Production Branches table below, and verify it is still accurate before
trusting it.

## Step 1 — Stash Any Uncommitted Work

```bash
git stash
```

## Step 1a — Check for an Existing Open PR on the Target Branch

```bash
git fetch origin
gh pr list --state open --base $0 --json number,headRefName,headRepositoryOwner,isCrossRepository,title
```

- **An open PR exists:** reuse it. Check out its head branch
  (`git checkout -B <headRefName> origin/<headRefName>`), skip Step 2, apply
  the fix (Steps 3–6, pushing to `<headRefName>`), then in Step 7 run
  `gh pr edit <number> --title ... --body ...` instead of `gh pr create`, so
  the title and body cover every fix in the PR (one section per issue).
  Do not rename the branch, even if its name only describes the first fix.
  If `isCrossRepository` is `true` (the PR comes from a fork), its branch is
  not on `origin` — stop and ask the developer how to proceed.
- **No open PR:** continue with Step 2 — but first check whether a remote
  `*-hotfix` branch for this same fix already exists without an open PR
  (`git branch -r | grep -i hotfix`). If one does, find out why before
  creating a new branch: if its PR was merged, the fix may already be
  deployed; if its PR was closed unmerged, it may have been abandoned or
  targeted the wrong branch. Ask the user whether to reuse it (reopen / open a
  PR from it) or start fresh — don't silently create a parallel branch.

## Step 2 — Create Hotfix Branch from Production

```bash
git fetch origin
git checkout -b $1-hotfix origin/$0
```

Branch name format: `<description>-hotfix`

Examples:
- `sequence-preallocation-hotfix`
- `critical-billing-fix-hotfix`
- `persistence-tuning-hotfix`

## Step 3 — Apply the Fix

Make only the minimal changes required. Do NOT port unrelated features or refactors.

Before editing `persistence.xml`, compare against the target production branch to understand the current state:
```bash
git show origin/$0:src/main/resources/META-INF/persistence.xml
```

## Step 3a — Test on the Production Branch's Own Code

- Use a sibling clone, not a worktree: `git clone --branch $0 --single-branch https://github.com/hmislk/hmis.git ../$0`, then set `user.name`/`user.email` (a fresh clone can't commit). Do Steps 2–3 in the clone, so the build you test includes the fix.
- Swap in local JNDI (unstaged), build, then deploy with `asadmin --port 9048 deploy --force=true --contextroot rh --name rh-3.0.0 target/rh-3.0.0.war`. This replaces the developer's deployed build, so redeploy theirs when done.
- If the menu is empty after login, sync the `Privileges` enum (see [Playwright E2E §138](../../../developer_docs/testing/playwright-e2e/privileges-config.md)), and ask whether the sync ships in the PR.

## Step 4 — Pre-Commit Checklist

- [ ] `persistence.xml` uses `${JDBC_DATASOURCE}` / `${JDBC_AUDIT_DATASOURCE}` — not hardcoded JNDI names (e.g., `jdbc/coop`)
- [ ] No credentials or sensitive files staged
- [ ] Changes are minimal — only what is needed for the hotfix

> **Note:** The `persistence.xml` on production branches typically uses `${JDBC_DATASOURCE}` environment variable placeholders, while the local development `persistence.xml` has hardcoded JNDI names (e.g., `jdbc/coop`). Never copy the hardcoded JNDI name into the production branch.

## Step 5 — Commit

```bash
git add <changed-files>
git commit -m "fix: <description of fix>

<Explanation of why this was needed and what it fixes.>
References issues #NNNN.

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

## Step 6 — Push

If you are reusing an existing PR (Step 1a), push its head branch:
```bash
git push origin <headRefName>
```

Otherwise push the new hotfix branch:
```bash
git push origin $1-hotfix
```

## Step 7 — Create PR Targeting the Production Branch

```bash
gh pr create \
  --title "fix: <description>" \
  --base $0 \
  --head $1-hotfix \
  --body "..."
```

The PR **must** target `$0` (the production branch), not `development` or `master`.

If `gh pr edit` prints a "Projects (classic) is being deprecated" error, it changed nothing. Use `gh api -X PATCH repos/hmislk/hmis/pulls/<n> -f title=... -F body=@file` instead.

A hotfix PR is where hospital-specific detail leaks most easily, because the
whole point is that one hospital is affected. The body is public: state the
defect, the fix and the deploy note, and keep the affected-record counts,
production bill numbers, schema names and data-fix steps out of it — those go
to the developer directly or in `tmp/`. See
[What May Go Into a GitHub Issue, PR, or Comment](../../../developer_docs/git/github-public-content-policy.md).

## Step 8 — Restore Stashed Work

```bash
git checkout <your-previous-branch>
git stash pop
```

## Step 9 — Clean Up Old Branch (Post-Merge)

After the PR is merged:
```bash
git branch -d $1-hotfix
```

## Step 9a — Check Whether `development` Needs the Same Fix

**Do this for every hotfix, immediately after it merges.** A hotfix branch and
`development` drift apart the moment they diverge; skipping this step is how
they end up meaningfully different a few hotfixes later, with confusing
stale-file conflicts on the next migration or cherry-pick.

- If the hotfix's commits were cherry-picked *from* commits that already exist
  in `development` (the common case when porting an already-merged
  development fix forward to a lagging production branch), there is nothing
  to do — `development` already has it.
- If any commit was written directly on the hotfix branch (a fix invented
  there, or a follow-up discovered while hotfixing, as opposed to a
  cherry-pick), check whether `development`'s copy of the same file(s)
  already contains it:
  ```bash
  git log origin/development --oneline --grep="<distinctive phrase from the commit message>"
  ```
  If it's missing, branch off `origin/development` (a normal feature branch,
  **not** `-hotfix` — the target here is `development`, not a production
  branch), re-apply the same change, and open a separate PR targeting
  `development`. Do not fold this into the hotfix PR itself; they target
  different branches.
- Either way, say explicitly in the hotfix PR body (or in a follow-up
  comment) whether `development` already had the fix or needed a mirror PR,
  and link that PR if one was opened. This is what a reviewer or the next
  developer checks instead of re-deriving branch drift from scratch.

## Common Production Branches

Verify against `git branch -r` before trusting this table — branches get
added/renamed, and similarly-named branches are NOT interchangeable.

| Hospital/Environment | Branch | Notes |
|----------------------|--------|-------|
| COOP — pharmacy | `coop-prod`, `coop-prod-migrated` | Pharmacy production. |
| COOP — inward | `coop-stg-migrated` | Despite the "stg" name, this is what COOP inward users run against day to day. |
| Ruhunu hospital | `ruhunu-prod`, `ruhunu-prod-migrated` | |
| Southern Lanka | `southernlanka-prod`, `southernlanka-prod-migrated`, `southernlanka-stg-migrated` | Confirm which one currently serves live users before branching. |

If a hospital has more than one candidate branch and it is not obvious which
one backs the environment the user means, ask which one before branching —
guessing from the branch name alone has caused a wrong-target hotfix PR
before (it had to be closed and redone against the correct branch).

## Reference

- [Commit Conventions — Hotfix Branches](../../developer_docs/git/commit-conventions.md#hotfix-branches)
- [Production Deployment Guide](../../wiki-docs/Deployment/Production-Deployment-Guide.md)
