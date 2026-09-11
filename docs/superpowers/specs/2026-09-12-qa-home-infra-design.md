# QA Home Infrastructure — Design

## Goal

Move the four QA environments (QA1–QA4) off the Azure "migrated" estate onto
four home-hosted machines, to cut hosting cost. Each QA instance runs against
a local hospital database that already exists on its target machine. Uptime
requirements are low — this is for occasional testing, not 24/7 availability.

## Physical / network layout

All four targets sit behind one home router, public IP `124.43.16.16`.
DNS is already correct: `qa1.carecode.org`–`qa4.carecode.org` all resolve to
that IP.

| Machine | Account | LAN IP | Instance | Local DB |
|---|---|---|---|---|
| hiu-laptop (this computer) | carecode | 192.168.1.203 | QA1 | ruhunu |
| hiu-laptop (this computer) | buddhika | 192.168.1.203 (different port) | QA2 | coop |
| Desktop | — | 192.168.1.201 | QA3 + nginx | southernlanka |
| carecode-laptop | — | 192.168.1.204 | QA4 | roseth |

The desktop already runs Payara/JDK/MySQL and has the southernlanka DB
restored. carecode-laptop already runs Payara/JDK/MySQL and has the roseth DB
restored. Both are treated as ready; no fresh install needed there.

Router configuration: forward **only** ports 80 and 443**, to the desktop
(192.168.1.201)**. No other port is opened to the internet — no SSH, no
Payara admin ports (e.g. this laptop's carecode-account admin port 9048 stays
LAN-only). This is a deliberate reduction in attack surface versus the
previous plan of exposing SSH per machine.

## nginx (desktop, single public entry point)

nginx on the desktop is the only internet-facing service. It terminates TLS
for all four subdomains via Let's Encrypt (certbot, HTTP-01 challenge, since
port 80 is forwarded to it) and reverse-proxies by hostname:

- `qa1.carecode.org` → `192.168.1.203:<qa1-http-port>`
- `qa2.carecode.org` → `192.168.1.203:<qa2-http-port>`
- `qa3.carecode.org` → `127.0.0.1:<qa3-http-port>` (QA3 lives on the desktop itself)
- `qa4.carecode.org` → `192.168.1.204:<qa4-http-port>`

Exact ports are filled in per-machine as each is set up (see Rollout below);
this laptop's carecode-account domain `rh` already listens on 9080/9081, so
QA1's port will be one of those (confirmed during QA1 setup).

## Application hosting

Each QA instance deploys into the **existing** Payara domain already running
under that account/machine — not a new dedicated domain. Rationale: there's
already isolation at the machine/account level, and uptime isn't critical
enough to justify managing a second domain per target.

Each QA app reuses **whichever existing local JNDI datasource already points
at the correct hospital database** — verified per machine, not assumed from
naming. This matters: on this laptop's carecode account, the existing
`jdbc/coop` JNDI resource turned out to point at a database literally named
`coop` (the developer's own dev/testing convention), not `ruhunu`, despite
the confusing name. QA1 therefore gets its **own new** JDBC pool/resource
(`jdbc/qa1Main`) pointing at the actual local `ruhunu` database, so it tests
real Ruhunu data and never shares state with the developer's personal
`coop`-pool dev work. QA1's audit JNDI reuses the existing `jdbc/ruhunuAudit`
→ `rhAuditPool` → `rhAudit` database as-is (shared with personal dev use;
audit tables are generic change logs, low risk to mix).

The other three machines must go through the same verification (check what
database a candidate JNDI/pool actually connects to — `asadmin get
resources.jdbc-connection-pool.<pool>.property.databaseName` — before
assuming it's reusable) rather than trusting the JNDI name alone. This
mirrors the existing CI substitution mechanism (`${JDBC_DATASOURCE}` /
`${JDBC_AUDIT_DATASOURCE}` in `persistence.xml`), just resolved to local pool
names instead of Azure's `jdbc/qaN`.

## CI/CD

Self-hosted GitHub Actions runners, one registered per target (4 total:
carecode account on hiu-laptop, buddhika account on hiu-laptop, desktop,
carecode-laptop). Each runner polls GitHub outbound, so **no inbound deploy
port is needed anywhere** — this replaces the SSH+rsync approach used for the
Azure estate.

Workflow shape mirrors the existing `hims_qaN_migrated_ci_cd.yml` files:

- **build** job stays on `ubuntu-latest` — same JDK/Maven build, same JNDI
  placeholder substitution and verification steps, uploads the WAR as a
  build artifact.
- **deploy** job changes to `runs-on: [self-hosted, qaN]` and drops the
  SSH/rsync/remote-`asadmin`-over-SSH steps — it downloads the artifact and
  runs `asadmin deploy` directly, since the runner already executes on the
  target machine.

New workflow files (e.g. `hims_qa1_home_ci_cd.yml`, one per QA instance)
live in the main `hmis` repo alongside the existing 30+ workflows, triggered
by `hims-qaN-home` branches, using the same `deploy-qa`-skill-driven
PR/merge flow as today. The Azure `hims-qaN-migrated` workflows and branches
are retired once cutover is verified per instance (not deleted immediately —
kept as a rollback path until the home instance has proven stable).

## Tracking repo

New private repo: **`hmislk/qa-home-infra`**. Contents:

- `MASTER-PLAN.md` — architecture summary (this document's essentials) plus
  a per-machine checklist (runner registered? nginx upstream added? cert
  issued? first deploy verified end-to-end?) that gets checked off via
  commits as each machine's setup progresses. This is the single source of
  truth other machines' Claude Code sessions read before starting their
  portion, and update when they finish it.
- One runbook per machine: `hiu-laptop-carecode.md`, `hiu-laptop-buddhika.md`,
  `desktop.md`, `carecode-laptop.md` — each with that machine's exact steps
  (self-hosted runner install/registration, JNDI names to reuse, ports,
  nginx upstream line to add).
- The nginx config itself (single file, applied on the desktop), checked in
  so changes to the upstream list are reviewable and reproducible.

## Rollout order

1. Create `hmislk/qa-home-infra`, write `MASTER-PLAN.md` and the four
   runbook stubs.
2. Do everything possible now for **QA1** (this session, carecode account,
   hiu-laptop): confirm/allocate the Payara port, register the self-hosted
   runner, write the `hims_qa1_home_ci_cd.yml` workflow, do a first deploy,
   verify against a real workflow end-to-end.
3. Hand off runbooks for QA2 (buddhika account, separate Claude Code
   session on this same laptop), QA3 (desktop, including nginx + certbot +
   router port-forward, since QA3 and nginx are colocated), and QA4
   (carecode-laptop).
4. Once all four are verified working through `qaN.carecode.org` publicly,
   decommission the Azure QA VM.

## What this session can and can't do directly

This session has shell access only to hiu-laptop's carecode account.
Router port-forwarding, and setup on the desktop, carecode-laptop, and the
buddhika account, require either the user directly or a separate Claude
Code session logged into that account/machine, driven by its runbook in
`qa-home-infra`.

## Out of scope

- Containerization / infrastructure-as-code for these machines (YAGNI given
  the low-uptime, four-machine scale of this migration).
- High-availability or failover for any QA instance — explicitly not needed.
- Changing how production/staging environments are deployed.
