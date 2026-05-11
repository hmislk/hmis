# Start Issue Workflow

Complete environment setup when beginning work on a GitHub issue.

## Overview

Before fixing an issue, the local development environment must be prepared:
1. Create a feature branch from `origin/development`
2. Set `persistence.xml` to local JNDI settings
3. Assign the issue on GitHub
4. Add the issue to the project board and set status to **In Progress**

Use the `/start-issue` skill to automate all of these steps.

## Branch Naming

Format: `<issueNumber>-<short-kebab-case-description>`

Examples:
- `20408-fix-cashier-bill-total-calculation`
- `19887-collection-center-receipt-pdf`

## Persistence.xml Swap

The `persistence.xml` committed on `development` uses CI/CD placeholders:
- `${JDBC_DATASOURCE}` → replace with local JNDI (e.g. `jdbc/ruhunu`)
- `${JDBC_AUDIT_DATASOURCE}` → replace with local audit JNDI (e.g. `jdbc/ruhunuAudit`)

The correct local JNDI names are stored in:
`src/main/resources/META-INF/persistence_for_local_testing.xml`

**Before pushing to remote**, revert `persistence.xml` back to placeholders (or use `/commit-code` / `verify-persistence` skill which checks this automatically).

Reference: [persistence-workflow.md](../persistence/persistence-workflow.md)

## GitHub Steps (requires `project` scope on token)

```bash
# Assign issue
gh issue edit <number> --repo hmislk/hmis --add-assignee <github-username>

# Add to project board and set In Progress — requires GraphQL with project scope
# If token lacks the scope, do it manually at:
# https://github.com/orgs/hmislk/projects/11
```

### Token Scope Note

Project board manipulation requires the `project` OAuth scope. If the `gh` CLI returns a scope error:

1. Run `! gh auth refresh -s project` in the Claude Code prompt to re-authenticate with the extra scope, **or**
2. Add the item and set status manually on the board at https://github.com/orgs/hmislk/projects/11

## Quick Reference

| Step | Command / Action |
|------|-----------------|
| Fetch & branch | `git checkout -b <branch> origin/development` |
| Push branch | `git push -u origin <branch>` |
| Local persistence | Replace `${JDBC_DATASOURCE}` → `jdbc/ruhunu` in `persistence.xml` |
| Assign issue | `gh issue edit <N> --repo hmislk/hmis --add-assignee <user>` |
| Project board | https://github.com/orgs/hmislk/projects/11 (manual if no token scope) |
