# Continuous Improvement Retrospective — Design

## Problem

Development sessions on this project repeatedly rediscover the same facts the
hard way: a skill's instructions have a gap that only surfaces when actually
run (e.g. `cleanup-branches`' final-tree guard false-flagging a fully-absorbed
branch as unsafe, fixed in PR #24113), a menu path or credential location gets
re-derived from scratch because it wasn't written down anywhere, or a
multi-round-trip detour happens that a one-line doc note would have avoided.
Each of these lessons currently either gets lost, or at best lands in one
person's private Claude memory — never in the shared, checked-in docs that
every session (and every other developer) reads.

## Goal

After finishing a nontrivial piece of development work, Claude should
critically look back at *how* the work went — not just whether it succeeded —
and, when a genuine reusable lesson emerged, propose a specific fix to
`CLAUDE.md`, a `developer_docs/*.md` page, or a `.claude/skills/*/SKILL.md`
file, and offer to open a PR for it.

## Non-goals

- This is not about filing HMIS product bugs found incidentally — that's
  already `demonstrate-issues` / `dev-issue` territory.
- This is not a mandate to retrospect after every trivial action (a single
  file read, a quick question). See the noise filter below.
- This does not create a queue or tracking board of pending improvements; each
  finding is surfaced in the moment, once, at the end of the task it came from.

## Design

### 1. Placement

Add one 🚨 bullet to `CLAUDE.md` under **Essential Rules (Always Apply)**,
following the existing pattern used by rules like Report Favorites and
Institution-Specific Behavior: a short always-apply statement in `CLAUDE.md`,
with the full explanation and worked examples in a linked
`developer_docs/process/continuous-improvement-retrospective.md` page.

### 2. Trigger

Fires after finishing any nontrivial development task: an issue worked
end-to-end, a `.claude/skills/*` run (`dev-issue`, `cleanup-branches`,
`hotfix-deploy`, `playwright-e2e`, `merge-gate`, `review-and-fix`,
`start-issue`, etc.), a multi-step debugging session, or a deployment.

Does **not** fire after: a one-off question, a single quick lookup, or work
that was already fully anticipated by existing docs (nothing new learned).

### 3. Noise filter — what counts as worth flagging

Surface a finding only if at least one of these actually happened:

- A mistake occurred that a documented fact would have prevented (wrong path,
  wrong port, wrong branch base, a known gotcha that bit us again).
- The task took extra round-trips/tool calls that a documented shortcut,
  credential location, or known-quirk note would have collapsed into one.
- A skill's instructions were ambiguous, wrong, or missing a step — discovered
  only by actually running the skill.
- Tribal knowledge got re-derived from scratch (a menu path, an entity
  relationship, a config key, a report's data model) that belongs in
  `developer_docs/`.

Explicitly **not** triggers: stylistic preference, a one-off fact with no
future relevance, or a fact that was already documented but simply wasn't
read (that's a "read the docs" miss, not a docs gap).

### 4. Workflow when something is found

1. State the finding plainly: what happened, why it's a reusable lesson (not
   a one-off), and the specific doc/skill change proposed — quoting the
   file and the exact text to add or fix.
2. Ask the user whether to open a PR for it. **Never auto-create the PR** —
   this mirrors the existing rule that PR creation is a visible/shared action
   requiring confirmation.
3. If approved, branch from `origin/development` (or the relevant `-hotfix`
   base if the lesson came from a hotfix flow), make the scoped doc/skill
   change **only** — never bundled into the feature/fix branch that surfaced
   the lesson — and open a PR targeting `development`.

### 5. Relationship to the private memory system

This is separate from and complementary to the user's private
`~/.claude/projects/.../memory/` system described in the harness system
prompt. That system captures session-specific, personal-workflow feedback
(e.g. "this user prefers terse responses"). This retrospective rule captures
*shared, project-level* lessons that belong in the public repo so every
session and every developer benefits — not just the current user's future
sessions. A finding may reasonably go to **both**: the repo doc for the
general fact, and personal memory for anything about how *this user*
specifically wants it handled going forward.

## Example (retroactive, from this session)

Today's `cleanup-branches` run flagged `rh-local-staging` as unsafe to delete
because its final-tree guard's diff degraded to a full-tree comparison and
found unrelated changes on `development`. The user pushed back, we compared
tree hashes directly, confirmed the branch was fully absorbed, and fixed the
skill in PR #24113. That fix is exactly the kind of finding this rule is meant
to produce automatically next time, without needing the user to notice and
ask "why can't we delete it".
