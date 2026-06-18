---
name: cleanup-branches
description: >
  Delete all merged local branches and fast-forward development to origin.
  Handles both feature branches (merged to development) and hotfix branches
  (ending in -hotfix, merged to any production branch). Stashes and restores
  persistence.xml local JNDI settings automatically. Use after PRs are merged
  to clean up local git state in one command.
allowed-tools: Bash
---

# Cleanup Merged Local Branches

Delete every local branch whose PR has already been merged, then bring
`development` up to date. Leave `persistence.xml` with local JNDI names
restored (unstaged) at the end.

## Step 1 — Stash Local persistence.xml Changes

Only stash `persistence.xml` — not all uncommitted work. This prevents
unrelated WIP from being moved onto `development` when the stash is popped.

```bash
git stash push -- src/main/resources/META-INF/persistence.xml
```

If this reports "No local changes to save", that is fine — continue.
Note whether a stash was created so you know whether to pop it in Step 7.

## Step 2 — Fetch Latest from Origin

```bash
git fetch origin --prune
```

`--prune` removes remote-tracking refs for branches deleted on GitHub.

## Step 3 — Collect All Local Branches Except development

List every local branch except `development`:

```bash
git branch --format='%(refname:short)' | grep -v '^development$'
```

For each branch, determine whether it is safe to delete:

### Feature branches (do NOT end with `-hotfix`)

Check if any PR targeting `development` from this branch is merged:

```bash
gh pr list --head <branch> --base development --state merged --repo hmislk/hmis --json number,title,mergedAt --jq '.[0]'
```

- If a merged PR is found → **mark for deletion**, record the PR number and title for the report.
- If no merged PR is found, also check without `--base` filter (in case the base was changed):

```bash
gh pr list --head <branch> --state merged --repo hmislk/hmis --json number,title,baseRefName,mergedAt --jq '.[0]'
```

- If still no merged PR → **skip** (warn the user about the branch).

### Hotfix branches (end with `-hotfix`)

Hotfix PRs target a production branch, not `development`. Check for any merged PR:

```bash
gh pr list --head <branch> --state merged --repo hmislk/hmis --json number,title,baseRefName,mergedAt --jq '.[0]'
```

- If a merged PR is found → **mark for deletion**, record PR number, title, and the production branch it targeted.
- If no merged PR → **skip** (warn the user).

## Step 4 — Switch to development

```bash
git checkout development
```

## Step 5 — Delete Marked Branches

For each branch marked for deletion, first try the safe delete:

```bash
git branch -d <branch>
```

If `-d` refuses, check whether the local branch has commits that are not on
the remote (i.e. local-only work added after the PR was merged):

```bash
git log origin/<branch>..<branch> --oneline
```

- If the output is **empty** — the local tip matches the remote; the refusal
  is just because the merge commit was squashed/rebased and git cannot trace
  it locally. It is safe to force-delete:

  ```bash
  git branch -D <branch>
  ```

- If the output shows **local-only commits** — the branch has work that was
  never pushed. **Skip this branch** and warn the user instead of deleting:

  ```
  ⚠ Skipped <branch>: has local commits not present on origin — delete manually after review.
  ```

## Step 6 — Fast-Forward development to origin/development

Use `--ff-only` so git stops with an error if the local `development` has
diverged (e.g. local commits not yet pushed), rather than silently discarding
them:

```bash
git merge --ff-only origin/development
```

If this fails with "Not possible to fast-forward", the local `development`
has commits that are not on `origin/development`. Report this to the user and
stop — do not force-reset. The user must resolve the divergence manually.

## Step 7 — Restore persistence.xml

If a stash was created in Step 1 (only `persistence.xml` was stashed):

```bash
git stash pop
```

If the stash pop reveals conflicts (unlikely but possible), report them to the
user and stop — do not resolve conflicts automatically.

If no stash was created, leave `persistence.xml` as-is.

## Step 8 — Report

Print a summary:

```
✓ Deleted branches:
  - <branch>  (PR #NNN merged → <base-branch>)
  ...

⚠ Skipped branches (no merged PR found):
  - <branch>
  ...

✓ development is now at <short-sha> (<commit subject>)
✓ persistence.xml restored to local JNDI settings (unstaged)
```

If all branches were deleted (nothing skipped), omit the skipped section.
If nothing was stashed, replace the last line with:
`✓ persistence.xml unchanged (no local changes were present)`

## Notes

- Never delete `development`, `master`, or any `*-prod` branch.
- The known production branches are:
  `coop-prod`, `ruhunu-prod`, `southernlanka-prod`, `rmh-prod`,
  `digasiri-prod`, `mp-prod`, `roseth-prod`, `horizon-prod`,
  `asiripharmacy-prod`, `engagewellness-prod`
- After this skill completes the developer is on `development`, up to date,
  with only unmerged feature branches remaining locally.
