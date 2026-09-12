---
name: deploy-qa
description: >
  Sync development into QA/testing environment branches (QA1-QA4, local RH staging)
  via PR + merge on GitHub. Use when deploying/promoting the latest development
  code to any HMIS QA instance or the local RH staging environment.
disable-model-invocation: true
allowed-tools: Bash, Read, Grep
argument-hint: "[qa1|qa2|qa3|qa4|rh-local|all|qa1-azure|qa2-azure|qa3-azure|qa4-azure]"
---

# Deploy to QA / Local Staging Environments

QA1-QA4 moved off the Azure "migrated" estate onto home-hosted machines
starting 2026-09-12 (tracked in the private
[`hmislk/qa-home-infra`](https://github.com/hmislk/qa-home-infra) repo —
`MASTER-PLAN.md` for status, `GOTCHAS.md` for detail). **The plan is to
retire the Azure targets entirely once cutover completes** — there will be
no more Azure QA environments. `qa1`-`qa4` below now mean the home
instances; the old Azure branches are kept only as `*-azure` arguments
until the `qa-home-infra` Cutover checklist is fully ticked, at which point
this skill should be edited again to remove the `*-azure` section outright.

`rh-local` is unaffected by any of this — same branch, same workflow, no
change.

## Branch Map

| Argument   | Target branch        | Notes                                   |
|------------|-----------------------|------------------------------------------|
| `qa1`      | `home-qa1`            | hiu-laptop / carecode account — self-hosted runner `qa1` |
| `qa2`      | `home-qa2`            | hiu-laptop / buddhika account — self-hosted runner `qa2` |
| `qa3`      | `home-qa3`            | Desktop (Windows) — self-hosted runner `qa3`; Desktop also runs nginx for all four public hostnames |
| `qa4`      | `home-qa4`            | carecode-laptop (Windows) — self-hosted runner `qa4` |
| `rh-local` | `rh-local-staging`    | Local RH staging — does NOT share prod's `ruhunu` DB, unaffected by the QA migration |
| `all`      | all four `home-qaN` + `rh-local-staging` | Run each independently; one failing doesn't block the rest |
| `qa1-azure` … `qa4-azure` (**deprecated**) | `hims-qa1-migrated` … `hims-qa4-migrated` | Legacy Azure estate. Use only if a home instance is genuinely unreachable. Will be deleted from this skill once `qa-home-infra`'s Cutover checklist is complete. |

**Do not use** the bare non-`-migrated`/non-`home-` `hims-qa*` branches, and
never target `hims-qaN-home` (singular, no trailing `-migrated`/`-azure`) —
that pattern collides with the repo's "QA Branches Rules" ruleset (PR-only
pushes + a `check-branch` status check that's only ever attached via a PR),
so a branch under it can never even be created by a direct push. The
correct home branch name is `home-qaN`, not `hims-qaN-home`.

## Deployment Process — home targets (`qa1`-`qa4`, default)

Home branches carry **no branch protection** (that's deliberate — see the
naming note above), and they only ever exist to mirror `development` and
fire the deploy workflow on push. There is no PR/merge step and no
`check-branch` wait:

```bash
git fetch origin

# Mirror development onto the target home branch. --force-with-lease is
# safe here even though these branches have no protection: it just means
# the push aborts (instead of silently clobbering) if someone else moved
# the branch since your last fetch.
git push origin origin/development:home-qa1 --force-with-lease
```

If it prints `Everything up-to-date`, the branch already matches
`development` — nothing to deploy, skip it (same idea as the Azure flow's
"No commits between X and development").

The push itself triggers `hims_qa1_home_ci_cd.yml` (swap the number for
`qa2`/`qa3`/`qa4`). Watch it:

```bash
gh run list --repo hmislk/hmis --workflow=hims_qa1_home_ci_cd.yml --limit 1
gh run watch --repo hmislk/hmis <run-id> --exit-status
```

Each home workflow builds and deploys **in one job, entirely on that
machine's own self-hosted runner** (labels `qa1`/`qa2`/`qa3`/`qa4`) — no
artifact upload/download, so a deploy is a couple of minutes end to end,
not the 20-30 minutes an artifact-transfer design costs over a home
connection (measured and fixed per-instance; see `qa-home-infra`'s
GOTCHAS.md § "deploys take ~30 min on a home link" if a `home-qaN` workflow
still shows the old split-job design — it should have been migrated to the
local-build pattern already, but if not, that section has the fix).

Each workflow keeps its own rollback copy of the previously-deployed WAR
and auto-restores it if the new deploy or its health check fails — no
manual rollback step needed on a failed run, but do read the run's log if
one fails rather than assuming it self-healed correctly.

## Deployment Process — legacy Azure targets (`qa1-azure` … `qa4-azure`)

Same PR + merge flow as before this migration:

```bash
git fetch origin

# 1. Dry-run the merge to catch conflicts before opening a PR
git merge-tree --write-tree origin/hims-qa1-migrated origin/development
# exit code 0 with a tree hash = clean; non-zero / "CONFLICT" text = stop and
# resolve manually before proceeding

# 2. Open the sync PR (development -> target branch)
gh pr create --repo hmislk/hmis --base hims-qa1-migrated --head development \
  --title "chore(sync): merge development into hims-qa1-migrated" \
  --body "Syncs the hims-qa1-migrated QA/testing environment branch with the latest development. QA/testing-only branch — safe to merge."
# If it fails with "No commits between X and development", the branch is
# already in sync — nothing more to do for it.

# 3. Confirm CI passed before merging
gh pr checks <PR-number> --repo hmislk/hmis

# 4. Merge (regular merge commit, keep both branches)
gh pr merge <PR-number> --repo hmislk/hmis --merge --delete-branch=false
```

GitHub Actions then builds with Maven, deploys to the Azure estate, and
restarts Payara. The four migrated workflows use a per-branch concurrency
group, so deploying several in one `deploy-qa all` run doesn't cancel each
other.

If a `hims-qaN-migrated` branch has drifted such that a `development`-headed
PR shows a merge conflict, the fix is **not** a conflict-resolution branch —
`check-branch` (`branch_merge_validation.yml`) only allows `development` as
the PR head for a `hims-qa*-migrated` base, so such a PR always fails CI. An
admin must reconcile it: temporarily add a bypass actor to the `QA Branches
Rules` ruleset (id 4778267), then
`git fetch origin && git push --force-with-lease origin origin/development:hims-qaN-migrated`
(lease-protected so a concurrent update to the target branch aborts the push
instead of being silently discarded), then restore the ruleset to
`bypass_actors: []`.

## Post-Deployment

- Monitor GitHub Actions for build status
- Check the environment is accessible after deployment (for home targets:
  the machine's local URL, e.g. `http://localhost:9080/qa1` for QA1 — the
  public `qaN.carecode.org` URL additionally depends on the Desktop's nginx
  being correctly wired for that instance)
- Verify the deployed feature works as expected — log in through the real
  UI and check a real screen against real data, not just a health-check
  page render (a stale local DB or a cross-DB credential mismatch has bitten
  every home instance at least once during setup; see `qa-home-infra`'s
  GOTCHAS.md if login fails after an otherwise-green deploy)

## Troubleshooting

If a home-target deployment fails:
1. Check the workflow run's logs (`gh run view <run-id> --log-failed`) —
   the run happened entirely on that machine's self-hosted runner, so the
   failure is local (JDBC pool unreachable, Payara admin port down, disk
   full), not a generic cloud CI issue
2. Confirm the runner is online: `gh api repos/hmislk/hmis/actions/runners --jq '.runners[] | {name, status}'`
3. `qa-home-infra`'s `GOTCHAS.md` and the relevant machine's `runbooks/`
   file are the first place to check for a known issue

If a legacy Azure-target deployment fails:
1. Check GitHub Actions logs for build errors
2. Verify `src/main/resources/META-INF/persistence.xml` uses `${JDBC_DATASOURCE}` /
   `${JDBC_AUDIT_DATASOURCE}` on these branches (not a hardcoded local JNDI name)
3. Check if the target server is accessible
4. See [QA Troubleshooting Guide](../../../developer_docs/deployment/qa-troubleshooting.md)
   (Azure-estate content only — does not cover home targets)
