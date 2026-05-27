---
name: verify-persistence
description: >
  Verify and manage persistence.xml configuration around git push. Use before
  pushing to ensure environment variable placeholders are in place, and after
  pushing to restore local JNDI names for continued testing. Prevents QA
  deployment failures from hardcoded datasource names.
disable-model-invocation: true
allowed-tools: Read, Grep, Bash, Edit
---

# Verify Persistence Configuration

Manage `src/main/resources/META-INF/persistence.xml` across the push workflow.

## Full Push Workflow

```
[Local dev]  jdbc/ruhunu  →  [pre-push] swap to placeholders  →  git push  →  [post-push] swap back to jdbc/ruhunu
```

## Pre-Push Steps (before committing/pushing)

1. **Read persistence.xml** — note the current `<jta-data-source>` values in both
   `hmisPU` and `hmisAuditPU` units. If they are already placeholders, nothing to do.

2. **If hardcoded JNDI names are present** (e.g. `jdbc/ruhunu`, `jdbc/coop`):
   - **Remember them** — you will need to restore them after the push.
   - Replace with placeholders:
     - `jdbc/...` → `${JDBC_DATASOURCE}` (main unit)
     - `jdbc/...Audit` → `${JDBC_AUDIT_DATASOURCE}` (audit unit)
   - Commit the placeholder version, then push.

3. **Check DDL generation paths** — must NOT contain hardcoded paths:
   - No `eclipselink.application-location` with `c:/tmp/` or `/home/*/tmp/`

## Post-Push Steps (after every push — MANDATORY)

**Immediately after every `git push`**, restore the JNDI names that were there before:

- Replace `${JDBC_DATASOURCE}` → the original local JNDI name (e.g. `jdbc/ruhunu`)
- Replace `${JDBC_AUDIT_DATASOURCE}` → the original local audit JNDI name (e.g. `jdbc/ruhunuAudit`)

Leave this change **unstaged** — never commit or push local JNDI names.

Confirm to the user: `persistence.xml restored to local JNDI for testing.`

> **Why**: The developer needs to run and test immediately after every push. Without this step they have to manually revert persistence.xml every time, which is unnecessary friction.

## What's Correct vs Wrong

| Setting | Deployment-ready (push) | Local dev (test) |
|---------|------------------------|------------------|
| Main datasource | `${JDBC_DATASOURCE}` | `jdbc/coop`, `jdbc/ruhunu`, etc. |
| Audit datasource | `${JDBC_AUDIT_DATASOURCE}` | `jdbc/ruhunuAudit`, etc. |
| DDL location | Not present or env var | `c:/tmp/`, `/home/buddhika/tmp/` |
