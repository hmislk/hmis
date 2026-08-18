# Inward Final Bill Reprint Page — Custom Bills & Professional Fee Fixes

**Date:** 2026-08-09
**Page:** `src/main/webapp/inward/inward_reprint_bill_final.xhtml`
**Controllers:** `com.divudi.bean.inward.BhtSummeryController`, `com.divudi.bean.inward.InwardSearch`

## Background

Two issues were found live-demoing the Inward Final Bill reprint page:

1. **Custom Bills tab** renders all three custom bill formats (internally
   `finalBillCustom2`, `finalBillCustom3`, `finalBillCustom4`) stacked
   unconditionally, and its "Settings" dialog shows a full preference
   checklist (Show Patient Address / NIC / Phone / Guardian / Corporate
   Sponsor) for formats regardless of whether that format is even in use.
   This does not match the project's own documented convention.
2. **Final Bill tab** (and identically Hospital Bill / Final Bill Summary
   tabs): the "With Professional Fee" checkbox's visible checked state and
   the rendered bill total are out of sync — e.g. Original panel loads
   unchecked but the total already includes the professional fee; Duplicate
   loads checked but the total excludes it.

## Problem 1: Custom Bills tab shows every format at once

### Root cause

There is no `rendered` gate on any of the three format blocks — they always
render together. The Settings dialog (`custom2ConfigDialog`) only exposes
per-field preferences (`custom2Show*`, `custom4Show*`); it has no control
over *which format is active*, and `finalBillCustom3` ("Custom Bill 2" in
the UI) has no preference fields at all today.

### Fix — config-gated single-format rendering

This repo already documents the correct pattern in
`developer_docs/configuration/printer-configuration-system.md`: format
selection is a `configOptionController`-backed boolean per format, gating
`rendered` on the block — **not** a runtime `p:selectOneMenu`, which that
doc explicitly deprecates. The existing Transfer Issue / GRN / Transfer
Receive pages already follow this shape (independent boolean keys per
format, not enforced-mutually-exclusive in code — admin responsibility).

**New config keys** (department-scoped, via `configOptionController`):

| Key | Format (internal) | UI label | Default |
|---|---|---|---|
| `Inward Final Bill - Show Custom Bill 2 Format` | `finalBillCustom2` | "Custom Bill" | `true` |
| `Inward Final Bill - Show Custom Bill 3 Format` | `finalBillCustom3` | "Custom Bill 2" | `false` |
| `Inward Final Bill - Show Custom Bill 4 Format` | `finalBillCustom4` | "Custom Bill 3 (Letterhead)" | `false` |

Defaulting only "Custom Bill" (Custom2) on preserves current behavior for
whichever format departments are actually relying on today, without a data
migration — departments that need Custom3/Custom4 switch them on once via
the Settings dialog.

**XHTML changes** (`inward_reprint_bill_final.xhtml`, Custom Bills tab):
- Wrap each format's `<div class="row">` (Original+Duplicate pair) in
  `rendered="#{configOptionController.getBooleanValueByKey('<key>', <default>)}"`.
- Add 3 new `h:selectBooleanCheckbox` controls at the top of
  `custom2ConfigForm` (before the existing "Custom Bill 3 (Letterhead)
  Settings" divider) bound to new `BhtSummeryController` properties, so an
  admin can flip which format(s) are active.
- Wrap the existing `custom2Show*` preference block and the `custom4Show*`
  preference block (plus its `<hr/>`+`<h6>` heading) each in their own
  `rendered` on the matching new key, so only the active format's
  preferences are visible. (Custom3 has none to filter.)

**Java changes** (`BhtSummeryController`):
- Add fields `showCustom2Format`, `showCustom3Format`, `showCustom4Format`
  (+ getters/setters).
- `loadCustom2Config()`: load the 3 new keys from `configOptionController`.
- `saveCustom2Config()`: persist the 3 new keys via
  `configOptionController.setBooleanValueByKey(...)`.

No changes to `finalBillCustom2`/`3`/`4` composite components themselves —
this is purely visibility/config, not template content.

## Problem 2: "With Professional Fee" checkbox desyncs from the rendered total

### Root cause

Every occurrence of the checkbox carries:

```xhtml
<p:selectBooleanCheckbox
    value="#{inwardSearch.withProfessionalFee}"
    ...
    onchange="#{inwardSearch.showProfessionalFee()}">
    <p:ajax process="@this" update="finalOriginalBillPriview"/>
</p:selectBooleanCheckbox>
```

`onchange` on `p:selectBooleanCheckbox` is a plain HTML pass-through
attribute, not a deferred `MethodExpression` binding like `action` or
`actionListener`. JSF evaluates `#{inwardSearch.showProfessionalFee()}`
**at render time**, once per occurrence in the render tree — not when the
browser actually fires a change event. And `showProfessionalFee()` toggles
the field as a side effect:

```java
public boolean showProfessionalFee() {
    if (withProfessionalFee == true) { withProfessionalFee = false; }
    else { withProfessionalFee = true; }
    return withProfessionalFee;
}
```

There are 6 occurrences on this page (Original+Duplicate × Final Bill /
Hospital Bill / Final Bill Summary tabs), all bound to the single field
`InwardSearch.withProfessionalFee`. Each render of a checkbox flips the
field again as an uncontrolled side effect, *before* that panel's own bill
preview (which reads the same field) renders below it — producing the
observed inversion between the checkbox's own checked-state (captured
earlier, before its own `onchange` fired) and the total (computed after).

The field's real initial value is `true`, set by
`InwardSearch.refreshFinalBillBackwordReferenceBills()`, wired as the
`actionListener` on the "View Bill" button that navigates here from the
final-bill search pages (`inward_search_final.xhtml`,
`inward_search_final_check.xhtml`, `inward_search_provisional.xhtml`).

### Fix

- Remove `onchange="#{inwardSearch.showProfessionalFee()}"` from all 6
  occurrences in `inward_reprint_bill_final.xhtml`. The existing
  `<p:ajax process="@this" update="...">` on each checkbox already commits
  its own `value` binding through the normal JSF Apply-Request-Values /
  Update-Model lifecycle — nothing else is required to persist a user's
  toggle.
- Delete `InwardSearch.showProfessionalFee()` — confirmed unused anywhere
  else in the codebase (Java or XHTML) after the 6 `onchange` removals.
- `withProfessionalFee` remains a single field shared by all 6 checkboxes
  (explicit choice — simplest fix, matches current design intent). Each
  checkbox's own `<p:ajax update="...">` still targets only its own panel,
  so after a user toggles one checkbox, the other 5 checkboxes' *visual*
  checked state may lag behind the (now-shared) underlying value until they
  are independently touched or the page is reloaded. This is a pre-existing
  characteristic of the one-field design, not a new bug, and is accepted as
  out of scope for this fix.

### Expected behavior after the fix

- Fresh navigation via "View Bill": all 6 checkboxes load **checked**, all
  6 previews show totals **including** the professional fee (field starts
  `true`).
- Toggling any single checkbox off updates only its own panel's preview to
  exclude the professional fee, and that checkbox itself stays visually in
  sync (its own AJAX update targets its own preview).

## Testing plan

- `mvn clean package -DskipTests`, redeploy to local Payara (`rh` domain).
- Playwright, same BHT used in the original demo:
  - **Custom Bills tab**: only "Custom Bill" (Custom2) row renders by
    default; Settings dialog shows 3 new format checkboxes + only Custom2's
    5 preference toggles; enabling "Custom Bill 2"/"Custom Bill 3" in
    Settings makes those rows appear after Apply & Close (full postback).
  - **Final Bill tab**: Original and Duplicate both load checked; both
    totals include the professional fee (4,750.00 in the demo data);
    unchecking Original updates only Original's total to 1,750.00 while
    Duplicate stays checked/4,750.00.
  - Repeat the checkbox check on Hospital Bill and Final Bill Summary tabs.
- No DB schema change; only new `CONFIGOPTION` rows created on first save
  (no migration needed — `configOptionController.setBooleanValueByKey`
  creates them).
