# What May Go Into a GitHub Issue, PR, or Comment

**`hmislk/hmis` is a public repository** — 231 stars and 138 forks at the time
of writing. Every issue, PR description, review comment, commit message and
attached image is world-readable and indexed by search engines. Forks and API
scrapers keep copies, so an edit after the fact removes the text from the page
but not necessarily from everywhere it has already been copied.

That makes a GitHub issue the wrong place for anything about a **specific
hospital's data or operations**, even though it is the right place for the code
defect that hospital reported.

## Never publish

Not in an issue body, PR description, comment, commit message, screenshot or
attached file:

- **Hospital-specific operational facts** — how many records a bug affected at
  one hospital, that hospital's cutover/migration date, its revenue or bill
  volumes, which of its staff created what, how far behind its deployment is.
- **Production record identifiers** — bill numbers (`SLHOPD//26/014982`), BHT
  numbers, PHNs, patient/person/bill IDs, invoice or GRN numbers. A real
  identifier lets anyone with access to that deployment pull the whole record.
- **Patient, doctor, or staff data** — names, NICs, phone numbers, addresses,
  ages, diagnoses, and demographic inference about them (for example
  "1,452 of them have local mobile numbers, so they are not tourists").
- **Production database or infrastructure names** — schema names
  (`southernlankaprod`), server aliases, hostnames, IPs, ports, JNDI names
  pointing at a real deployment, Payara domain paths.
- **Credentials of any kind** — see CLAUDE.md § Security.
- **Production data-fix logs** — "reset 1,635 rows on <schema> at 20:50". The
  fact that rows were repaired, the count, and the schema together describe a
  live hospital's database contents.

## Publish instead

Describe the **defect**, not the deployment:

| Instead of | Write |
|---|---|
| "1,635 patients at Southern Lanka were saved as foreigners since their 14 Aug cutover" | "Affects every new patient registered on this page since the fix's parent commit reached a deployment" |
| "Example: bill SLHOPD//26/014982 was re-billed a minute later" | "Repro: register a patient, settle, then open a new bill for the same patient" |
| "On `southernlankaprod`, 1,624 rows were reset to Local" | "The affected rows on the reporting deployment are being corrected separately" |
| "Cashiers Amali and Nipuni flagged 85% of their patients" | "Reproduces for any cashier using this page" |

Anything sharper than that — counts, identifiers, schema names, the data-fix
script, per-user statistics — belongs in one of these, none of which is public:

- `tmp/<topic>/` in the local checkout (git-ignored working area),
- the developer's own notes / credentials folder outside the repo,
- a direct message to the developer.

A GitHub issue may still say **which hospital reported it** when that is needed
to route the work: the hospital-name **labels** and the hospital-named branches
(`southernlanka-prod-migrated`) are part of how this repo is organised. The line
is between naming the reporter and publishing that hospital's data. Naming the
hospital plus a bug is fine; naming the hospital plus its record counts,
identifiers, dates and schema is not.

## Why this is easy to get wrong

Impact evidence is good engineering practice — "here is how many records this
touched, here is a real example, here is the data fix" is exactly what a
private tracker should contain, and it is what an investigation naturally
produces. The habit has to be interrupted at the moment of writing, because by
then the numbers are already in hand and they feel like the strongest part of
the report.

Two further traps:

- **The repo looks internal.** Hospital-named labels, branches and workflow
  files make it read like a private tracker. It is not.
- **The rule used to live only in skill files.** If the entry point is an ad hoc
  investigation rather than the `dev-issue` / `dev-issue-unattended` /
  `demonstrate-issues` workflow, a skill-local rule is never loaded. That is why
  this now sits in CLAUDE.md § Security, which is always in context.

## If something sensitive was already published

1. Edit the body/comment immediately (`gh issue edit`, `gh pr edit`,
   `gh api --method PATCH .../comments/<id>`), or delete the comment.
2. Tell the developer what was exposed and for how long — they decide whether
   anything further is needed. Assume forks, notification emails and scrapers
   may retain it.
3. If a **credential** was exposed, editing is not enough: it must be rotated.
4. Do not quietly fix the wording and move on — the disclosure is the part the
   developer needs to know about.
