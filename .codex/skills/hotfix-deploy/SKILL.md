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

## Step 1 — Stash Any Uncommitted Work

```bash
git stash
```

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

Co-Authored-By: Codex <noreply@openai.com>"
```

## Step 6 — Push

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

## Common Production Branches

| Hospital/Environment | Branch         |
|----------------------|----------------|
| COOP hospitals       | `coop-prod`    |
| Ruhunu hospital      | `ruhunu-prod`  |
| Southern Lanka       | `southernlanka-prod` |

## Reference

- [Commit Conventions — Hotfix Branches](../../../developer_docs/git/commit-conventions.md#hotfix-branches)
- [Production Deployment Guide](../../wiki-docs/Deployment/Production-Deployment-Guide.md)
