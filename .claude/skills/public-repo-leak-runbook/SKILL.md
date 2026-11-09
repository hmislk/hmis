---
name: public-repo-leak-runbook
description: >
  Runbook for when junk, binary artifacts or data (screenshots, dumps, patient-identifiable
  renders, credentials) were pushed to the PUBLIC hmislk/hmis repo, for example by a scripted
  `git add -A`. Use immediately when such a push is noticed. A force-push does NOT purge it;
  only a GitHub Sensitive Data Removal request filed by the repo owner clears dangling commits
  and cached PR views.
allowed-tools: Bash, Read
---

# Public repo leak runbook

Act in this order. Speed matters: the content is public from the moment of the push.

1. **Stop the source.** If a batch, loop or worker session is still running and committing, `TaskStop` it first so nothing more is added.
2. **Rotate** any credential that was exposed, now, before cleanup; treat it as compromised regardless of removal.
3. **Clear a stuck index:** if a killed git process left `.git/index.lock`, confirm no git process is running (`pgrep -a git`), then `rm .git/index.lock`. Do not use `git add -A` to "recover".
4. **Force-reset the affected branch** to the last good commit:
   ```bash
   git fetch origin
   git switch <branch> && git status            # right branch? save any legitimate uncommitted work (git stash) first
   git log --oneline <last-good-sha>..origin/<branch>   # commits after the leak? re-apply the legitimate ones after the reset
   git reset --hard <last-good-sha>
   git push --force-with-lease=<branch>:<reviewed-origin-sha> origin <branch>
   ```
   `<reviewed-origin-sha>` is the `origin/<branch>` tip you just reviewed, so a newer push makes the push fail instead of being lost. If the leak is in an open PR, close it (do not describe the leaked content in the close reason). If the branch is protected, ask the owner to do this step.
5. **Find every other reference** to the leaked commit and clean each the same way: `git branch -r --contains <bad-sha>`, `git tag --contains <bad-sha>`, and open PRs from forks. GitHub Support needs these gone before it purges.
6. **Owner files a GitHub Sensitive Data Removal request** (only the repo owner can) when the leak holds sensitive data (patient/staff PII, credentials). Plain junk with nothing sensitive needs no request; the reset is enough. Pick the form category that fits (individual PII, or credentials) and include:
   - the affected PR URL(s) and their count (cached views persist),
   - the first changed / dangling commit SHA(s) by name (a commit stays reachable by SHA after a force-push).
7. **Log a hub incident** with `log_work` (title, folder, branch, status) on the private dev-hub. Put the detail there, not on GitHub.
8. **Prevent a repeat:** check that `.gitignore` covers the artifact type, and that the worker was told the rules (see `developer_docs/process/worker-dispatch-template.md`).

Public-surface rule: do not describe the leaked content, counts, or any patient detail in issues, PR comments or commit messages. Say only that an accidental commit was removed, tracked under the relevant issue number.
