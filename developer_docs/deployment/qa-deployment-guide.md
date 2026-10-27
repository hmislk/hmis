# QA Environment Deployment Guide

> **Rewritten 2026-10-11.** All QA instances are home-hosted. The Azure QA
> estate (`hims-qaN-migrated` branches, `qa-migrated.carecode.org`) and the
> older `hims-qaN` branches on `qa.carecode.org` are **retired**.

## How QA deployment works

Each QA instance runs on its own home machine and deploys through a
self-hosted GitHub Actions runner on that machine. Machine set-up, runbooks
and known gotchas live in the private
[`hmislk/qa-home-infra`](https://github.com/hmislk/qa-home-infra) repo.

| Environment | Branch     | Workflow                  | URL                                  | Data |
|-------------|------------|---------------------------|--------------------------------------|------|
| QA1         | `home-qa1` | `hims_qa1_home_ci_cd.yml` | https://qa1.carecode.org/qa1         | see qa-home-infra |
| QA2         | `home-qa2` | `hims_qa2_home_ci_cd.yml` | https://qa2.carecode.org/qa2         | copy of a COOP database |
| QA3         | `home-qa3` | `hims_qa3_home_ci_cd.yml` | https://qa3.carecode.org/qa3         | copy of a Southern Lanka database |
| QA4         | `home-qa4` | `hims_qa4_home_ci_cd.yml` | https://qa4.carecode.org/qa4         | see qa-home-infra |

Each workflow builds and deploys in one job on its machine's runner, and
triggers on `push` to its own branch (plus manual `workflow_dispatch`).

## Deploying

Use the **`deploy-qa`** skill (`/deploy-qa qa1`, `/deploy-qa all`), or open a
`development → home-qaN` PR and merge it. Either way the push to `home-qaN`
starts the deploy.

Manual redeploy of the current branch content:

```bash
gh workflow run hims_qa3_home_ci_cd.yml --repo hmislk/hmis --ref home-qa3
```

## Refreshing QA data

A QA database is refreshed from a production copy by the developer who
manages that machine (see the `restore-local-db` skill). Data entered by
testers before a refresh is lost from the QA instance.

A refreshed QA database **contains real patient data**. Handle it, its dump
files and any rollback copy with the same care as production data: keep them
on the QA machine owner's disk only and never share or upload them. Once
testers confirm the refreshed instance works, delete the temporary dump and
rollback files (not the live QA database). Testers get their QA logins from the QA
instance owner, not from production staff.

## persistence.xml

`development` carries the placeholders `${JDBC_DATASOURCE}` /
`${JDBC_AUDIT_DATASOURCE}`, which each workflow substitutes with its
environment's JNDI names at build time. A hardcoded local JNDI name reaching a
deploy branch is the most common cause of a post-deploy 404 — see
[qa-troubleshooting.md](qa-troubleshooting.md).

## Related

- [`deploy-qa` skill](../../.claude/skills/deploy-qa/SKILL.md) — branch map and exact steps
- [Deployment map](ci-cd-pipeline-overview.md) — every production/staging pipeline
- [persistence-verification.md](persistence-verification.md) — dev vs production JNDI settings
