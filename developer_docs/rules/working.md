# Working rules (detail)

Moved from the root CLAUDE.md; each rule there links here. Changed in this move: the Discuss rule is narrowed to the list below (implementation-level choices are yours to make).

- No worktrees: never use `isolation: "worktree"`; edit in the main checkout. If you are under `.claude/worktrees/*`, stop and move there. Worktrees desync the developer's branch.
- No Artifact tool for deliverables: recipients cannot open them without a Claude account. Write `.md` in `tmp/` (working), `developer_docs/` (tracked) or `../hmis.wiki/` (wiki; never inside this repo).
- Discuss with the user only decisions about PRODUCTION (merges, live data, live config), MONEY, CLINICAL meaning, and FLOW, DESIGN or BUSINESS-PROCESS changes; decide every other non-production, implementation-level choice yourself, say so, and keep working.
- Finish everything the request covers before stopping. If the user asked for several items ("all videos", "fix all of these"), never end a turn asking whether to do the next in-scope item; an unanswered question idles the work for hours. Ask only about things outside the request or decisions only the user can make, and keep working meanwhile.
- Removing or hiding a feature: check production usage first. Run a read-only query of its bill types/tables on every production DB before coding, not at merge. Keep the results in `tmp/`.
