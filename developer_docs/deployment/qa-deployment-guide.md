# QA Environment Deployment Guide

> **This guide was rewritten on 2026-09-09.** The old `hims-qa1` / `hims-qa2` /
> `hims-qa3` / `hims-qa4` branches on `qa.carecode.org` and the
> `git push origin development:hims-qa1 --force` flow they used are **retired**.
> QA now runs on the migrated Azure estate via the `hims-qaN-migrated` branches.

## How QA deployment works now

Four QA apps (`qa1`–`qa4`) run on **one** Payara domain on the migrated Azure
estate, served from `https://qa-migrated.carecode.org/qaN`. Each has its own
deploy pipeline:

| Environment | Branch              | Workflow                          | URL                                        |
|-------------|---------------------|-----------------------------------|--------------------------------------------|
| QA1         | `hims-qa1-migrated` | `hims_qa1_migrated_ci_cd.yml`     | https://qa-migrated.carecode.org/qa1       |
| QA2         | `hims-qa2-migrated` | `hims_qa2_migrated_ci_cd.yml`     | https://qa-migrated.carecode.org/qa2       |
| QA3         | `hims-qa3-migrated` | `hims_qa3_migrated_ci_cd.yml`     | https://qa-migrated.carecode.org/qa3       |
| QA4         | `hims-qa4-migrated` | `hims_qa4_migrated_ci_cd.yml`     | https://qa-migrated.carecode.org/qa4       |

All four workflow files live on `development` and are reviewed there like any
other code. Each triggers only on `push` to its own `hims-qaN-migrated` branch,
and each also accepts a manual `workflow_dispatch` run.

The four workflows use a **per-branch** concurrency group
(`payara-qa-migrated-${{ github.ref_name }}`), so deploying one environment
never cancels another — but a rapid re-push to the *same* branch still queues
rather than aborting a deploy in flight.

## Deploying

Use the **`deploy-qa`** skill — it opens a `development → hims-qaN-migrated`
PR, waits for `check-branch` to pass, and merges it (a merge commit, both
branches kept). GitHub Actions then builds with Maven, deploys to the target
QA server, and restarts Payara.

```
/deploy-qa qa1        # one environment
/deploy-qa all        # rh-local-staging + all four hims-qaN-migrated
```

Manual single deploy without a code change (redeploy current branch content):

```bash
gh workflow run hims_qa2_migrated_ci_cd.yml --repo hmislk/hmis --ref hims-qa2-migrated
```

### Merge rules (`branch_merge_validation.yml`)

- Only `development` may merge into a `hims-qaN-migrated` branch.
- A conflict-resolution or sync branch as the PR head will **fail
  `check-branch`** — the head must be `development`. If a `hims-qaN-migrated`
  branch has drifted such that `development` no longer merges cleanly, an
  admin must reconcile it (temporarily bypass the `QA Branches Rules` ruleset
  and `git push --force origin origin/development:hims-qaN-migrated`), not
  work around the merge check.

## persistence.xml

Feature branches keep the local JNDI names (`jdbc/coop`, `jdbc/ruhunuAudit`);
`development` carries the CI/CD placeholders `${JDBC_DATASOURCE}` /
`${JDBC_AUDIT_DATASOURCE}`, which each workflow substitutes for its
environment's real datasource JNDI names at build time. A hardcoded local JNDI
name reaching a QA branch is the most common cause of a post-deploy 404 — see
[qa-troubleshooting.md](qa-troubleshooting.md).

## Related

- [`deploy-qa` skill](../../.claude/skills/deploy-qa/SKILL.md) — the branch map and the exact PR/merge steps
- [qa-troubleshooting.md](qa-troubleshooting.md) — 404s, JNDI misconfiguration, health checks
- [persistence-verification.md](persistence-verification.md) — dev vs production JNDI settings
