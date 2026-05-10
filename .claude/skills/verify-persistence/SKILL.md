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
[Local dev]  jdbc/ruhunu  →  [pre-push] restore placeholders  →  git push  →  [post-push] restore local names
```

## Pre-Push Steps (before committing/pushing)

1. **Read persistence.xml** at `src/main/resources/META-INF/persistence.xml`

2. **Check JNDI datasources** — must use environment variables:
   - `${JDBC_DATASOURCE}` (not `jdbc/coop`, `jdbc/ruhunu`, etc.)
   - `${JDBC_AUDIT_DATASOURCE}` (not `jdbc/ruhunuAudit`, etc.)

3. **Check DDL generation paths** — must NOT contain hardcoded paths:
   - No `eclipselink.application-location` with `c:/tmp/` or `/home/*/tmp/`

4. **Report findings** clearly:
   - If all correct: "Persistence.xml is deployment-ready"
   - If issues found: list each issue with current value and what it should be

5. **If issues found**: offer to fix by replacing hardcoded values with placeholders. Do NOT auto-fix without user confirmation.

## Post-Push Steps (after pushing)

After a successful push, ask the user for their local JNDI datasource names (e.g. `jdbc/ruhunu` and `jdbc/ruhunuAudit`), then offer to restore those values in `persistence.xml` so local testing can resume. This change must be left **unstaged** — never commit or push local datasource names.

## What's Correct vs Wrong

| Setting | Deployment-ready | Local dev only |
|---------|-----------------|----------------|
| Main datasource | `${JDBC_DATASOURCE}` | `jdbc/coop`, `jdbc/ruhunu`, etc. |
| Audit datasource | `${JDBC_AUDIT_DATASOURCE}` | `jdbc/ruhunuAudit`, etc. |
| DDL location | Not present or env var | `c:/tmp/`, `/home/buddhika/tmp/` |
