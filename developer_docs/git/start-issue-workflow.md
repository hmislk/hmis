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

See [Commit Conventions § Feature Branches](commit-conventions.md#feature-branches) for the naming format. Always branch from `origin/development`, never `master`.

## Persistence.xml Swap

The `persistence.xml` committed on `development` uses CI/CD placeholders:
- `${JDBC_DATASOURCE}` → replace with local JNDI (e.g. `jdbc/ruhunu`)
- `${JDBC_AUDIT_DATASOURCE}` → replace with local audit JNDI (e.g. `jdbc/ruhunuAudit`)

The correct local JNDI names are stored in:
`src/main/resources/META-INF/persistence_for_local_testing.xml`

**Before pushing to remote**, revert `persistence.xml` back to placeholders — check it directly, or let `/commit-code` catch it (it verifies persistence.xml whenever it's staged).

Reference: [persistence-workflow.md](../persistence/persistence-workflow.md)

## GitHub Steps (requires `project` scope on token)

```bash
# Assign issue
gh issue edit <number> --repo hmislk/hmis --add-assignee <github-username>

# Add to project board and set In Progress — requires GraphQL with project scope
# If token lacks the scope, do it manually at:
# https://github.com/orgs/hmislk/projects/11
```

### Setting the board status (and verifying it)

The full command sequence is in the [`start-issue` skill, Step 5](../../.claude/skills/start-issue/SKILL.md#step-5--project-board-carecode-hmis-board). It has four steps:

1. Get the issue's node ID.
2. `addProjectV2ItemById`. This returns the existing board item if the issue is already on the board, which is common because the "Item added to project" automation puts new issues in **Backlog**.
3. `updateProjectV2ItemFieldValue` to set Status = In Progress.
4. **Read the Status back** with `fieldValueByName(name:"Status")`.

Step 4 is required. The update mutation's success response returns only the item ID, not the saved value, so it does not show that the change took effect. For issue #24105 the status was reported as "In Progress" based on that response alone, while the board still showed Backlog. Report only the value read back. If it isn't "In Progress", ask for a manual change on the board.

### Token Scope Note

Project board manipulation requires the `project` OAuth scope. If the `gh` CLI returns a scope error:

1. Run `! gh auth refresh -s project` in the Claude Code prompt to re-authenticate with the extra scope, **or**
2. Add the item and set status manually on the board at https://github.com/orgs/hmislk/projects/11

## Quick Reference

| Step | Command / Action |
|------|-----------------|
| Check current branch | `git branch --show-current` — must NOT be `master` before branching |
| Fetch & branch | `git checkout -b <branch> origin/development` |
| Push branch | `git push -u origin <branch>` |
| Local persistence | Replace `${JDBC_DATASOURCE}` → `jdbc/ruhunu` in `persistence.xml` |
| Assign issue | `gh issue edit <N> --repo hmislk/hmis --add-assignee <user>` |
| Project board | https://github.com/orgs/hmislk/projects/11 (manual if no token scope) |
