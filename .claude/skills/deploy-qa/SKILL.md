---
name: deploy-qa
description: >
  Sync development into QA/testing environment branches (QA1-QA4, local RH staging)
  via PR + merge on GitHub. Use when deploying/promoting the latest development
  code to any HMIS QA instance or the local RH staging environment.
disable-model-invocation: true
allowed-tools: Bash, Read, Grep
argument-hint: "[qa1|qa2|qa3|qa4|rh-local|all]"
---

# Deploy to QA / Local Staging Environments

Sync `development` into one or more QA/testing branches via a GitHub PR + merge
(not a direct push). These branches are QA/testing-only — no production traffic —
so merging is low-risk, but going through a PR still gets CI (`check-branch`) to
run before the merge lands.

## Branch Map

| Argument   | Target branch        | Notes                                   |
|------------|-----------------------|------------------------------------------|
| `qa1`      | `hims-qa1-migrated`   |                                          |
| `qa2`      | `hims-qa2-migrated`   |                                          |
| `qa3`      | `hims-qa3-migrated`   |                                          |
| `qa4`      | `hims-qa4-migrated`   |                                          |
| `rh-local` | `rh-local-staging`    | Local RH staging — does NOT share prod's `ruhunu` DB |
| `all`      | all five branches above | Run each independently; one failing doesn't block the rest |

**Do not use** the non-`-migrated` branches (`hims-qa1`, `hims-qa2`, `hims-qa4`) or
`hims-qa2-old` / `rh-stg-old` — these are stale/legacy (weeks-to-months since last
commit) and superseded by the `-migrated` branches. `hims-qa3` (no suffix) and
`rh-stg` / `rh-stg-migrated` are separate, actively-diverging lineages — do not
touch them under this skill without explicit confirmation from the user, since
they are not necessarily the same environments as `qa3` / `rh-local` above.

Note: `hims-qa1-migrated` through `hims-qa4-migrated` are sometimes synced
automatically by an external process — if `gh pr create` reports "No commits
between X and development", that branch is already up to date; skip it.

## Deployment Process (per branch)

```bash
git fetch origin

# 1. Dry-run the merge to catch conflicts before opening a PR
git merge-tree --write-tree origin/<target-branch> origin/development
# exit code 0 with a tree hash = clean; non-zero / "CONFLICT" text = stop and
# resolve manually before proceeding

# 2. Open the sync PR (development -> target branch)
gh pr create --repo hmislk/hmis --base <target-branch> --head development \
  --title "chore(sync): merge development into <target-branch>" \
  --body "Syncs the <target-branch> QA/testing environment branch with the latest development. QA/testing-only branch — safe to merge."
# If it fails with "No commits between X and development", the branch is
# already in sync — nothing more to do for it.

# 3. Confirm CI passed before merging
gh pr checks <PR-number> --repo hmislk/hmis

# 4. Merge (regular merge commit, keep both branches)
gh pr merge <PR-number> --repo hmislk/hmis --merge --delete-branch=false
```

GitHub Actions will then automatically build with Maven, deploy to the target
QA/staging server, and restart the Payara application server — **except for
`hims-qa1-migrated` through `hims-qa4-migrated`, which currently have no
matching CI/CD workflow** (`.github/workflows/hims_qa*_ci_cd.yml` still trigger
only on the non-migrated `hims-qa1`..`hims-qa4` branch names). Merging into
those four branches updates the branch content but does not deploy it — verify
with whoever owns those environments before relying on this to actually push
code live. `rh-local-staging` does have a working deploy trigger
(`ruhunu_local_server_ci_cd.yml`).

## Post-Deployment

- Monitor GitHub Actions for build status
- Check the environment is accessible after deployment
- Verify the deployed feature works as expected

## Troubleshooting

If deployment fails:
1. Check GitHub Actions logs for build errors
2. Verify `src/main/resources/META-INF/persistence.xml` uses `${JDBC_DATASOURCE}` /
   `${JDBC_AUDIT_DATASOURCE}` on these branches (not a hardcoded local JNDI name)
3. Check if the target server is accessible
4. See [QA Troubleshooting Guide](../../../developer_docs/deployment/qa-troubleshooting.md)
