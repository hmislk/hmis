---
name: pick-and-ship-issues
description: >
  Use when asked to pick N open issues filed by a given person (optionally
  only those assigned to a given user) and ship mergeable PRs for them
  without the user's input, e.g. "take the 5 latest issues Irani raised and
  make mergeable PRs", "/pick-and-ship-issues reporter=Irani96 count=5
  assignee=buddhika75". Also use to re-run that selection later, since it
  re-checks issues parked on an earlier run.
---

# Pick and Ship Issues

Choose issues by evidence, then hand them to `dev-issue-unattended`. An issue
is picked only if it is **clear** and its defect is **confirmed** in the code or
**demonstrated** in the app. Every issue that is not picked gets a visible
outcome on GitHub, so the next run knows what to skip.

Invoking this skill authorizes everything below: issue comments, labels,
assignee edits, board moves, closing issues, and the PRs that
`dev-issue-unattended` opens. Never merge. Never put hospital data in a
comment (see the
[public content policy](../../../developer_docs/git/github-public-content-policy.md)).

**Never ask for a go-ahead.** Do not ask "shall I proceed?", "on that basis?" or
how to narrow the run, however long the list or the work. Invocation is the
go-ahead. Workload is bounded by the caps below, not by asking. Ask only if
`reporter` or `count` is missing.

**Cap:** `count` above **5** is reduced to 5; say so in the report.

**Never stop early.** Keep triaging until either `count` issues have a PR
(or a recorded `dev-issue-unattended` stop) or the matching issues run out.
Never end a run with candidates "not yet triaged" or a PR "not yet done".
Triage is cheap, so work through the whole list; a long list is not a reason
to stop.

**Finish what you start; no local-only leftovers.** Every change this run
makes is pushed and in a PR before the run ends: code fixes, and also any edit
to a skill, `CLAUDE.md` or doc (own branch from `origin/development`, PR to
`development`, never merge). Do not leave commits on a local branch, and do not
end with a "loose ends" list of things you chose not to push. Invoking this
skill is the authorization; do not hold back for permission.

**The report is written as you go.** Create
`tmp/pick-and-ship/<yyyy-mm-dd>-<reporter>.md` at the start of the run and
append one line after every issue outcome (picked, PR opened, needs-info,
needs-human, closed, skipped). If the run is cut off (out of memory, context
limit, tool failure), the file already records everything that happened and
where to resume.

**Everything runs against `origin/development`.** Fetch it first. Check each
defect, branch, build, test and PR target against it, never `master` or a
production/staging branch. An issue already fixed there follows step 6.

## Inputs

| Input | Required | Meaning |
|---|---|---|
| `reporter` | yes | GitHub login of the issue author (`Irani96`). If a display name is given, resolve it from recent issue authors and say which login you used. |
| `count` | yes | How many PRs to ship (maximum 5). |
| `assignee` | no | Only issues assigned to this login. |

## State kept on GitHub (labels)

Create a label with `gh label create` if it is missing.

| Label | Set when | Re-eligible when |
|---|---|---|
| `triage: needs-info` | The issue is not clear (step 2). | A comment without the triage marker is posted after the latest marker comment. Remove the label, then triage again. |
| `triage: needs-human` | The issue is clear but cannot be confirmed or demonstrated with the tools here, or it needs a decision this workflow cannot make (step 5). | Never automatically. Only when the user removes the label. |

Every comment this skill posts ends with a hidden marker, either
`<!-- pick-and-ship:needs-info -->` or `<!-- pick-and-ship:needs-human -->`.
The user's own replies come from the same `gh` account, so the marker is the
only way to tell the skill's comments apart from replies.

## 1. Build the candidate list

```bash
gh issue list --repo hmislk/hmis --state open --author <reporter> \
  [--assignee <assignee>] --limit 100 \
  --json number,title,labels,assignees,createdAt --jq 'sort_by(.createdAt) | reverse'
```

Walk the list newest first. Skip the issue without commenting if any of these
holds:
- It has an open PR (`gh pr list --state open --search "<n> in:body"`, or a
  branch named `<n>-*` on origin).
- With no `assignee` input: it is assigned to someone other than the current
  `gh` user. Another developer owns it.
- It is labelled `triage: needs-human`.
- It is labelled `triage: needs-info` and nobody has replied since the latest
  marker comment. If somebody has replied, remove the label and triage it.

Triage issues one at a time until `count` issues are picked or the list runs
out. Triage outcomes do not count toward `count`; only picked issues do.

## 2. Gate: clear, detailed and unambiguous

The issue must state all of the following, either in its body or in later
comments:
1. **Where:** the page or menu path, report name, or API.
2. **Current vs expected behaviour**, specific enough to tell pass from fail.
3. **How to reach it:** steps or conditions to trigger it. For reports and
   features, the filters, columns or acceptance criteria.
4. **No open question** with two plausible answers that the code cannot
   settle.

If any is missing:
- Comment `@<author>`. Say what was looked at, then list **only** the missing
  items, using the same wording as
  `dev-issue-unattended` › "Insufficient issue description". Ask for a
  redacted example, never a real bill or BHT number.
- Add the `triage: needs-info` label. Add the author as an assignee
  alongside any existing assignees. Set the board Status to **Backlog**.

Then move on to the next candidate.

## 3. Confirm by reading the code

**Read the business rules first.** Before judging any issue, read the relevant
pages in `../hmis.wiki` and the matching `developer_docs/` files (billing,
pharmacy, inward, reports, etc.). If the fix would change a business rule
(what a figure means, who may do what, how a bill type behaves, a calculation,
a privilege), do not pick it: handle it as step 5 (`triage: needs-human`).
A fix that makes the system follow an already documented rule is fine.
Find the controller, service and XHTML involved, and the git history
(`git log -S`, `git blame`, related issues). If the code clearly shows the
defect, or clearly lacks the requested feature, the issue is **confirmed**:
pick it and go to step 6. Record the file:line evidence; the PR uses it.

## 4. Otherwise, demonstrate it

Use local Payara with Playwright, reaching pages through the menus only, as
in the `playwright-e2e` skill. Use or create local test data as
`dev-issue-unattended` step 4 describes. If you see the defect, pick the
issue and keep the screenshots.

If a reasonable, documented attempt does not show the defect, and the gate in
step 2 has passed:
- Comment `@<author>` with what you tried and what you saw. Ask them to
  **reopen with fresh steps and details, or schedule a meeting** to go through
  it live.
- `gh issue close <n> --reason "not planned"`, set the board Status to
  **Dev Completed**, and read the status back (see
  [Project board §4](../../../developer_docs/github/project-board-integration.md#4-closing-an-issue-without-a-pr)).

## 5. Not suitable for this workflow

Apply this instead of step 4's close when the defect cannot be confirmed or
demonstrated **because of the tooling**, not because of the report. Typical
causes:
- Hardware, or an external device or service: printers, receipt layouts on
  paper, barcode scanners, lab analyzers, SMS or email gateways, card
  terminals, third-party APIs without local credentials.
- Needs production-scale data, real concurrency or timing, more than one live
  user session, or a non-Linux client.
- The fix would change a business rule documented in the wiki or `developer_docs/`,
  or one they do not cover, so the correct behaviour is a business decision.
- The fix needs a schema migration, a privilege or security change, or a
  business decision. These are hard limits of `dev-issue-unattended`.

Comment with the reason (what blocks automation, not a request to the
author), add `triage: needs-human`, and do not close the issue. These issues
go at the top of the final report for the user.

## 6. Ship the picked issues

**No PR without end-to-end proof.** Every PR must be preceded by a passing
end-to-end test of the changed workflow, using Playwright (menus only) and/or
the API, against the rebuilt local deployment, with DB or API read-back of the
result. XHTML-only changes still need the browser check. If the test cannot be
run or does not pass, do not open the PR: report the issue under
`triage: needs-human` or as a stop. Put the test evidence in the PR body.

Run the `dev-issue-unattended` skill once, giving it the picked issue numbers
as a batch. If it stops on an issue it was given (cannot reproduce, needs
info, a hard limit, failed end-to-end test), that stop gets its step 2-5
outcome on GitHub and does **not** count toward `count`. Take the next
candidate and run `dev-issue-unattended` again. Repeat until `count` PRs are
open or the matching issues run out; only then report any shortfall.

If triage finds the defect **already fixed on `development`**, comment asking the reporter to
re-verify, then set the board Status to **Dev Completed** (read it back) so QA re-checks it.
Do not leave it in Backlog.

Before reporting, read back the board Status of every issue you touched. Shipped issues must be in
**Reviewing & Merging**, per
[Project board: status at every stage](../../../developer_docs/github/project-board-integration.md#status-at-every-stage-qa-works-from-this-column).

## 7. Report

Finish the `tmp/pick-and-ship/<yyyy-mm-dd>-<reporter>.md` file you have been
appending to, and give the same summary in chat:

1. **Needs your intervention**: every open `triage: needs-human` issue from
   this reporter, this run's first, each with a one-line reason.
2. **Shipped**: the `dev-issue-unattended` step 15 summary.
3. **Waiting for the reporter**: needs-info issues with comment links.
4. **Closed, could not demonstrate**: with comment links.
5. **Skipped**: issue numbers with a reason (open PR, owned by someone else).
6. The retrospective from `dev-issue-unattended`.

**Links.** Every issue, PR, comment and retro doc PR in the report file and in
the chat summary is a full clickable URL (`https://github.com/hmislk/hmis/...`),
never a bare `#N`. If the retrospective needs a doc PR, open it (after the user
agrees) and put its link at the top of the report file's retrospective section.

## Common mistakes

| Mistake | Fix |
|---|---|
| Counting a needs-info or closed issue toward `count` | Only picked issues count. |
| Taking up a needs-info issue again because the user commented from the same account | Look for the marker. A reply is any later comment without it. |
| Closing an issue that Playwright simply cannot reach | That is `triage: needs-human`, not "could not demonstrate". |
| Asking for everything in a fixed template | Ask only for what is missing. |
| Reporting an issue as shipped while its board Status is still Backlog or a sprint | QA never sees it. Set and read back the Status. |
| Picking from a reporter's issues that another developer is assigned to | Skip them unless the user named that assignee. |
