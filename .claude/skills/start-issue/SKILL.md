---
name: start-issue
description: >
  Full environment setup when starting work on a GitHub issue. Creates a feature
  branch from origin/development, swaps persistence.xml to local JNDI settings,
  assigns the issue, and guides project board setup. Use when beginning work on
  any new issue: "start issue 20408", "/start-issue 20408", or "prepare env for issue 20408".
disable-model-invocation: false
allowed-tools: Bash, Read, Edit, Grep
argument-hint: "<issue-number> [short-description] [local-jndi-name]"
---

# Start Issue Workflow

Prepare the local development environment to fix a GitHub issue.

## Arguments

- `$0` — Issue number (required, e.g. `20408`)
- `$1` — Short kebab-case description (optional; derive from issue title if omitted)
- `$2` — Local JNDI datasource name (optional; read from `persistence_for_local_testing.xml` if omitted)

## Step 1 — Fetch Issue Title

```bash
gh issue view $0 --repo hmislk/hmis --json title,body --jq '.title'
```

Use the title to derive a short kebab-case description if `$1` is not provided.
Branch name format: `$0-<short-description>` (all lowercase, hyphens only).

## Step 2 — Create Feature Branch from development

```bash
git fetch origin
git checkout -b $0-<description> origin/development
git push -u origin $0-<description>
```

**Rule**: ALWAYS base on `origin/development`, never on `master`.

> ⚠️ **Check current branch first.** If `git branch --show-current` shows `master`, DO NOT proceed with uncommitted changes present — those changes will be carried onto the new branch and their ancestry will include `master`, causing PR conflicts. Instead, stash the changes (`git stash`), create the branch cleanly, then pop the stash (`git stash pop`).

## Step 3 — Set persistence.xml to Local JNDI

Read the local JNDI name from:
`src/main/resources/META-INF/persistence_for_local_testing.xml`

Look for the `<jta-data-source>` values — use those exact names.

Then update `src/main/resources/META-INF/persistence.xml`:
- Replace `${JDBC_DATASOURCE}` → the local main JNDI (e.g. `jdbc/ruhunu`)
- Replace `${JDBC_AUDIT_DATASOURCE}` → the local audit JNDI (e.g. `jdbc/ruhunuAudit`)

**Do NOT commit this change.** It stays local only. Before any push, revert to placeholders.
**Remember these JNDI names** — after every push they must be restored immediately so the
developer can test without any manual step.

## Step 4 — Assign Issue on GitHub

```bash
gh issue edit $0 --repo hmislk/hmis --add-assignee buddhika75
```

If the command fails with a scope error, inform the user that they need to run:
```
! gh auth refresh -s project
```

## Step 5 — Project Board (CareCode: HMIS Board)

Make sure the issue is on project #11, set **Status = In Progress**, then **read the status
back**. All three steps are mandatory: the update call's success response returns only the
item ID, not the new value, so it is not proof that the change was saved. Issue #24105 was
reported as "In Progress" from that response alone, but the board still showed Backlog.

The issue is often **already on the board**: the "Item added to project" automation puts new
issues in Backlog. `addProjectV2ItemById` handles both cases. If the issue is already on the
board, it returns the existing item instead of adding a duplicate.

```bash
# 1. Issue node ID
ISSUE_NODE=$(gh api repos/hmislk/hmis/issues/$0 --jq '.node_id')

# 2. Add to project #11, or get the existing item, and capture the item ID
ITEM_ID=$(gh api graphql -f query='
mutation($p:ID!,$c:ID!){ addProjectV2ItemById(input:{projectId:$p,contentId:$c}){ item{ id } } }' \
  -f p=PVT_kwDOAHw-zs4ApMln -f c="$ISSUE_NODE" --jq '.data.addProjectV2ItemById.item.id')

# 3. Set Status = In Progress
gh api graphql -f query='
mutation($p:ID!,$i:ID!){ updateProjectV2ItemFieldValue(input:{
  projectId:$p, itemId:$i,
  fieldId:"PVTSSF_lADOAHw-zs4ApMlnzggpN3k",
  value:{singleSelectOptionId:"47fc9ee4"} }){ projectV2Item{ id } } }' \
  -f p=PVT_kwDOAHw-zs4ApMln -f i="$ITEM_ID"

# 4. READ BACK: this output is the only thing that may be reported as the board status
gh api graphql -f query='
query($i:ID!){ node(id:$i){ ... on ProjectV2Item{
  fieldValueByName(name:"Status"){ ... on ProjectV2ItemFieldSingleSelectValue{ name updatedAt } } } } }' \
  -f i="$ITEM_ID" --jq '.data.node.fieldValueByName'
```

IDs, verified 2026-10-01: project `PVT_kwDOAHw-zs4ApMln`, Status field
`PVTSSF_lADOAHw-zs4ApMlnzggpN3k`, "In Progress" option `47fc9ee4`. If a mutation fails with
an unknown or invalid ID error, re-query them:
`gh api graphql -f query='query{organization(login:"hmislk"){projectV2(number:11){id field(name:"Status"){... on ProjectV2SingleSelectField{id options{id name}}}}}}'`

**If step 4 does not print `"name":"In Progress"`**, do not retry silently and do not report
success. Tell the user that the automated status change did not stick, show what the board
actually holds, and ask them to set it manually at https://github.com/orgs/hmislk/projects/11.

If the token lacks the `project` scope (error: INSUFFICIENT_SCOPES), tell the user:

> **Action required**: Your GitHub token lacks the `project` scope for board automation.
> Please add the issue to the board manually:
> https://github.com/orgs/hmislk/projects/11
> Set **Status = In Progress**.
>
> To enable automation in future, run: `! gh auth refresh -s project`

## Step 6 — Summary

Report what was done:
- Branch name created and pushed
- persistence.xml local JNDI set to (name)
- Issue assigned to buddhika75
- Project board status: quote the value read back in Step 5 step 4, or say that a manual
  change is needed. Never report the status from the update call's success response alone.

Remind the user: **persistence.xml must be reverted to `${JDBC_DATASOURCE}` before any git push.**
Check it directly (or let `commit-code` catch it — it verifies persistence.xml when staged).

## Reference

See [start-issue-workflow.md](../../../developer_docs/git/start-issue-workflow.md) for full details.
