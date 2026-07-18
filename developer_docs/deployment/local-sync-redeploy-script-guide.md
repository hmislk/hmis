# Local Sync-and-Redeploy Script Guide

This guide documents how to generate a personal `sync-and-redeploy` script for
your own machine, so that fetching `origin/development`, restoring your local
JNDI settings, building, and redeploying to your local Payara domain is a
single command instead of a hand-edited, machine-specific script.

This is **local-development-only**. QA/production deployment is already
automated by CI/CD — see [QA Deployment Guide](qa-deployment-guide.md) and
[CI/CD Pipeline Overview](ci-cd-pipeline-overview.md).

## Why a Generator, Not a Shared Script

Every developer's machine differs in ways that make a single checked-in
script wrong for everyone else:

- **OS**: Windows vs Ubuntu
- **Java location**: bundled with an IDE, a system package, or a standalone JDK
- **Payara install location**: no fixed path across machines (see
  [DEVELOPMENT_ENVIRONMENTS.md](../DEVELOPMENT_ENVIRONMENTS.md))
- **Payara domain name**: usually `domain1`, but not always (e.g. `rh`)
- **Admin/HTTP ports**: usually `4848`/`8080`, but not always — one known
  machine runs its `rh` domain on admin port `9048` and HTTP port `9080`
- **Deployed application name/context-root**: must match what's already
  registered in that domain, or redeploy fails (see the collision bug below)

Hardcoding any of these into a single script that gets copy-pasted between
machines silently breaks the moment someone's setup differs. Instead, run the
generator once per machine; it detects your setup and writes a script
tailored to it.

## Running the Generator

**Windows:**
```cmd
scripts\generate-sync-redeploy-script.bat
```

**Linux/Ubuntu:**
```bash
./scripts/generate-sync-redeploy-script.sh
```

Both accept:
- `--yes` — skip the confirmation prompt (non-interactive)
- `--domain <name>` — pin the Payara domain instead of being asked when more
  than one is found

The generator writes the runtime script to your **home directory**
(`$HOME/sync-and-redeploy.sh` on Linux, `%USERPROFILE%\sync-and-redeploy.bat`
on Windows) — never inside the repo checkout, matching how the original
one-off script was used. It also writes a small per-machine config file next
to it (`$HOME/.hmis-sync-redeploy.conf` / `%USERPROFILE%\hmis-sync-redeploy.conf.bat`)
recording what it detected, so re-running the generator later reuses your
answers instead of re-asking.

The generated script and its config file are personal to your machine and are
never committed to this repo.

## Detection Steps

### 1. Operating System

- Linux/Mac/Git Bash: `uname -s`
- Windows (native `.bat`): the script *is* the OS-specific branch, so no
  runtime detection is needed there; `%OS%` (`Windows_NT`) is checked only as
  a sanity guard.

### 2. Java Install

Used to run Payara and to build with Maven. Detection order:

1. `JAVA_HOME` environment variable, if set.
2. `which java` (Linux) / `where java` (Windows).
3. Compare against the JDK level this project actually compiles with —
   **check `pom.xml`, don't hardcode "11"**:
   ```bash
   grep -A2 "maven-compiler-plugin" pom.xml | grep release
   ```
   As of this writing, `pom.xml` sets `<release>11</release>`, so a JDK 11
   (Temurin/Adoptium builds are known-good) is required. If `pom.xml` changes
   this in the future, the generator's detected Java must be checked against
   whatever `pom.xml` says at generation time, not this document.

### 3. Payara Install Location

There is no single fixed path. Detection order:

1. `PAYARA_HOME` environment variable, if set.
2. Known candidate paths per OS (extend
   [DEVELOPMENT_ENVIRONMENTS.md](../DEVELOPMENT_ENVIRONMENTS.md) as new
   machines are documented):
   - Linux: `$HOME/payara`, `/opt/payara5`, `/opt/payara6`, `/home/carecode/payara`
   - Windows: `C:\Payara5`, `D:\Payara`, `%USERPROFILE%\Payara_Server`
3. Fallback bounded search for an `asadmin` / `asadmin.bat` binary a few
   directories deep under common install roots (never a full filesystem
   scan):
   - Linux: `find "$root" -maxdepth 4 -iname asadmin -type f 2>/dev/null`
     over `$HOME`, `/opt`, `/usr/local`
   - Windows: `where /r "%root%" asadmin.bat` over `C:\`, `D:\`,
     `%USERPROFILE%`, bounded to a few known roots rather than scanning every
     drive

### 4. Payara Domain, Admin Port, HTTP Port

A machine can have more than one domain. **Never assume the default admin
port 4848** — one known machine (`carecode`, domain `rh`) runs admin on
`9048` and HTTP on `9080`.

1. Enumerate domains: `asadmin list-domains` (or list the directory names
   under `<payara_home>/glassfish/domains/`).
2. For each domain found, read its actual ports out of
   `<payara_home>/glassfish/domains/<domain>/config/domain.xml`:
   ```bash
   grep 'network-listener' domain.xml
   # <network-listener name="admin-listener" port="4848" .../>
   # <network-listener name="http-listener-1" port="8080" .../>
   ```
3. If exactly one domain is found, use it. If more than one is found, **ask
   the operator which one to target** (or accept a `--domain` flag) — never
   silently guess.

### 5. Already-Deployed Application Name and Context Root

**This step is required before ever calling `asadmin deploy`** — see the
collision bug below for why.

```bash
asadmin --port <admin-port> list-applications
asadmin --port <admin-port> list-applications --long=true
```

If an app is already deployed, use its exact reported name and context root
for every subsequent redeploy. If nothing is deployed yet, default
`--name`/`--contextroot` to `rh` (matching the CI/CD convention — cross-check
`.github/workflows/` if this ever seems out of date), but this default must
be overridable.

### 6. Local JNDI Names

The generator does **not** reimplement the JNDI swap logic that already
exists in this repo:

- [`scripts/prepare-for-push.sh`](../../scripts/prepare-for-push.sh) — swaps
  local JNDI names → `${JDBC_DATASOURCE}` / `${JDBC_AUDIT_DATASOURCE}`,
  backing up the local values to `.jndi-backup-main` / `.jndi-backup-audit`
- [`scripts/restore-local-jndi.sh`](../../scripts/restore-local-jndi.sh) —
  restores local JNDI names from that backup

These scripts already assume you know your local JNDI values; the generator
only needs to *ask for them once* (or read them from
`src/main/resources/META-INF/persistence_for_local_testing.xml` if present)
and remember the answer in the per-machine config file described above.

## Reused vs. New Logic

| Concern | Source |
|---|---|
| JNDI swap (local → placeholder, placeholder → local) | Reused: `scripts/prepare-for-push.sh`, `scripts/restore-local-jndi.sh` |
| Maven binary selection | Reused: `detect-maven.sh` / `detect-maven.bat` if present, else `mvn` on `PATH` |
| OS/Java/Payara/domain/port/app detection | New: `scripts/generate-sync-redeploy-script.sh` / `.bat` |
| Merge-and-redeploy runtime flow | New: the generated `sync-and-redeploy.sh` / `.bat` itself |

## What the Generated Script Does at Run Time

1. `cd` into the repo checkout path recorded at generation time.
2. Handle uncommitted changes: stash any uncommitted edit to
   `persistence.xml` **separately** first (so hardcoded local JNDI values
   never land in a commit), then commit anything else uncommitted.
3. `git fetch origin development && git merge origin/development --no-edit`.
   **On any conflict, the script stops immediately** and leaves the
   repository mid-merge for manual resolution — it never attempts automatic
   conflict resolution.
4. Re-apply the stashed `persistence.xml` edit (or run
   `scripts/restore-local-jndi.sh` if a `.jndi-backup-*` pair exists instead).
5. Build with the Maven detected at generation time (via `detect-maven.sh` /
   `.bat` when present): `mvn clean package -DskipTests`.
6. Verify the resulting WAR exists and is non-trivially sized before
   deploying (defends against the truncation issue below); rebuild once if
   it looks wrong.
7. Deploy with the **discovered** `--name` / `--contextroot` / admin port —
   never re-derived from the WAR filename.
8. Poll `server.log` for the domain's actual success/failure line and report
   it clearly.

## Troubleshooting

### App-name / context-root collision on deploy

**Symptom** — exact error text:

```
remote failure: Error occurred during deployment: java.lang.Exception: Virtual server server already
has a web module rh loaded at /rh therefore web module rh-3.0.0 cannot be loaded at this context path
on this virtual server.
Command deploy failed.
```

**Cause**: `asadmin deploy` derives the application name from the WAR
filename when `--name` is omitted (e.g. `rh-3.0.0.war` → app name
`rh-3.0.0`), and Payara also auto-derives a context root from that name. If
the domain already has a different app registered under a different
name/context-root serving the same virtual server path, the mismatched name
collides with the existing module instead of replacing it.

**Fix**: Always discover the already-deployed app's actual `--name` and
`--contextroot` first (`asadmin --port <port> list-applications`,
`--long=true` for more detail) and pass them explicitly on every `deploy
--force=true` call, so the existing module is replaced in place instead of
colliding. This is why step 5 above is mandatory, not optional.

### WAR file found truncated (0 bytes) after a failed deploy

Observed once, in the same session as the collision bug above: immediately
after a failed `asadmin deploy` call, `target/rh-3.0.0.war` was found
truncated to 0 bytes, requiring a full `mvn clean package` rebuild before the
next deploy attempt had a file to send. **The causation between the failed
deploy and the truncation was never root-caused** — treat it as an observed
correlation, not a confirmed mechanism. The generated script defends against
it defensively rather than by fixing a root cause: it checks the WAR's size
immediately before every `asadmin deploy` call and rebuilds if it looks
stale or empty (step 6 above).

## Related Documentation

- [Development Environments](../DEVELOPMENT_ENVIRONMENTS.md) — per-machine
  Maven/Payara/domain/port table
- [Persistence.xml Verification Guide](persistence-verification.md) — the
  JNDI pre-push/post-push convention this script must not deviate from
- [QA Deployment Guide](qa-deployment-guide.md) — the separate, already
  automated QA/production deployment path (out of scope here)
