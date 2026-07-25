# Discharge Checklist Sample Form + Dynamic Form Fixes — Design

## Background

A printed "items handed over to patient at discharge" checklist (22 Yes/No/N/A
line items plus a few free-text/date fields, with handwritten amendments in
green ink) needs to be recreated as a dynamic form via the existing
`/api/forms` REST API (see `developer_docs/forms/form-api-guide.md` and
`developer_docs/forms/custom-layout-guide.md`), for trial use on rh staging
(`<rh staging URL — see C:\Credentials\>`).

Investigation of `admission_forms.xhtml` and `InwardFormController.java`
confirmed the dynamic form system (`DesignComponent`/`CaptureComponent`) can
represent this form — each checklist row becomes a `SelectOneRadio` field
with `Yes` / `No` / `N/A` choices, which is self-labeling per row without
needing a shared table header. Two real defects were found that must be
fixed first so the form renders and behaves correctly.

## Phase 1 — Code Fixes

### Fix 1: Hardcoded radio column count

`admission_forms.xhtml` line 167 hardcodes
`<p:selectOneRadio layout="grid" columns="2">` for every `SelectOneRadio`
field regardless of how many choices it has. A 3-choice Yes/No/N/A field
wraps awkwardly (2 options on one row, the 3rd dangling) instead of sitting
inline.

**Fix:** add `InwardFormController.getRadioColumns(CaptureComponent cc)`
returning the field's live choice count (via `getChoicesFor(cc).size()`),
clamped to `[1, 4]`. Reference it from the xhtml:
`columns="#{inwardFormController.getRadioColumns(cc)}"` in place of the
literal `"2"`.

### Fix 2: Edit-from-list doesn't default to read-only

`InwardFormController.editForm(PatientFormEntry entry)` never touches
`viewMode`, so opening an already-filled form from the entries list inherits
whatever `viewMode` was last left at (e.g. `false` from a previous
`startNewForm()`/`saveForm()`/`cancelForm()` call in the same session),
rather than deterministically opening read-only.

**Fix:** set `viewMode = true;` at the start of `editForm()`. Filled forms
opened from the list always start in read-only View Mode; the existing
"Switch to Edit Mode" button remains the only way to make it editable.
`startNewForm()` is unaffected — new/unfilled forms still open in Edit Mode
(`viewMode = false`), since there's nothing to view yet.

### Issue

One GitHub issue tracking both fixes, filed against `hmislk/hmis`, plus a
note that the dynamic form system has no first-class "matrix/table
checklist" component (a shared YES/NO/N/A header row spanning many fields) —
not a blocker for this form since each radio row labels its own options, but
worth tracking as a future form-designer/API enhancement.

### Rollout

Branch from `origin/development` (never `master`), apply both fixes, commit,
push, open a PR targeting `development` referencing `Closes #<issue>`. The
user merges the PR and redeploys rh staging themselves (deploys go through
CI/CD per project rules — not done manually here).

## Phase 2 — Sample Form via API (after merge + staging redeploy)

Using `curl` with the `Finance` header (`<Finance API key — see C:\Credentials\>`)
against `<rh staging URL — see C:\Credentials\>/api/forms`, create:

**Form template** — title makes clear this is a sample/trial form for
evaluating the form-design system, e.g.
`"[Sample] Patient Discharge Checklist — Form Design Trial"`. The existing,
unrelated form template already in the system is left untouched.

**Fields** (in order), each a `SelectOneRadio` with choices `Yes` / `No` /
`N/A` unless noted. Handwritten green amendments from the source image are
folded directly into the printed labels (not modeled as separate data):

1. Bill Settled (and Payment Receipt Checked)
2. Diagnosis Card
3. Medical Certificate
4. Claims Forms
5. Doctor's Prescription
6. IV Cannulae (Removed)
7. Drains / Catheters (Removed)
8. Laboratory Reports
9. ECG Reports
10. EEG Reports
11. Scan Reports (MRI/CAT Scan)
12. X-Ray
13. Echocardiography
14. Special Investigation (Endoscopy Reports)
15. OPD Review
16. Medication on Discharge
17. Admission Letter from Doctor
18. Maternity
19. Birth Registration Form
20. CHDR (Child Health and Development Record)
21. Death Certificate Issued (if applicable only)
22. Biopsy Report / Any Other Report to be Collected

Then trailing fields:

23. Instruction Given by Dr (for the cannula/catheter/drain left in) —
    `Input_text_Area`
24. Days to be Left In — `Input_Number` (or `Spinner`)
25. Signature & Name of Person Receiving the Reports — `Input_text`
26. Date — `Calendar`
27. Time — `Input_text`
28. Name of the Nursing Sister/Nurse in Charge — `Input_text`

Custom `editHtml`/`viewHtml` per field (C3 hybrid pattern) styled as a
compact single-column checklist row (label left, Yes/No/N/A inline right),
`formCssClass` = single-column row.

### Verification

Confirm on rh staging:
- New form appears in the "Select Form" dropdown on
  `inward/admission_forms.xhtml`.
- Filling it in and saving produces a read-only view by default when
  reopened from the list (Fix 2), with a working edit toggle.
- The 3-choice radio rows render inline on one line (Fix 1).

### Wiki

Write a user-facing page in `../hmis.wiki` (sibling directory, never inside
the main repo) describing the sample checklist form for end users — no
GitHub issue/PR references per wiki conventions.
