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

**Do not use** the non-`-migrated` `hims-qa*` branches. The old
`hims-qa1` / `hims-qa2` / `hims-qa3` / `hims-qa4` branches (on
`qa.carecode.org`) plus `hims-qa2-old` and `rh-stg-old` were retired and
**deleted** on 2026-09-09 — they were superseded by the `-migrated` branches
and their deploy workflows were removed in PR #23634. If any reappears, it is
stale and must not be deployed to.

`rh-stg` / `rh-stg-migrated` are separate, actively-diverging lineages — do
not touch them under this skill without explicit confirmation from the user,
since they are not necessarily the same environment as `rh-local` above.

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
QA/staging server, and restart the Payara application server. Each
`hims-qaN-migrated` branch has its own working pipeline
(`.github/workflows/hims_qaN_migrated_ci_cd.yml`, all four on `development`
since PR #23634); `rh-local-staging` deploys via
`ruhunu_local_server_ci_cd.yml`. The four migrated workflows use a per-branch
concurrency group, so deploying several in one `deploy-qa all` run no longer
cancels each other (that was a real bug before #23634 — a shared
`payara-qa-migrated` group silently cancelled whichever branch merged in the
middle).

If a `hims-qaN-migrated` branch has drifted such that a `development`-headed
PR shows a merge conflict, the fix is **not** a conflict-resolution branch —
`check-branch` (`branch_merge_validation.yml`) only allows `development` as the
PR head for a `hims-qa*-migrated` base, so such a PR always fails CI. An admin
must reconcile it: temporarily add a bypass actor to the `QA Branches Rules`
ruleset (id 4778267), `git push --force origin origin/development:hims-qaN-migrated`,
then restore the ruleset to `bypass_actors: []`.

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
