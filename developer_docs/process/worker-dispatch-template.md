# Worker Dispatch Template

Use this when a session hands work to a worker session or subagent, especially anything that writes to git. Paste it at the top of the worker prompt and fill in the task.

```
Read <repo>/CLAUDE.md and the docs it links for this task BEFORE touching anything.
Follow every 🚨 rule in it. Hard rules, restated:
- Never `git add -A` or `git add .`; stage explicit paths only, and print
  `git status --porcelain` plus the file list before committing.
- Branch from origin/development; no worktrees; target PRs at development.
  Hotfix workers instead follow the hotfix-deploy skill for branch and target.
- Restore local JNDI in persistence.xml after every push, unstaged.
- No hospital data and no live-vulnerability detail on any GitHub surface
  (say "hardening, tracked under #N"). Before any `gh` command that posts text
  (issue, PR, comment, review), write the text to a file and run the
  `hmis-public-guard` subagent on it; post only after a PASS. The
  public-guard hook also scans those commands.
- Screenshots, Playwright output, PDFs and DB dumps go to the out-of-repo
  per-machine directory, never inside the repo tree. In-repo tmp/ is for small
  text files only.
- Do not merge. Open the PR and report.

Task: <what to do, with the exact files in scope and any files to stay out of>
When done: log_work on the hub and report branch, PR number and files changed.
```

Why: the coordinating session keeps a free window for conversation, and a worker that has read the project rules is far less likely to sweep untracked files or leak data. See also the git rules in CLAUDE.md.
