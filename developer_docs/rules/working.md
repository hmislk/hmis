# Working rules (detail)

Detail for the working rules in the root CLAUDE.md; some rules here have no CLAUDE.md entry. The Discuss rule covers only the list below (implementation-level choices are yours to make).

- No worktrees: never use `isolation: "worktree"`; edit in the main checkout. If you are under `.claude/worktrees/*`, stop and move there. Worktrees desync the developer's branch.
- No Artifact tool for deliverables: recipients cannot open them without a Claude account. Write `.md` in `tmp/` (working), `developer_docs/` (tracked) or `../hmis.wiki/` (wiki; never inside this repo).
- Discuss with the user only decisions about PRODUCTION (merges, live data, live config), MONEY, CLINICAL meaning, and FLOW, DESIGN or BUSINESS-PROCESS changes; decide every other non-production, implementation-level choice yourself, say so, and keep working.
- Finish everything the request covers before stopping. If the user asked for several items ("all videos", "fix all of these"), never end a turn asking whether to do the next in-scope item; an unanswered question idles the work for hours. Ask only about things outside the request or decisions only the user can make, and keep working meanwhile.
- Removing or hiding a feature: check production usage first. Run a read-only query of its bill types/tables on every production DB before coding, not at merge. Keep the results in `tmp/`.

## Before you call a long task done

- Re-read the request line by line and tick off each item, including scheduling or timing rules it states.
- Recount every number you report (jobs, files, rows, PRs) from the source, not from memory.
- Stale-entity save: an entity loaded earlier in the flow must not be saved (`edit`/`merge`) after a recount, a lock, or another service's update to the same row; that overwrites the fresh values. Re-`find` it (or refresh) first, and save only after the last update.
- Say what you verified by running and what you only read.
- Waiting on a review or another member: take a second task, finish it, then check the review. Never idle while the board has work.
- Before dispatching a follow-up fix, check the open PRs that touch the same page or class so two sessions do not fix the same thing: `gh pr list -R hmislk/hmis --state open --limit 200 --json number,files --jq '.[] | select(any(.files[]; .path | test("<name>"))) | .number'`. Do not rely on `--search`; it matches text, not changed files.
- Before debugging a data mismatch locally, check that the local DB covers the dates in the issue. If it does not, ask for the read-only production query first.
