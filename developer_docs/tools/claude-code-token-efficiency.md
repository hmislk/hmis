# Claude Code Token Efficiency — Per-Machine Checklist

Follow this on every developer machine that runs Claude Code on HMIS. Each machine has its own memory, plugins and MCP servers, so each has to be checked separately.

## Why

Most of our token use is **context loaded on every turn** (memory index, plugin start-up text, MCP tool lists), not the code Claude writes. Trimming that saves tokens on every message with no loss of quality.

## Decision on token-saving plugins (2026-10-18)

| Tool | Claimed | Independently measured | Our decision |
|---|---|---|---|
| Ponytail (write-less-code rules) | ~22% fewer tokens | ~10% per task | **Do not install for now** |
| Caveman (terse replies) | ~65% | ~8.5% | Optional, on request only |
| RTK | — | no saving, sometimes higher cost | Do not install |

Reasons for not installing Ponytail:
- A known Windows bug can stall every message for several seconds. Most of our machines are Windows.
- Its "ultra" deletion mode conflicts with our rule to check production usage before removing a feature, and its "one line" push conflicts with matching the surrounding code.
- It runs Node hooks on machines that can reach production credentials. Do not install any hook-running plugin without reading its source first.

Revisit only if the Windows bug is fixed and a trial on a low-risk issue shows a real saving with `/cost`.

## Checklist

### 1. Measure first
- [ ] Start a fresh session in the HMIS folder and run `/context`. Note the totals for memory, MCP tools and system/plugin text.

### 2. Trim the memory index (`MEMORY.md`)
The memory folder is under your user profile: `~/.claude/projects/<project-folder-name>/memory/`.
- [ ] Back up `MEMORY.md` first.
- [ ] Keep `MEMORY.md` as an **index only**: one short line per memory (`- [Title](file.md) — short hook`). Aim for under ~8 KB.
- [ ] Move facts that are written inline in the index into topic files and link them.
- [ ] Move finished work (merged PRs, closed issues, past sessions) into one `archive_completed_work.md` file with a single index line pointing to it. Do not delete the topic files.
- [ ] Remove lines that only repeat `CLAUDE.md` (it is loaded anyway).
- [ ] Check that every link in the index resolves to an existing file.
- [ ] Remove passwords from the index. Credentials belong only in the external credentials file.

Simplest way: ask Claude Code *"Trim MEMORY.md following developer_docs/tools/claude-code-token-efficiency.md"*.

### 3. MCP servers
- [ ] Run `/mcp` and disable servers you do not use in normal work.
- [ ] We use Playwright for browser testing (see [claude-in-chrome vs Playwright](claude-in-chrome-vs-playwright.md)), so claude-in-chrome can usually be off. Turn it on only when needed.
- [ ] Remove MCP servers left over from other projects.

### 4. Plugins
- [ ] Run `/plugin` and list what is enabled.
- [ ] For each plugin, ask whether it earns the text it injects at session start. Disable anything unused.
- [ ] Do not add token-saving plugins (see the decision above) without team agreement.

### 5. Settings sanity check
- [ ] Open `~/.claude/settings.json` and make sure any environment description (e.g. `autoMode.environment`) describes **this** repo, not another project.

### 6. Measure again
- [ ] Start a fresh session, run `/context` again and compare with step 1.

## Keeping it lean

- When work is finished, move its memory line to the archive rather than leaving it in the index.
- Keep index hooks short; detail goes in the topic file.
- Re-run this checklist every few months, or when `/context` shows memory above ~8 KB.
