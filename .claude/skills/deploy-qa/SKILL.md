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

QA1-QA4 are home-hosted (tracked in the private
[`hmislk/qa-home-infra`](https://github.com/hmislk/qa-home-infra) repo —
`MASTER-PLAN.md` for status, `GOTCHAS.md` for detail). The Azure QA
pipelines (`hims-qaN-migrated`) were retired on 2026-10-11; there are no
Azure QA environments.

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

**Do not use** any `hims-qa*` branch, and never target `hims-qaN-home` —
that pattern collides with the repo's "QA Branches Rules" ruleset (PR-only
pushes + a `check-branch` status check that's only ever attached via a PR),
so a branch under it can never even be created by a direct push. The
correct home branch name is `home-qaN`, not `hims-qaN-home`.

## Deployment Process

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
`development` — nothing to deploy, skip it.

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

## Post-Deployment

- Monitor GitHub Actions for build status
- Check the environment is accessible after deployment (the machine's
  local URL, e.g. `http://localhost:9080/qa1` for QA1 — the
  public `qaN.carecode.org` URL additionally depends on the Desktop's nginx
  being correctly wired for that instance)
- Verify the deployed feature works as expected — log in through the real
  UI and check a real screen against real data, not just a health-check
  page render (a stale local DB or a cross-DB credential mismatch has bitten
  every home instance at least once during setup; see `qa-home-infra`'s
  GOTCHAS.md if login fails after an otherwise-green deploy)

## Troubleshooting

If a deployment fails:
1. Check the workflow run's logs (`gh run view <run-id> --log-failed`) —
   the run happened entirely on that machine's self-hosted runner, so the
   failure is local (JDBC pool unreachable, Payara admin port down, disk
   full), not a generic cloud CI issue
2. Confirm the runner is online: `gh api repos/hmislk/hmis/actions/runners --jq '.runners[] | {name, status}'`
3. `qa-home-infra`'s `GOTCHAS.md` and the relevant machine's `runbooks/`
   file are the first place to check for a known issue
