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

Delete every local branch whose PR has already been merged — plus any branch
with no PR of its own whose commits are all already on `origin/development`
(e.g. a `gh pr checkout <N>` review checkout) — then bring `development` up to
date. Leave `persistence.xml` with local JNDI names restored (unstaged) at the
end.

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

- If a merged PR is found → **mark for deletion (merged-PR)**, record the PR
  number and title for the report. No further checks — Step 5 force-deletes it.
- If no merged PR is found, also check without `--base` filter (in case the base was changed):

```bash
gh pr list --head <branch> --state merged --repo hmislk/hmis --json number,title,baseRefName,mergedAt --jq '.[0]'
```

- If still no merged PR is found, do **not** delete on merge status alone — run
  a patch-equivalence check. A `gh pr checkout <N>` review checkout (e.g.
  `pr-23617`) has no PR with *it* as the head branch, but its commits may
  already be on `origin/development`. Compare against `origin/development`
  (freshly fetched in Step 2 — local `development` is not fast-forwarded until
  Step 6):

  ```bash
  git cherry -v origin/development <branch>
  ```

  - **Empty output**, or every line starts with `-` → every commit on the branch
    is already patch-equivalent on `origin/development`. **Mark for deletion
    (content-merged)** — reported in its own section, separate from the
    merged-PR deletions, so a non-standard removal is never silent.
  - **Any line starts with `+`** → the branch has a commit not on
    `origin/development` — genuinely unmerged work, *or* a multi-commit branch
    whose PR was squash-merged (no per-commit equivalent exists). Either way,
    **skip** and warn; the report line tells the user to check whether it was a
    squashed PR and delete manually if so.

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

Every branch that reaches this step was already vetted in Step 3 — it has
either a **merged PR** or an **empty `git cherry`** against `origin/development`.
Branches with unmerged work were skipped there and never marked. So Step 5 only
deletes; it does not re-decide safety.

For each marked branch, try the safe delete first, then force:

```bash
git branch -d <branch> || git branch -D <branch>
```

`-d` succeeds only when the branch tip is reachable from local `development`.
It normally **refuses** here — the PR was merged with a merge commit or a
squash, and local `development` is not fast-forwarded until Step 6, so the tip
is not yet an ancestor. That refusal is expected, not a warning sign; the
`|| git branch -D` completes the delete.

Do **not** add a `git log origin/<branch>..<branch>` guard: Step 2's
`git fetch --prune` deletes the `origin/<branch>` upstream as soon as the PR is
merged and GitHub removes the remote branch, so that command errors with
`unknown revision or path not in the working tree` rather than returning empty.
`git branch -D` still prints the deleted SHA (`Deleted branch X (was 906d5ebbb4)`)
and the reflog retains it, so an over-eager delete is recoverable.

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
✓ Deleted branches (merged PR):
  - <branch>  (PR #NNN merged → <base-branch>)
  ...

✓ Deleted branches (no PR, but all commits already in development):
  - <branch>
  ...

⚠ Skipped branches:
  - <branch>  (has commit(s) not in development — if its PR was squash-merged,
    delete manually after confirming)
  ...

✓ development is now at <short-sha> (<commit subject>)
✓ persistence.xml restored to local JNDI settings (unstaged)
```

Omit any of the three branch sections that has no entries. If nothing was
stashed, replace the last line with:
`✓ persistence.xml unchanged (no local changes were present)`

## Notes

- Never delete `development`, `master`, or any `*-prod` branch.
- The known production branches are:
  `coop-prod`, `ruhunu-prod`, `southernlanka-prod`, `rmh-prod`,
  `digasiri-prod`, `mp-prod`, `roseth-prod`, `horizon-prod`,
  `asiripharmacy-prod`, `engagewellness-prod`
- After this skill completes the developer is on `development`, up to date,
  with only unmerged feature branches remaining locally.
