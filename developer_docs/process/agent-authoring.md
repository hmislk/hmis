# Writing a subagent (`.claude/agents/*.md`)

Read this before adding or editing an agent. CI `agent-lint` (`.github/scripts/check_agent_frontmatter.py`) enforces the frontmatter rules below. Claude Code silently ignores an unknown or misspelt field, so a typo just turns that setting off.

## Frontmatter

Allowed fields only: `name`, `description`, `tools`, `disallowedTools`, `model`, `effort`, `color`, `skills`, `hooks`, `maxTurns`. Required: `name`, `description`, `tools`, `model`. The frontmatter must be valid YAML, so quote a value that contains `: `.

- **Never** `memory` (it silently adds Edit/Write), `isolation` (worktrees are banned), `permissionMode` or `mcpServers`, or `Agent` in `tools` (no nested agents). The lint reports these as "banned by policy", distinct from a misspelt "unknown field".
- `description`: 1–2 sentences, at most 300 characters: what it does, when to use it, what it is not for. No `<example>` blocks; every description loads into every request. Say "use proactively" only for a cheap agent we really want auto-invoked.
- `color`: `red`, `blue`, `green`, `yellow`, `purple`, `orange`, `pink` or `cyan`.
- `skills`: at most two, and never a skill with `disable-model-invocation: true` (it cannot be preloaded; tell the agent to Read that `SKILL.md` instead).

## Tools by role

Leaving out `tools` gives the agent every tool, MCP included. Always set it.

| Role | `tools` | `disallowedTools` |
|---|---|---|
| Read-only (reviewer, verifier, architect, analyst) | `Read, Grep, Glob, Bash` | `Edit, Write, NotebookEdit, Agent` |
| Text guard (judges text only) | `Read, Grep` | `Bash, Glob, Edit, Write, NotebookEdit, Agent` |
| Developer | `Read, Grep, Glob, Bash, Edit, Write` | `NotebookEdit, Agent` |
| Worker (branch to PR) | everything except `Agent`, listed explicitly (dev-hub MCP tools by name) | `Agent` |
| Browser QA | read-only set plus `mcp__playwright__*` | `Edit, Write, NotebookEdit, Agent` |

An agent named `*-reviewer`, `*-guard` or `*-verifier` must not list Edit, Write or NotebookEdit (CI checks). Bash can still write, so a rule that must always hold (read-only SQL, a public-text scan) belongs in a hook, not a sentence. A hook is a backstop, not enforcement: `.claude/hooks/public-guard.py` fails open when no python runs, and anyone can skip it with the `HMIS_PUBLIC_GUARD_BYPASS=1` prefix.

## Model and effort

| Role | `model` / `effort` | Agents today |
|---|---|---|
| Mechanical checks (text scans, link checks) | `haiku` / `low` | `hmis-public-guard` |
| End-user docs | `sonnet` / `low` | `hmis-docs-writer` |
| Developers, reviewers, QA, analysts | `sonnet` / `medium` | `hmis-rules-reviewer`, `hmis-qa-verifier`, `hmis-ux-reviewer`, `hmis-db-specialist`, `hmis-worker`, `java-backend-developer`, `jsf-frontend-dev`, `lab-analyzer-integration`, `hmis-deploy-porter` |
| Security review | `sonnet` / `high` | `hmis-security-reviewer` |
| Architects, on request only | `inherit` / `medium` | `hmis-domain-architect` |

"Think hard" in the body is just text; `effort` is the real control. Dispatch costly or rare agents conditionally (by diff path), never as "use proactively".

## Body

Sections in this order, short:

1. **Role**: one line, the concrete job in this repo. No persona biography.
2. **When invoked**: numbered steps.
3. **Checklist**: project-specific points only.
4. **Output contract**: the exact format, at most 40 lines, with `file:line` evidence.
5. **Out of scope**: what it must not do (posting to GitHub, merging, pushing unless the brief says so).
6. **Stop conditions**: when to stop and report, and what is still unproven.

**Point, don't copy.** CLAUDE.md is already loaded into a named agent, so never restate it; name the rule and the doc or skill that holds it. A copied rule goes stale, as `java-backend-developer` did (Hibernate, `@Transactional`, free scope choice).

Reviewers return findings to the caller only. Posting is the coordinator's job, after checking the text against the [Public Content Policy](../git/github-public-content-policy.md).

To retire an agent, delete it. `.claude/agents/` is scanned recursively, so a moved file is still live. `.codex/agents/` holds Codex mirrors of some agents; delete the mirror in the same PR. New agents are not mirrored there yet.

Run the lint locally: `python3 .github/scripts/check_agent_frontmatter.py` (needs PyYAML).
