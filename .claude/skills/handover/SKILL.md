---
name: handover
description: Use when the user asks for a handover, says they are leaving or the session must end before the work is finished, or when a similar but different task comes next and what this session learned (environment, gotchas, decisions) should carry into a fresh Claude Code session. Also /handover.
---

# Handover

Write a prompt that a fresh session can paste in and continue from, without rediscovering
the local environment, re-asking settled questions, or redoing finished steps.

## Output: two files and one index line

1. **The prompt:** `tmp/handover_<YYYY-MM-DD>_<topic>.md` in the project root. Never use the
   Artifact tool.
2. **A pointer memory:** `project_handover_<topic>_<YYYY_MM_DD>.md` in the project memory
   directory (`type: project`). It names the prompt file and says: "delete this memory and its
   MEMORY.md line when the next session starts".
3. **One line in `MEMORY.md`** under "Active / Waiting" that links the pointer and starts with `HANDOVER`.
   If a pointer for the same work already exists, replace it and its line; don't add a second one.

## Before writing: check facts live

For every PR or issue named in the prompt, run the commands and copy the results in. Do not copy
them from memory or from the conversation.
- `gh pr view <N> --json state,mergeStateStatus,headRefOid,headRefName,baseRefName,files`
- `gh pr checks <N>`
- bot reviews, which `gh pr checks` does not show: `gh api repos/<owner>/<repo>/commits/<sha>/status --jq .state`
  and whether CodeRabbit's summary comment still says "review in progress"
- unresolved review threads (GraphQL `reviewThreads { isResolved }`)
- `gh issue view <N> --json state,title`

Write each fact with its date: "CI green on 5dd8a34 (checked 2026-11-01)". If a live result
contradicts what the session believed, write the live result and tell the user about the difference.

## The prompt: these sections, in this order

1. **Title and paste line:** `# Handover prompt — <topic> (written <date>)`, then "Paste everything
   below the line into a new Claude Code session started in `<project root>`." and `---`.
2. **First action:** delete the pointer memory (exact filename and directory) and its MEMORY.md line.
3. **Tasks:** one `## Task N` per task. State each task as the user asked for it, including any skill to run
   (`/merge-gate <url>`). Under each task:
   - **Facts (checked <date>):** the live results above.
   - **Done (do not redo):** finished steps, with commit SHAs (and comment URLs you have).
   - **Decisions the user made:** each one as a rule, so the next session doesn't ask again.
   - **Remaining:** numbered steps, including waits ("wait for CodeRabbit's re-review of <sha>").
4. **Working flow:** the exact sequence that worked on this or the previous task, as numbered
   commands copied from what actually ran. Name the skills used. Mark a command you didn't
   run "(not run this session)".
5. **Local environment:** paths, ports, app name, DB schema, test login *by label*, and, when this
   session confirmed them, how to grant privileges and switch department.
6. **Gotchas that cost time:** one line each, `symptom → fix`.
7. **Rules:** never merge; leave test data and list what you created; no hospital data in GitHub;
   restore `persistence.xml` local JNDI after every push and leave it unstaged; plus any
   task-specific rule.

## Credentials

Never write a password, key, token, host or IP into the prompt or the memory. Write "the <label> pair in
`C:\Credentials\Credentials.txt`, section <name>".

## Before saying it's done

- Reread the prompt as a newcomer: could you start Task 1 with no other context?
- `grep -n -i -E "password|pwd *[:=]| -p[A-Za-z]*[0-9]|api[_ -]?key *[:=]|token *[:=]|(^|[^-.0-9])[0-9]{1,3}(\.[0-9]{1,3}){3}($|[^.0-9])"`
  on the prompt and the memory finds nothing, or only lines you checked by hand.
- Tell the user the prompt path, and that they paste it into the new session.

## Common mistakes

| Mistake | Fix |
|---|---|
| Copying "CI green" from earlier in the chat | Re-run `gh pr checks` and date the result |
| Omitting the user's decisions | List them as rules under the task |
| "Remaining: finish the PR" | Give numbered steps with the exact commands and waits |
| A pointer memory written for work this session is still doing | Write the pointer only when handing over |
| Gotchas written as stories | `symptom → fix`, one line |
