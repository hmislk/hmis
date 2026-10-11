# HMIS Deployment Map — Branches, Pipelines and Servers

*Last updated: 2026-10-11 (after the move to the India South Central Azure estate).*

This is the single reference for **which branch deploys what, where**. It is
meant for developers, the server management team and QA. It contains no
passwords, IP addresses or database details; those stay in the
infrastructure sheet held by server management.

## 1. The short version

- Code is written on feature branches and merged into **`development`**.
- QA tests on the **home-hosted QA instances** (`home-qa1` … `home-qa4`).
- Each pipeline-managed hospital app is deployed from its own branch:
  **`<hospital>-production`** or **`<hospital>-staging`**. Merging into that
  branch deploys it. Apps marked *manual WAR deploy*, *none* or *not live* in
  the table below are **not** deployed by any branch merge.
- Changes reach a hospital branch only through a PR from a **`*-hotfix`**
  branch (see [Hotfix branches](../git/commit-conventions.md#hotfix-branches)).
  Note: the `-hotfix` suffix is a team rule, **not yet enforced by CI** for `*-production` / `*-staging` targets (`check-branch` still only matches the retired `*-prod` names; tracked in #24453).
- The old `*-prod`, `*-stg`, `*-dev`, `*-prod-migrated` and `*-stg-migrated`
  branches and their pipelines were **retired on 2026-10-11**. Each was kept as
  a git tag `archive/2026-10-11/<old-branch>` in case it is ever needed.

## 2. Production and staging pipelines

All production apps run on Payara 5 on five Azure VMs in the
`rg-hmis-prod-indiasouthcentral` resource group.

| Hospital / app | Branch | Workflow file | VM | URL | Notes |
|---|---|---|---|---|---|
| COOP (pharmacy) | `coop-production` | `coop_production_ci_cd.yml` | shared3 | https://coop.carecode.org/coop | |
| COOP (inward) | `coop-staging` | `coop_staging_ci_cd.yml` | staging | https://stg-migrated.carecode.org/coop | **Live users.** Uses the production database. |
| Ruhunu | `rh-production` | `rh_production_ci_cd.yml` | shared2 | https://rh.carecode.org/rh | |
| Ruhunu (staging) | `rh-staging` | `rh_staging_ci_cd.yml` | staging | https://stg-migrated.carecode.org/rh | Uses the production database. |
| RMH | `rmh-production` | `rmh_production_ci_cd.yml` | shared2 | https://rmh.carecode.org/rmh | |
| RMH (staging) | `rmh-staging` | `rmh_staging_ci_cd.yml` | staging | https://stg-migrated.carecode.org/rmh | Uses the production database. |
| Southern Lanka | `southernlanka-staging` | `southernlanka_staging_ci_cd.yml` | staging | https://stg-migrated.carecode.org/southernlanka | **Live users are here.** Uses the production database. |
| Southern Lanka (prod) | `southernlanka-production` | `southernlanka_production_ci_cd.yml` | shared1 | https://southernlanka.carecode.org/southernlanka | App is **disabled** in Payara; deploys keep it disabled. |
| Suwani | `suwani-production` | `suwani_production_ci_cd.yml` | shared1 | https://suwani.carecode.org/suwani | |
| Asiri Pharmacy | `asiripharmacy-production` | `asiripharmacy_production_ci_cd.yml` | shared1 | https://asiripharmacy.carecode.org/asiripharmacy | |
| Digasiri | `digasiri-production` | `digasiri_production_ci_cd.yml` | shared1 | https://digasiri.carecode.org/digasiri | |
| Horizon | `horizon-production` | `horizon_production_ci_cd.yml` | shared1 | https://horizon.carecode.org/horizon | |
| Matara Pharmacy | `mp-production` | `mp_production_ci_cd.yml` | shared2 | https://mp.carecode.org/mp | |
| Matara Pharmacy (old) | `mpold-production` | `mpold_production_ci_cd.yml` | shared3 | https://mpold.carecode.org/mpold | |
| Roseth | `roseth-production` | `roseth_production_ci_cd.yml` | shared3 | https://roseth.carecode.org/roseth | |
| Engage Wellness Centre | `ewc-production` | `ewc_production_ci_cd.yml` | shared4 | https://ewc.carecode.org/ewc | |
| BMS | `bms-production` | — none | shared4 | https://bms.carecode.org/bms | App disabled; branch only keeps the deployed code. No pipeline. |
| RH HRT | — | — manual WAR deploy | shared1 | https://rh-hrt.carecode.org/rh-hrt | |
| RH Drawer | — | — manual WAR deploy | shared3 | https://rhdrawer.carecode.org/drawer | |
| Messenger | — | — | shared2 | messenger.carecode.org | **Not live yet**: being set up on shared2 by server management. |

Each pipeline's workflow file lives **only on its own branch**, not on
`development`.

### VM names

| Short name | Azure VM |
|---|---|
| shared1 | `vm-hmis-shared1-prod` |
| shared2 | `vm-hmis-shared2-prod` |
| shared3 | `vm-hmis-shared3-prod` |
| shared4 | `vm-hmis-shared4-prod` |
| staging | `vm-hmis-staging-prod` |

"Staging" apps on the staging VM are **not** test environments: they read
and write the same production databases as the hospital's production app.
Treat a staging deploy as a production deploy.

## 3. QA environments (home-hosted)

| QA | Branch | Workflow | URL |
|---|---|---|---|
| QA1 | `home-qa1` | `hims_qa1_home_ci_cd.yml` | https://qa1.carecode.org/qa1 |
| QA2 | `home-qa2` | `hims_qa2_home_ci_cd.yml` | https://qa2.carecode.org/qa2 |
| QA3 | `home-qa3` | `hims_qa3_home_ci_cd.yml` | https://qa3.carecode.org/qa3 |
| QA4 | `home-qa4` | `hims_qa4_home_ci_cd.yml` | https://qa4.carecode.org/qa4 |
| Ruhunu local staging | `rh-local-staging` | `ruhunu_local_server_ci_cd.yml` | on the hospital's own server |

There are no Azure QA environments any more. Details:
[QA Deployment Guide](qa-deployment-guide.md).

## 4. How a deployment runs

Every production/staging pipeline has the same two jobs:

1. **Build** (GitHub-hosted runner): substitute the JNDI placeholders
   `${JDBC_DATASOURCE}` / `${JDBC_AUDIT_DATASOURCE}` in `persistence.xml` with
   the app's datasource names, then `mvn clean package -DskipTests`.
2. **Deploy** (only from the pipeline's own branch):
   - copy the WAR to the VM;
   - check that both datasources exist and their connection pools answer
     **before** touching the running app, so a bad pool leaves the hospital up;
   - keep the previous WAR as `<app>.war.previous`;
   - undeploy and deploy, **keeping the app's current enabled/disabled state**;
   - health-check `https://<host>/<app>/faces/index1.xhtml` for up to 10
     minutes (skipped if the app is disabled).

Deploys to the same VM queue behind each other (one concurrency group per VM)
so two deploys never hit one Payara domain at once.

### What CI needs (for server management)

| Setting | Kind | Used for |
|---|---|---|
| `PROD_MIGRATED_SSH_PRIVATE_KEY` | secret | CI SSH key, authorised for the deploy user on every India VM |
| `PROD_MIGRATED_SERVER_USER` | secret | SSH user on the VMs |
| `<APP>_PRODUCTION_SERVER_IP` / `<APP>_STAGING_SERVER_IP` | repository variable | target VM of each pipeline |
| `ISC_SHARED1_IP` … `ISC_SHARED4_IP`, `ISC_STAGING_IP` | repository variable | the restart workflow |

On each VM the pipelines expect Payara at `/opt/payara5`, an admin password
file at `/opt/payara5/.asadminpass`, and the WAR directory
`/home/appuser/app/latest/`. **If a VM is rebuilt or its IP changes, update
the repository variables above**; nothing else in the pipelines needs editing.

## 5. Day-to-day procedures

**Ship a fix to a hospital** — use the `hotfix-deploy` skill, or by hand:

```bash
git fetch origin
git checkout -b <short-description>-hotfix origin/<hospital-branch>
# make the minimal fix, commit with "Closes #N"
git push origin <short-description>-hotfix
gh pr create --base <hospital-branch> --head <short-description>-hotfix
```

Merging the PR deploys it. Keep **one open PR per hospital branch**. If the fix
was written on the hotfix branch rather than cherry-picked from `development`,
mirror it into `development` with a normal PR.

**Redeploy without a code change** — the pipeline files live only on their
hospital branches, not on the default branch, so GitHub does not offer *Run
workflow* for them. Re-run the branch's latest deploy instead (Actions → the
pipeline's last run → *Re-run all jobs*), or:

```bash
gh run list --branch <hospital-branch> --limit 1      # find the last run id
gh run rerun <run-id>                                 # runs older than 30 days cannot be re-run; merge a hotfix instead
```

**Restart an app, Payara, or a VM** — Actions → *Restart a service (India
South Central)* → choose the target and type its name again to confirm.

**Update a QA instance** — open a `development → home-qaN` PR and merge it, or
use the `deploy-qa` skill.

**Roll back** — on the VM, `<app>.war.previous` is the build before the last
deploy. Ask server management to redeploy it, then fix forward with a hotfix PR.

## 6. Who to ask

| Topic | Team |
|---|---|
| Which branch to target, failing builds, pipeline changes | Development |
| VM access, Payara domains, datasources/pools, DNS, new VMs, IP changes | Server management |
| Which QA instance has which data, QA refreshes | Development (QA instance owner) |
| What is deployed where | This document, then GitHub Actions run history |
