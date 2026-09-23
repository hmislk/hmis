---
name: hotfix-deploy
description: >
  Full hotfix workflow for deploying urgent fixes to a production branch
  (coop-prod, ruhunu-prod, southernlanka-prod, etc.). Covers branch creation,
  fix, commit, push, and PR targeting the production branch. Use when you need
  to apply an urgent fix directly to a production environment without going
  through the normal development → QA → prod pipeline.
disable-model-invocation: true
allowed-tools: Bash, Read, Grep, Edit, Write
argument-hint: "<prod-branch> <description>"
---

# Hotfix Deployment Workflow

Deploy an urgent fix directly to a production branch.

## Arguments

- `$0` — Target production branch (e.g., `coop-prod`, `ruhunu-prod`, `southernlanka-prod`)
- `$1` — Short description of the fix (e.g., `sequence-preallocation`, `critical-billing-fix`)

## Critical Rule

**The branch name MUST end with `-hotfix`.**
CI merge validation will block PRs from branches that do not end with `-hotfix`.

**Confirm `$0` is the exact branch actually deployed to the environment that
needs the fix before doing anything else.** Similarly-named branches serve
different environments and are not interchangeable — e.g. for COOP,
`coop-prod`, `coop-prod-migrated`, and `coop-stg-migrated` are three distinct
branches/deployments; `coop-stg-migrated` is the one COOP inward users
actually run against, while `coop-prod`/`coop-prod-migrated` serve pharmacy.
If there's any doubt which branch backs the environment in question, ask the
user rather than guess from the name. See the Common Production Branches
table below, and verify it's still accurate before trusting it.

## Step 1 — Check for an Existing Hotfix First

**Never create a second open hotfix PR/branch for the same target branch and
same fix.** Downtime windows for production deploys are limited, and stacking
multiple hotfix PRs against one branch multiplies deploy/downtime events for
no reason. Before creating anything, search for one already in flight:

```bash
git fetch origin
gh pr list --repo hmislk/hmis --state open --base $0 \
  --json number,title,headRefName,url
git branch -r | grep -i "hotfix"
```

- **No open PR targets `$0`** → proceed to Step 2, create a new hotfix branch.
- **An open PR already targets `$0` for this same fix** (same bug/issue, or
  clearly overlapping description) → **do not open a new PR.** Check out that
  PR's existing branch instead, add the fix as a new commit on it, push, and
  let the existing PR pick up the update. Tell the user you found and reused
  PR #NNNN rather than creating a new one.
- **An open PR targets `$0` for an unrelated fix** → tell the user before
  proceeding; they may want the new fix batched onto that same open PR (fewer
  deploys) rather than opened separately, since either choice affects the
  downtime count.
- If unsure whether an existing open PR is "the same fix" or "unrelated," ask
  the user — don't guess silently either way.

## Step 2 — Stash Any Uncommitted Work

```bash
git stash
```

## Step 3 — Create Hotfix Branch from Production

```bash
git fetch origin
git checkout -b $1-hotfix origin/$0
```

Branch name format: `<description>-hotfix`

Examples:
- `sequence-preallocation-hotfix`
- `critical-billing-fix-hotfix`
- `persistence-tuning-hotfix`

Skip this step entirely if Step 1 found an existing PR/branch to reuse —
check out that branch instead (`git checkout <existing-branch>`) and go
straight to Step 4.

## Step 4 — Apply the Fix

Make only the minimal changes required. Do NOT port unrelated features or refactors.

Before editing `persistence.xml`, compare against the target production branch to understand the current state:
```bash
git show origin/$0:src/main/resources/META-INF/persistence.xml
```

## Step 5 — Pre-Commit Checklist

- [ ] `persistence.xml` uses `${JDBC_DATASOURCE}` / `${JDBC_AUDIT_DATASOURCE}` — not hardcoded JNDI names (e.g., `jdbc/coop`)
- [ ] No credentials or sensitive files staged
- [ ] Changes are minimal — only what is needed for the hotfix

> **Note:** The `persistence.xml` on production branches typically uses `${JDBC_DATASOURCE}` environment variable placeholders, while the local development `persistence.xml` has hardcoded JNDI names (e.g., `jdbc/coop`). Never copy the hardcoded JNDI name into the production branch.

## Step 6 — Commit

```bash
git add <changed-files>
git commit -m "fix: <description of fix>

<Explanation of why this was needed and what it fixes.>
References issues #NNNN.

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

If reusing an existing branch/PR found in Step 1, this commit is added on top
of what's already there — do not rewrite or squash the existing history.

## Step 7 — Push

```bash
git push origin $1-hotfix
```

(If reusing an existing branch, push to that branch's actual name instead of
`$1-hotfix`.)

## Step 8 — Create or Update the PR Targeting the Production Branch

**If Step 1 found no existing PR to reuse**, create one:

```bash
gh pr create \
  --title "fix: <description>" \
  --base $0 \
  --head $1-hotfix \
  --body "..."
```

The PR **must** target `$0` (the production branch), not `development` or `master`.

**If Step 1 found an existing PR to reuse**, do not run `gh pr create` —
the push in Step 7 already updated it. Optionally leave a short comment
(`gh pr comment <number> --body "..."`) noting what was added and why, so the
PR's history stays clear for the reviewer.

A hotfix PR is where hospital-specific detail leaks most easily, because the
whole point is that one hospital is affected. The body is public: state the
defect, the fix and the deploy note, and keep the affected-record counts,
production bill numbers, schema names and data-fix steps out of it — those go
to the developer directly or in `tmp/`. See
[What May Go Into a GitHub Issue, PR, or Comment](../../../developer_docs/git/github-public-content-policy.md).

## Step 9 — Restore Stashed Work

```bash
git checkout <your-previous-branch>
git stash pop
```

## Step 10 — Clean Up Old Branch (Post-Merge)

After the PR is merged:
```bash
git branch -d $1-hotfix
```

## Common Production Branches

Verify against `git branch -r` before trusting this table — branches get
added/renamed, and similarly-named branches are NOT interchangeable.

| Hospital/Environment | Branch | Notes |
|----------------------|--------|-------|
| COOP — pharmacy | `coop-prod`, `coop-prod-migrated` | Pharmacy production. |
| COOP — inward | `coop-stg-migrated` | Despite the "stg" name, this is what COOP inward users run against day to day. Confirmed 2026-09-23. |
| Ruhunu hospital | `ruhunu-prod` | |
| Southern Lanka | `southernlanka-prod` | |

If a hospital has more than one candidate branch and it's not obvious which
one backs the environment the user means, ask which one before branching —
guessing from the branch name alone has caused a wrong-target hotfix PR
before (had to be closed and redone against the correct branch).

## Reference

- [Commit Conventions — Hotfix Branches](../../developer_docs/git/commit-conventions.md#hotfix-branches)
- [Production Deployment Guide](../../wiki-docs/Deployment/Production-Deployment-Guide.md)
