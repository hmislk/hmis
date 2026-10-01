# Continuous Improvement Retrospective

## Why

Development sessions on this project repeatedly rediscover the same facts the
hard way: a skill's instructions have a gap that only surfaces when actually
run (e.g. `cleanup-branches`' final-tree guard false-flagging a fully-absorbed
branch as unsafe — fixed in PR #24113), a menu path or credential location
gets re-derived from scratch because it wasn't written down anywhere, or a
multi-round-trip detour happens that a one-line doc note would have avoided.
Each of these lessons currently either gets lost, or at best lands in one
person's private Claude memory — never in the shared, checked-in docs that
every session (and every other developer) reads.

This rule closes that loop: after nontrivial development work, Claude looks
back at *how* the work went, and when a genuine reusable lesson emerged,
proposes a fix to the shared docs instead of letting the lesson evaporate at
the end of the conversation.

## When this fires

After finishing a nontrivial piece of development work:
- An issue worked end-to-end (investigation → fix → PR).
- A `.claude/skills/*` run (`dev-issue`, `cleanup-branches`, `hotfix-deploy`,
  `playwright-e2e`, `merge-gate`, `review-and-fix`, `start-issue`, etc.).
- A multi-step debugging session.
- A deployment or migration.

It does **not** fire after a one-off question, a single quick lookup, or work
that went exactly as the existing docs said it would (nothing new learned).

## The noise filter — what's actually worth flagging

Surface a finding only if at least one of these genuinely happened:

- **A mistake occurred that a documented fact would have prevented** — wrong
  path, wrong port, wrong branch base, a known gotcha that bit us again.
- **The task took extra round-trips** that a documented shortcut, credential
  location, or known-quirk note would have collapsed into one.
- **A skill's instructions were ambiguous, wrong, or missing a step** —
  discovered only by actually running the skill.
- **Tribal knowledge got re-derived from scratch** (a menu path, an entity
  relationship, a config key, a report's data model) that belongs in
  `developer_docs/`.

Explicitly **not** triggers: stylistic preference, a one-off fact with no
future relevance, or a fact that was already documented but simply wasn't
read (that's a "read the docs" miss, not a docs gap to fix).

Retrospecting on every trivial action would just be noise — this is reserved
for cases where a future session would hit the exact same wall.

## Workflow when something is found

1. **State the finding plainly**: what happened, why it's a reusable lesson
   (not a one-off), and the specific doc/skill change proposed — quoting the
   file and the exact text to add or fix.
2. **Ask before opening a PR.** Never auto-create it — PR creation is already
   a visible/shared action that requires confirmation (see CLAUDE.md's general
   "Executing actions with care" guidance).
3. **If approved**, branch from `origin/development` (or the relevant
   `-hotfix` base if the lesson came from a hotfix flow), make the scoped
   doc/skill change **only** — never bundled into the feature/fix branch that
   surfaced the lesson — and open a PR targeting `development`.
4. **Write the lesson as a rule, not a story**: a symptom → fix line, with no
   background, dates or issue history (those go in the PR description).
   `CLAUDE.md` is loaded on every message, so add to it only if leaving the
   rule out would cause mistakes in every session. Otherwise put it in the
   relevant skill or topic doc. See [Anthropic's skill-authoring guidance](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices).

## Relationship to the private memory system

This is separate from, and complementary to, the user's private
`~/.claude/projects/.../memory/` system. That system captures
session-specific, personal-workflow feedback (e.g. "this user prefers terse
responses"). This rule captures *shared, project-level* lessons that belong
in the public repo so every session and every developer benefits — not just
the current user's future sessions. A single finding may reasonably go to
**both**: the repo doc for the general fact, and personal memory for anything
about how a specific user wants it handled going forward.

## Worked example

Running `/cleanup-branches`, the skill's final-tree guard flagged
`rh-local-staging` as unsafe to delete because its diff check degraded to a
full-tree comparison and found unrelated changes on `development`. The user
pushed back ("why can't we delete it"); comparing tree hashes directly showed
the branch was fully absorbed. That gap in the skill's guard logic was fixed
in [PR #24113](https://github.com/hmislk/hmis/pull/24113) — the kind of
finding this rule is meant to produce proactively next time, without needing
the user to notice and ask first.

See also: [Design spec](../specs/2026-09-28-continuous-improvement-retrospective-design.md).
