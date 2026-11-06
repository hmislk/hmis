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
2. **Force-reset the branch** to the last good commit and force-push with lease:
   ```bash
   git fetch origin
   git reset --hard <last-good-sha>
   git push --force-with-lease origin <branch>
   ```
   If the leak is in an open PR, close it (do not describe the leaked content in the close reason). If the branch is protected, ask the owner to do this step.
3. **Clear a stuck index:** `rm .git/index.lock` if a killed git process left one behind. Verify with `git status`; do not use `git add -A` to "recover".
4. **Owner files a GitHub Sensitive Data Removal request** (only the repo owner can). Use the private-information form and include:
   - the PR URL(s) that show the content (cached views persist),
   - the dangling commit SHA(s) by name in the details (the commit stays reachable by SHA after a force-push),
   - category: individual PII (when patient or staff data is involved).
5. **Rotate** any credential that was exposed; treat it as compromised regardless of removal.
6. **Log a hub incident** with `log_work` (title, folder, branch, status) on the private dev-hub. Put the detail there, not on GitHub.
7. **Prevent a repeat:** check that `.gitignore` covers the artifact type, and that the worker was told the rules (see `developer_docs/process/worker-dispatch-template.md`).

Public-surface rule: do not describe the leaked content, counts, or any patient detail in issues, PR comments or commit messages. Say only that an accidental commit was removed, tracked under the relevant issue number.
