# Deployment Recovery Guide

## Root-Owned Files Breaking CI/CD Deployment

### What Happened

If a Claude Code session (or any manual process) runs `asadmin`, copies WARs, or touches
Payara directories **as root** (e.g. via `sudo`), it leaves files owned by `root` inside
Payara's domain directories. The normal CI/CD pipeline runs as `appuser`, so subsequent
`asadmin undeploy` / `deploy` commands fail with errors like:

```
Command deploy failed.
remote failure: Error occurred during deployment: Exception while loading the app :
Error in committing security policy for ejbs of coop --
javax.security.jacc.PolicyContextException: java.lang.RuntimeException:
Failure removing policy file: .../generated/policy/coop/...granted.policy.

java.io.FileNotFoundException: .../applications/__internal/coop/coop.war (Permission denied)
```

The app gets **completely undeployed** and cannot be redeployed until the stale files are removed.

### Prevention

**Never run `asadmin`, `sudo cp`, `sudo rsync`, or any Payara command as root.**
All deployments must go through GitHub Actions CI/CD. See rule #16 in CLAUDE.md.

### Recovery Steps

Run these commands (SSH to the affected VM, e.g. `ssh hmis08`):

**Step 1 — Fix log file ownership** (blocks Payara from writing logs):
```bash
sudo chown appuser:appuser /opt/payara5/glassfish/domains/domain1/logs/server.log
```

**Step 2 — Remove stale root-owned application files:**
```bash
sudo rm -rf /opt/payara5/glassfish/domains/domain1/applications/__internal/<appname>
sudo rm -rf /opt/payara5/glassfish/domains/domain1/applications/<appname>
sudo rm -rf /opt/payara5/glassfish/domains/domain1/generated/policy/<appname>
sudo rm -rf /opt/payara5/glassfish/domains/domain1/generated/ejb/<appname>
sudo rm -rf /opt/payara5/glassfish/domains/domain1/generated/xml/<appname>
```

**Step 3 — Fix ownership of the entire Payara domain:**
```bash
sudo chown -R appuser:appuser /opt/payara5/glassfish/domains/domain1/applications/
sudo chown -R appuser:appuser /opt/payara5/glassfish/domains/domain1/generated/
```

**Step 4 — Redeploy via asadmin** (only if CI/CD pipeline cannot be re-run):
```bash
echo 'AS_ADMIN_PASSWORD=<payara-admin-password>' > /tmp/p.txt
/opt/payara5/bin/asadmin --user admin --passwordfile /tmp/p.txt deploy \
  --contextroot <appname> /home/appuser/app/latest/<appname>.war
rm /tmp/p.txt
```

Replace `<appname>` with the app context (e.g. `coop`, `roseth`) and use the Payara admin
password from `C:\Credentials\Credentials.txt`.

**Step 5 — Verify the app is up:**
```bash
curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/<appname>/
# Expected: 302 (redirect to login)
```

### Preferred Alternative to Step 4

Re-run the existing CI/CD pipeline from the GitHub Actions UI instead of deploying manually:

1. Go to https://github.com/hmislk/hmis/actions
2. Find the relevant prod pipeline (e.g. "COOP-PROD Build & Deployment Pipeline")
3. Click the failed/cancelled run → **Re-run all jobs**

This is safer than manual `asadmin` because the pipeline also handles WAR upload, health
checks, and correct file permissions.

### Why This Is Dangerous

- Root-owned files persist across Payara restarts and VM reboots
- A root-owned `server.log` silently swallows all startup logs — making diagnosis very hard
- `asadmin undeploy` removes the application directory but leaves `__internal/` and
  `generated/policy/` stale, blocking any subsequent deploy
- The app ends up completely undeployed with no error visible to users until they try to log in
