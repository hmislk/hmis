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

For each branch, determine whether it is safe to delete.

### Protected branches — never delete, never classify

Before the feature/hotfix split, skip any branch that is `master` or ends with
`-prod` (the local mirrors of admin-managed / production branches — see Notes
for the full list). They diverge from `development` by design, so a naïve
patch-equivalence check could still misfire on them; the explicit skip is the
guarantee. List them in the report under their own "Protected — not touched"
heading (they are expected, not a problem to flag).

### Feature branches (do NOT end with `-hotfix`)

The comparison base is `origin/development`. Look for a merged PR from this head:

```bash
gh pr list --head <branch> --base development --state merged --repo hmislk/hmis --json number,title,mergedAt --jq '.[0]'
# if that is empty, retry without --base — the PR's base may have been changed:
gh pr list --head <branch> --state merged --repo hmislk/hmis --json number,title,baseRefName,mergedAt --jq '.[0]'
```

Record the PR number/title if found. If the second query returns a PR with a
`baseRefName` other than `development`, the comparison base is
`origin/<that baseRefName>`, not `origin/development`. Whether or not a PR is
found, continue to **Vet the branch tip** — a merged PR does not by itself
prove the local branch is safe to force-delete (it may carry post-merge
commits, or have been reused).

### Hotfix branches (end with `-hotfix`)

Hotfix PRs target a production branch. Look for a merged PR:

```bash
gh pr list --head <branch> --state merged --repo hmislk/hmis --json number,title,baseRefName,mergedAt --jq '.[0]'
```

- **No merged PR** → **skip** (warn the user). A hotfix branch is never vetted
  by patch-equivalence against `development`; its commits legitimately are not
  there.
- **Merged PR found** → record the PR number/title and its `baseRefName`, then
  **Vet the branch tip** with the comparison base `origin/<baseRefName>`.

### Vet the branch tip (both branch kinds)

Carry two facts forward for each branch: its **PR reference** — either
`#<n> → <base-branch>` (from the `gh pr list` step) or *none* (a no-PR review
checkout) — and its comparison base `<base>`: `origin/development` for a feature
branch or a no-PR checkout, `origin/<baseRefName>` for a feature PR whose base
was changed, `origin/<prod>` for a merged hotfix.

```bash
git merge-base --is-ancestor <branch> <base> && echo CONTAINED || echo AHEAD
```

- **CONTAINED** — the branch tip is already reachable from `<base>`; it holds
  nothing unmerged and no post-merge commits. **Mark for deletion.**
- **AHEAD** — the tip is not reachable from `<base>`. Normal for a squash- or
  rebase-merged PR, but also how a branch with genuine post-merge commits (or a
  reused branch) looks. Disambiguate:

  ```bash
  git cherry -v <base> <branch>
  git rev-list --merges --count <base>..<branch>
  ```

  - **`git cherry` empty or every line starts with `-`, AND the merge count is
    `0`** → every non-merge commit is patch-equivalent to something already on
    `<base>` (a clean squash/rebase merge, or a fully-absorbed no-PR checkout
    such as `pr-23617`). **Mark for deletion.**
  - **any `+` line, or a non-zero merge count** → the branch has a non-merge
    commit not on `<base>`, or a merge commit `git cherry` cannot inspect
    (conflict-resolution content can be absent from `<base>` while every
    non-merge commit still shows `-`). Could be post-merge work, a reused
    branch, or a multi-commit squash whose combined diff no longer matches
    commit-for-commit. **Skip** and warn, quoting the branch's real
    `<base>` and its PR reference (or noting it has none): "*ahead of `<base>`
    — if PR #`<n>` was squash/rebase-merged, `git branch -D <branch>`
    manually; otherwise inspect for unmerged work first*".

## Step 4 — Switch to development

```bash
git checkout development
```

## Step 5 — Delete Marked Branches

Step 3's **Vet the branch tip** fully decided every marked branch — each is
either CONTAINED in its comparison base, or AHEAD but proven patch-equivalent
(no `+` commits, no merge commits). Branches with post-merge or unmerged work
were skipped there. Step 5 only deletes; it does not re-decide safety.

```bash
git branch -d <branch> || git branch -D <branch>
```

`git branch -d` refuses when the tip is not reachable from *local* `development`
— normal here, because a squash/rebase merge leaves the tip off `development`
and local `development` is not fast-forwarded until Step 6. The `|| git branch
-D` completes the delete; Step 3 already established the branch is safe to drop.
`git branch -D` prints the deleted SHA (`Deleted branch X (was 906d5ebbb4)`) and
the reflog keeps it ~30 days, so a mistaken delete is still recoverable.

Do **not** re-check with `git log origin/<branch>..<branch>`: Step 2's
`git fetch --prune` has already removed the `origin/<branch>` upstream, so that
command errors with `unknown revision or path not in the working tree` instead
of returning empty.

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

```text
✓ Deleted branches:
  - <branch>  (PR #NNN merged → <base-branch>)
  - <branch>  (no PR; all commits already on <base-branch>)
  ...

⚠ Skipped branches:
  - <branch>  (PR #NNN merged → <base-branch>, but tip is ahead of it —
    squash/rebase merge? `git branch -D` manually; else inspect for unmerged work)
  - <branch>  (no PR; tip has commit(s) not on <base-branch>)
  - <branch>  (hotfix, no merged PR found)
  ...

• Protected — not touched:
  - master, <name>-prod

✓ development is now at <short-sha> (<commit subject>)
✓ persistence.xml restored to local JNDI settings (unstaged)
```

Each deleted / skipped line carries the branch's real PR reference (or "no PR")
and its real comparison base — never assume a PR exists or that the base is
`development`. Omit any section with no entries. If nothing was stashed, replace
the last line with:
`✓ persistence.xml unchanged (no local changes were present)`

## Notes

- Never delete `development`, `master`, or any `*-prod` branch.
- The known production branches are:
  `coop-prod`, `ruhunu-prod`, `southernlanka-prod`, `rmh-prod`,
  `digasiri-prod`, `mp-prod`, `roseth-prod`, `horizon-prod`,
  `asiripharmacy-prod`, `engagewellness-prod`
- After this skill completes the developer is on `development`, up to date,
  with only unmerged feature branches remaining locally.
