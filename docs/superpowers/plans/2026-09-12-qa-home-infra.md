# QA Home Infra (QA1 bring-up + tracking repo) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up QA1 on this laptop's carecode-account Payara domain (`rh`), deployed via a new self-hosted-runner GitHub Actions pipeline, verified end-to-end; and create the private `hmislk/qa-home-infra` tracking repo with the master plan and runbooks the other three machines (QA2 buddhika-account, QA3 desktop, QA4 carecode-laptop) will follow in their own sessions.

**Architecture:** Per `docs/superpowers/specs/2026-09-12-qa-home-infra-design.md` — QA1 deploys into the existing `rh` Payara domain as a second application (context-root `qa1`) using a **new** JDBC pool pointing at the real local `ruhunu` database, reusing the existing `jdbc/ruhunuAudit` pool for audit. A self-hosted GitHub Actions runner registered on this machine/account replaces SSH-based deploy entirely — the runner executes the deploy job locally, so no inbound port or secret is needed for CI/CD. nginx/router/public-URL wiring is QA3's job (out of scope here); this plan verifies QA1 over `localhost` only.

**Tech Stack:** Payara 5 (existing domain `rh`), MySQL (existing local `ruhunu` DB), GitHub Actions (self-hosted runner), Maven/JDK 11 (cloud build only).

## Global Constraints

- Never commit a real password into any tracked file (`qa-home-infra` or `hmis`) — CLAUDE.md "NEVER COMMIT CREDENTIALS OR SENSITIVE DATA". Every command below that needs the local MySQL password reads it from a shell variable you set yourself from your existing credentials memory/file, never typed into a file.
- All PRs into the `hmis` repo target `development`, branched from `origin/development` — CLAUDE.md "ALWAYS BASE FEATURE BRANCHES ON development".
- Do not touch `src/main/resources/META-INF/persistence.xml` in this local checkout — it's already correctly set to the local JNDI convention (`jdbc/coop` / `jdbc/ruhunuAudit`) per CLAUDE.md, and untouched by this plan (the QA1 build job substitutes its own JNDI names in a separate GitHub-hosted checkout).
- No sudo against `/opt/payara5` or any production Payara path — not applicable here (this plan only touches this account's own `/home/carecode/payara`, owned by `carecode:carecode`), but the self-hosted runner service install does need `sudo` once, called out explicitly at that step.

---

## Task 1: Create the `hmislk/qa-home-infra` private repo with its skeleton

**Files:**
- Create (new repo, cloned to `/home/carecode/development/qa-home-infra/`):
  - `README.md`
  - `MASTER-PLAN.md`
  - `runbooks/hiu-laptop-carecode-qa1.md`
  - `runbooks/hiu-laptop-buddhika-qa2.md`
  - `runbooks/desktop-qa3.md`
  - `runbooks/carecode-laptop-qa4.md`

**Interfaces:**
- Produces: the repo URL `https://github.com/hmislk/qa-home-infra` that later tasks push commits to, and the four runbook files that other machines' sessions read.

- [ ] **Step 1: Create the private GitHub repo**

```bash
gh repo create hmislk/qa-home-infra --private \
  --description "Master plan + per-machine runbooks for home-hosted QA1-QA4 (replacing Azure QA estate)"
```

Expected: prints `https://github.com/hmislk/qa-home-infra`.

- [ ] **Step 2: Clone it locally, next to the hmis checkout**

```bash
cd /home/carecode/development
git clone https://github.com/hmislk/qa-home-infra.git
cd qa-home-infra
ls -la
```

Expected: a `.git` directory, otherwise empty.

- [ ] **Step 3: Write `README.md`**

```markdown
# QA Home Infra

Tracking repo for migrating QA1-QA4 off the Azure "migrated" estate onto four
home-hosted machines, to cut hosting cost. Not application code — this repo
holds the master plan, per-machine runbooks, and the nginx config that ties
them together.

Start here: [`MASTER-PLAN.md`](MASTER-PLAN.md).

Design spec (in the `hmis` repo): `docs/superpowers/specs/2026-09-12-qa-home-infra-design.md`.
```

Write this to `README.md`.

- [ ] **Step 4: Write `MASTER-PLAN.md`**

```markdown
# QA Home Infra — Master Plan

## Architecture

Four QA instances move off Azure onto four home machines, all behind one
router (public IP `124.43.16.16`; DNS for `qa1-4.carecode.org` already points
there). nginx on the desktop is the **only** internet-facing service —
router forwards ports 80/443 to it and nothing else. Each instance deploys
into its machine's **existing** Payara domain (not a new one) via a
**self-hosted GitHub Actions runner** registered on that machine/account, so
no inbound deploy port or SSH key is needed anywhere.

| Instance | Machine | Account | LAN IP | Hospital DB | Status |
|---|---|---|---|---|---|
| QA1 | hiu-laptop (this laptop) | carecode | 192.168.1.203 | ruhunu | 🔲 in progress |
| QA2 | hiu-laptop (this laptop) | buddhika | 192.168.1.203 | coop | 🔲 not started |
| QA3 | Desktop | — | 192.168.1.201 | southernlanka | 🔲 not started |
| QA4 | carecode-laptop | — | 192.168.1.204 | roseth | 🔲 not started |

Full design rationale: `docs/superpowers/specs/2026-09-12-qa-home-infra-design.md`
in the `hmis` repo.

## Per-machine checklist

Each machine's session works from its own runbook in `runbooks/` and checks
off its row here as it completes each stage. Commit after each checkbox
change so progress is visible to the other machines.

### QA1 — hiu-laptop / carecode account
- [ ] New JDBC pool `qa1MainPool` → local `ruhunu` DB created and pinging
- [ ] Self-hosted GitHub Actions runner registered (label `qa1`), running as a service
- [ ] `hims_qa1_home_ci_cd.yml` merged to `development`
- [ ] `hims-qa1-home` branch pushed, first deploy succeeded
- [ ] `http://localhost:9080/qa1/faces/index1.xhtml` verified reachable (HTTP 200)
- [ ] Login verified against the real `ruhunu` data (not just page render)

### QA2 — hiu-laptop / buddhika account
- [ ] Runbook read: `runbooks/hiu-laptop-buddhika-qa2.md`
- [ ] Confirmed which local JDBC pool actually points at the `coop` database (don't trust the name — verify with `asadmin get resources.jdbc-connection-pool.<pool>.property.databaseName`)
- [ ] Self-hosted GitHub Actions runner registered (label `qa2`), running as a service
- [ ] `hims_qa2_home_ci_cd.yml` merged to `development`
- [ ] `hims-qa2-home` branch pushed, first deploy succeeded
- [ ] Local health check passed; this row's Payara HTTP port recorded here: `____`

### QA3 — Desktop (also runs nginx)
- [ ] Runbook read: `runbooks/desktop-qa3.md`
- [ ] Confirmed which local JDBC pool actually points at the `southernlanka` database
- [ ] Self-hosted GitHub Actions runner registered (label `qa3`), running as a service
- [ ] `hims_qa3_home_ci_cd.yml` merged to `development`
- [ ] `hims-qa3-home` branch pushed, first deploy succeeded
- [ ] Local health check passed; this row's Payara HTTP port recorded here: `____`
- [ ] nginx installed, config from `nginx/qa-home.conf` applied
- [ ] certbot issued certs for all 4 subdomains (HTTP-01, port 80 forwarded)
- [ ] Router forwards 80/443 to 192.168.1.201 only — no other port forwarded
- [ ] `https://qa1.carecode.org` reachable publicly
- [ ] `https://qa2.carecode.org` reachable publicly
- [ ] `https://qa3.carecode.org` reachable publicly
- [ ] `https://qa4.carecode.org` reachable publicly

### QA4 — carecode-laptop
- [ ] Runbook read: `runbooks/carecode-laptop-qa4.md`
- [ ] Confirmed which local JDBC pool actually points at the `roseth` database
- [ ] Self-hosted GitHub Actions runner registered (label `qa4`), running as a service
- [ ] `hims_qa4_home_ci_cd.yml` merged to `development`
- [ ] `hims-qa4-home` branch pushed, first deploy succeeded
- [ ] Local health check passed; this row's Payara HTTP port recorded here: `____`

### Cutover
- [ ] All four public URLs verified end-to-end (a real login + one workflow, not just page render)
- [ ] Azure QA VM decommissioned
```

Write this to `MASTER-PLAN.md`.

- [ ] **Step 5: Write the QA1 runbook (fully — this plan executes it)**

```markdown
# Runbook: QA1 — hiu-laptop / carecode account

Executed by the Claude Code session already running as the `carecode` OS
account on hiu-laptop, working in the `hmis` checkout at
`/home/carecode/development/rh`. See
`docs/superpowers/plans/2026-09-12-qa-home-infra.md` (Tasks 2-4) in that repo
for the exact commands run.

Summary of what this machine ended up with:
- JDBC pool: `qa1MainPool` → local `ruhunu` database, resource name `jdbc/qa1Main`
- Audit JDBC: reuses existing `jdbc/ruhunuAudit` → `rhAuditPool` → `rhAudit` database
- Payara domain: existing `rh` domain, HTTP port 9080, admin port 9048
- App context-root: `qa1` (alongside the existing `rh` app on the same port)
- Self-hosted runner label: `qa1`
- Workflow: `.github/workflows/hims_qa1_home_ci_cd.yml`, branch `hims-qa1-home`
- Local URL: `http://localhost:9080/qa1/faces/index1.xhtml`
```

Write this to `runbooks/hiu-laptop-carecode-qa1.md`.

- [ ] **Step 6: Write the QA2 runbook stub (buddhika account, this laptop)**

```markdown
# Runbook: QA2 — hiu-laptop / buddhika account

Run this from a Claude Code session logged into the `buddhika` OS account on
this same laptop (see `MASTER-PLAN.md` for why: account-level isolation from
QA1). Start from a checkout of `hmis` under that account's home directory.

## 1. Find the real database

Do **not** assume a JNDI name matching "coop" actually points at the coop
database — verify:

```bash
export PATH=$PATH:<path-to-this-account's-payara>/glassfish/bin
asadmin --port <this-account's-admin-port> list-jdbc-resources
# for each candidate resource:
asadmin --port <admin-port> get "resources.jdbc-resource.<name>.pool-name"
asadmin --port <admin-port> get "resources.jdbc-connection-pool.<pool-name>.property.databaseName"
```

Find the pool whose `databaseName` is genuinely `coop` (or create a new one
the same way QA1 did in `qa-home-infra`'s Task 3, substituting `coop` for
`ruhunu`).

## 2. Register the self-hosted runner

Same steps as QA1's Task 4 in `docs/superpowers/plans/2026-09-12-qa-home-infra.md`
(in the `hmis` repo), with `--labels qa2` instead of `qa1`, and installed as a
service under the `buddhika` account (a separate systemd service instance
from QA1's, since it's a different OS user).

## 3. Workflow file

Copy `.github/workflows/hims_qa1_home_ci_cd.yml`, rename to
`hims_qa2_home_ci_cd.yml`, and change: branch trigger to `hims-qa2-home`,
`APP_NAME`/`CONTEXT_ROOT` to `qa2`, `JNDI_MAIN` to whatever resource name you
created in step 1, `JNDI_AUDIT` to that account's existing audit resource,
`HEALTH_URL` to `http://localhost:<this account's HTTP port>/qa2/faces/index1.xhtml`,
and `runs-on: [self-hosted, qa2]` on the deploy job. Open a PR to
`development` from a branch based on `origin/development`, per CLAUDE.md.

## 4. First deploy

Push `origin/development` to a new `hims-qa2-home` branch, watch the
workflow run (`gh run watch`), confirm the health check passes, then log in
through the real UI to confirm it works against real coop data (per CLAUDE.md
"always end-to-end test a real workflow, not just page render").

## 5. Update the master plan

Check off QA2's rows in `../MASTER-PLAN.md` (this repo) and record the HTTP
port you used, then commit and push.
```

Write this to `runbooks/hiu-laptop-buddhika-qa2.md`.

- [ ] **Step 7: Write the QA3 runbook stub (desktop, also runs nginx)**

```markdown
# Runbook: QA3 — Desktop (also hosts nginx for all 4 subdomains)

Run this from a Claude Code session on the Desktop (LAN IP 192.168.1.201),
from a checkout of `hmis`.

## 1-4. App + runner + workflow + first deploy

Same pattern as QA2's runbook (`hiu-laptop-buddhika-qa2.md`) steps 1-4,
substituting: database `southernlanka`, label `qa3`, workflow file
`hims_qa3_home_ci_cd.yml`, branch `hims-qa3-home`, context-root `qa3`.
QA3's `runs-on` is still `[self-hosted, qa3]` even though this machine also
runs nginx — those are unrelated services.

## 5. nginx

Install nginx and certbot:

```bash
sudo apt-get update && sudo apt-get install -y nginx certbot python3-certbot-nginx
```

Write `/etc/nginx/sites-available/qa-home.conf` — copy the template from
`qa-home-infra/nginx/qa-home.conf` (this tracking repo), filling in the
actual HTTP ports recorded in `MASTER-PLAN.md` for QA1/QA2/QA4 (QA3's own
port is whatever this deploy step above allocated locally). Then:

```bash
sudo ln -s /etc/nginx/sites-available/qa-home.conf /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## 6. Router port-forward

On the home router admin page: forward external TCP 80 and 443 to
192.168.1.201 (this desktop) only. No other port for any of the four
machines.

## 7. TLS certificates

```bash
sudo certbot --nginx -d qa1.carecode.org -d qa2.carecode.org -d qa3.carecode.org -d qa4.carecode.org
```

This only succeeds once the port-80 forward (step 6) is live and each
subdomain's app is already reachable over the LAN paths configured in step
5 — certbot's HTTP-01 challenge is served through nginx to the outside.

## 8. Verify all four publicly

```bash
for h in qa1 qa2 qa3 qa4; do
  curl -sS -o /dev/null -w "%{http_code} $h\n" "https://$h.carecode.org/$h/faces/index1.xhtml"
done
```

Expected: `200` for each, once all four machines have completed their own
deploy tasks.

## 9. Update the master plan

Check off QA3's and the "Cutover" section's rows in `../MASTER-PLAN.md`,
commit and push.
```

Write this to `runbooks/desktop-qa3.md`.

- [ ] **Step 8: Write the QA4 runbook stub (carecode-laptop)**

```markdown
# Runbook: QA4 — carecode-laptop

Run this from a Claude Code session on carecode-laptop (LAN IP
192.168.1.204), from a checkout of `hmis`.

Same pattern as QA2's runbook (`hiu-laptop-buddhika-qa2.md`) steps 1-5,
substituting: database `roseth`, label `qa4`, workflow file
`hims_qa4_home_ci_cd.yml`, branch `hims-qa4-home`, context-root `qa4`,
`HEALTH_URL` `http://localhost:<this machine's HTTP port>/qa4/faces/index1.xhtml`.

This machine does not run nginx — once its own local deploy and health check
pass, tell whoever is running the Desktop's QA3 runbook this machine's LAN
IP/port so they can add it to the nginx upstream list, and check off QA4's
rows in `../MASTER-PLAN.md`.
```

Write this to `runbooks/carecode-laptop-qa4.md`.

- [ ] **Step 9: Create the nginx config template**

```nginx
# qa-home-infra/nginx/qa-home.conf
# Applied on the Desktop only. Port numbers below are placeholders filled in
# from MASTER-PLAN.md once each machine has completed its own deploy task.

server {
    listen 80;
    server_name qa1.carecode.org;
    location / {
        proxy_pass http://192.168.1.203:<QA1_PORT>;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name qa2.carecode.org;
    location / {
        proxy_pass http://192.168.1.203:<QA2_PORT>;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name qa3.carecode.org;
    location / {
        proxy_pass http://127.0.0.1:<QA3_PORT>;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name qa4.carecode.org;
    location / {
        proxy_pass http://192.168.1.204:<QA4_PORT>;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# certbot --nginx rewrites these server blocks in place to add the 443
# listeners and cert paths once TLS is issued (desktop runbook step 7) -
# don't hand-edit the SSL directives afterward, re-run certbot instead.
```

Write this to `nginx/qa-home.conf`.

- [ ] **Step 10: Commit and push**

```bash
cd /home/carecode/development/qa-home-infra
git add README.md MASTER-PLAN.md runbooks/ nginx/
git commit -m "Initial master plan and per-machine runbooks for QA1-QA4 home migration"
git push origin main
```

Expected: push succeeds, no errors.

---

## Task 2: Create the QA1 JDBC pool pointing at the real local `ruhunu` database

**Files:** none in the `hmis` git repo — this modifies the live `rh` Payara domain's runtime config directly (`/home/carecode/payara/glassfish/domains/rh/config/domain.xml`, via `asadmin`, never hand-edited).

**Interfaces:**
- Consumes: the existing `rhAuditPool` / `jdbc/ruhunuAudit` resource (already present, verified in Task discovery) — reused as-is for QA1's audit datasource.
- Produces: JNDI resource `jdbc/qa1Main`, backed by connection pool `qa1MainPool` → local `ruhunu` database. Task 4's workflow YAML references this exact name.

- [ ] **Step 1: Set the local MySQL password in your shell (not in any file)**

Read the password from your existing local-carecode-dev-credentials memory
or `/home/carecode/Credentials/credentials.txt`, then:

```bash
export MYSQL_LOCAL_PASSWORD='<the password you just read>'
```

- [ ] **Step 2: Create the connection pool**

```bash
export PATH=$PATH:/home/carecode/payara/glassfish/bin
asadmin --port 9048 create-jdbc-connection-pool \
  --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource \
  --restype javax.sql.DataSource \
  --property "serverName=localhost:port=3306:databaseName=ruhunu:user=buddhika:password=${MYSQL_LOCAL_PASSWORD}:useSSL=false:allowPublicKeyRetrieval=true:rewriteBatchedStatements=true" \
  qa1MainPool
```

Expected: `Command create-jdbc-connection-pool executed successfully.`

- [ ] **Step 3: Create the JNDI resource**

```bash
asadmin --port 9048 create-jdbc-resource --connectionpoolid qa1MainPool jdbc/qa1Main
```

Expected: `Command create-jdbc-resource executed successfully.`

- [ ] **Step 4: Verify both pools QA1 will use are reachable**

```bash
asadmin --port 9048 ping-connection-pool qa1MainPool
asadmin --port 9048 ping-connection-pool rhAuditPool
unset MYSQL_LOCAL_PASSWORD
```

Expected: both print `Command ping-connection-pool executed successfully.`
If `qa1MainPool` fails, re-check the password and that MySQL user `buddhika`
has privileges on the `ruhunu` database (`SHOW GRANTS FOR 'buddhika'@'localhost';`).

- [ ] **Step 5: Sanity-check the `ruhunu` schema isn't stale**

```bash
MYSQL_PWD="$MYSQL_LOCAL_PASSWORD" mysql -h localhost -u buddhika -e \
  "SELECT table_schema, COUNT(*) AS tables FROM information_schema.tables WHERE table_schema IN ('ruhunu','coop') GROUP BY table_schema;"
```

(Set `MYSQL_LOCAL_PASSWORD` again first if you unset it in Step 4.) Expected:
`ruhunu`'s table count should be in the same ballpark as `coop`'s (both run
the same HMIS schema). A large gap (e.g. `ruhunu` has a third of `coop`'s
tables) means `ruhunu` predates recent migrations — if so, stop and tell the
user before continuing to Task 3, since QA1's first deploy will otherwise
fail or run against a stale schema.

---

## Task 3: Register a self-hosted GitHub Actions runner for QA1

**Files:**
- Create: `/home/carecode/actions-runner-qa1/` (runner install directory, outside any git repo)

**Interfaces:**
- Produces: a runner online in `hmislk/hmis` with label `qa1`, running as a
  systemd service, which Task 4's workflow's deploy job targets via
  `runs-on: [self-hosted, qa1]`.

- [ ] **Step 1: Get a registration token**

```bash
gh api -X POST repos/hmislk/hmis/actions/runners/registration-token --jq '.token'
```

Expected: a token string (valid ~1 hour). Copy it for the next step.

- [ ] **Step 2: Download and unpack the runner**

```bash
mkdir -p /home/carecode/actions-runner-qa1 && cd /home/carecode/actions-runner-qa1
RUNNER_URL=$(curl -s https://api.github.com/repos/actions/runner/releases/latest \
  | grep -oP '"browser_download_url":\s*"\K[^"]*linux-x64-[^"]*\.tar\.gz')
curl -o runner.tar.gz -L "$RUNNER_URL"
tar xzf runner.tar.gz
```

Expected: extracts `config.sh`, `svc.sh`, `run.sh` into the current directory.

- [ ] **Step 3: Configure it**

```bash
./config.sh --url https://github.com/hmislk/hmis \
  --token '<token from Step 1>' \
  --name qa1-hiu-laptop-carecode \
  --labels qa1 \
  --work _work \
  --unattended
```

Expected: ends with `√ Settings Saved.`

- [ ] **Step 4: Install and start it as a systemd service**

```bash
sudo ./svc.sh install
sudo ./svc.sh start
sudo ./svc.sh status
```

Expected: status shows `active (running)`. This is the one `sudo` use in
this plan — it installs a systemd unit so the runner survives reboots and
logouts; it does not touch any Payara or production path.

- [ ] **Step 5: Confirm it's visible to GitHub**

```bash
gh api repos/hmislk/hmis/actions/runners --jq '.runners[] | select(.name=="qa1-hiu-laptop-carecode") | {name, status, labels: [.labels[].name]}'
```

Expected: `"status": "online"`, labels include `qa1`.

---

## Task 4: Add the `hims_qa1_home_ci_cd.yml` workflow and run the first deploy

**Files:**
- Create: `.github/workflows/hims_qa1_home_ci_cd.yml` (in the `hmis` repo)

**Interfaces:**
- Consumes: `jdbc/qa1Main` / `jdbc/ruhunuAudit` (Task 2), the `qa1`-labeled
  self-hosted runner (Task 3).
- Produces: a working `http://localhost:9080/qa1/faces/index1.xhtml`.

- [ ] **Step 1: Branch from origin/development**

```bash
cd /home/carecode/development/rh
git fetch origin
git checkout -b qa1-home-ci-cd origin/development
```

- [ ] **Step 2: Write the workflow file**

```yaml
# .github/workflows/hims_qa1_home_ci_cd.yml
#
# HMIS QA1 — home-hosted (hiu-laptop, carecode account)
# Branch:  hims-qa1-home
# Serves:  http://localhost:9080/qa1 today; https://qa1.carecode.org/qa1
#          once the Desktop's nginx (QA3 runbook) is wired up.
#
# Differs from hims_qa1_migrated_ci_cd.yml: the deploy job runs ON the
# target machine itself (self-hosted runner, label qa1) instead of SSHing
# into an Azure VM, so there are no SSH secrets and no inbound deploy port.

name: HIMS QA1 Home CI/CD

on:
  push:
    branches: [hims-qa1-home]
  workflow_dispatch:

concurrency:
  group: payara-qa1-home
  cancel-in-progress: false

env:
  APP_NAME:     qa1
  JNDI_MAIN:    jdbc/qa1Main
  JNDI_AUDIT:   jdbc/ruhunuAudit
  HEALTH_URL:   http://localhost:9080/qa1/faces/index1.xhtml

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 11
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '11'
          cache: maven

      - name: Substitute datasource JNDI names
        run: |
          set -euo pipefail
          PU=src/main/resources/META-INF/persistence.xml
          test -f "$PU" || { echo "::error::$PU not found"; exit 1; }

          sed -i "s|[$][{]JDBC_DATASOURCE[}]|${JNDI_MAIN}|g"        "$PU"
          sed -i "s|[$][{]JDBC_AUDIT_DATASOURCE[}]|${JNDI_AUDIT}|g" "$PU"

          echo "--- resolved datasources ---"
          grep -E 'jta-data-source|non-jta-data-source' "$PU"

          if grep -q 'JDBC_DATASOURCE\|JDBC_AUDIT_DATASOURCE' "$PU"; then
            echo "::error::unsubstituted JDBC placeholder remains in $PU"
            exit 1
          fi

          for want in "$JNDI_MAIN" "$JNDI_AUDIT"; do
            grep -q "<[a-z-]*jta-data-source>${want}</" "$PU" || {
              echo "::error::$PU does not reference ${want}."
              grep -E 'jta-data-source' "$PU"
              exit 1; }
          done
          echo "datasources verified: $JNDI_MAIN, $JNDI_AUDIT"

      - name: Build
        run: mvn -B -ntp clean package -DskipTests

      - name: Rename artifact
        run: |
          set -euo pipefail
          war=$(find target -maxdepth 1 -name '*.war' | head -1)
          test -n "$war" || { echo "::error::no WAR produced"; exit 1; }
          mv "$war" "target/${APP_NAME}.war"

      - uses: actions/upload-artifact@v4
        with:
          name: qa1-war
          path: target/qa1.war
          retention-days: 7

  deploy:
    needs: build
    runs-on: [self-hosted, qa1]
    env:
      APP_NAME:   qa1
      JNDI_MAIN:  jdbc/qa1Main
      JNDI_AUDIT: jdbc/ruhunuAudit
      HEALTH_URL: http://localhost:9080/qa1/faces/index1.xhtml
      PAYARA:     /home/carecode/payara
      ASADMIN_PORT: "9048"
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: qa1-war
          path: ./artifact

      - name: Deploy to Payara
        run: |
          set -euo pipefail
          ASADMIN="$PAYARA/glassfish/bin/asadmin --port $ASADMIN_PORT"
          WAR="$GITHUB_WORKSPACE/artifact/qa1.war"
          test -f "$WAR" || { echo "WAR missing: $WAR"; exit 1; }

          for j in "$JNDI_MAIN" "$JNDI_AUDIT"; do
            $ASADMIN list-jdbc-resources | tr -d '\r' | grep -qx "$j" || {
              echo "JNDI resource $j not found. Run Task 2 of the QA1 plan first."
              exit 1; }
            # Pool name is NOT derivable from the JNDI name (e.g.
            # jdbc/ruhunuAudit -> rhAuditPool) - look it up.
            key="resources.jdbc-resource.${j}.pool-name"
            pool=$($ASADMIN get "$key" 2>/dev/null | tr -d '\r' | grep -F "$key=" | head -1)
            pool="${pool#*=}"
            [ -n "$pool" ] || { echo "could not resolve pool behind $j"; exit 1; }
            $ASADMIN ping-connection-pool "$pool" || { echo "pool $pool (behind $j) is not reachable"; exit 1; }
          done

          $ASADMIN undeploy "$APP_NAME" 2>/dev/null || echo "not previously deployed"
          $ASADMIN deploy --contextroot "$APP_NAME" --name "$APP_NAME" --force=true "$WAR"
          $ASADMIN list-applications

      - name: Health check
        run: |
          set -euo pipefail
          for i in $(seq 1 30); do
            code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$HEALTH_URL" || echo 000)
            echo "attempt $i: HTTP $code"
            [ "$code" = "200" ] && { echo "healthy"; exit 0; }
            sleep 10
          done
          echo "::error::qa1 did not become healthy at $HEALTH_URL"
          exit 1
```

- [ ] **Step 3: Commit and open the PR**

```bash
git add .github/workflows/hims_qa1_home_ci_cd.yml
git commit -m "$(cat <<'EOF'
ci: add home-hosted QA1 deploy workflow (self-hosted runner)

Replaces SSH-based deploy for QA1 with a self-hosted GitHub Actions
runner on hiu-laptop's carecode account, deploying into the existing
rh Payara domain against the real local ruhunu database.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01SdnJvLPPvG7vmHoSXnVhF2
EOF
)"
git push origin qa1-home-ci-cd
gh pr create --repo hmislk/hmis --base development --head qa1-home-ci-cd \
  --title "ci: add home-hosted QA1 deploy workflow" \
  --body "$(cat <<'EOF'
## Summary
- Adds `hims_qa1_home_ci_cd.yml`: QA1 now deploys via a self-hosted GitHub
  Actions runner on hiu-laptop's carecode account instead of SSH into Azure.
- No secrets needed — the runner executes the deploy job on the target
  machine directly.

Part of the home-hosted QA infra migration tracked in
`hmislk/qa-home-infra`.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01SdnJvLPPvG7vmHoSXnVhF2
EOF
)"
```

- [ ] **Step 4: Wait for `check-branch` and merge**

```bash
gh pr checks --repo hmislk/hmis qa1-home-ci-cd --watch
gh pr merge --repo hmislk/hmis qa1-home-ci-cd --merge
```

Expected: checks pass, PR merges into `development`.

- [ ] **Step 5: Restore local persistence.xml, create and push the trigger branch**

```bash
git checkout development
git pull origin development
git checkout -b hims-qa1-home origin/development
git push origin hims-qa1-home
```

This push triggers the workflow for the first time.

- [ ] **Step 6: Watch the run**

```bash
gh run watch --repo hmislk/hmis $(gh run list --repo hmislk/hmis --workflow=hims_qa1_home_ci_cd.yml --limit 1 --json databaseId --jq '.[0].databaseId')
```

Expected: both `build` and `deploy` jobs complete with success, health check
step passes.

- [ ] **Step 7: Verify locally**

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:9080/qa1/faces/index1.xhtml
```

Expected: `200`.

- [ ] **Step 8: End-to-end verify against real data**

Per CLAUDE.md, a page render alone isn't sufficient proof. Log into
`http://localhost:9080/qa1` through the actual UI (menu navigation, not a
direct inner-page URL) using this domain's existing local login credentials,
select a department, and confirm at least one real screen loads with real
`ruhunu` data (e.g. patient search returns rows). This confirms QA1 is
genuinely usable, not just that Payara accepted the deploy.

- [ ] **Step 9: Restore local JNDI in persistence.xml (leave unstaged)**

```bash
git checkout development
git status
```

Expected: `persistence.xml` shows modified (the local `jdbc/coop` /
`jdbc/ruhunuAudit` JNDI names) — confirm it's unstaged, per CLAUDE.md's
"RESTORE LOCAL JNDI AFTER EVERY PUSH" rule (this local checkout was never
actually changed by this task, since the substitution happened in the cloud
build job's own checkout — this step is just confirming that's still true).

---

## Task 5: Close out QA1 in the master plan

**Files:**
- Modify (in `qa-home-infra`): `MASTER-PLAN.md`

- [ ] **Step 1: Check off QA1's rows**

Edit `MASTER-PLAN.md` in the `qa-home-infra` checkout: mark all six QA1
checklist items done, and change QA1's Status cell in the architecture table
from `🔲 in progress` to `✅ live (local only, pending QA3's nginx wiring)`.

- [ ] **Step 2: Commit and push**

```bash
cd /home/carecode/development/qa-home-infra
git add MASTER-PLAN.md
git commit -m "QA1 live on hiu-laptop (carecode account) - local verification complete"
git push origin main
```

- [ ] **Step 3: Report to the user**

Summarize: QA1 is deployed and verified locally at
`http://localhost:9080/qa1`; it's not yet publicly reachable at
`qa1.carecode.org` because that depends on QA3's nginx/router/certbot work
on the Desktop (its own runbook). Point them at
`https://github.com/hmislk/qa-home-infra` and remind them the buddhika
account can start its own session now using `runbooks/hiu-laptop-buddhika-qa2.md`.
